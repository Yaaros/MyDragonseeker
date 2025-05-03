package syric.dragonseeker.item.tool;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityFireDragon;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import net.minecraftforge.common.world.ForgeChunkManager;
import syric.dragonseeker.Dragonseeker;
import syric.dragonseeker.DragonseekerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static syric.dragonseeker.Dragonseeker.MODID;

public class dragonseekerGeneric extends Item {

    //Defining statistics
    //Ping chance stats
    private int opDist;
    private int maxDist;
    private double minPing;
    private double maxPing;

    //Ping characteristic stats
    private int minSig;
    private double pow;
    private float minVol;
    private float maxVol;
    private float minPitch;
    private float maxPitch;
    private final SoundEvent negSound;
    private final SoundEvent pingSound;

    //Other stats
    private boolean detectsCorpses;
    private boolean detectsTame;
//    private int durability;
//    private Rarity rarity;
    private final Item repairItem;
    private final int seekerType;
    private boolean isDefault;

    //Constructor
//    public dragonseekerGeneric() {
//        super(new Properties()
//                .stacksTo(1)
//                .tab(IceAndFire.TAB_ITEMS)
//        );
//    }

    public dragonseekerGeneric(int opDistIn,
                               int maxDistIn,
                               double minPingIn,
                               double maxPingIn,
                               int minSigIn,
                               double powIn,
                               double minVolIn,
                               double maxVolIn,
                               double minPitchIn,
                               double maxPitchIn,
                               SoundEvent negSoundIn,
                               SoundEvent pingSoundIn,
                               boolean detectsCorpsesIn,
                               boolean detectsTameIn,
                               int durabilityIn,
                               Rarity rarityIn,
                               Item repairItemIn,
                               int seekerTypeIn)
    {
        super(new Properties()
                .stacksTo(1)
                .durability(durabilityIn)
                .rarity(rarityIn)
        );
        opDist = opDistIn;
        maxDist = maxDistIn;
        minPing = minPingIn;
        maxPing = maxPingIn;
        minSig = minSigIn;
        pow = powIn;
        minVol = (float) minVolIn;
        maxVol = (float) maxVolIn;
        minPitch = (float) minPitchIn;
        maxPitch = (float) maxPitchIn;
        negSound = negSoundIn;
        pingSound = pingSoundIn;
        detectsCorpses = detectsCorpsesIn;
        detectsTame = detectsTameIn;
//        durability = durabilityIn;
//        rarity = rarityIn;
        repairItem = repairItemIn;
        seekerType = seekerTypeIn;
        isDefault = true;
    }


