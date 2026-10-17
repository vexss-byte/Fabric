package id.mati;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Countdown boss bar + pemicu hidup kembali + aura di dunia mati. */
final class Timers {
    static final Set<UUID> bb = new HashSet<>();

    static void aura() {
        for (String s : Cfg.D.mati) {
            UUID u = UUID.fromString(s);
            if (MatiMod.ACTIVE.containsKey(u) || U.player(u) == null) continue;
            Fx.aura(u);
        }
    }

    static void second() {
        long now = System.currentTimeMillis();
        for (String s : new HashSet<>(Cfg.D.mati)) {
            UUID u = UUID.fromString(s);
            ServerPlayer p = U.player(u);
            if (p == null || MatiMod.ACTIVE.containsKey(u)) continue;
            Long b = Cfg.D.bangkit.get(s);
            if (b == null) continue;
            if (now >= b) { MatiMod.startRebirth(p); continue; }
            // kebal selama di dunia mati
            U.cmd("effect give " + u + " minecraft:resistance 3 4 true");
            long rem = (long) Math.ceil((b - now) / 1000.0);
            update(u, rem, Cfg.D.total.getOrDefault(s, rem));
        }
    }

    static void update(UUID u, long rem, long total) {
        String id = "mati:" + u;
        String txt = U.fmt(rem);
        if (!bb.contains(u)) {
            bb.add(u);
            U.cmd("bossbar add " + id + " {\"text\":\"" + txt + "\"}");
            U.cmd("bossbar set " + id + " color blue");
            U.cmd("bossbar set " + id + " max " + Math.max(1, total));
            U.cmd("bossbar set " + id + " players " + u);
        }
        U.cmd("bossbar set " + id + " name {\"text\":\"Rebrith dalam " + txt + "\",\"color\":\"aqua\"}");
        U.cmd("bossbar set " + id + " value " + rem);
    }
}
