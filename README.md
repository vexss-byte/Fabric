# Dunia Mati (mod Fabric, Minecraft 26.3)

Konversi skrip `animasi-mati-revisi21.sk` jadi satu mod. Hanya di SERVER (pemain tidak perlu pasang apa-apa),
tidak butuh resource pack. Aset kelopak mata ikut tertanam di jar, dipakai hanya kalau eyelid = "font".

## Cara membuat .jar
A) GitHub (tanpa install apa pun): buat repo, upload SEMUA isi folder ini, buka tab Actions -> build -> download artifact `dunia-mati-jar`.
B) Lokal: pasang JDK 25 + Gradle 9.x, lalu `gradle build`. Hasil: `build/libs/dunia-mati-1.0.0.jar`.
C) Kalau Gradle/Loom menolak: buat template di https://fabricmc.net/develop/template/ (Minecraft 26.3),
   lalu ganti folder `src/main` template dengan `src/main` dari sini (dan samakan nama plugin Loom di build.gradle).

## Pasang
Taruh jar + Fabric API 0.160.5+26.3 di folder `mods` server. Berdiri di dunia mati, ketik `/setmati`.

## Command (op)
/setmati, /hidupin <player>, /resetmati <player>, /setwaktumati <menit>, /matikan <player>,
/tesrebirth (tes animasi hidup kembali di diri sendiri), /tesledakan, /hapusbadan
Kalau op tidak terdeteksi: `/tag <nama> add mati_admin`.

## Config: config/dunia-mati.json
naik/blur/silau (tick) = melayang, blur mendadak, putih pelan. tanahDiam = diam setelah mendarat (maks 40 tick = 2 detik).
radius = jarak kamera orbit. duniaUtama = dimensi untuk random teleport. eyelid = overlay | font | blindness.
