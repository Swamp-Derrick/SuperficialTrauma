package com.swampd.superficialtrauma.common.loot;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public final class CorpseEquipmentTransfer {
    private static final List<EquipmentSlot> AUTO_EQUIP_ORDER = List.of(
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.HEAD,
            EquipmentSlot.FEET
    );

    private CorpseEquipmentTransfer() {
    }

    public static int equipUpgrades(ServerPlayer looter, Container corpseInventory) {
        int equipped = 0;
        for (EquipmentSlot equipmentSlot : AUTO_EQUIP_ORDER) {
            if (tryEquipUpgrade(looter, corpseInventory, equipmentSlot)) {
                equipped++;
            }
        }
        if (equipped > 0) {
            looter.getInventory().setChanged();
            looter.inventoryMenu.broadcastChanges();
            corpseInventory.setChanged();
        }
        return equipped;
    }

    public static boolean isUpgrade(
            ItemStack candidate,
            ItemStack equipped,
            EquipmentSlot equipmentSlot,
            boolean mayRemoveBindingCurse
    ) {
        if (!(candidate.getItem() instanceof ArmorItem candidateArmor)
                || candidateArmor.getEquipmentSlot() != equipmentSlot
                || EnchantmentHelper.hasBindingCurse(candidate)) {
            return false;
        }
        if (equipped.isEmpty()) {
            return true;
        }
        if (!(equipped.getItem() instanceof ArmorItem equippedArmor)
                || equippedArmor.getEquipmentSlot() != equipmentSlot) {
            return false;
        }
        if (!mayRemoveBindingCurse && EnchantmentHelper.hasBindingCurse(equipped)) {
            return false;
        }
        return isHigherQuality(quality(candidateArmor, candidate), quality(equippedArmor, equipped));
    }

    public static boolean isHigherQuality(ArmorQuality candidate, ArmorQuality equipped) {
        return candidate.compareTo(equipped) > 0;
    }

    private static boolean tryEquipUpgrade(
            ServerPlayer looter,
            Container corpseInventory,
            EquipmentSlot equipmentSlot
    ) {
        int corpseSlot = 36 + equipmentSlot.getIndex();
        ItemStack candidate = corpseInventory.getItem(corpseSlot);
        ItemStack equipped = looter.getItemBySlot(equipmentSlot);
        if (!isUpgrade(candidate, equipped, equipmentSlot, looter.getAbilities().instabuild)) {
            return false;
        }

        int backpackDestination = equipped.isEmpty()
                ? -1
                : findBackpackDestination(looter.getInventory(), equipped);
        if (!equipped.isEmpty() && backpackDestination < 0) {
            return false;
        }

        if (!equipped.isEmpty()) {
            storeInBackpack(looter.getInventory(), backpackDestination, equipped.copy());
        }
        looter.setItemSlot(equipmentSlot, candidate.copy());
        corpseInventory.setItem(corpseSlot, ItemStack.EMPTY);
        return true;
    }

    private static int findBackpackDestination(Inventory inventory, ItemStack stack) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (ItemStack.isSameItemSameTags(existing, stack)
                    && existing.getCount() + stack.getCount()
                    <= Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize())) {
                return slot;
            }
        }
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void storeInBackpack(Inventory inventory, int slot, ItemStack stack) {
        ItemStack existing = inventory.getItem(slot);
        if (existing.isEmpty()) {
            inventory.setItem(slot, stack);
        } else {
            existing.grow(stack.getCount());
        }
    }

    private static ArmorQuality quality(ArmorItem armor, ItemStack stack) {
        return new ArmorQuality(
                armor.getDefense(),
                armor.getToughness(),
                armor.getMaterial().getKnockbackResistance(),
                protectionScore(stack),
                remainingDurability(stack)
        );
    }

    private static int protectionScore(ItemStack stack) {
        var enchantments = EnchantmentHelper.getEnchantments(stack);
        return enchantments.getOrDefault(Enchantments.ALL_DAMAGE_PROTECTION, 0) * 4
                + enchantments.getOrDefault(Enchantments.BLAST_PROTECTION, 0)
                + enchantments.getOrDefault(Enchantments.FIRE_PROTECTION, 0)
                + enchantments.getOrDefault(Enchantments.PROJECTILE_PROTECTION, 0)
                + enchantments.getOrDefault(Enchantments.FALL_PROTECTION, 0);
    }

    private static double remainingDurability(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 1.0D;
        }
        return (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
    }

    public record ArmorQuality(
            int defense,
            float toughness,
            float knockbackResistance,
            int protection,
            double remainingDurability
    ) implements Comparable<ArmorQuality> {
        @Override
        public int compareTo(ArmorQuality other) {
            int comparison = Integer.compare(defense, other.defense);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Float.compare(toughness, other.toughness);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Float.compare(knockbackResistance, other.knockbackResistance);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Integer.compare(protection, other.protection);
            if (comparison != 0) {
                return comparison;
            }
            return Double.compare(remainingDurability, other.remainingDurability);
        }
    }
}
