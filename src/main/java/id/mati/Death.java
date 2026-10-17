package id.mati;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Animasi mati: roboh ke kanan (POV), kedip makin berat, pindah ke dunia mati, bangun. */
final class Death extends Anim {
    final Cfg c = Cfg.I;
    final String dim;
    final double x0, y0, z0;
    final float yaw0, pit0;
    final UUID cam = UUID.randomUUID();
    final long durasiSec;
    final int ke;
    double rx, rz, lat = 0.8;
    List<Double> ol, wl;
    int ci = 0, phase = 0, k = 0;

    Death(ServerPlayer p) {
        super(p.getUUID());
        dim = U.dimId(p.level());
        x0 = p.getX(); y0 = p.getY(); z0 = p.getZ();
        yaw0 = p.getYRot(); pit0 = p.getXRot();
        String id = uuid.toString();
        Cfg.D.mati.add(id);
        ke = Cfg.D.jumlah.merge(id, 1, Integer::sum);
        long menit = Math.min((long) ke * Cfg.tambahanNow(), c.maksimal);
        durasiSec = menit * 60;
        p.setHealth(p.getMaxHealth());
        p.getInventory().dropAll();
        Cfg.saveData();
        U.snd(uuid, "minecraft:entity.player.hurt", 1, 1);
        U.cmd("gamemode spectator " + uuid);
        double yr = Math.toRadians(yaw0);
        rx = -Math.cos(yr);
        rz = -Math.sin(yr);
        ServerLevel lv = U.level(dim);
        if (lv != null) {
            BlockPos b = new BlockPos((int) Math.floor(x0 + rx * 0.8), (int) Math.floor(y0 + 0.5), (int) Math.floor(z0 + rz * 0.8));
            if (!lv.getBlockState(b).isAir()) lat = 0.2;
        }
        ol = U.curve(c.kurvaMati);
        wl = U.curve(c.kurvaBangun);
        U.cmd("execute in " + dim + " run summon minecraft:item_display " + U.f(x0) + " " + U.f(y0 + 1.62) + " " + U.f(z0)
            + " {UUID:" + U.nbtUuid(cam) + ",Tags:[\"mati_tmp\",\"mati_u_" + uuid + "\"],teleport_duration:" + c.halus
            + ",Rotation:[" + yaw0 + "f," + pit0 + "f]}");
    }

    void camto(double px, double py, double pz, double yaw, double pit) {
        U.cmd("execute in " + dim + " run tp " + cam + " " + U.f(px) + " " + U.f(py + 1.62) + " " + U.f(pz) + " " + U.f(yaw) + " " + U.f(pit));
    }

    void holdCam() { camto(x0 + rx * lat, y0 - 1.37, z0 + rz * lat, yaw0 + 55, 24); }

