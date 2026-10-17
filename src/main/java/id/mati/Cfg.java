package id.mati;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

/** Konfigurasi (config/dunia-mati.json) dan data tersimpan (config/dunia-mati-data.json). */
public final class Cfg {
    public int tambahan = 15;          // menit per tingkat kematian
    public int maksimal = 1440;        // batas tunggu (menit)
    public int jarak = 3000;           // radius random teleport (blok)
    public String duniaUtama = "minecraft:overworld";
    public boolean debug = false;
    public String eyelid = "overlay";  // overlay (tanpa resource pack) | font (butuh mod di client) | blindness
    public int halus = 3;              // kehalusan kamera (tick, 1-6)
    public int jatuh = 22;             // lama roboh (tick)
    public double cincin = 0.8;
    public double radius = 4.5;        // jarak kamera orbit saat turun
    public int tinggi = 14;            // tinggi maksimal turun dari langit
    public int naik = 80;              // lama melayang naik sebelum blur (tick)
    public int blur = 60;              // lama fase blur/kabut mendadak (tick)
    public int silau = 60;             // lama putih pelan-pelan menuju putih penuh (tick)
    public int turunMax = 400;         // batas lama turun (tick)
    public int tahan = 24;             // lama putih penuh ditahan setelah pindah (tick, min 20)
    public int tanahDiam = 30;         // diam setelah mendarat (tick, maks 40 = 2 detik)
    public int ledakan1 = 30;
    public int ledakan2 = 36;
    public String kurvaMati = "1,1,3;1,0.5,8,h;0.5,1,9;1,1,3;1,0.3,9,h;0.3,1,10;1,1,3;1,0.16,10,h;0.16,1,11;1,1,3;1,0.08,11,h;0.08,0.7,12;0.7,0.7,3;0.7,0,22,h;0,0,14";
    public String kurvaBangun = "0,0,8;0,0.5,22;0.5,0.5,4;0.5,0.18,7;0.18,0.8,10;0.8,0.8,2;0.8,0.4,6;0.4,1,16;1,1,2";

    public static class Data {
        public Map<String, Integer> jumlah = new HashMap<>();
        public Map<String, Long> bangkit = new HashMap<>();   // uuid -> epoch ms
        public Map<String, Long> total = new HashMap<>();     // uuid -> detik
        public Set<String> mati = new HashSet<>();
        public boolean matiSet = false;
        public String matiDim = "minecraft:overworld";
        public double mx, my, mz;
        public float myaw, mpit;
        public int tambahanOverride = 0;
    }

    static Cfg I = new Cfg();
    static Data D = new Data();
    static final Gson G = new GsonBuilder().setPrettyPrinting().create();

    static Path cfgPath() { return FabricLoader.getInstance().getConfigDir().resolve("dunia-mati.json"); }
    static Path dataPath() { return FabricLoader.getInstance().getConfigDir().resolve("dunia-mati-data.json"); }

    static void load() {
        try {
            Path p = cfgPath();
            if (Files.exists(p)) {
                Cfg c = G.fromJson(Files.readString(p), Cfg.class);
                if (c != null) I = c;
            }
            Files.writeString(p, G.toJson(I));
        } catch (Exception e) { U.LOG.warn("config gagal dibaca: {}", e.toString()); }
        try {
            Path p = dataPath();
            if (Files.exists(p)) {
                Data d = G.fromJson(Files.readString(p), Data.class);
                if (d != null) D = d;
            }
        } catch (Exception e) { U.LOG.warn("data gagal dibaca: {}", e.toString()); }
        if (I.tahan < 20) I.tahan = 20;
        if (I.tanahDiam > 40) I.tanahDiam = 40;
        if (I.tanahDiam < 10) I.tanahDiam = 10;
        if (I.halus < 1) I.halus = 1;
        if (I.halus > 6) I.halus = 6;
    }

    static void saveData() {
        try { Files.writeString(dataPath(), G.toJson(D)); }
        catch (Exception e) { U.LOG.warn("data gagal disimpan: {}", e.toString()); }
    }

    static int tambahanNow() { return D.tambahanOverride > 0 ? D.tambahanOverride : I.tambahan; }
}
