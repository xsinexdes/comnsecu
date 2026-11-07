package com.etheria.consecuencias;

import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;

/** Un tipo de desastre. Se registra una vez; cada vez que se activa se crea un {@link Active}. */
public abstract class Disaster {
    public final String id;
    public final String name;
    public final String category;
    public final Material icon;
    public final int defMinutes;
    public final String desc;
    /** Color de chat del desastre (p. ej. "§c"). */
    public final String color;

    protected Disaster(String id, String name, String category, Material icon, int defMinutes, String color, String desc) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.icon = icon;
        this.defMinutes = defMinutes;
        this.color = color;
        this.desc = desc;
    }

    /** Cada cuántos "latidos" (2 ticks) se llama a tick(). 10 = una vez por segundo. */
    public int every() {
        return 10;
    }

    public boolean worldOk(World w) {
        return true;
    }

    /** Ids de desastres que no pueden estar a la vez que este en el mismo mundo. */
    public List<String> conflicts() {
        return List.of();
    }

    /** Las bendiciones son eventos buenos (no cuentan como desastre ni dan recompensa de superviviente). */
    public boolean blessing() {
        return "Bendición".equals(category);
    }

    /** Frase corta del aviso previo. */
    public String warning() {
        return desc;
    }

    public void start(Active a) {
    }

    public abstract void tick(Active a);

    public void stop(Active a) {
    }
}
