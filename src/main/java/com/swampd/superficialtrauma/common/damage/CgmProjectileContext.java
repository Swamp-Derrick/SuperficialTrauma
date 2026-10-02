package com.swampd.superficialtrauma.common.damage;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

public record CgmProjectileContext(
        String projectileEntityId,
        ItemStack ammoStack,
        ItemStack weaponStack
) {
    private static final ResourceLocation CGM_PROJECTILE_ID = ResourceLocation.fromNamespaceAndPath("cgm", "projectile");
    private static final String CGM_PROJECTILE_CLASS = "com.mrcrayfish.guns.entity.ProjectileEntity";

    public CgmProjectileContext {
        projectileEntityId = normalize(projectileEntityId);
        ammoStack = ammoStack.copy();
        weaponStack = weaponStack.copy();
    }

    public static Optional<CgmProjectileContext> inspect(DamageSource source) {
        Entity directEntity = source.getDirectEntity();
        if (directEntity == null || !isCgmProjectile(directEntity)) {
            return Optional.empty();
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(directEntity.getType());
        return Optional.of(new CgmProjectileContext(
                entityId == null ? "unknown" : entityId.toString(),
                invokeItemStack(directEntity, "getItem"),
                invokeItemStack(directEntity, "getWeapon")
        ));
    }

    public String ammoId() {
        return itemId(ammoStack);
    }

    public String weaponId() {
        return itemId(weaponStack);
    }

    private static boolean isCgmProjectile(Entity entity) {
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (CGM_PROJECTILE_ID.equals(entityId)) {
            return true;
        }

        Class<?> entityClass = entity.getClass();
        while (entityClass != null) {
            if (CGM_PROJECTILE_CLASS.equals(entityClass.getName())) {
                return true;
            }
            entityClass = entityClass.getSuperclass();
        }
        return false;
    }

    private static ItemStack invokeItemStack(Entity entity, String methodName) {
        try {
            Method method = entity.getClass().getMethod(methodName);
            Object result = method.invoke(entity);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String itemId(ItemStack stack) {
        if (stack.isEmpty()) {
            return "none";
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId == null ? "unknown" : itemId.toString();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
