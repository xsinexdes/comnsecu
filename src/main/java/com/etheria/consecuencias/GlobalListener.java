package com.etheria.consecuencias;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Eventos de Bukkit que necesitan varios desastres a la vez. */
public class GlobalListener implements Listener {
    private final ConsecuenciasPlugin pl;
    private final Random rnd = new Random();

    public GlobalListener(ConsecuenciasPlugin pl) {
        this.pl = pl;
    }

    private DisasterManager m() {
        return pl.manager;
    }

    // ---- bloques que caen (tornado, derrumbe, erupción): se apunta el hueco donde aterrizan
    @EventHandler(ignoreCancelled = true)
    public void onLand(EntityChangeBlockEvent e) {
        if (!(e.getEntity() instanceof FallingBlock fb)) return;
        String t = Util.tagOf(fb);
        if (t == null) return;
        Block b = e.getBlock();
        Active a = owner(t, b.getWorld());
        if (!pl.ledger.record(b, a)) {
            e.setCancelled(true);
            fb.remove();
            return;
        }
        if (t.startsWith("erupcion:")) {
            e.setCancelled(true);
            fb.remove();
            pl.ledger.set(b, Material.MAGMA_BLOCK, a);
            Block up = b.getRelative(0, 1, 0);
            if (up.getType().isAir()) pl.ledger.set(up, Material.FIRE, a);
        }
    }

    private Active owner(String tag, World w) {
        for (Active a : m().of(w)) if ((a.d.id + ":" + a.id).equals(tag)) return a;
        return null;
    }

