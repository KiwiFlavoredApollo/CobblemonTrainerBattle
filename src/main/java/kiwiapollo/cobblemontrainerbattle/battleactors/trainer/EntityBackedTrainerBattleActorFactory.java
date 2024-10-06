package kiwiapollo.cobblemontrainerbattle.battleactors.trainer;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kiwiapollo.cobblemontrainerbattle.common.Generation5AI;
import kiwiapollo.cobblemontrainerbattle.entities.TrainerEntity;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.Trainer;
import kotlin.Unit;

import java.util.UUID;

public class EntityBackedTrainerBattleActorFactory {
    public BattleActor create(Trainer trainer, TrainerEntity trainerEntity) {
        return new EntityBackedTrainerBattleActor(
                trainer.name,
                UUID.randomUUID(),
                trainer.pokemons.stream()
                        .map(pokemon -> new BattlePokemon(
                                pokemon,
                                pokemon,
                                pokemonEntity -> {
                                    pokemonEntity.discard();
                                    return Unit.INSTANCE;
                                }
                        ))
                        .toList(),
                new Generation5AI(),
                trainerEntity
        );
    }
}