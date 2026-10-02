package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
            BuiltInRegistries.ENTITY_TYPE,
            SuperficialTrauma.MOD_ID
    );

    public static final DeferredHolder<EntityType<?>, EntityType<CorpseEntity>> CORPSE = ENTITY_TYPES.register(
            "corpse",
            () -> EntityType.Builder.<CorpseEntity>of(CorpseEntity::new, MobCategory.MISC)
                    .sized(DownedGeometry.BODY_WIDTH, DownedGeometry.BODY_HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(SuperficialTrauma.MOD_ID + ":corpse")
    );

    private ModEntities() {
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    @EventBusSubscriber(
            modid = SuperficialTrauma.MOD_ID
    )
    public static final class AttributesRegistration {
        private AttributesRegistration() {
        }

        @SubscribeEvent
        public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
            event.put(
                    CORPSE.get(),
                    CorpseEntity.createLivingAttributes()
                            .add(Attributes.MAX_HEALTH, 1.0D)
                            .add(Attributes.MOVEMENT_SPEED, 0.0D)
                            .build()
            );
        }
    }
}
