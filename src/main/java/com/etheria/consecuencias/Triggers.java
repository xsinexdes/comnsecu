package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Disparadores: lanza un desastre cuando OTRO plugin lanza un evento de Bukkit (p. ej. cuando un gremio pierde un asedio).
 * Se configuran en config.yml → disparadores. Funciona por reflexión: no hace falta tener ese plugin al compilar.
 */
public class Triggers {
    private final ConsecuenciasPlugin pl;

    public Triggers(ConsecuenciasPlugin pl) {
        this.pl = pl;
    }

    @SuppressWarnings("unchecked")
    public void load() {
        int n = 0;
        for (Map<?, ?> m : pl.getConfig().getMapList("disparadores")) {
            String cls = String.valueOf(m.get("evento"));
            String cond = m.get("condicion") == null ? "" : String.valueOf(m.get("condicion"));
            String dis = m.get("desastre") == null ? "" : String.valueOf(m.get("desastre"));
            String perm = m.get("permanente") == null ? "" : String.valueOf(m.get("permanente"));
            String quitar = m.get("quitar-permanente") == null ? "" : String.valueOf(m.get("quitar-permanente"));
            int dias = m.get("dias") instanceof Number x ? x.intValue() : 0;
            boolean scar = m.get("cicatriz") != null && Boolean.parseBoolean(String.valueOf(m.get("cicatriz")));
            int min = m.get("minutos") instanceof Number x ? x.intValue() : 0;
            int lvl = m.get("nivel") instanceof Number x ? x.intValue() : 1;
            String mundo = m.get("mundo") == null ? null : String.valueOf(m.get("mundo"));
            try {
                Class<?> c = Class.forName(cls, true, pl.getClass().getClassLoader());
                Class<? extends Event> ec = (Class<? extends Event>) c;
                Disaster d = dis.isEmpty() ? null : pl.manager.get(dis);
                if (!dis.isEmpty() && d == null) {
                    pl.getLogger().warning("Disparador: el desastre '" + dis + "' no existe.");
                    continue;
                }
                if (dis.isEmpty() && perm.isEmpty() && quitar.isEmpty()) {
                    pl.getLogger().warning("Disparador sin 'desastre', 'permanente' ni 'quitar-permanente': " + cls);
                    continue;
                }
                if (!perm.isEmpty() && Permanents.def(perm) == null) {
                    pl.getLogger().warning("Disparador: la permanente '" + perm + "' no existe (ver /desastre permanente lista).");
                    continue;
                }
                Listener l = new Listener() {
                };
                Bukkit.getPluginManager().registerEvent(ec, l, EventPriority.MONITOR, (listener, ev) -> {
                    if (!ec.isInstance(ev)) return;
                    try {
                        if (!cond.isBlank() && !check(ev, cond)) return;
                        if (!perm.isEmpty()) pl.permanents.enable(perm, dias, "disparador");
                        if (!quitar.isEmpty()) pl.permanents.disable(quitar, "disparador");
                        if (d == null) return;
                        World w = mundo != null ? Bukkit.getWorld(mundo) : (pl.manager.worlds().isEmpty() ? null : pl.manager.worlds().get(0));
                        String err = pl.manager.start(d, w, min > 0 ? min : pl.manager.minutes(d), lvl, pl.getConfig().getInt("aviso-segundos", 15), "disparador", true, scar);
                        if (err != null) pl.getLogger().info("Disparador " + dis + ": " + err);
                    } catch (Throwable t) {
                        pl.getLogger().warning("Disparador " + cls + ": " + t);
                    }
                }, pl, false);
                n++;
            } catch (ClassNotFoundException e) {
                pl.getLogger().info("Disparador ignorado (la clase " + cls + " no existe: ¿falta el plugin?).");
            } catch (Throwable t) {
                pl.getLogger().warning("Disparador " + cls + ": " + t);
            }
        }
        if (n > 0) pl.getLogger().info(n + " disparadores registrados.");
    }

    /** "metodo=valor": llama al método sin argumentos del evento y compara el resultado. */
    private boolean check(Event ev, String cond) throws Exception {
        String[] p = cond.split("=", 2);
        if (p.length < 2) return true;
        Method m = ev.getClass().getMethod(p[0].trim());
        Object r = m.invoke(ev);
        return String.valueOf(r).equalsIgnoreCase(p[1].trim());
    }
}
