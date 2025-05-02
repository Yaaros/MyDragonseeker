package syric.dragonseeker.registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import syric.dragonseeker.Dragonseeker;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = Dragonseeker.MODID)
public class TeamManager {
    private static final String DATA_NAME = "dragonseeker_team_init";

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        MinecraftServer server = Objects.requireNonNull(level.getServer());
        ServerLevel overworld = server.overworld().getLevel();

        // 只在主世界处理一次
        TeamInitData data = overworld.getDataStorage().computeIfAbsent(
                TeamInitData::load,
                TeamInitData::new,
                DATA_NAME
        );

        if (!data.initialized) {
            Scoreboard scoreboard = server.getScoreboard();
            addSCB(scoreboard);

            data.initialized = true;
            data.setDirty(); // 脏数据
        }
    }
    private static void addSCB(Scoreboard scoreboard) {
        // 创建红队
        PlayerTeam redTeam = scoreboard.addPlayerTeam("red_iaf");
        redTeam.setColor(net.minecraft.ChatFormatting.RED);

        // 创建青队
        PlayerTeam aquaTeam = scoreboard.addPlayerTeam("aqua_iaf");
        aquaTeam.setColor(net.minecraft.ChatFormatting.AQUA);

        // 创建紫队
        PlayerTeam purpleTeam = scoreboard.addPlayerTeam("lightning_iaf");
        purpleTeam.setColor(net.minecraft.ChatFormatting.LIGHT_PURPLE);
    }

    public static class TeamInitData extends SavedData {
        private boolean initialized = false;

        public static TeamInitData load(CompoundTag tag) {
            TeamInitData data = new TeamInitData();
            data.initialized = tag.getBoolean("Initialized");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("Initialized", initialized);
            return tag;
        }
    }
}
