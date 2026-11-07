package com.etheria.consecuencias;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Menú de administración: pulsa un desastre para activarlo o detenerlo. */
public class DisasterGui implements Listener {
    private static final int[] DURATIONS = {0, 1, 3, 5, 10, 15, 30, 60};
    private static final String[] CATS = {"Todos", "Clima", "Tierra", "Cielo", "Vida", "Bendición", "Permanentes"};

    private static final class State {
        int page;
        String world;
        int dur;     // índice en DURATIONS (0 = la de cada desastre)
        int level = 1;
        int cat;
    }

    private static final class Holder implements InventoryHolder {
        Inventory inv;
        State st;
        List<Disaster> shown = new ArrayList<>();

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final ConsecuenciasPlugin pl;
    private final Map<UUID, State> states = new HashMap<>();

    public DisasterGui(ConsecuenciasPlugin pl) {
        this.pl = pl;
    }

    public void open(Player p) {
        State st = states.computeIfAbsent(p.getUniqueId(), k -> new State());
        if (st.world == null || Bukkit.getWorld(st.world) == null) st.world = p.getWorld().getName();
        Holder h = new Holder();
        h.st = st;
        h.inv = Bukkit.createInventory(h, 54, "§8☠ §cDesastres §8· §7" + pl.manager.actives().size() + " activos");
        draw(h);
        p.openInventory(h.inv);
    }

    private ItemStack item(Material m, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (glow) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        it.setItemMeta(meta);
        return it;
    }

    private void drawPerm(Inventory inv) {
        int i = 0;
        for (Permanents.Def d : Permanents.DEFS) {
            long t = pl.permanents.left(d.id());
            List<String> lore = new ArrayList<>();
            lore.add("§8Consecuencia PERMANENTE");
            lore.add("§7" + d.desc());
            lore.add("");
            if (t == 0) {
                lore.add("§7Estado: §8apagada");
                lore.add("§cClic: §7activar PARA SIEMPRE");
                lore.add("§eClic derecho: §7activar durante 7 días");
            } else {
                lore.add(t < 0 ? "§c§lACTIVA para siempre" : "§cACTIVA §7· quedan §f" + Util.time(t));
                lore.add("§aClic: §7levantar la maldición");
            }
            inv.setItem(i++, item(d.icon(), d.color() + "§l" + d.name(), lore, t != 0));
        }
        inv.setItem(44, item(Material.BOOK, "§6Más permanentes", List.of("§7Zonas malditas (veneno en cuevas, fuego, hielo...):", "§f/desastre maldita crear <tipo> <radio> [subterranea]", "§7Cicatrices: §f/desastre iniciar <id> cicatriz", "§7o Mayús+clic en un desastre."), false));
    }

