package tallestegg.guardvillagers;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.entities.Guard;
import tallestegg.guardvillagers.entities.ai.goals.ArmorerRepairGuardArmorGoal;
import tallestegg.guardvillagers.entities.ai.goals.AttackEntityDaytimeGoal;
import tallestegg.guardvillagers.entities.ai.goals.HealGolemGoal;
import tallestegg.guardvillagers.entities.ai.goals.HealGuardAndPlayerGoal;

import java.util.List;

@EventBusSubscriber(modid = GuardVillagers.MODID)
public class HandlerEvents {
    @SubscribeEvent
    public static void onEntityTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null || entity.getType() == GuardEntityType.GUARD.get()) return;
        boolean isVillager = target.getType() == EntityType.VILLAGER || target.getType() == GuardEntityType.GUARD.get();
        if (isVillager) {
            List<Mob> list = entity.level().getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(GuardConfig.COMMON.GuardVillagerHelpRange.get(), 5.0D, GuardConfig.COMMON.GuardVillagerHelpRange.get()));
            for (Mob mob : list) {
                if ((mob.getTarget() == null) && (mob.getType() == GuardEntityType.GUARD.get() || mob.getType() == EntityType.IRON_GOLEM)) {
                    if (mob.getTeam() != null && entity.getTeam() != null && entity.getTeam().isAlliedTo(mob.getTeam()))
                        return;
                    else
                        mob.setTarget(entity);
                }
            }
        }

        if (entity instanceof IronGolem golem && target instanceof Guard) golem.setTarget(null);
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        Entity trueSource = event.getSource().getEntity();
        if (entity == null || trueSource == null) return;
        boolean isVillager = entity.getType() == EntityType.VILLAGER || entity.getType() == GuardEntityType.GUARD.get();
        boolean isGolem = isVillager || entity.getType() == EntityType.IRON_GOLEM;
        if (isGolem && trueSource.getType() == GuardEntityType.GUARD.get() && !GuardConfig.COMMON.guardArrowsHurtVillagers.get()) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
        if (isVillager && event.getSource().getEntity() instanceof Mob) {
            List<Mob> list = trueSource.level().getEntitiesOfClass(Mob.class, trueSource.getBoundingBox().inflate(GuardConfig.COMMON.GuardVillagerHelpRange.get(), 5.0D, GuardConfig.COMMON.GuardVillagerHelpRange.get()));
            for (Mob mob : list) {
                boolean type = mob.getType() == GuardEntityType.GUARD.get() || mob.getType() == EntityType.IRON_GOLEM;
                boolean trueSourceGolem = trueSource.getType() == GuardEntityType.GUARD.get() || trueSource.getType() == EntityType.IRON_GOLEM;
                if (!trueSourceGolem && type && mob.getTarget() == null) {
                    if (mob.getTeam() != null && entity.getTeam() != null && entity.getTeam().isAlliedTo(mob.getTeam()))
                        return;
                    else
                        mob.setTarget((Mob) event.getSource().getEntity());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof AbstractHorse horse) {
            Vec3 vec3 = new Vec3(horse.xxa, horse.yya, horse.zza);
            if (horse.hasControllingPassenger() && horse.getControllingPassenger() instanceof Guard) {
                horse.setSpeed((float) horse.getAttributeValue(Attributes.MOVEMENT_SPEED));
                horse.travel(vec3);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingSpawned(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            if (mob instanceof Raider) {
                if (((Raider) mob).hasActiveRaid() && GuardConfig.COMMON.RaidAnimals.get())
                    mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(((Raider) mob), Animal.class, false));
            }
            if (GuardConfig.COMMON.AttackAllMobs.get()) {
                if (mob instanceof Enemy && !GuardConfig.COMMON.MobBlackList.get().contains(mob.getEncodeId())) {
                    if (!(mob instanceof Spider))
                        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Guard.class, false));
                    else
                        mob.targetSelector.addGoal(3, new AttackEntityDaytimeGoal<>((Spider) mob, Guard.class));
                }
            }

            if (mob instanceof AbstractIllager illager) {
                if (GuardConfig.COMMON.IllagersRunFromPolarBears.get())
                    illager.goalSelector.addGoal(2, new AvoidEntityGoal<>(illager, PolarBear.class, 6.0F, 1.0D, 1.2D));
                illager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(illager, Guard.class, false));
            }

            if (mob instanceof AbstractVillager abstractvillager) {
                if (GuardConfig.COMMON.VillagersRunFromPolarBears.get())
                    abstractvillager.goalSelector.addGoal(2, new AvoidEntityGoal<>(abstractvillager, PolarBear.class, 6.0F, 1.0D, 1.2D));
                if (GuardConfig.COMMON.WitchesVillager.get())
                    abstractvillager.goalSelector.addGoal(2, new AvoidEntityGoal<>(abstractvillager, Witch.class, 6.0F, 1.0D, 1.2D));
            }

            if (mob instanceof Villager villager) {
                if (GuardConfig.COMMON.BlacksmithHealing.get())
                    villager.goalSelector.addGoal(1, new HealGolemGoal(villager));
                if (GuardConfig.COMMON.ClericHealing.get())
                    villager.goalSelector.addGoal(1, new HealGuardAndPlayerGoal(villager, 1.0D, 100, 0, 10.0F));
            }

            if (mob instanceof IronGolem golem) {
                HurtByTargetGoal tolerateFriendlyFire = new HurtByTargetGoal(golem, Guard.class).setAlertOthers();
                golem.targetSelector.getAvailableGoals().stream().map(it -> it.getGoal()).filter(it -> it instanceof HurtByTargetGoal).findFirst().ifPresent(angerGoal -> {
                    golem.targetSelector.removeGoal(angerGoal);
                    golem.targetSelector.addGoal(2, tolerateFriendlyFire);
                });
            }

            if (mob instanceof Zombie zombie) {
                zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, Guard.class, false));
            }

            if (mob instanceof Ravager ravager) {
                ravager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(ravager, Guard.class, false));
            }

            if (mob instanceof Witch witch) {
                if (GuardConfig.COMMON.WitchesVillager.get()) {
                    witch.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witch, AbstractVillager.class, true));
                    witch.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(witch, IronGolem.class, true));
                    witch.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(witch, Guard.class, false));
                }
            }

            if (mob instanceof Cat cat) {
                cat.goalSelector.addGoal(1, new AvoidEntityGoal<>(cat, AbstractIllager.class, 12.0F, 1.0D, 1.2D));
            }
        }
    }
}
