package id.mati;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Partikel (command /particle vanilla, relatif ke player). */
final class Fx {
    static void part(UUID u, String nm, double dx, double dy, double dz, double ox, double oy, double oz, double sp, int n) {
        U.cmd("execute as " + u + " at " + u + " run particle minecraft:" + nm + " ~" + U.f(dx) + " ~" + U.f(dy) + " ~" + U.f(dz)
            + " " + U.f(ox) + " " + U.f(oy) + " " + U.f(oz) + " " + U.f(sp) + " " + n + " force");
    }

    static final String BLUE = "dust{color:[0.05,0.1,0.65],scale:1.4}";
    static final String CYAN = "dust{color:[0.45,0.85,1.0],scale:1.4}";
    static final String WHITE = "dust{color:[1.0,1.0,1.0],scale:1.3}";
    static final String PURPLE = "dust{color:[0.65,0.2,0.95],scale:1.4}";

    static void ledakan(UUID u, double s) {
        String[] tp = {"end_rod", BLUE, CYAN, WHITE, "totem_of_undying", PURPLE};
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (String nm : tp) {
            boolean move = nm.equals("end_rod") || nm.equals("totem_of_undying");
            for (int i = 0; i < 5; i++) {
                double rr = r.nextDouble(2.5, 4.5) * s;
                double th = Math.toRadians(r.nextDouble(0, 360));
                double ph = Math.toRadians(r.nextDouble(-20, 65));
                double px = rr * Math.cos(ph) * Math.cos(th);
                double pz = rr * Math.cos(ph) * Math.sin(th);
                double py = 1 + rr * Math.sin(ph);
                if (move) part(u, nm, px, py, pz, 0.6, 0.6, 0.6, 0.12, 7);
                else part(u, nm, px, py, pz, 0.8, 0.8, 0.8, 0, 6);
            }
        }
    }

    /** lingkaran sinar datar di tanah, st = tahap 1..5 */
    static void lingkaran(UUID u, int st) {
        double r = st * Cfg.I.cincin;
        int n = 10 + st * 5;
        for (int i = 1; i <= n; i++) {
            double a = Math.toRadians(i * (360.0 / n));
            double px = r * Math.cos(a), pz = r * Math.sin(a);
            part(u, "end_rod", px, 0.05, pz, 0, 0, 0, 0, 1);
            part(u, "dust{color:[0.45,0.85,1.0],scale:1.0}", px, 0.05, pz, 0, 0, 0, 0, 1);
        }
    }

    static void aura(UUID u) {
        part(u, "end_rod", 0, 1, 0, 3.5, 1.6, 3.5, 0.02, 2);
        part(u, "dust{color:[0.05,0.1,0.65],scale:1.2}", 0, 1, 0, 3.5, 1.6, 3.5, 0, 2);
        part(u, "dust{color:[0.45,0.85,1.0],scale:1.2}", 0, 1, 0, 3.5, 1.6, 3.5, 0, 2);
        part(u, "dust{color:[1.0,1.0,1.0],scale:1.1}", 0, 1, 0, 3.5, 1.6, 3.5, 0, 2);
        part(u, "dust{color:[0.65,0.2,0.95],scale:1.2}", 0, 1, 0, 3.5, 1.6, 3.5, 0, 2);
    }

    /** spiral 3 untai di sekitar tubuh (relatif ke kaki player). dr = 1 naik, -1 turun */
    static void spiral(UUID u, int t, double rad, int dr) {
        for (int i = 1; i <= 3; i++) {
            double a = Math.toRadians(t * 22 + i * 120);
            double hy = ((dr * t * 0.13) + 500 + i * 0.8) % 2.4;
            if (hy < 0) hy += 2.4;
            double rr = rad * (0.7 + 0.3 * Math.sin(Math.toRadians((hy / 2.4) * 180)));
            double px = rr * Math.cos(a), pz = rr * Math.sin(a), py = hy - 0.2;
            part(u, "end_rod", px, py, pz, 0, 0, 0, 0, 1);
            double a2 = a - Math.toRadians(14);
            part(u, "end_rod", rr * Math.cos(a2), py - 0.12 * dr, rr * Math.sin(a2), 0, 0, 0, 0, 1);
            if (i == 1) part(u, "dust{color:[0.45,0.85,1.0],scale:1.0}", px, py, pz, 0, 0, 0, 0, 1);
            else if (i == 2) part(u, "dust{color:[1.0,1.0,1.0],scale:0.9}", px, py, pz, 0, 0, 0, 0, 1);
            else part(u, "dust{color:[0.65,0.2,0.95],scale:1.0}", px, py, pz, 0, 0, 0, 0, 1);
        }
    }

    /** riak cincin kecil saat mendarat (relatif ke player, r = radius) */
    static void cincinMendarat(UUID u, double r) {
        for (int i = 1; i <= 20; i++) {
            double a = Math.toRadians(i * 18);
            part(u, "end_rod", r * Math.cos(a), 0.08, r * Math.sin(a), 0, 0, 0, 0, 1);
        }
    }
}