    // ---- explosiones de meteoritos
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        String t = Util.tagOf(e.getEntity());
        if (t == null) return;
        Active a = owner(t, e.getEntity().getWorld());
        if (!pl.getConfig().getBoolean("destruccion", true)) {
            e.blockList().clear();
        } else {
            e.blockList().removeIf(b -> !pl.ledger.record(b, a));
        }
        Active ma = a;
        if (ma != null && t.startsWith("meteoritos:")) org.bukkit.Bukkit.getScheduler().runTask(pl, () -> com.etheria.consecuencias.disasters.SkyDisasters.impact(ma, e.getLocation()));
    }

    // ---- fuego: se apunta lo que se quema o se incendia
    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        World w = e.getBlock().getWorld();
        if (m().has(w, "incendio", "erupcion", "lluvia_fuego", "meteoritos")) pl.ledger.record(e.getBlock(), first(w));
        else if (m().has(w, "grisu")) pl.ledger.record(e.getBlock(), first(w));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        World w = e.getBlock().getWorld();
        if (e.getNewState().getType() == Material.FIRE && m().has(w, "incendio", "erupcion", "lluvia_fuego", "meteoritos")) {
            if (!pl.ledger.record(e.getBlock(), first(w))) e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(org.bukkit.event.block.BlockIgniteEvent e) {
        World w = e.getBlock().getWorld();
        if (m().has(w, "incendio", "erupcion", "lluvia_fuego", "meteoritos", "tormenta_electrica")) {
            if (!pl.ledger.record(e.getBlock(), first(w))) e.setCancelled(true);
        }
    }

    private Active first(World w) {
        for (Active a : m().of(w)) if (a.running) return a;
        return null;
    }

    // ---- hambruna / sequía: los cultivos no crecen, el ganado no cría, comer alimenta menos
    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        World w = e.getBlock().getWorld();
        if (m().has(w, "hambruna") || (m().has(w, "sequia") && rnd.nextInt(100) < 70) || m().has(w, "ventisca")) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreed(EntityBreedEvent e) {
        if (m().has(e.getEntity().getWorld(), "hambruna", "peste_ganado")) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!m().has(p.getWorld(), "hambruna")) return;
        int gain = e.getFoodLevel() - p.getFoodLevel();
        if (gain > 1) e.setFoodLevel(p.getFoodLevel() + Math.max(1, gain / 2));
    }

    // ---- eclipse / luna de sangre / invasión: los no-muertos no se queman con el sol
    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (e instanceof org.bukkit.event.entity.EntityCombustByBlockEvent || e instanceof org.bukkit.event.entity.EntityCombustByEntityEvent) return;
        if (Util.tagOf(e.getEntity()) != null) {
            e.setCancelled(true);
            return;
        }
        if (!(e.getEntity() instanceof Monster)) return;
        World w = e.getEntity().getWorld();
        if (m().has(w, "eclipse", "luna_sangre") || Util.tagOf(e.getEntity()) != null) e.setCancelled(true);
    }

    // ---- luna de sangre: monstruos más fuertes
    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Monster mob)) return;
        Active a = m().find("luna_sangre", mob.getWorld());
        if (a == null) return;
        CreatureSpawnEvent.SpawnReason r = e.getSpawnReason();
        if (r != CreatureSpawnEvent.SpawnReason.NATURAL && r != CreatureSpawnEvent.SpawnReason.SPAWNER && r != CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        int inf = PotionEffect.INFINITE_DURATION;
        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, inf, a.level - 1, false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, inf, Math.min(1, a.level - 1), false, false));
        mob.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, inf, a.level > 1 ? 1 : 0, false, false));
        mob.getWorld().spawnParticle(org.bukkit.Particle.DUST, mob.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.3f));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (!(le instanceof Monster)) return;
        Active a = m().find("luna_sangre", le.getWorld());
        if (a != null) {
            e.setDroppedExp(e.getDroppedExp() * 2);
            if (rnd.nextInt(100) < 8 + a.level * 4) {
                var list = pl.getConfig().getStringList("desastres.luna_sangre.botin");
                if (!list.isEmpty()) {
                    Material mat = Material.matchMaterial(list.get(rnd.nextInt(list.size())));
                    if (mat != null) e.getDrops().add(new org.bukkit.inventory.ItemStack(mat, 1 + rnd.nextInt(2)));
                }
            }
        }
        // zombis de la invasión: recompensa
        if (Util.tagged(le, "invasion_zombi") && le instanceof Zombie && rnd.nextInt(100) < 10) {
            var list = pl.getConfig().getStringList("desastres.invasion_zombi.botin");
            if (!list.isEmpty()) {
                Material mat = Material.matchMaterial(list.get(rnd.nextInt(list.size())));
                if (mat != null) e.getDrops().add(new org.bukkit.inventory.ItemStack(mat, 1));
            }
        }
    }

    // ---- antídoto y peste
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        Player p = e.getPlayer();
        if ("antidoto".equals(Gear.idOf(e.getItem()))) {
            for (PotionEffectType t : new PotionEffectType[]{PotionEffectType.POISON, PotionEffectType.NAUSEA, PotionEffectType.WEAKNESS, PotionEffectType.HUNGER, PotionEffectType.WITHER, PotionEffectType.BLINDNESS})
                p.removePotionEffect(t);
            Gear.giveImmunity(p, 180_000L);
            Active pe = m().find("peste", p.getWorld());
            if (pe != null) {
                Set<UUID> inf = pe.get("infectados", new java.util.HashSet<>());
                inf.remove(p.getUniqueId());
            }
            p.sendMessage("§a✔ §7Antídoto: estás curado y protegido del gas y el agua tóxica durante §f3 min§7.");
            p.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0);
            return;
        }
        Active a = m().find("peste", p.getWorld());
        if (a == null) return;
        Material m = e.getItem().getType();
        if (m == Material.MILK_BUCKET || m == Material.GOLDEN_APPLE || m == Material.ENCHANTED_GOLDEN_APPLE) {
            Set<UUID> inf = a.get("infectados", new java.util.HashSet<>());
            if (inf.remove(p.getUniqueId())) {
                p.sendMessage("§a✔ §7Te has curado de la peste. Procura no acercarte a los enfermos.");
                a.data.put("inmunes", a.get("inmunes", new java.util.HashMap<UUID, Long>()));
                @SuppressWarnings("unchecked")
                java.util.Map<UUID, Long> im = (java.util.Map<UUID, Long>) a.data.get("inmunes");
                im.put(p.getUniqueId(), System.currentTimeMillis() + 120_000L);
            }
        }
    }

    // ---- muertes durante desastres: se apuntan y se mencionan
    @EventHandler
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        Player p = e.getEntity();
        StringBuilder names = new StringBuilder();
        for (Active a : m().of(p.getWorld())) {
            if (!a.running || a.d.blessing()) continue;
            a.died.add(p.getUniqueId());
            if (names.length() > 0) names.append(", ");
            names.append(a.d.name);
        }
        if (names.length() == 0) return;
        pl.stats.died(p.getUniqueId(), p.getName());
        String msg = e.getDeathMessage();
        if (msg != null) e.setDeathMessage(msg + " §8(durante: " + names + ")");
    }

    // ---- tormenta magnética: nada de perlas ni frutas del coro
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(org.bukkit.event.player.PlayerTeleportEvent e) {
        var c = e.getCause();
        if (c != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL && c != org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) return;
        if (m().has(e.getPlayer().getWorld(), "tormenta_magnetica")) {
            e.setCancelled(true);
            Util.actionbar(e.getPlayer(), "§e⚡ El campo magnético bloquea el teletransporte");
        }
    }
}
