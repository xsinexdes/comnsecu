package com.etheria.consecuencias;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Anuncios grandes: chat, título y sonido. */
public final class Announcer {
    private Announcer() {
    }

    private static String center(String s) {
        int len = Util.strip(s).length();
        int pad = Math.max(0, (52 - len) / 2);
        return " ".repeat(pad) + s;
    }

    private static List<String> banner(String color, String title, List<String> body, boolean blessing) {
        String line = "§8§m" + " ".repeat(54);
        List<String> out = new ArrayList<>();
        out.add(line);
        out.add("");
        out.add(center(color + "§l" + (blessing ? "✦  " : "☠  ") + title + (blessing ? "  ✦" : "  ☠")));
        out.add("");
        for (String b : body) out.add(center(b));
        out.add("");
        out.add(line);
        return out;
    }

    private static Sound soundFor(String cat) {
        return switch (cat) {
            case "Bendición" -> Sound.ENTITY_PLAYER_LEVELUP;
            case "Cielo" -> Sound.ENTITY_ENDER_DRAGON_GROWL;
            case "Tierra" -> Sound.ENTITY_WITHER_SPAWN;
            case "Vida" -> Sound.ENTITY_ZOMBIE_VILLAGER_CURE;
            default -> Sound.EVENT_RAID_HORN;
        };
    }

    /** Aviso previo: el desastre empieza en unos segundos. */
    public static void warn(ConsecuenciasPlugin pl, Active a, int seconds) {
        var cfg = pl.getConfig();
        List<String> body = List.of("§7" + a.d.warning(), "§eEmpieza en §f" + seconds + " s §8· §7Intensidad " + Util.bars(a.level), "§7Duración: §f" + a.minutes + " min");
        for (Player p : a.world.getPlayers()) {
            if (cfg.getBoolean("anuncios.chat", true)) for (String l : banner(a.d.color, a.d.name.toUpperCase(), body, a.d.blessing())) p.sendMessage(l);
            if (cfg.getBoolean("anuncios.titulo", true)) p.sendTitle(a.d.color + "§l" + (a.d.blessing() ? "✦ " : "⚠ ") + a.d.name.toUpperCase(), "§7Empieza en " + seconds + " s", 10, 70, 20);
            if (cfg.getBoolean("anuncios.sonido", true)) p.playSound(p.getLocation(), soundFor(a.d.category), 1f, 0.8f);
        }
        Webhook.send(pl, (a.d.blessing() ? "✦ **" : "⚠ **") + a.d.name + "** comienza en " + seconds + " s en el mundo " + a.world.getName() + " (intensidad " + a.level + ", " + a.minutes + " min). " + a.d.warning());
    }

    /** El desastre empieza de verdad. */
    public static void begin(ConsecuenciasPlugin pl, Active a) {
        var cfg = pl.getConfig();
        for (Player p : a.world.getPlayers()) {
            if (cfg.getBoolean("anuncios.titulo", true)) p.sendTitle(a.d.color + "§l" + a.d.name.toUpperCase(), "§f¡Ya ha comenzado!", 5, 50, 15);
            if (cfg.getBoolean("anuncios.sonido", true)) p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.7f);
        }
    }

    public static void end(ConsecuenciasPlugin pl, Active a, boolean natural) {
        var cfg = pl.getConfig();
        if (!cfg.getBoolean("anuncios.final", true)) return;
        for (Player p : a.world.getPlayers()) {
            p.sendMessage("§a✔ §7" + (natural ? "Ha terminado: " : "Se ha detenido: ") + a.d.color + a.d.name + "§7. " + (cfg.getBoolean("regenerar", true) ? "El terreno se irá reparando solo." : ""));
            if (cfg.getBoolean("anuncios.titulo", true)) p.sendTitle("§a§l✔ " + a.d.name, "§7Ha terminado", 5, 40, 15);
            if (cfg.getBoolean("anuncios.sonido", true)) p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1.2f);
        }
        Webhook.send(pl, "✔ **" + a.d.name + "** ha terminado en el mundo " + a.world.getName() + ".");
    }

    /** Mensaje corto a todos los jugadores del mundo (impactos, olas, etc.). */
    public static void news(Active a, String msg) {
        for (Player p : a.world.getPlayers()) p.sendMessage(a.d.color + "§l» §f" + msg);
    }
}
