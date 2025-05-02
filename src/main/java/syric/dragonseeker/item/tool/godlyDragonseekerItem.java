package syric.dragonseeker.item.tool;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

public class godlyDragonseekerItem extends dragonseekerGeneric {

    //Defining statistics
    //Ping chance stats
//    private static final int opDist = DragonseekerConfig.COMMON.mythic_optimalDistance.get();
//    private static final int maxDist = DragonseekerConfig.COMMON.mythic_maxDistance.get();
//    private static final double minPing = DragonseekerConfig.COMMON.mythic_minPingChance.get();
//    private static final double maxPing = DragonseekerConfig.COMMON.mythic_maxPingChance.get();

    private static final SoundEvent negSound = SoundEvents.NOTE_BLOCK_BASS.get();
    private static final SoundEvent pingSound = SoundEvents.EXPERIENCE_ORB_PICKUP;

    //Other stats
//    private static final boolean detectsCorpses = DragonseekerConfig.COMMON.mythic_detectsCorpses.get();
//    private static final boolean detectsTame = DragonseekerConfig.COMMON.mythic_detectsTame.get();
//    private static final int durability = DragonseekerConfig.COMMON.mythic_durability.get();
    private static final int durability = -1;
    private static final Rarity rarity = Rarity.EPIC;
    private static final Item repairItem = Items.NETHERITE_INGOT;
    private static final int seekerType = 4;

    //Constructor
    public godlyDragonseekerItem() {
        super(400, 400, 0, 1, 200, 3.5, 0.05, 1, 0.5, 1, negSound, pingSound, false, false, durability, rarity, repairItem, seekerType);
    }

}
