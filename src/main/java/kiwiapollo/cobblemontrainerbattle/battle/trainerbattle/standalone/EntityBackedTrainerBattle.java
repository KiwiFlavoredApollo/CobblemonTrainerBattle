package kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.standalone;

import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.NormalBattlePlayer;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.trainer.EntityBackedTrainer;
import kiwiapollo.cobblemontrainerbattle.entity.TrainerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class EntityBackedTrainerBattle extends StandaloneTrainerBattle {
    public EntityBackedTrainerBattle(ServerPlayerEntity player, TrainerEntity entity, Identifier trainer) {
        super(new NormalBattlePlayer(player), new EntityBackedTrainer(trainer, entity, player));
    }
}
