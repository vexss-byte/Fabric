package id.mati;

import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

abstract class Anim {
    static final String CH = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKL";
    final UUID uuid;
    int t = 0;
    Ov ov;

    Anim(UUID u) { uuid = u; }

    /** true kalau animasi selesai */
    abstract boolean tick();

    /** dipanggil saat player keluar server / error di tengah animasi */
    abstract void abort(ServerPlayer p);

    /** satu frame kelopak mata: v = keterbukaan 0..1 (+2 = detak jantung) */
    void lid(double v) {
        if (v >= 2) {
            v -= 2;
            U.snd(uuid, "minecraft:entity.warden.heartbeat", 1, 1);
        }
        String m = Cfg.I.eyelid;
        if (m.equals("overlay")) {
            if (ov == null) ov = new Ov(uuid);
            ov.open();
            ov.set(0x000000, (int) Math.round((1 - v) * 255));
            ov.follow(uuid, true);
        } else if (m.equals("font")) {
            int k = (int) Math.round((1 - v) * 48);
            if (k < 1) {
                U.cmd("title " + uuid + " clear");
            } else {
                if (k > 48) k = 48;
                U.cmd("title " + uuid + " times 0 4 0");
                U.cmd("title " + uuid + " title {\"text\":\"" + CH.charAt(k - 1) + "\",\"font\":\"eyelid:lid\",\"color\":\"white\"}");
            }
        } else if (v < 0.6) {
            U.cmd("effect give " + uuid + " minecraft:blindness 1 0 true");
        }
    }

    void closeOv() {
        if (ov != null) ov.close();
        if (Cfg.I.eyelid.equals("font")) U.cmd("title " + uuid + " clear");
    }

    /** bersihkan semua entity sementara milik player ini */
    static void sweep(UUID u) {
        U.cmd("kill @e[tag=mati_u_" + u + "]");
    }

    /** lepas kamera dari entity (kirim paket kamera = player sendiri) */
    static void releaseCamera(ServerPlayer p) {
        try { p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCameraPacket(p)); }
        catch (Throwable e) { if (Cfg.I.debug) U.LOG.warn("lepas kamera gagal: {}", e.toString()); }
    }
}
