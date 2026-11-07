package com.etheria.consecuencias;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Equipo contra desastres: máscara antigás, abrigo térmico y antídoto. */
public final class Gear {
    private Gear() {
    }

    private static final Map<UUID, Long> IMMUNE = new HashMap<>();

    private static NamespacedKey key(String k) {
        return new NamespacedKey(ConsecuenciasPlugin.get(), k);
    }

    private static ItemStack tag(ItemStack it, String id) {
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(key("equipo"), PersistentDataType.STRING, id);
        it.setItemMeta(m);
        return it;
    }

    public static String idOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(key("equipo"), PersistentDataType.STRING);
    }

    public static ItemStack mask() {
        ItemStack it = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta m = (LeatherArmorMeta) it.getItemMeta();
        m.setColor(Color.fromRGB(60, 70, 55));
        m.setDisplayName("§a§lMáscara antigás");
        m.setLore(List.of("§7Filtra gas venenoso, ceniza, polvo", "§7y lluvia ácida. Se desgasta al usarla.", "§8Equipo anti-desastres"));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return tag(it, "mascara");
    }

    public static ItemStack coat() {
        ItemStack it = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta m = (LeatherArmorMeta) it.getItemMeta();
        m.setColor(Color.fromRGB(235, 235, 240));
        m.setDisplayName("§b§lAbrigo térmico");
        m.setLore(List.of("§7Te protege del frío de las ventiscas", "§7y de las heladas.", "§8Equipo anti-desastres"));
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return tag(it, "abrigo");
    }

    public static ItemStack antidote() {
        ItemStack it = new ItemStack(Material.POTION);
        PotionMeta m = (PotionMeta) it.getItemMeta();
        m.setColor(Color.fromRGB(120, 230, 90));
        m.setDisplayName("§a§lAntídoto");
        m.setLore(List.of("§7Cura la peste y quita venenos.", "§7Durante 3 min te protege del gas", "§7y del agua tóxica.", "§8Equipo anti-desastres"));
        m.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        it.setItemMeta(m);
        return tag(it, "antidoto");
    }

    public static void registerRecipes(ConsecuenciasPlugin pl) {
        if (!pl.getConfig().getBoolean("equipo.recetas", true)) return;
        ShapelessRecipe r1 = new ShapelessRecipe(key("receta_mascara"), mask());
        r1.addIngredient(Material.LEATHER_HELMET);
        r1.addIngredient(Material.CHARCOAL);
        r1.addIngredient(Material.CHARCOAL);
        r1.addIngredient(Material.IRON_INGOT);
        r1.addIngredient(Material.GLASS_PANE);
        ShapelessRecipe r2 = new ShapelessRecipe(key("receta_abrigo"), coat());
        r2.addIngredient(Material.LEATHER_CHESTPLATE);
        r2.addIngredient(Material.WHITE_WOOL);
        r2.addIngredient(Material.WHITE_WOOL);
        r2.addIngredient(Material.WHITE_WOOL);
        r2.addIngredient(Material.COAL);
        ShapelessRecipe r3 = new ShapelessRecipe(key("receta_antidoto"), antidote());
        r3.addIngredient(Material.GLASS_BOTTLE);
        r3.addIngredient(Material.GLISTERING_MELON_SLICE);
        r3.addIngredient(Material.SPIDER_EYE);
        r3.addIngredient(Material.SUGAR);
        for (ShapelessRecipe r : List.of(r1, r2, r3)) {
            Bukkit.removeRecipe(r.getKey());
            Bukkit.addRecipe(r);
        }
    }

    public static void unregisterRecipes() {
        for (String k : List.of("receta_mascara", "receta_abrigo", "receta_antidoto")) Bukkit.removeRecipe(key(k));
    }

    public static boolean hasMask(Player p) {
        return "mascara".equals(idOf(p.getInventory().getHelmet()));
    }

    public static boolean hasCoat(Player p) {
        return "abrigo".equals(idOf(p.getInventory().getChestplate()));
    }

    public static boolean immune(Player p) {
        Long t = IMMUNE.get(p.getUniqueId());
        return t != null && t > System.currentTimeMillis();
    }

    public static void giveImmunity(Player p, long ms) {
        IMMUNE.put(p.getUniqueId(), System.currentTimeMillis() + ms);
    }

    /** Protección contra aire/agua contaminada: máscara o antídoto. */
    public static boolean airSafe(Player p) {
        return hasMask(p) || immune(p);
    }

    /** Desgasta la máscara (si la lleva). */
    public static void wearMask(Player p, int amount) {
        ItemStack h = p.getInventory().getHelmet();
        if (!"mascara".equals(idOf(h)) || !(h.getItemMeta() instanceof Damageable dm)) return;
        int max = h.getType().getMaxDurability();
        int now = dm.getDamage() + amount;
        if (now >= max) {
            p.getInventory().setHelmet(null);
            p.sendMessage("§c¡Tu máscara antigás se ha gastado!");
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
        } else {
            dm.setDamage(now);
            h.setItemMeta(dm);
        }
    }
}
