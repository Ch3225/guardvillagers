package tallestegg.guardvillagers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import tallestegg.guardvillagers.client.GuardSounds;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.entities.Guard;
import tallestegg.guardvillagers.networking.GuardFollowPacket;
import tallestegg.guardvillagers.networking.GuardOpenInventoryPacket;
import tallestegg.guardvillagers.networking.GuardSetPatrolPosPacket;

@Mod(GuardVillagers.MODID)
public class GuardVillagers {
    public static final String MODID = "guardvillagers";

    public GuardVillagers(IEventBus modEventBus, Dist dist) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GuardConfig.COMMON_SPEC);
        GuardConfig.loadConfig(GuardConfig.COMMON_SPEC, FMLPaths.CONFIGDIR.get().resolve(MODID + "-common.toml").toString());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GuardConfig.CLIENT_SPEC);
        modEventBus.addListener(this::setup);
        NeoForge.EVENT_BUS.register(HandlerEvents.class);
        NeoForge.EVENT_BUS.register(VillagerToGuard.class);
        GuardEntityType.ENTITIES.register(modEventBus);
        GuardItems.ITEMS.register(modEventBus);
        GuardSounds.SOUNDS.register(modEventBus);
        modEventBus.addListener(this::addAttributes);
        modEventBus.addListener(this::addCreativeTabs);
        modEventBus.addListener(this::register);
    }


    private void register(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar reg = event.registrar(MODID).versioned("2.0.1");
        reg.play(GuardSetPatrolPosPacket.ID, GuardSetPatrolPosPacket::new, payload -> payload.server(GuardSetPatrolPosPacket::handle));
        reg.play(GuardOpenInventoryPacket.ID, GuardOpenInventoryPacket::new, payload -> payload.client(GuardOpenInventoryPacket::handle));
        reg.play(GuardFollowPacket.ID, GuardFollowPacket::new, payload -> payload.server(GuardFollowPacket::handle));

    }

    public static boolean hotvChecker(Player player, Guard guard) {
        return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.giveGuardStuffHOTV
                || !GuardConfig.giveGuardStuffHOTV || guard.getPlayerReputation(player) > GuardConfig.reputationRequirement && !player.level().isClientSide();
    }

    @SubscribeEvent
    private void addCreativeTabs(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(GuardItems.GUARD_SPAWN_EGG.get());
            event.accept(GuardItems.ILLUSIONER_SPAWN_EGG.get());
        }
    }

    @SubscribeEvent
    private void setup(final FMLCommonSetupEvent event) {
        if (GuardConfig.IllusionerRaids)
            Raid.RaiderType.create("thebluemengroup", EntityType.ILLUSIONER, new int[]{0, 0, 0, 0, 0, 1, 1, 2});
    }

    @SubscribeEvent
    private void addAttributes(final EntityAttributeCreationEvent event) {
        event.put(GuardEntityType.GUARD.get(), Guard.createAttributes().build());
    }
}