    //Repairing
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.getItem() == repairItem;
    }


    //Using the Item
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        if(world.isClientSide()||player.isSpectator()){
            return InteractionResultHolder.pass(itemstack);
        }

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(itemstack);
        }

        if (isDefault) {
            importConfig();
            isDefault = false;
        }

        assert world instanceof ServerLevel;

        itemstack.hurtAndBreak(1, player, (entity) -> player.broadcastBreakEvent(player.getUsedItemHand()));

        double distance = getDistance(world, player);
        EntityDragonBase closest = getClosestDragon(world, player, distance);

        if (closest != null) {
            assignDragonToTeam(closest);
            applyGlowEffect(closest, getGlowDuration());
            racing((ServerLevel)world,player,closest);

        } else {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), negSound, SoundSource.MASTER, minVol, minPitch);
        }
        player.getCooldowns().addCooldown(this, 40);

        return InteractionResultHolder.sidedSuccess(itemstack, world.isClientSide());

    }
    private void racing(ServerLevel world, Player player, EntityDragonBase closest) {
        // 获取玩家头部位置（Y 偏移 1.5 格，即玩家站立时的眼睛高度）
        Vec3 playerHead = player.getEyePosition(1.0f)
                .add(1.5, 0, 1.5)
                .subtract(0, 0.5, 0);

        // 获取龙的位置（取龙的躯干中心）
        Vec3 dragonPos = closest.position()
                .add(0, closest.getBbHeight() / 2, 0);

        // 计算方向向量（从玩家指向龙）
        Vec3 direction = dragonPos.subtract(playerHead).normalize();

        // 激光总长度（5 * √3 ≈ 8.66 格）
        double length = 5 * Math.sqrt(3);
        Vec3 endPoint = playerHead.add(direction.scale(length));

        // 生成激光粒子（沿直线生成密集粒子）
        generateLaserBeam(world, playerHead, endPoint);
    }

    // 生成激光束的核心方法
    private void generateLaserBeam(ServerLevel world, Vec3 start, Vec3 end) {
        // 激光参数配置
        final int particlesPerBlock = 6; // 每格生成粒子数
        final double step = 1.0 / particlesPerBlock;
        final double distance = start.distanceTo(end);
        final Vec3 direction = end.subtract(start).normalize();

        // 使用两种粒子增强效果
        for (double d = 0; d <= distance; d += step) {
            // 计算当前粒子位置
            Vec3 currentPos = start.add(direction.scale(d));

            // 生成末影核心粒子（END_ROD）
            world.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    currentPos.x,
                    currentPos.y,
                    currentPos.z,
                    1, // 数量
                    0, 0, 0, // 随机偏移
                    0.01 // 速度
            );

            // 生成环绕火焰粒子（FLAME）
            if (d % 0.5 < step) { // 每 0.5 格生成一次
                generateHaloParticles(world, currentPos, direction);
            }
        }
    }

    // 生成环绕光晕粒子
    private void generateHaloParticles(ServerLevel world, Vec3 center, Vec3 direction) {
        // 生成垂直于激光方向的随机偏移
        Vec3 perpendicular = direction.y == 0 ?
                new Vec3(0, 1, 0) : // 如果方向水平，取垂直 Y 轴
                new Vec3(direction.y, -direction.x, 0).normalize();

        final double radius = 0.2;
        final int haloParticles = 4;

        for (int i = 0; i < haloParticles; i++) {
            double angle = i * Math.PI * 2 / haloParticles;
            Vec3 offset = perpendicular.scale(radius * Math.cos(angle))
                    .add(new Vec3(0, radius * Math.sin(angle), 0));

            world.sendParticles(
                    ParticleTypes.FLAME,
                    center.x + offset.x,
                    center.y + offset.y,
                    center.z + offset.z,
                    1,
                    0, 0, 0,
                    0.05
            );
        }
    }


    private EntityDragonBase getClosestDragon(Level world, Player player, double distance) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB box = new AABB(x - maxDist, -64, z - maxDist, x + maxDist, y + 100, z + maxDist);
        List<EntityDragonBase> listOfTargets = world.getEntitiesOfClass(EntityDragonBase.class, box);

        EntityDragonBase closest = null;
        double minDistance = Double.MAX_VALUE;

        for (EntityDragonBase target : listOfTargets) {
            if ((detectsCorpses || !target.isModelDead()) && (detectsTame || !target.isTame())) {
                double currentDistance = target.distanceTo(player);
                if (currentDistance < minDistance && currentDistance <= maxDist) {
                    minDistance = currentDistance;
                    closest = target;
                }
            }
        }

        return closest;
    }

    private void applyGlowEffect(EntityDragonBase dragon, int duration) {
        dragon.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration * 20, 0, false, false));
    }

    private int getGlowDuration() {
        if (seekerType == 1) return 3; // dragonseeker item
        if (seekerType == 2) return 15; // epic dragonseeker item
        if (seekerType == 3) return 60; // legendary dragonseeker item
        return 0;
    }

    //methods
    private double getDistance(Level world, Player player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB box = new AABB(x-300,-64,z-300,x+300,y+100,z+300);
//        AABB box = new AABB(x - 10, y - 10, z - 10, x + 10, y + 10, z + 10);
//        List<LivingEntity> listOfTargets = world.getNearbyEntities(ShulkerEntity.class,pred,player,box);
        List<EntityDragonBase> listOfTargets = world.getEntitiesOfClass(EntityDragonBase.class, box);
//        String s = "Identified " + listOfTargets.size() + " dragons.";
//        player.displayClientMessage(Component.literal(s), false);
//
//        List<LivingEntity> allEntities = world.getNearbyEntities(LivingEntity.class, TargetingConditions.DEFAULT, player, box);
//        s = "Identified " + allEntities.size() + " total entities.";
//        player.displayClientMessage(Component.literal(s), false);

        float min = 0;
        EntityDragonBase closest = null;
        for (EntityDragonBase target : listOfTargets) {
            if ((detectsCorpses || !target.isModelDead()) && (detectsTame || !target.isTame())) {
                float distance = target.distanceTo(player);
                if ((min == 0) || distance < min) {
                    min = distance;
                    closest = target;
//                    s = "Found dragon, updating minimum distance";
//                    player.displayClientMessage(Component.literal(s), false);
                } else if (distance >= min) {
//                    s = "Found further dragon, ignoring";
//                    player.displayClientMessage(Component.literal(s), false);
                }
            } else if (!detectsCorpses && target.isModelDead()) {
//                s = "Found corpse, ignoring";
//                player.displayClientMessage(Component.literal(s), false);
            } else if (!detectsTame && target.isTame()) {
//                s = "Found tamed dragon, ignoring";
//                player.displayClientMessage(Component.literal(s), false);
            }
        }
        if (seekerType == 4) {
            String s = closest != null ? Math.round(min) + ", x=" + (int) closest.getX() + ", y=" + (int) closest.getY() + ", z=" + (int) closest.getZ() : "No dragon found";
            player.displayClientMessage(Component.literal(s), false);
        }

        return min;
    }

    private void printDistance(double distance, Level world, Player player) {
        if (!world.isClientSide) {
            int distancenew = (int) Math.round(distance);
            String s = String.valueOf(distancenew);
            player.displayClientMessage(Component.literal(s), false);
        }
    }
    // 入队
    private void assignDragonToTeam(EntityDragonBase dragon) {
        if (dragon == null) return;

        // 判断是否已经入队
        CompoundTag data = dragon.getPersistentData();
        if (data.contains(MODID)) {
            CompoundTag tag = data.getCompound(MODID);
            if (tag.getBoolean("GlowChecked")) {
                return; // 已经入队，无需重复操作
            }
        }

        String teamName = "";
        if (dragon instanceof EntityFireDragon) {
            teamName = "red_iaf";
        } else if (dragon instanceof EntityIceDragon) {
            teamName = "aqua_iaf";
        } else if (dragon instanceof EntityLightningDragon) {
            teamName = "lightning_iaf";
        } else {
            return; // 未知类型不处理
        }

        String entityUUID = dragon.getStringUUID(); // UUID作为玩家名加入队伍
        Scoreboard scoreboard = dragon.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team != null) {
            scoreboard.addPlayerToTeam(entityUUID, team);

            // 标记为已处理
            CompoundTag dragonPersistentData = dragon.getPersistentData();
            CompoundTag tag;
            if (!dragonPersistentData.contains(MODID)) {
                tag = new CompoundTag();
                dragonPersistentData.put(MODID, tag);
            } else {
                tag = dragonPersistentData.getCompound(MODID);
            }
            tag.putBoolean("GlowChecked", true);
            // 无需调用任何“set回去”的方法，tag 是引用
        }
    }



    public void importConfig() {
        if (seekerType == 1) {
            opDist = DragonseekerConfig.COMMON.basic_optimalDistance.get();
            maxDist = DragonseekerConfig.COMMON.basic_maxDistance.get();
            minPing = DragonseekerConfig.COMMON.basic_minPingChance.get();
            maxPing = DragonseekerConfig.COMMON.basic_maxPingChance.get();

            //Ping characteristic stats
            minSig = DragonseekerConfig.COMMON.basic_pingCapRadius.get();
            pow = DragonseekerConfig.COMMON.basic_sigPower.get();
            minVol = DragonseekerConfig.COMMON.basic_minVol.get().floatValue();
            maxVol = DragonseekerConfig.COMMON.basic_maxVol.get().floatValue();
            minPitch = DragonseekerConfig.COMMON.basic_minPitch.get().floatValue();
            maxPitch = DragonseekerConfig.COMMON.basic_maxPitch.get().floatValue();

            //Other stats
            detectsCorpses = DragonseekerConfig.COMMON.basic_detectsCorpses.get();
            detectsTame = DragonseekerConfig.COMMON.basic_detectsTame.get();
            isDefault = false;

        } else if (seekerType == 2) {
            opDist = DragonseekerConfig.COMMON.epic_optimalDistance.get();
            maxDist = DragonseekerConfig.COMMON.epic_maxDistance.get();
            minPing = DragonseekerConfig.COMMON.epic_minPingChance.get();
            maxPing = DragonseekerConfig.COMMON.epic_maxPingChance.get();

            //Ping characteristic stats
            minSig = DragonseekerConfig.COMMON.epic_pingCapRadius.get();
            pow = DragonseekerConfig.COMMON.epic_sigPower.get();
            minVol = DragonseekerConfig.COMMON.epic_minVol.get().floatValue();
            maxVol = DragonseekerConfig.COMMON.epic_maxVol.get().floatValue();
            minPitch = DragonseekerConfig.COMMON.epic_minPitch.get().floatValue();
            maxPitch = DragonseekerConfig.COMMON.epic_maxPitch.get().floatValue();

            //Other stats
            detectsCorpses = DragonseekerConfig.COMMON.epic_detectsCorpses.get();
            detectsTame = DragonseekerConfig.COMMON.epic_detectsTame.get();
            isDefault = false;

        } else if (seekerType == 3) {
            opDist = DragonseekerConfig.COMMON.legendary_optimalDistance.get();
            maxDist = DragonseekerConfig.COMMON.legendary_maxDistance.get();
            minPing = DragonseekerConfig.COMMON.legendary_minPingChance.get();
            maxPing = DragonseekerConfig.COMMON.legendary_maxPingChance.get();

            //Ping characteristic stats
            minSig = DragonseekerConfig.COMMON.legendary_pingCapRadius.get();
            pow = DragonseekerConfig.COMMON.legendary_sigPower.get();
            minVol = DragonseekerConfig.COMMON.legendary_minVol.get().floatValue();
            maxVol = DragonseekerConfig.COMMON.legendary_maxVol.get().floatValue();
            minPitch = DragonseekerConfig.COMMON.legendary_minPitch.get().floatValue();
            maxPitch = DragonseekerConfig.COMMON.legendary_maxPitch.get().floatValue();

            //Other stats
            detectsCorpses = DragonseekerConfig.COMMON.legendary_detectsCorpses.get();
            detectsTame = DragonseekerConfig.COMMON.legendary_detectsTame.get();
            isDefault = false;

        } else if (seekerType == 4) {
            opDist = DragonseekerConfig.COMMON.mythic_optimalDistance.get();
            maxDist = DragonseekerConfig.COMMON.mythic_maxDistance.get();
            minPing = DragonseekerConfig.COMMON.mythic_minPingChance.get();
            maxPing = DragonseekerConfig.COMMON.mythic_maxPingChance.get();

            //Ping characteristic stats
            minSig = DragonseekerConfig.COMMON.mythic_pingCapRadius.get();
            pow = DragonseekerConfig.COMMON.mythic_sigPower.get();
            minVol = DragonseekerConfig.COMMON.mythic_minVol.get().floatValue();
            maxVol = DragonseekerConfig.COMMON.mythic_maxVol.get().floatValue();
            minPitch = DragonseekerConfig.COMMON.mythic_minPitch.get().floatValue();
            maxPitch = DragonseekerConfig.COMMON.mythic_maxPitch.get().floatValue();

            //Other stats
            detectsCorpses = DragonseekerConfig.COMMON.mythic_detectsCorpses.get();
            detectsTame = DragonseekerConfig.COMMON.mythic_detectsTame.get();
            isDefault = false;
        }
    }

}
