package com.swampd.superficialtrauma.common.init;

import com.swampd.superficialtrauma.SuperficialTrauma;
import com.swampd.superficialtrauma.common.body.DownedGeometry;
import com.swampd.superficialtrauma.common.entity.CorpseEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES,
            SuperficialTrauma.MOD_ID
    );

    public static final RegistryObject<EntityType<CorpseEntity>> CORPSE = ENTITY_TYPES.register(
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

    @Mod.EventBusSubscriber(
            modid = SuperficialTrauma.MOD_ID,
            bus = Mod.EventBusSubscriber.Bus.MOD
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
