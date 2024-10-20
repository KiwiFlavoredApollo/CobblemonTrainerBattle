package kiwiapollo.cobblemontrainerbattle.postbattle;

import com.mojang.brigadier.CommandDispatcher;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PostBattleActionSetHandler implements BattleResultHandler {
    private final ServerPlayerEntity player;
    private final PostBattleActionSet victory;
    private final PostBattleActionSet defeat;

    public PostBattleActionSetHandler(ServerPlayerEntity player, PostBattleActionSet victory, PostBattleActionSet defeat) {
        this.player = player;
        this.victory = victory;
        this.defeat = defeat;
    }

    @Override
    public void onVictory() {
        CobblemonTrainerBattle.economy.addBalance(player, victory.balance);
        victory.commands.forEach(this::executeCommand);
    }

    @Override
    public void onDefeat() {
        CobblemonTrainerBattle.economy.removeBalance(player, defeat.balance);
        defeat.commands.forEach(this::executeCommand);
    }

    private void executeCommand(String command) {
        command = command.replace("%player%", player.getGameProfile().getName());

        MinecraftServer server = player.getCommandSource().getServer();
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();

        server.getCommandManager().execute(dispatcher.parse(command, server.getCommandSource()), command);
    }
}
