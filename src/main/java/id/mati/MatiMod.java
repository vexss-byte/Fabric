package id.mati;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MatiMod implements ModInitializer {
    static final Map<UUID, Anim> ACTIVE = new LinkedHashMap<>();
    static int n = 0;

    @Override
    public void onInitialize() {
        Cfg.load();
        ServerLifecycleEvents.SERVER_STARTED.register(s -> { U.S = s; U.cmd("kill @e[tag=mati_tmp]"); });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            for (Anim a : ACTIVE.values()) { ServerPlayer p = U.player(a.uuid); if (p != null) a.abort(p); }
            ACTIVE.clear();
            Cfg.saveData();
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> { U.S = s; tick(); });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer p)) return true;
            String id = p.getUUID().toString();
            if (Cfg.D.mati.contains(id) || ACTIVE.containsKey(p.getUUID())) { p.setHealth(p.getMaxHealth()); return false; }
            if (!Cfg.D.matiSet) {
                p.sendSystemMessage(Component.literal("[Mati] Titik dunia mati belum diset. Ketik /setmati di dunia mati."));
                return true;
            }
            ACTIVE.put(p.getUUID(), new Death(p));
            return false;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            UUID u = p.getUUID();
            Anim.sweep(u);
            if (Cfg.D.mati.contains(u.toString()) && !ACTIVE.containsKey(u)) {
                if (Cfg.D.matiSet)
                    U.cmd("execute in " + Cfg.D.matiDim + " run tp " + u + " " + U.f(Cfg.D.mx) + " " + U.f(Cfg.D.my) + " " + U.f(Cfg.D.mz)
                        + " " + U.f(Cfg.D.myaw) + " " + U.f(Cfg.D.mpit));
                U.cmd("gamemode adventure " + u);
                Anim.releaseCamera(p);
                if (!Cfg.D.bangkit.containsKey(u.toString())) {
                    long sec = (long) Math.min((long) Cfg.D.jumlah.getOrDefault(u.toString(), 1) * Cfg.tambahanNow(), Cfg.I.maksimal) * 60;
                    Cfg.D.bangkit.put(u.toString(), System.currentTimeMillis() + sec * 1000);
                    Cfg.D.total.put(u.toString(), sec);
                    Cfg.saveData();
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer p = handler.getPlayer();
            Anim a = ACTIVE.remove(p.getUUID());
            if (a != null) a.abort(p);
            Timers.bb.remove(p.getUUID());
        });

        CommandRegistrationCallback.EVENT.register((d, reg, env) -> {
            d.register(Commands.literal("setmati").requires(MatiMod::admin).executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                Cfg.D.matiDim = U.dimId(p.level());
                Cfg.D.mx = p.getX(); Cfg.D.my = p.getY(); Cfg.D.mz = p.getZ();
                Cfg.D.myaw = p.getYRot(); Cfg.D.mpit = p.getXRot();
                Cfg.D.matiSet = true;
                Cfg.saveData();
                ok(ctx.getSource(), "Titik dunia mati disimpan!");
                return 1;
            }));
            d.register(Commands.literal("hidupin").requires(MatiMod::admin)
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    if (!Cfg.D.mati.contains(p.getUUID().toString())) { fail(ctx.getSource(), p.getScoreboardName() + " nggak lagi mati."); return 0; }
                    if (ACTIVE.containsKey(p.getUUID())) { fail(ctx.getSource(), "Animasi masih berjalan, coba lagi beberapa detik."); return 0; }
                    startRebirth(p);
                    ok(ctx.getSource(), p.getScoreboardName() + " berhasil dihidupkan.");
                    return 1;
                })));
            d.register(Commands.literal("resetmati").requires(MatiMod::admin)
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    Cfg.D.jumlah.remove(p.getUUID().toString());
                    Cfg.saveData();
                    ok(ctx.getSource(), "Jumlah kematian direset, mati berikutnya kembali ke tingkat 1.");
                    return 1;
                })));
            d.register(Commands.literal("setwaktumati").requires(MatiMod::admin)
                .then(Commands.argument("menit", IntegerArgumentType.integer(1)).executes(ctx -> {
                    Cfg.D.tambahanOverride = IntegerArgumentType.getInteger(ctx, "menit");
                    Cfg.saveData();
                    ok(ctx.getSource(), "Tambahan waktu per kematian sekarang " + Cfg.D.tambahanOverride + " menit.");
                    return 1;
                })));
            d.register(Commands.literal("matikan").requires(MatiMod::admin)
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    U.cmd("damage " + p.getUUID() + " 1000 minecraft:generic");
                    return 1;
                })));
            // tes animasi hidup kembali tanpa harus mati (kamu dianggap mati lalu langsung dihidupkan)
            d.register(Commands.literal("tesrebirth").requires(MatiMod::admin).executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                if (ACTIVE.containsKey(p.getUUID())) { fail(ctx.getSource(), "Animasi masih berjalan."); return 0; }
                Cfg.D.mati.add(p.getUUID().toString());
                startRebirth(p);
                return 1;
            }));
            d.register(Commands.literal("tesledakan").requires(MatiMod::admin).executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                U.snd(p.getUUID(), "minecraft:block.beacon.activate", 1, 1);
                Fx.ledakan(p.getUUID(), 1);
                for (int i = 1; i <= 5; i++) Fx.lingkaran(p.getUUID(), i);
                return 1;
            }));
            d.register(Commands.literal("hapusbadan").requires(MatiMod::admin).executes(ctx -> {
                U.cmd("kill @e[type=minecraft:mannequin]");
                ok(ctx.getSource(), "Mannequin sisa skrip lama dihapus (di area yang termuat).");
                return 1;
            }));
        });
    }

    static void startRebirth(ServerPlayer p) {
        ACTIVE.put(p.getUUID(), new Rebirth(p));
    }

    static void tick() {
        Iterator<Map.Entry<UUID, Anim>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Anim a = it.next().getValue();
            boolean done;
            try { done = a.tick(); }
            catch (Throwable e) {
                U.LOG.error("animasi error, dibersihkan: ", e);
                emergency(a.uuid);
                done = true;
            }
            if (done) it.remove();
        }
        n++;
        if (n % 5 == 0) Timers.aura();
        if (n % 20 == 0) Timers.second();
    }

    /** kalau ada error di tengah animasi: jangan sampai player terjebak */
    static void emergency(UUID u) {
        Anim.sweep(u);
        ServerPlayer p = U.player(u);
        if (p != null) Anim.releaseCamera(p);
        U.cmd("execute as " + u + " run spectate");
        U.cmd("gamemode survival " + u);
        for (String e : new String[]{"slowness", "levitation", "nausea", "blindness"}) U.cmd("effect clear " + u + " minecraft:" + e);
        U.cmd("title " + u + " clear");
        Cfg.D.mati.remove(u.toString());
        Cfg.D.bangkit.remove(u.toString());
        Cfg.saveData();
    }

    static void ok(CommandSourceStack s, String m) { s.sendSuccess(() -> Component.literal(m), false); }
    static void fail(CommandSourceStack s, String m) { s.sendFailure(Component.literal(m)); }

    /** cek op: API permission berubah antar versi, jadi dicoba lewat reflection; cadangan: tag mati_admin */
    @SuppressWarnings("unchecked")
    static boolean admin(CommandSourceStack s) {
        try {
            Method m = s.getClass().getMethod("hasPermission", int.class);
            return (boolean) m.invoke(s, 2);
        } catch (Throwable ignored) { }
        try {
            Field f = Commands.class.getField("LEVEL_GAMEMASTERS");
            Object chk = f.get(null);
            for (Method m : Commands.class.getMethods()) {
                if (m.getName().equals("hasPermission") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(f.getType())) {
                    Object pred = m.invoke(null, chk);
                    return ((Predicate<Object>) pred).test(s);
                }
            }
        } catch (Throwable ignored) { }
        net.minecraft.world.entity.Entity e = s.getEntity();
        return e == null || e.getTags().contains("mati_admin");
    }
}
