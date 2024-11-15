package kiwiapollo.cobblemontrainerbattle.trainerbattle;

import kiwiapollo.cobblemontrainerbattle.battleparticipant.player.NormalBattlePlayer;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.trainer.NormalBattleTrainer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class StandaloneNormalTrainerBattle extends StandaloneTrainerBattle {
    public StandaloneNormalTrainerBattle(ServerPlayerEntity player, Identifier trainer) {
        super(new NormalBattlePlayer(player), new NormalBattleTrainer(trainer, player));
    }
}