    @Override
    boolean tick() {
        ServerPlayer p = U.player(uuid);
        if (p == null) { sweep(uuid); return true; }
        t++;
        if (phase <= 4) {
            if (t < 400) p.setHealth(p.getMaxHealth());
            if (phase >= 1 && ov != null) ov.follow(uuid, true);
        }
        switch (phase) {
            case 0: // tunggu kamera siap lalu tonton
                if (t == 3) U.cmd("spectate " + cam + " " + uuid);
                if (t >= 3) { phase = 1; k = 0; }
                break;
            case 1: { // jatuh melengkung ke kanan
                k++;
                double u = (double) k / c.jatuh;
                double th = Math.toRadians(90 * Math.pow(u, 2.6));
                double hh = 0.25 + 1.37 * Math.cos(th);
                double lt = lat * Math.sin(th);
                camto(x0 + rx * lt, (y0 - 1.62) + hh, z0 + rz * lt, yaw0 + 55 * Math.pow(u, 3), pit0 + (24 - pit0) * u * u);
                keepSpectate(p);
                if (k >= c.jatuh) {
                    phase = 2; k = 0;
                    U.snd(uuid, "minecraft:entity.player.big_fall", 1, 1);
                }
                break;
            }
            case 2: { // benturan + kedipan dimulai
                k++;
                double tt = k / 12.0, dm = (1 - tt) * (1 - tt);
                double bn = 0.14 * Math.sin(Math.toRadians(tt * 180)) * dm;
                double ry = (Math.random() * 6 - 3) * dm, rp = (Math.random() * 6 - 3) * dm;
                camto(x0 + rx * lat, (y0 - 1.37) + bn, z0 + rz * lat, (yaw0 + 55) + ry, 24 + rp);
                if (ci < ol.size()) lid(ol.get(ci++));
                keepSpectate(p);
                if (k >= 12) phase = 3;
                break;
            }
            case 3: // kedip makin berat sampai tertutup
                if (ci < ol.size()) { holdCam(); lid(ol.get(ci++)); keepSpectate(p); }
                else phase = 4;
                break;
            case 4: // pindah ke dunia mati (layar sedang hitam)
                U.cmd("execute as " + uuid + " run spectate");
                U.cmd("kill " + cam);
                if (!Cfg.D.matiSet) {
                    U.cmd("gamemode survival " + uuid);
                    closeOv();
                    Cfg.D.mati.remove(uuid.toString());
                    Cfg.saveData();
                    return true;
                }
                U.cmd("execute in " + Cfg.D.matiDim + " run tp " + uuid + " " + U.f(Cfg.D.mx) + " " + U.f(Cfg.D.my) + " " + U.f(Cfg.D.mz)
                    + " " + U.f(Cfg.D.myaw) + " " + U.f(Cfg.D.mpit));
                U.cmd("gamemode adventure " + uuid);
                U.cmd("effect give " + uuid + " minecraft:slowness 5 4 true");
                U.snd(uuid, "minecraft:block.beacon.activate", 1, 1);
                U.cmd("tellraw @a [{\"text\":\"" + U.nm(p) + " has entered the \",\"color\":\"white\"},{\"text\":\"Nebula\",\"color\":\"aqua\"},{\"text\":\" world.\",\"color\":\"white\"}]");
                phase = 5; k = 0;
                break;
            case 5: { // bangun di dunia mati
                k++;
                if (k == c.ledakan1) Fx.ledakan(uuid, 1);
                if (k == c.ledakan2) Fx.ledakan(uuid, 1.6);
                int ph = k - c.ledakan1;
                if (ph >= 0 && ph < 15 && ph % 3 == 0) Fx.lingkaran(uuid, ph / 3 + 1);
                if (k <= wl.size()) lid(wl.get(k - 1));
                if (ov != null) ov.follow(uuid, true);
                if (k >= wl.size() + 2) {
                    closeOv();
                    long now = System.currentTimeMillis();
                    Cfg.D.bangkit.put(uuid.toString(), now + durasiSec * 1000);
                    Cfg.D.total.put(uuid.toString(), durasiSec);
                    Cfg.saveData();
                    String sub = "Kematian ke-" + ke + ", tunggu " + U.fmt(durasiSec);
                    U.cmd("title " + uuid + " times 10 60 20");
                    U.cmd("title " + uuid + " subtitle {\"text\":\"" + sub + "\",\"color\":\"aqua\"}");
                    U.cmd("title " + uuid + " title {\"text\":\"KAMU MENGALAMI REBRITH DAN INI\",\"color\":\"aqua\",\"bold\":true}");
                    return true;
                }
                break;
            }
            default: return true;
        }
        return false;
    }

    void keepSpectate(ServerPlayer p) {
        try {
            Entity e = U.entity(cam);
            if (e != null && p.getCamera() != e) U.cmd("spectate " + cam + " " + uuid);
        } catch (Throwable ignored) { }
    }

    @Override
    void abort(ServerPlayer p) {
        U.cmd("execute as " + uuid + " run spectate");
        closeOv();
        sweep(uuid);
        U.cmd("gamemode adventure " + uuid);
        if (Cfg.D.matiSet) {
            U.cmd("execute in " + Cfg.D.matiDim + " run tp " + uuid + " " + U.f(Cfg.D.mx) + " " + U.f(Cfg.D.my) + " " + U.f(Cfg.D.mz)
                + " " + U.f(Cfg.D.myaw) + " " + U.f(Cfg.D.mpit));
            if (!Cfg.D.bangkit.containsKey(uuid.toString())) {
                Cfg.D.bangkit.put(uuid.toString(), System.currentTimeMillis() + durasiSec * 1000);
                Cfg.D.total.put(uuid.toString(), durasiSec);
            }
        } else {
            U.cmd("gamemode survival " + uuid);
            Cfg.D.mati.remove(uuid.toString());
        }
        Cfg.saveData();
    }
}
