package id.mati;

import java.util.UUID;

/** Overlay layar penuh (2 lapis text_display) di depan mata/kamera. Tanpa resource pack. */
final class Ov {
    final UUID owner;
    final UUID[] ids = {UUID.randomUUID(), UUID.randomUUID()};
    boolean open = false;
    int last = 1;

    Ov(UUID owner) { this.owner = owner; }

    void open() {
        if (open) return;
        for (int i = 0; i < 2; i++) {
            int ty = i == 0 ? 4 : -4;
            U.cmd("execute as " + owner + " at " + owner + " anchored eyes positioned ^ ^ ^0.35 run summon text_display ~ ~ ~ {UUID:"
                + U.nbtUuid(ids[i]) + ",Tags:[\"mati_tmp\",\"mati_u_" + owner + "\"],text:\" \",billboard:\"center\",background:0,"
                + "see_through:0b,shadow:0b,brightness:{sky:15,block:15},view_range:3f,teleport_duration:1,"
                + "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f," + ty + "f,0f],scale:[40f,40f,1f]}}");
        }
        open = true;
        last = 1;
    }

    /** rgb 0xRRGGBB, a 0..255 */
    void set(int rgb, int a) {
        if (!open) return;
        a = Math.max(0, Math.min(255, a));
        int argb = (a << 24) | (rgb & 0xFFFFFF);
        if (argb == last) return;
        last = argb;
        for (UUID id : ids) U.cmd("data modify entity " + id + " background set value " + argb);
    }

    /** tempelkan overlay 0,35 blok di depan mata player, atau di depan entity kamera. */
    void follow(UUID anchor, boolean isPlayer) {
        if (!open) return;
        String eyes = isPlayer ? " anchored eyes" : "";
        for (UUID id : ids)
            U.cmd("execute as " + anchor + " at " + anchor + eyes + " positioned ^ ^ ^0.35 run tp " + id + " ~ ~ ~");
    }

    void close() {
        if (!open) return;
        for (UUID id : ids) U.cmd("kill " + id);
        open = false;
    }
}
