package com.feed_the_beast.ftbu;

import com.feed_the_beast.ftbl.api.EnumReloadType;
import com.feed_the_beast.ftbl.api.FTBLibAPI;
import com.feed_the_beast.ftbl.api.FTBLibPlugin;
import com.feed_the_beast.ftbl.api.IFTBLibClientRegistry;
import com.feed_the_beast.ftbl.api.IFTBLibPlugin;
import com.feed_the_beast.ftbl.api.IFTBLibRegistry;
import com.feed_the_beast.ftbl.lib.EnumEnabled;
import com.feed_the_beast.ftbl.lib.config.PropertyDouble;
import com.feed_the_beast.ftbl.lib.config.PropertyEnum;
import com.feed_the_beast.ftbl.lib.config.PropertyShort;
import com.feed_the_beast.ftbl.lib.config.PropertyString;
import com.feed_the_beast.ftbl.lib.util.LMUtils;
import com.feed_the_beast.ftbu.api_impl.ChunkloaderType;
import com.feed_the_beast.ftbu.api_impl.LoadedChunkStorage;
import com.feed_the_beast.ftbu.client.FTBUActions;
import com.feed_the_beast.ftbu.client.FTBUClientConfig;
import com.feed_the_beast.ftbu.cmd.FTBUCommands;
import com.feed_the_beast.ftbu.config.FTBUConfigBackups;
import com.feed_the_beast.ftbu.config.FTBUConfigCommands;
import com.feed_the_beast.ftbu.config.FTBUConfigGeneral;
import com.feed_the_beast.ftbu.config.FTBUConfigLogin;
import com.feed_the_beast.ftbu.config.FTBUConfigRanks;
import com.feed_the_beast.ftbu.config.FTBUConfigWebAPI;
import com.feed_the_beast.ftbu.config.FTBUConfigWorld;
import com.feed_the_beast.ftbu.config.PropertyChatSubstituteList;
import com.feed_the_beast.ftbu.ranks.Ranks;
import com.feed_the_beast.ftbu.world.FTBUPlayerData;
import com.feed_the_beast.ftbu.world.FTBUTeamData;
import com.feed_the_beast.ftbu.world.FTBUUniverseData;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.command.CommandTreeBase;

import java.io.File;
import java.util.Collections;

/**
 * Created by LatvianModder on 20.09.2016.
 */
public enum FTBLibIntegration implements IFTBLibPlugin
{
    @FTBLibPlugin
    INSTANCE;

    public static FTBLibAPI API;
    public static final ResourceLocation FTBU_DATA = FTBUFinals.get("data");

    @Override
    public void init(FTBLibAPI api)
    {
        API = api;
    }

    @Override
    public void onReload(Side side, ICommandSender sender, EnumReloadType type)
    {
        if(side.isServer())
        {
            Ranks.reload();
            ServerInfoPage.reloadCachedInfo();

            FTBUUniverseData.reloadServerBadges();
            LoadedChunkStorage.INSTANCE.checkAll();
        }
        else
        {
            FTBU.PROXY.onReloadedClient();
        }
    }

