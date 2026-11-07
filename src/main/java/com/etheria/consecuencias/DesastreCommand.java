package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DesastreCommand implements CommandExecutor, TabCompleter {
    private final ConsecuenciasPlugin pl;

    private final Map<Integer, Object[]> sched = new HashMap<>();
    private int schedId;

    public DesastreCommand(ConsecuenciasPlugin pl) {
        this.pl = pl;
    }

    private void status(CommandSender s) {
        s.sendMessage("§c§l☠ Desastres §7en curso:");
        boolean any = false;
        for (Active x : pl.manager.actives()) {
            any = true;
            s.sendMessage(" " + x.d.color + "§l" + x.d.name + " §7en §f" + x.world.getName() + " §8· " + (x.running ? "§f" + Util.time(x.msLeft()) + " restantes" : "§eempieza pronto") + " §8· " + Util.bars(x.level));
        }
        if (!any) s.sendMessage(" §7Ahora mismo todo está tranquilo.");
        if (s instanceof Player p) {
            int[] st = pl.stats.get(p.getUniqueId());
            s.sendMessage("§7Tú: §a" + st[0] + " desastres superados §8· §c" + st[1] + " muertes §8(/desastre top, /desastre receta)");
        }
    }

    private void help(CommandSender s) {
        s.sendMessage("§c§l☠ Consecuencias §7build " + ConsecuenciasPlugin.BUILD);
        s.sendMessage("§e/desastre §7(menú) §8| §e/desastre lista §8| §e/desastre info <id>");
        s.sendMessage("§e/desastre iniciar <id> [minutos] [nivel 1-3] [mundo] [aviso_seg]");
        s.sendMessage("§e/desastre parar <id|todos> [mundo] §8| §e/desastre aleatorio");
        s.sendMessage("§e/desastre auto <on|off|estado> §8| §e/desastre zonasegura <radio>|lista|borrar <n>");
        s.sendMessage("§e/desastre reparar §8(restaura ya todos los bloques apuntados) §8| §e/desastre recargar");
        s.sendMessage("§e/desastre programar <id> <en_min> [dur] [nivel] [mundo] §8| §e/desastre programados §8| §e/desastre cancelar <n>");
        s.sendMessage("§e/desastre iniciar <id> ... cicatriz §8(el terreno cambiado NO se repara nunca)");
        s.sendMessage("§e/desastre permanente <lista|activar <id> [días]|quitar <id>> §8(maldiciones permanentes)");
        s.sendMessage("§e/desastre maldita <crear <tipo> <radio> [subterranea]|lista|borrar <n>|tp <n>> §8(zonas malditas)");
        s.sendMessage("§e/desastre kit [jugador] §8(equipo anti-desastres) §8| §e/desastre estado|top|receta §8(todos)");
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        boolean admin = s.hasPermission("consecuencias.admin");
        final boolean scar = java.util.Arrays.stream(a).anyMatch(x -> x.equalsIgnoreCase("cicatriz"));
        if (scar) a = java.util.Arrays.stream(a).filter(x -> !x.equalsIgnoreCase("cicatriz")).toArray(String[]::new);
        String sub = a.length == 0 ? (admin && s instanceof Player ? "menu" : "estado") : a[0].toLowerCase(Locale.ROOT);
        if (!admin && !List.of("estado", "top", "receta", "ayuda").contains(sub)) {
            sub = "estado";
        }
        switch (sub) {
            case "estado", "status" -> status(s);
            case "top" -> {
                s.sendMessage("§6🛡 Mejores supervivientes de desastres");
                int i = 1;
                for (var e : pl.stats.top(10)) s.sendMessage(" §e" + (i++) + ". §f" + pl.stats.name(e.getKey()) + " §7- §a" + e.getValue()[0] + " superados §8· §c" + e.getValue()[1] + " muertes");
                if (i == 1) s.sendMessage("§7Todavía nadie ha superado un desastre.");
            }
            case "receta", "recetas" -> {
                s.sendMessage("§6Equipo anti-desastres §7(mesa de crafteo, sin forma):");
                s.sendMessage(" §a§lMáscara antigás §7= casco de cuero + 2 carbón vegetal + lingote de hierro + panel de cristal");
                s.sendMessage(" §b§lAbrigo térmico §7= peto de cuero + 3 lana blanca + carbón");
                s.sendMessage(" §a§lAntídoto §7= botella de cristal + rodaja de melón brillante + ojo de araña + azúcar");
            }
            case "kit" -> {
                Player t = a.length > 1 ? Bukkit.getPlayer(a[1]) : (s instanceof Player p ? p : null);
                if (t == null) {
                    s.sendMessage("§c/desastre kit [jugador]");
                    return true;
                }
                t.getInventory().addItem(Gear.mask(), Gear.coat(), Gear.antidote(), Gear.antidote());
                s.sendMessage("§aEquipo entregado a " + t.getName() + ".");
            }
            case "programar", "schedule" -> {
                Disaster d = a.length > 2 ? pl.manager.get(a[1]) : null;
                if (d == null) {
                    s.sendMessage("§c/desastre programar <id> <en_minutos> [duración] [nivel] [mundo]");
                    return true;
                }
                try {
                    int in = Math.max(0, Integer.parseInt(a[2]));
                    int dur = a.length > 3 ? Math.max(1, Integer.parseInt(a[3])) : pl.manager.minutes(d);
                    int lvl = a.length > 4 ? Integer.parseInt(a[4]) : 1;
                    World w = a.length > 5 ? Bukkit.getWorld(a[5]) : (s instanceof Player p ? p.getWorld() : (pl.manager.worlds().isEmpty() ? null : pl.manager.worlds().get(0)));
                    if (w == null) {
                        s.sendMessage("§cMundo no válido.");
                        return true;
                    }
                    final String by = s.getName();
                    int id = ++schedId;
                    var task = Bukkit.getScheduler().runTaskLater(pl, () -> {
                        sched.remove(id);
                        String err = pl.manager.start(d, w, dur, lvl, pl.getConfig().getInt("aviso-segundos", 15), by + " (programado)", true);
                        if (err != null) pl.getLogger().info("Programado " + d.id + ": " + err);
                    }, in * 1200L);
                    sched.put(id, new Object[]{task, d.name + " en " + w.getName() + " dentro de " + in + " min"});
                    s.sendMessage("§aProgramado #" + id + ": " + d.name + " dentro de " + in + " min. §7(Se pierde si se reinicia el servidor.)");
                } catch (NumberFormatException ex) {
                    s.sendMessage("§cMinutos/duración/nivel deben ser números.");
                }
            }
            case "historial", "history" -> {
                int n = 12;
                try {
                    if (a.length > 1) n = Math.max(1, Math.min(40, Integer.parseInt(a[1])));
                } catch (NumberFormatException ignored) {
                }
                var rows = pl.history.tail(n);
                s.sendMessage("§6Historial de desastres §7(últimos " + rows.size() + ")");
                if (rows.isEmpty()) s.sendMessage("§7Todavía no ha pasado nada.");
                for (String r : rows) s.sendMessage("§8" + r);
            }
            case "programados" -> {
                if (sched.isEmpty()) s.sendMessage("§7No hay desastres programados.");
                for (var e : sched.entrySet()) s.sendMessage(" §e#" + e.getKey() + " §7" + e.getValue()[1]);
            }
            case "cancelar" -> {
                try {
                    var e = sched.remove(Integer.parseInt(a[1]));
                    if (e != null) ((org.bukkit.scheduler.BukkitTask) e[0]).cancel();
                    s.sendMessage(e != null ? "§aProgramación cancelada." : "§7No existe esa programación.");
                } catch (Exception ex) {
                    s.sendMessage("§c/desastre cancelar <n>  (ver /desastre programados)");
                }
            }
            case "menu", "gui" -> {
                if (s instanceof Player p) pl.gui.open(p);
                else s.sendMessage("Solo en el juego.");
            }
            case "lista", "list" -> {
                Map<String, List<Disaster>> by = new java.util.LinkedHashMap<>();
                for (Disaster d : pl.manager.all()) by.computeIfAbsent(d.category, k -> new ArrayList<>()).add(d);
                for (var e : by.entrySet()) {
                    StringBuilder b = new StringBuilder("§6" + e.getKey() + "§7: ");
                    for (Disaster d : e.getValue()) b.append(d.color).append(d.id).append("§7, ");
                    s.sendMessage(b.substring(0, b.length() - 2));
                }
                s.sendMessage("§7Activos ahora: §f" + pl.manager.actives().size());
                for (Active x : pl.manager.actives()) s.sendMessage(" §8- " + x.d.color + x.d.name + " §7en §f" + x.world.getName() + " §7(" + (x.running ? Util.time(x.msLeft()) : "aviso") + ", nivel " + x.level + ")");
            }
            case "info" -> {
                Disaster d = a.length > 1 ? pl.manager.get(a[1]) : null;
                if (d == null) {
                    s.sendMessage("§c/desastre info <id>");
                    return true;
                }
                s.sendMessage(d.color + "§l" + d.name + " §8(" + d.id + ", " + d.category + ")");
                s.sendMessage("§7" + d.desc);
                s.sendMessage("§7Duración por defecto: §f" + pl.manager.minutes(d) + " min §8· §7Peso automático: §f" + pl.manager.weight(d) + " §8· §7Activo: §f" + pl.manager.enabled(d));
            }
            case "iniciar", "start", "probar" -> {
                Disaster d = a.length > 1 ? pl.manager.get(a[1]) : null;
                if (d == null) {
                    s.sendMessage("§c/desastre iniciar <id> [minutos] [nivel] [mundo] [aviso]  (ids: /desastre lista)");
                    return true;
                }
                int min = pl.manager.minutes(d), lvl = 1, warn = sub.equals("probar") ? 3 : pl.getConfig().getInt("aviso-segundos", 15);
                if (sub.equals("probar")) min = 1;
                try {
                    if (a.length > 2) min = Math.max(1, Integer.parseInt(a[2]));
                    if (a.length > 3) lvl = Integer.parseInt(a[3]);
                    if (a.length > 5) warn = Integer.parseInt(a[5]);
                } catch (NumberFormatException ex) {
                    s.sendMessage("§cMinutos/nivel/aviso deben ser números.");
                    return true;
                }
                World w = a.length > 4 ? Bukkit.getWorld(a[4]) : (s instanceof Player p ? p.getWorld() : (pl.manager.worlds().isEmpty() ? null : pl.manager.worlds().get(0)));
                String err = pl.manager.start(d, w, min, lvl, warn, s.getName(), true, scar);
                s.sendMessage(err == null ? "§a" + d.name + " en marcha en " + w.getName() + "." + (scar ? " §c(CICATRIZ: el terreno quedará cambiado para siempre)" : "") : "§c" + err);
            }
            case "parar", "stop", "detener" -> {
                if (a.length < 2) {
                    s.sendMessage("§c/desastre parar <id|todos> [mundo]");
                    return true;
                }
                World w = a.length > 2 ? Bukkit.getWorld(a[2]) : null;
                int n = a[1].equalsIgnoreCase("todos") ? pl.manager.stopAll() : pl.manager.stop(a[1].toLowerCase(Locale.ROOT), w);
                s.sendMessage(n > 0 ? "§aDetenidos: " + n : "§7No había nada que detener con ese nombre.");
            }
            case "aleatorio", "random" -> {
                String err = pl.manager.autoPick();
                s.sendMessage(err == null ? "§aDesastre aleatorio lanzado." : "§c" + err);
            }
            case "auto" -> {
                String v = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "estado";
                if (v.equals("on") || v.equals("off")) {
                    pl.getConfig().set("auto.activo", v.equals("on"));
                    pl.saveConfig();
                    pl.manager.resetAutoTimer();
                }
                boolean on = pl.getConfig().getBoolean("auto.activo", false);
                s.sendMessage("§7Modo automático: " + (on ? "§aACTIVO §7(próximo en " + Util.time(pl.manager.msToAuto()) + ")" : "§cAPAGADO"));
            }
            case "zonasegura", "zona" -> safeZone(s, a);
            case "permanente", "permanentes" -> permanent(s, a);
            case "maldita", "malditas" -> cursed(s, a);
            case "reparar" -> {
                int n = pl.ledger.restoreNow();
                s.sendMessage("§aRestaurados " + n + " bloques. Pendientes: " + pl.ledger.size());
            }
            case "recargar", "reload" -> {
                pl.reloadConfig();
                s.sendMessage("§aConfiguración recargada. (Los disparadores nuevos requieren reiniciar.)");
            }
            default -> help(s);
        }
        return true;
    }

    private void permanent(CommandSender s, String[] a) {
        String v = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "lista";
        if (v.equals("lista")) {
            s.sendMessage("§c§l☠ Consecuencias permanentes §7(solo las activas tú):");
            for (Permanents.Def d : Permanents.DEFS) {
                long t = pl.permanents.left(d.id());
                s.sendMessage(" " + d.color() + d.id() + " §8- §7" + d.name() + " §8· " + (t == 0 ? "§7apagada" : t < 0 ? "§c§lACTIVA para siempre" : "§cACTIVA §7(" + Util.time(t) + ")"));
            }
            s.sendMessage("§7/desastre permanente activar <id> [dias]  §8|  §7/desastre permanente quitar <id>");
            return;
        }
        if (a.length < 3) {
            s.sendMessage("§c/desastre permanente <activar|quitar> <id> [días]  (ids: /desastre permanente lista)");
            return;
        }
        if (v.equals("activar") || v.equals("on")) {
            int days = 0;
            try {
                if (a.length > 3) days = Math.max(0, Integer.parseInt(a[3]));
            } catch (NumberFormatException ex) {
                s.sendMessage("§cLos días deben ser un número.");
                return;
            }
            s.sendMessage(pl.permanents.enable(a[2], days, s.getName()) ? "§aConsecuencia permanente activada." : "§cNo existe o ya estaba activa.");
        } else if (v.equals("quitar") || v.equals("off") || v.equals("levantar")) {
            s.sendMessage(pl.permanents.disable(a[2], s.getName()) ? "§aConsecuencia levantada." : "§7No estaba activa.");
        } else s.sendMessage("§c/desastre permanente <lista|activar|quitar> ...");
    }

    private void cursed(CommandSender s, String[] a) {
        String v = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "lista";
        var zs = pl.zones.list();
        switch (v) {
            case "crear" -> {
                if (!(s instanceof Player p)) {
                    s.sendMessage("Solo en el juego.");
                    return;
                }
                if (a.length < 4 || !CurseZones.TYPES.contains(a[2].toLowerCase(Locale.ROOT))) {
                    s.sendMessage("§c/desastre maldita crear <" + String.join("|", CurseZones.TYPES) + "> <radio> [subterranea]");
                    return;
                }
                try {
                    boolean under = a.length > 4 && a[4].toLowerCase(Locale.ROOT).startsWith("sub");
                    pl.zones.add(p.getLocation(), a[2].toLowerCase(Locale.ROOT), Integer.parseInt(a[3]), under);
                    s.sendMessage("§aZona maldita creada aquí: §f" + a[2] + " §7radio " + a[3] + (under ? " §8(solo bajo tierra)" : "") + ". §7Permanente hasta que la borres.");
                } catch (NumberFormatException ex) {
                    s.sendMessage("§cEl radio debe ser un número.");
                }
            }
            case "borrar" -> {
                try {
                    s.sendMessage(pl.zones.remove(Integer.parseInt(a[2]) - 1) ? "§aZona borrada." : "§cNúmero inválido.");
                } catch (Exception ex) {
                    s.sendMessage("§c/desastre maldita borrar <n>");
                }
            }
            case "tp" -> {
                try {
                    var z = zs.get(Integer.parseInt(a[2]) - 1);
                    World w = Bukkit.getWorld(z.world);
                    if (s instanceof Player p && w != null) p.teleport(new Location(w, z.x + 0.5, z.y, z.z + 0.5));
                } catch (Exception ex) {
                    s.sendMessage("§c/desastre maldita tp <n>");
                }
            }
            default -> {
                if (zs.isEmpty()) s.sendMessage("§7No hay zonas malditas. Crea una: /desastre maldita crear <tipo> <radio> [subterranea]");
                for (int i = 0; i < zs.size(); i++) {
                    var z = zs.get(i);
                    s.sendMessage("§e" + (i + 1) + " §f" + z.type + " §7radio " + z.radius + " §8@ " + z.world + " " + z.x + "," + z.y + "," + z.z + (z.underground ? " §8(subterránea)" : ""));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void safeZone(CommandSender s, String[] a) {
        List<Map<?, ?>> list = new ArrayList<>(pl.getConfig().getMapList("zonas-seguras"));
        String v = a.length > 1 ? a[1].toLowerCase(Locale.ROOT) : "lista";
        if (v.equals("lista")) {
            if (list.isEmpty()) s.sendMessage("§7No hay zonas seguras (el spawn siempre está protegido: zona-spawn-protegida).");
            for (int i = 0; i < list.size(); i++) s.sendMessage("§e" + (i + 1) + " §7" + list.get(i));
            return;
        }
        if (v.equals("borrar") && a.length > 2) {
            try {
                int i = Integer.parseInt(a[2]) - 1;
                list.remove(i);
                pl.getConfig().set("zonas-seguras", list);
                pl.saveConfig();
                s.sendMessage("§aZona borrada.");
            } catch (Exception ex) {
                s.sendMessage("§cNúmero inválido.");
            }
            return;
        }
        if (!(s instanceof Player p)) {
            s.sendMessage("Solo en el juego.");
            return;
        }
        int r;
        try {
            r = Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            s.sendMessage("§c/desastre zonasegura <radio> (en tu posición) | lista | borrar <n>");
            return;
        }
        Location l = p.getLocation();
        Map<String, Object> m = new HashMap<>();
        m.put("mundo", l.getWorld().getName());
        m.put("x", l.getBlockX());
        m.put("z", l.getBlockZ());
        m.put("radio", Math.max(8, r));
        List<Object> out = new ArrayList<>(list);
        out.add(m);
        pl.getConfig().set("zonas-seguras", out);
        pl.saveConfig();
        s.sendMessage("§aZona segura creada aquí (radio " + Math.max(8, r) + "): ningún desastre modificará bloques dentro. Úsala en tus territorios.");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("consecuencias.admin")) {
            if (a.length == 1) for (String x : List.of("estado", "top", "receta")) if (x.startsWith(a[0].toLowerCase())) out.add(x);
            return out;
        }
        if (a.length == 1) {
            for (String x : List.of("menu", "lista", "info", "iniciar", "probar", "parar", "aleatorio", "auto", "zonasegura", "reparar", "recargar", "programar", "programados", "cancelar", "kit", "estado", "top", "receta", "historial", "permanente", "maldita")) if (x.startsWith(a[0].toLowerCase())) out.add(x);
        } else if (a.length == 2 && List.of("iniciar", "probar", "info", "parar", "programar").contains(a[0].toLowerCase())) {
            if (a[0].equalsIgnoreCase("parar")) out.add("todos");
            for (Disaster d : pl.manager.all()) if (d.id.startsWith(a[1].toLowerCase())) out.add(d.id);
        } else if (a.length == 2 && a[0].equalsIgnoreCase("permanente")) out.addAll(List.of("lista", "activar", "quitar"));
        else if (a.length == 3 && a[0].equalsIgnoreCase("permanente")) for (Permanents.Def d : Permanents.DEFS) out.add(d.id());
        else if (a.length == 2 && a[0].equalsIgnoreCase("maldita")) out.addAll(List.of("crear", "lista", "borrar", "tp"));
        else if (a.length == 3 && a[0].equalsIgnoreCase("maldita") && a[1].equalsIgnoreCase("crear")) out.addAll(CurseZones.TYPES);
        else if (a.length == 2 && a[0].equalsIgnoreCase("auto")) out.addAll(List.of("on", "off", "estado"));
        else if (a.length == 5 && a[0].equalsIgnoreCase("iniciar")) for (World w : Bukkit.getWorlds()) out.add(w.getName());
        else if (a.length == 4 && a[0].equalsIgnoreCase("iniciar")) out.addAll(List.of("1", "2", "3"));
        return out;
    }
}
