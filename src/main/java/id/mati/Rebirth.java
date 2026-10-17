package id.mati;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Hidup kembali: player ASLI melayang naik -> blur mendadak -> putih pelan sampai penuh -> pindah dunia
 * -> turun dari langit, kamera memutari player 360 derajat -> mendarat (diam maks 2 detik).
 * Tidak ada mannequin / clone.
 */
final class Rebirth extends Anim {
    final Cfg c = Cfg.I;
    String tDim = "minecraft:overworld";
    double gx, gy, gz;
    int hh, dur, n1, hold;
    final float yaw0;
    final UUID cam = UUID.randomUUID();
    boolean camMade = false, camOn = false, ovClosed = false;
    double prevYaw = Double.NaN;
    String ket = "Kamu Telah Mengalami Rebirth";

    Rebirth(ServerPlayer p) {
        super(p.getUUID());
        yaw0 = p.getYRot();
        pickTarget(p);
        n1 = c.naik + c.blur + c.silau;
        hold = c.tanahDiam;
        int d = hh <= 0 ? 40 : (int) Math.ceil(hh / 0.05);
        dur = Math.max(40, Math.min(c.turunMax, d));
        Cfg.D.bangkit.remove(uuid.toString());
        Cfg.D.total.remove(uuid.toString());
        U.cmd("bossbar remove mati:" + uuid);
        Timers.bb.remove(uuid);
    }

