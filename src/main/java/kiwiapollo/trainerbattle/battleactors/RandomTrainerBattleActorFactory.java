package kiwiapollo.trainerbattle.battleactors;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.ai.RandomBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kiwiapollo.trainerbattle.common.RandomTrainerFactory;
import kiwiapollo.trainerbattle.common.Trainer;
import kotlin.Unit;

import java.util.UUID;

public class RandomTrainerBattleActorFactory {
    public BattleActor create(int level) {
        Trainer trainer = new RandomTrainerFactory().create();

        return new TrainerBattleActor(
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
                new RandomBattleAI()
        );
    }
}