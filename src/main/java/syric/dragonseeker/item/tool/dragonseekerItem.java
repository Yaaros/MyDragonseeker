package syric.dragonseeker.item.tool;

import com.github.alexthe666.iceandfire.IceAndFire;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;

public class dragonseekerItem extends dragonseekerGeneric {

    //Defining statistics
    //Ping chance stats
//    private static final int opDist = DragonseekerConfig.COMMON.basic_optimalDistance.get();
//    private static final int maxDist = DragonseekerConfig.COMMON.basic_maxDistance.get();
//    private static final double minPing = DragonseekerConfig.COMMON.basic_minPingChance.get();
//    private static final double maxPing = DragonseekerConfig.COMMON.basic_maxPingChance.get();

    //Ping characteristic stats
//    private static final int minSig = DragonseekerConfig.COMMON.basic_pingCapRadius.get();
//    private static final double pow = DragonseekerConfig.COMMON.basic_sigPower.get();
//    private static final Double minVol = DragonseekerConfig.COMMON.basic_minVol.get();
//    private static final Double maxVol = DragonseekerConfig.COMMON.basic_maxVol.get();
//    private static final Double minPitch = DragonseekerConfig.COMMON.basic_minPitch.get();
//    private static final Double maxPitch = DragonseekerConfig.COMMON.basic_maxPitch.get();
    private static final SoundEvent negSound = SoundEvents.NOTE_BLOCK_BASS.get();
    private static final SoundEvent pingSound = SoundEvents.NOTE_BLOCK_BASS.get();

    //Other stats
//    private static final boolean detectsCorpses = DragonseekerConfig.COMMON.basic_detectsCorpses.get();
//    private static final boolean detectsTame = DragonseekerConfig.COMMON.basic_detectsTame.get();
//    private static int durability = DragonseekerConfig.COMMON.basic_durability.get();
    private static final int durability = 64;
    private static final Rarity rarity = Rarity.UNCOMMON;
    private static final Item repairItem = IafItemRegistry.DRAGON_BONE.get();
    private static final int seekerType = 1;


    public dragonseekerItem() {
        super(50, 50, .12, .8, 200, 1.5, .05, .05, .5, .8, negSound, pingSound, true, true, durability, rarity, repairItem, seekerType);
    }

}
