package id.mati;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Utilitas: semua efek visual memakai command vanilla supaya tahan perubahan API. */
final class U {
    static MinecraftServer S;
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("dunia-mati");

    static void cmd(String c) {
        if (S == null) return;
        try {
            S.getCommands().performPrefixedCommand(S.createCommandSourceStack().withSuppressedOutput(), c);
        } catch (Throwable e) {
            if (Cfg.I.debug) LOG.warn("cmd gagal: {} -> {}", c, e.toString());
        }
    }

    static ServerPlayer player(UUID id) { return S == null ? null : S.getPlayerList().getPlayer(id); }

    static Entity entity(UUID id) {
        if (S == null) return null;
        for (ServerLevel l : S.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) return e;
        }
        return null;
    }

    static String parseKey(String s) {
        int i = s.indexOf('/');
        String r = i >= 0 ? s.substring(i + 1) : s;
        return r.replace("]", "").trim();
    }

    static String dimId(Level l) { return parseKey(l.dimension().toString()); }

    static ServerLevel level(String id) {
        if (S == null) return null;
        for (ServerLevel l : S.getAllLevels()) if (dimId(l).equals(id)) return l;
        return null;
    }

    static String nbtUuid(UUID u) {
        long m = u.getMostSignificantBits(), l = u.getLeastSignificantBits();
        return "[I;" + (int) (m >> 32) + "," + (int) m + "," + (int) (l >> 32) + "," + (int) l + "]";
    }

    static String f(double d) { return String.format(Locale.ROOT, "%.3f", d); }

    static double smth(double u) {
        if (u <= 0) return 0;
        if (u >= 1) return 1;
        return u * u * (3 - 2 * u);
    }

    static double sm5(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return t * t * t * (t * (6 * t - 15) + 10);
    }

    static double wrap(double a) {
        while (a > 180) a -= 360;
        while (a < -180) a += 360;
        return a;
    }

    /** suara terdengar oleh player (diputar di posisi player). */
    static void snd(UUID u, String sound, double vol, double pitch) {
        cmd("execute at " + u + " run playsound " + sound + " master " + u + " ~ ~ ~ " + f(vol) + " " + f(pitch));
    }

    /** jadwal kurva: dari,ke,jumlahTick[,h] dipisah titik-koma. nilai +2 = bunyi detak jantung. */
    static List<Double> curve(String spec) {
        List<Double> r = new ArrayList<>();
        for (String part : spec.split(";")) {
            String[] q = part.trim().split(",");
            double a = Double.parseDouble(q[0].trim()), b = Double.parseDouble(q[1].trim());
            int n = Integer.parseInt(q[2].trim());
            boolean h = q.length > 3 && q[3].trim().equals("h");
            for (int i = 1; i <= n; i++) {
                double v = a + (b - a) * sm5((double) i / n);
                if (h && i == 1) v += 2;
                r.add(v);
            }
        }
        return r;
    }

    static String fmt(long s) {
        long h = s / 3600, m = (s % 3600) / 60, x = s % 60;
        String ms = (m < 10 ? "0" : "") + m, xs = (x < 10 ? "0" : "") + x;
        return h > 0 ? h + ":" + ms + ":" + xs : ms + ":" + xs;
    }

    static String nm(ServerPlayer p) { return p.getScoreboardName(); }
}
