package kiwiapollo.cobblemontrainerbattle.battleactors.trainer;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.entities.TrainerEntity;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.Trainer;

public class EntityBackedFlatLevelFullHealthTrainerBattleActorFactory {
    public BattleActor create(Trainer trainer, int level, TrainerEntity trainerEntity) {
        trainer.pokemons.forEach(Pokemon::heal);
        trainer.pokemons.forEach(pokemon -> pokemon.setLevel(level));
        return new EntityBackedTrainerBattleActorFactory().create(trainer, trainerEntity);
    }
}