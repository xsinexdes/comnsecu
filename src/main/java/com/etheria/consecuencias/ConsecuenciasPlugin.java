package com.etheria.consecuencias;

import com.etheria.consecuencias.disasters.BlessingDisasters;
import com.etheria.consecuencias.disasters.EarthDisasters;
import com.etheria.consecuencias.disasters.LifeDisasters;
import com.etheria.consecuencias.disasters.SkyDisasters;
import com.etheria.consecuencias.disasters.WeatherDisasters;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ConsecuenciasPlugin extends JavaPlugin {
    public static final String BUILD = "2026-11-07-c";
    private static ConsecuenciasPlugin inst;
    public DisasterManager manager;
    public Ledger ledger;
    public DisasterGui gui;
    public Stats stats;
    public History history;
    public Permanents permanents;
    public CurseZones zones;

    public static ConsecuenciasPlugin get() {
        return inst;
    }

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        getDataFolder().mkdirs();
        ledger = new Ledger(this);
        manager = new DisasterManager(this);
        WeatherDisasters.register(manager);
        EarthDisasters.register(manager);
        SkyDisasters.register(manager);
        LifeDisasters.register(manager);
        BlessingDisasters.register(manager);
        stats = new Stats(this);
        history = new History(this);
        permanents = new Permanents(this);
        Bukkit.getPluginManager().registerEvents(permanents, this);
        zones = new CurseZones(this);
        Gear.registerRecipes(this);
        gui = new DisasterGui(this);
        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new GlobalListener(this), this);
        DesastreCommand cmd = new DesastreCommand(this);
        var pc = getCommand("desastre");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }
        manager.startLoop();
        // arranque diferido: los mundos ya están cargados
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (ledger.size() > 0) getLogger().info("Reparando " + ledger.size() + " bloques de la sesión anterior...");
            new Triggers(this).load();
        }, 40L);
        getLogger().info("Consecuencias " + BUILD + " activado: " + manager.all().size() + " desastres.");
    }

    @Override
    public void onDisable() {
        Gear.unregisterRecipes();
        if (manager != null) manager.shutdown();
        if (ledger != null) {
            int n = ledger.restoreNow();
            if (n > 0) getLogger().info("Restaurados " + n + " bloques al apagar.");
        }
    }
}