    /** titik muncul: spawn point (bed/anchor) kalau ada, kalau tidak random di dunia utama */
    void pickTarget(ServerPlayer p) {
        ServerLevel lv = null;
        boolean ok = false;
        try {
            Object cfg = p.getClass().getMethod("getRespawnConfig").invoke(p);
            if (cfg != null) {
                Object data = cfg.getClass().getMethod("respawnData").invoke(cfg);
                BlockPos pos = (BlockPos) data.getClass().getMethod("pos").invoke(data);
                String d = U.parseKey(data.getClass().getMethod("dimension").invoke(data).toString());
                ServerLevel l = U.level(d);
                if (l != null && !l.getBlockState(pos).isAir()) {
                    boolean bed = l.getBlockState(pos).getBlock().toString().contains("bed");
                    lv = l; tDim = d;
                    gx = pos.getX() + 0.5; gz = pos.getZ() + 0.5; gy = pos.getY() + (bed ? 0.6 : 1.0);
                    ket = "Kamu Telah Mengalami Rebrith";
                    ok = true;
                }
            }
        } catch (Throwable ignored) { }
        if (!ok) {
            lv = U.level(c.duniaUtama);
            if (lv == null) lv = U.S.overworld();
            tDim = U.dimId(lv);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 12 && !ok; i++) {
                int x = r.nextInt(-c.jarak, c.jarak + 1), z = r.nextInt(-c.jarak, c.jarak + 1);
                int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (!lv.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty()) continue;
                gx = x + 0.5; gy = y; gz = z + 0.5; ok = true;
            }
            if (!ok) {
                int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
                gx = 0.5; gy = y; gz = 0.5;
            }
        }
        int bx = (int) Math.floor(gx), bz = (int) Math.floor(gz), by = (int) Math.floor(gy);
        int free = 0;
        for (int i = 1; i <= c.tinggi; i++) {
            if (lv.getBlockState(new BlockPos(bx, by + i, bz)).isAir()) free++; else break;
        }
        hh = Math.max(0, free - 2);
    }

    void tpPlayer(double y) {
        U.cmd("execute in " + tDim + " run tp " + uuid + " " + U.f(gx) + " " + U.f(y) + " " + U.f(gz) + " " + U.f(yaw0) + " 10");
    }

    void whiteFollow(boolean toCam) {
        if (ov == null) return;
        if (toCam && camOn) ov.follow(cam, false); else ov.follow(uuid, true);
    }

    /** posisi kamera orbit pada tick ke-j (0 = awal). Selesai tepat di mata player, menghadap arah player. */
    void orbit(int j) {
        double T = dur + hold;
        double tau = Math.min(1.0, j / T);
        double e = tau * tau * (3 - 2 * tau);
        double yr = Math.toRadians(yaw0);
        double thEnd = Math.atan2(-Math.cos(yr), Math.sin(yr));
        double th = thEnd - 2 * Math.PI * (1 - e);
        double fin = U.smth((j - (T - 12)) / 12.0);
        double R = c.radius * (1 - fin);
        double py = gy + hh * (1 - Math.min(j, dur) / (double) dur);
        double cx = gx + R * Math.cos(th), cz = gz + R * Math.sin(th);
        double cy = py + 1.62 + 0.4 * (1 - fin);
        double dx = gx - cx, dz = gz - cz, dy = (py + 1.0) - cy;
        double lookYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double lookPit = -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        double yaw = lookYaw + U.wrap(yaw0 - lookYaw) * fin;
        double pit = lookPit + (10 - lookPit) * fin;
        if (!Double.isNaN(prevYaw)) {
            while (yaw - prevYaw > 180) yaw -= 360;
            while (yaw - prevYaw < -180) yaw += 360;
        }
        prevYaw = yaw;
        U.cmd("execute in " + tDim + " run tp " + cam + " " + U.f(cx) + " " + U.f(cy) + " " + U.f(cz) + " " + U.f(yaw) + " " + U.f(pit));
    }

    void sendCam(ServerPlayer p) {
        try {
            Entity e = U.entity(cam);
            if (e != null) p.connection.send(new ClientboundSetCameraPacket(e));
        } catch (Throwable ex) { if (c.debug) U.LOG.warn("kirim kamera gagal: {}", ex.toString()); }
    }

    @Override
    boolean tick() {
        ServerPlayer p = U.player(uuid);
        if (p == null) { sweep(uuid); return true; }
        t++;
        if (t <= n1) { up(p, t); return false; }
        if (t <= n1 + c.tahan) { hold(p, t - n1); return false; }
        return desc(p, t - n1 - c.tahan);
    }

    // ---------- FASE 1: melayang naik, blur mendadak, putih pelan ----------
    void up(ServerPlayer p, int t) {
        if (t == 1) {
            U.cmd("gamemode adventure " + uuid);
            U.cmd("effect give " + uuid + " minecraft:slowness 120 7 true");
            U.cmd("effect give " + uuid + " minecraft:resistance 120 4 true");
            U.cmd("effect give " + uuid + " minecraft:levitation 30 0 true");
            U.snd(uuid, "minecraft:block.beacon.power_select", 1, 1);
            ov = new Ov(uuid);
            ov.open();
        }
        Fx.spiral(uuid, t, 0.9 + 0.5 * U.smth(t / (double) n1), 1);
        if (t == c.naik) U.snd(uuid, "minecraft:item.trident.riptide_3", 1, 1);
        if (t >= c.naik) {
            int a;
            int k = t - c.naik;
            if (k < 2) a = 75;                                   // blur muncul MENDADAK
            else if (k <= c.blur) a = 150 + (int) (14 * Math.sin(k * 0.5));
            else a = (int) Math.round(150 + 105 * U.sm5((k - c.blur) / (double) c.silau)); // putih pelan -> penuh
            ov.set(0xFFFFFF, a);
        }
        ov.follow(uuid, true);
        if (t == n1) {
            ov.set(0xFFFFFF, 255);
            U.cmd("effect clear " + uuid + " minecraft:levitation");
            tpPlayer(gy + hh);
            ov.follow(uuid, true);
        }
    }

    // ---------- FASE 2: layar putih penuh, tahan sebentar (chunk termuat), siapkan kamera ----------
    void hold(ServerPlayer p, int k) {
        tpPlayer(gy + hh);
        ov.set(0xFFFFFF, 255);
        if (k == 1) U.snd(uuid, "minecraft:block.beacon.activate", 1, 1);
        if (k == 2 && !camMade) {
            camMade = true;
            U.cmd("execute in " + tDim + " run summon minecraft:item_display " + U.f(gx) + " " + U.f(gy + hh + 2) + " " + U.f(gz)
                + " {UUID:" + U.nbtUuid(cam) + ",Tags:[\"mati_tmp\",\"mati_u_" + uuid + "\"],teleport_duration:" + c.halus + "}");
        }
        if (camMade) orbit(0);
        if (k == 8 || k == 14) sendCam(p);
        if (k >= 10) camOn = true;
        whiteFollow(true);
    }

    // ---------- FASE 3: turun pelan, kamera orbit 360 derajat, mendarat ----------
    boolean desc(ServerPlayer p, int j) {
        double u = Math.min(j, dur) / (double) dur;
        tpPlayer(gy + hh * (1 - u));
        orbit(j);
        if (j % 10 == 0 && j <= 40) sendCam(p);
        if (j <= 80) ov.set(0xFFFFFF, (int) Math.round(255 * (1 - U.smth(j / 80.0))));
        else if (!ovClosed) { ov.close(); ovClosed = true; }
        if (!ovClosed) whiteFollow(true);
        if (j <= dur + 20) Fx.spiral(uuid, j, 1.1 * (1 - 0.7 * U.smth((j - (dur - 30)) / 30.0)), -1);
        if (j == dur) U.snd(uuid, "minecraft:block.amethyst_block.resonate", 1, 1);
        if (j > dur && j <= dur + 12) Fx.cincinMendarat(uuid, (j - dur) * 0.3);
        if (j >= dur + hold) { finish(p); return true; }
        return false;
    }

    void finish(ServerPlayer p) {
        releaseCamera(p);
        closeOv();
        sweep(uuid);
        restore(p);
        U.cmd("effect give " + uuid + " minecraft:resistance 5 4 true");      // resistance 5 detik saja
        Fx.ledakan(uuid, 0.8);
        Fx.part(uuid, "totem_of_undying", 0, 1, 0, 0.5, 1, 0.5, 0.3, 40);
        U.snd(uuid, "minecraft:item.totem.use", 1, 1);
        U.cmd("tellraw @a {\"text\":\"" + U.nm(p) + " has reborn.\",\"color\":\"white\"}");
        U.cmd("title " + uuid + " times 10 60 20");
        U.cmd("title " + uuid + " subtitle {\"text\":\"" + ket + "\",\"color\":\"aqua\"}");
        U.cmd("title " + uuid + " title {\"text\":\"HIDUP KEMBALI\",\"color\":\"green\",\"bold\":true}");
    }

    /** status akhir hidup kembali */
    void restore(ServerPlayer p) {
        U.cmd("gamemode survival " + uuid);
        p.setHealth(p.getMaxHealth());
        U.cmd("effect give " + uuid + " minecraft:saturation 1 20 true");
        String id = uuid.toString();
        Cfg.D.mati.remove(id);
        Cfg.D.bangkit.remove(id);
        Cfg.D.total.remove(id);
        Cfg.saveData();
        Timers.bb.remove(uuid);
        U.cmd("bossbar remove mati:" + uuid);
        for (String e : new String[]{"slowness", "levitation", "nausea", "resistance", "blindness"})
            U.cmd("effect clear " + uuid + " minecraft:" + e);
    }

    @Override
    void abort(ServerPlayer p) {
        releaseCamera(p);
        closeOv();
        sweep(uuid);
        U.cmd("execute in " + tDim + " run tp " + uuid + " " + U.f(gx) + " " + U.f(gy) + " " + U.f(gz) + " " + U.f(yaw0) + " 10");
        restore(p);
    }
}