    private void draw(Holder h) {
        State st = h.st;
        Inventory inv = h.inv;
        inv.clear();
        World w = Bukkit.getWorld(st.world);
        h.shown.clear();
        final boolean perm = CATS[st.cat].equals("Permanentes");
        if (!perm) for (Disaster d : pl.manager.all()) {
            if (st.cat > 0 && !d.category.equals(CATS[st.cat])) continue;
            h.shown.add(d);
        }
        int pages = Math.max(1, (h.shown.size() + 44) / 45);
        if (st.page >= pages) st.page = pages - 1;
        if (perm) drawPerm(inv);
        else for (int i = 0; i < 45; i++) {
            int idx = st.page * 45 + i;
            if (idx >= h.shown.size()) break;
            Disaster d = h.shown.get(idx);
            Active a = w == null ? null : pl.manager.find(d.id, w);
            boolean warming = false;
            if (a == null && w != null) for (Active x : pl.manager.of(w)) if (x.d == d) {
                a = x;
                warming = true;
            }
            List<String> lore = new ArrayList<>();
            lore.add("§8" + d.category);
            lore.add("§7" + d.desc);
            lore.add("");
            int mins = DURATIONS[st.dur] == 0 ? pl.manager.minutes(d) : DURATIONS[st.dur];
            if (a != null) {
                lore.add(warming ? "§e⚠ Anunciado, a punto de empezar" : "§a● ACTIVO §7· quedan §f" + Util.time(a.msLeft()));
                lore.add("§7Intensidad: " + Util.bars(a.level));
                lore.add("");
                lore.add("§cClic: §7detener");
            } else {
                lore.add("§7Duración: §f" + mins + " min §8· §7Intensidad: " + Util.bars(st.level));
                lore.add("§7Aparece solo: " + (pl.manager.enabled(d) ? "§a✔" : "§c✘") + " §8(peso " + pl.manager.weight(d) + ")");
                lore.add("");
                lore.add("§aClic: §7activar en §f" + st.world);
                lore.add("§eClic derecho: §7permitir/vetar en modo automático");
                lore.add("§4Mayús+clic: §7lanzar como §cCICATRIZ §7(terreno cambiado para siempre)");
            }
            inv.setItem(i, item(d.icon, d.color + "§l" + d.name, lore, a != null));
        }
        // barra inferior
        inv.setItem(45, item(Material.ARROW, "§7◀ Anterior", List.of("§8Página " + (st.page + 1) + "/" + pages), false));
        inv.setItem(46, item(Material.BARRIER, "§c§lDetener TODOS", List.of("§7Detiene todos los desastres", "§7de este mundo y repara el terreno."), false));
        inv.setItem(47, item(Material.ENDER_EYE, "§d§lDesastre aleatorio", List.of("§7Lanza uno al azar (según pesos)."), false));
        boolean auto = pl.getConfig().getBoolean("auto.activo", false);
        inv.setItem(48, item(auto ? Material.CLOCK : Material.GRAY_DYE, "§eModo automático: " + (auto ? "§aACTIVO" : "§cAPAGADO"),
                List.of("§7Cada " + pl.getConfig().getInt("auto.minimo-minutos", 45) + "–" + pl.getConfig().getInt("auto.maximo-minutos", 120) + " min lanza un desastre solo.", auto ? "§7Próximo en: §f" + Util.time(pl.manager.msToAuto()) : "", "§eClic: activar/apagar"), auto));
        inv.setItem(49, item(Material.GRASS_BLOCK, "§bMundo: §f" + st.world, List.of("§7Dónde se activan los desastres.", "§eClic: cambiar"), false));
        inv.setItem(50, item(Material.CLOCK, "§bDuración: §f" + (DURATIONS[st.dur] == 0 ? "la de cada uno" : DURATIONS[st.dur] + " min"), List.of("§eClic: cambiar"), false));
        inv.setItem(51, item(Material.REDSTONE, "§bIntensidad: " + Util.bars(st.level), List.of("§7Más intensidad = más fuerte y más cosas.", "§eClic: cambiar"), false));
        inv.setItem(52, item(Material.COMPASS, "§bCategoría: §f" + CATS[st.cat], List.of("§eClic: cambiar"), false));
        inv.setItem(53, item(Material.ARROW, "§7Siguiente ▶", List.of("§8Página " + (st.page + 1) + "/" + pages), false));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != h.inv) return;
        if (!p.hasPermission("consecuencias.admin")) {
            p.closeInventory();
            return;
        }
        State st = h.st;
        int slot = e.getRawSlot();
        World w = Bukkit.getWorld(st.world);
        boolean right = e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT;
        if (slot < 45 && CATS[st.cat].equals("Permanentes")) {
            if (slot < Permanents.DEFS.size()) {
                Permanents.Def d = Permanents.DEFS.get(slot);
                if (pl.permanents.active(d.id())) pl.permanents.disable(d.id(), p.getName());
                else pl.permanents.enable(d.id(), right ? 7 : 0, p.getName());
            }
        } else if (slot < 45) {
            int idx = st.page * 45 + slot;
            if (idx >= h.shown.size()) return;
            Disaster d = h.shown.get(idx);
            if (right) {
                boolean now = !pl.manager.enabled(d);
                pl.getConfig().set("desastres." + d.id + ".activo", now);
                pl.saveConfig();
            } else {
                boolean stopped = false;
                if (w != null) for (Active a : new ArrayList<>(pl.manager.of(w))) if (a.d == d) {
                    pl.manager.stop(a, false);
                    stopped = true;
                }
                if (!stopped) {
                    int mins = DURATIONS[st.dur] == 0 ? pl.manager.minutes(d) : DURATIONS[st.dur];
                    String err = pl.manager.start(d, w, mins, st.level, pl.getConfig().getInt("aviso-segundos", 15), p.getName(), true, e.getClick() == ClickType.SHIFT_LEFT);
                    if (err != null) p.sendMessage("§c" + err);
                }
            }
        } else switch (slot) {
            case 45 -> st.page = Math.max(0, st.page - 1);
            case 53 -> st.page++;
            case 46 -> {
                int n = 0;
                if (w != null) for (Active a : new ArrayList<>(pl.manager.of(w))) {
                    pl.manager.stop(a, false);
                    n++;
                }
                p.sendMessage("§aDetenidos " + n + " desastres en " + st.world + ".");
            }
            case 47 -> {
                String err = pl.manager.autoPick();
                if (err != null) p.sendMessage("§c" + err);
            }
            case 48 -> {
                pl.getConfig().set("auto.activo", !pl.getConfig().getBoolean("auto.activo", false));
                pl.saveConfig();
                pl.manager.resetAutoTimer();
            }
            case 49 -> {
                List<World> ws = Bukkit.getWorlds();
                int i = 0;
                for (int k = 0; k < ws.size(); k++) if (ws.get(k).getName().equals(st.world)) i = k;
                st.world = ws.get((i + 1) % ws.size()).getName();
            }
            case 50 -> st.dur = (st.dur + 1) % DURATIONS.length;
            case 51 -> st.level = st.level % 3 + 1;
            case 52 -> {
                st.cat = (st.cat + 1) % CATS.length;
                st.page = 0;
            }
            default -> {
            }
        }
        // se redibuja en el siguiente tick (los desastres arrancan en este)
        Bukkit.getScheduler().runTask(pl, () -> {
            if (p.getOpenInventory().getTopInventory().getHolder() == h) draw(h);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