    @Override
    public void registerCommon(IFTBLibRegistry reg)
    {
        reg.addOptionalServerMod(FTBUFinals.MOD_ID);
        reg.addConfigFileProvider(FTBUFinals.MOD_ID, () -> new File(LMUtils.folderLocal, "ftbu/config.json"));
        reg.addConfigValueProvider(PropertyChatSubstituteList.ID, () -> new PropertyChatSubstituteList(Collections.emptyMap()));

        FTBUConfigBackups.init(reg);
        FTBUConfigCommands.init(reg);
        FTBUConfigGeneral.init(reg);
        FTBUConfigLogin.init(reg);
        FTBUConfigWebAPI.init(reg);
        FTBUConfigWorld.init(reg);
        FTBUConfigRanks.init(reg);

        reg.addNotification(FTBUNotifications.NO_TEAM);
        reg.addNotification(FTBUNotifications.CANT_MODIFY_CHUNK);
        reg.addNotification(FTBUNotifications.CLAIMING_NOT_ENABLED);
        reg.addNotification(FTBUNotifications.CLAIMING_NOT_ALLOWED);
        reg.addNotification(FTBUNotifications.UNCLAIMED_ALL);
        reg.addNotification(FTBUNotifications.CHUNK_CLAIMED);
        reg.addNotification(FTBUNotifications.CHUNK_UNCLAIMED);
        reg.addNotification(FTBUNotifications.CHUNK_LOADED);
        reg.addNotification(FTBUNotifications.CHUNK_UNLOADED);
        reg.addNotification(FTBUNotifications.WILDERNESS);

        reg.addUniverseDataProvider(FTBU_DATA, owner -> new FTBUUniverseData());
        reg.addPlayerDataProvider(FTBU_DATA, owner -> new FTBUPlayerData());
        reg.addTeamDataProvider(FTBU_DATA, owner -> new FTBUTeamData());

        reg.addRankConfig(FTBUPermissions.BADGE, new PropertyString(""), new PropertyString(""), "Prefix of player's nickname");
        reg.addRankConfig(FTBUPermissions.HOMES_MAX, new PropertyShort(1, 0, 30000), new PropertyShort(100), "Max home count");
        reg.addRankConfig(FTBUPermissions.CLAIMS_MAX_CHUNKS, new PropertyShort(100, 0, 30000), new PropertyShort(1000), "Max amount of chunks that player can claim", "0 - Disabled");
        reg.addRankConfig(FTBUPermissions.CLAIMS_FORCED_EXPLOSIONS, new PropertyEnum<>(EnumEnabled.NAME_MAP_WITH_NULL, null), new PropertyEnum<>(EnumEnabled.NAME_MAP_WITH_NULL, null), "-: Player setting", "disabled: Explosions will never happen in claimed chunks", "enabled: Explosions will always happen in claimed chunks");
        reg.addRankConfig(FTBUPermissions.CHUNKLOADER_TYPE, new PropertyEnum<>(ChunkloaderType.NAME_MAP, ChunkloaderType.OFFLINE), new PropertyEnum<>(ChunkloaderType.NAME_MAP, ChunkloaderType.OFFLINE), "disabled: Players won't be able to chunkload", "offline: Chunks stay loaded when player loggs off", "online: Chunks only stay loaded while owner is online");
        reg.addRankConfig(FTBUPermissions.CHUNKLOADER_MAX_CHUNKS, new PropertyShort(50, 0, 30000), new PropertyShort(64), "Max amount of chunks that player can load", "0 - Disabled");
        reg.addRankConfig(FTBUPermissions.CHUNKLOADER_OFFLINE_TIMER, new PropertyDouble(-1D).setMin(-1D), new PropertyDouble(-1D), "Max hours player can be offline until he's chunks unload", "0 - Disabled, will unload instantly when he disconnects", "-1 - Chunk will always be loaded");
    }

    @Override
    public void registerClient(IFTBLibClientRegistry reg)
    {
        reg.addClientConfig(FTBUFinals.MOD_ID, "render_badges", FTBUClientConfig.RENDER_BADGES);
        reg.addClientConfig(FTBUFinals.MOD_ID, "journeymap_overlay", FTBUClientConfig.JOURNEYMAP_OVERLAY);

        reg.addSidebarButton(FTBUActions.GUIDE);
        reg.addSidebarButton(FTBUActions.SERVER_INFO);
        reg.addSidebarButton(FTBUActions.CLAIMED_CHUNKS);
        reg.addSidebarButton(FTBUActions.TRASH_CAN);
        reg.addSidebarButton(FTBUActions.SHOP);

        if(!LMUtils.isNEILoaded)
        {
            reg.addSidebarButton(FTBUActions.HEAL);
            reg.addSidebarButton(FTBUActions.TOGGLE_GAMEMODE);
            reg.addSidebarButton(FTBUActions.TOGGLE_RAIN);
            reg.addSidebarButton(FTBUActions.TOGGLE_DAY);
            reg.addSidebarButton(FTBUActions.TOGGLE_NIGHT);
        }
    }

    @Override
    public void registerFTBCommands(CommandTreeBase command, boolean dedi)
    {
        FTBUCommands.register(command, dedi);
    }
}