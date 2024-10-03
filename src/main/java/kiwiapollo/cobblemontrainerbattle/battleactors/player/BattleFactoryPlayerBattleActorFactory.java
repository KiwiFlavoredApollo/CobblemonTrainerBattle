package kiwiapollo.cobblemontrainerbattle.battleactors.player;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.battlefactory.BattleFactory;
import kiwiapollo.cobblemontrainerbattle.battlefactory.BattleFactorySession;
import kotlin.Unit;
import net.minecraft.server.network.ServerPlayerEntity;

public class BattleFactoryPlayerBattleActorFactory {
    public BattleActor create(ServerPlayerEntity player) {
        BattleFactorySession session = BattleFactory.SESSIONS.get(player.getUuid());
        session.partyPokemons.forEach(Pokemon::heal);
        session.partyPokemons.forEach(pokemon -> pokemon.setLevel(BattleFactory.LEVEL));

        return new PlayerBattleActor(
                player.getUuid(),
                session.partyPokemons.stream()
                        .map(pokemon -> new BattlePokemon(
                                pokemon,
                                pokemon.clone(true, true),
                                pokemonEntity -> {
                                    pokemonEntity.discard();
                                    return Unit.INSTANCE;
                                }
                        ))
                        .toList()
        );
    }
}