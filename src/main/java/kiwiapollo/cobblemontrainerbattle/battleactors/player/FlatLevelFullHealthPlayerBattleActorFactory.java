package kiwiapollo.cobblemontrainerbattle.battleactors.player;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FlatLevelFullHealthPlayerBattleActorFactory {
    public BattleActor create(ServerPlayerEntity player, int level) {
        PartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        UUID leadingPokemon = playerPartyStore.toGappyList().stream()
                .filter(Objects::nonNull)
                .findFirst().get().getUuid();

        List<BattlePokemon> playerParty = playerPartyStore.toBattleTeam(true, true, leadingPokemon);
        playerParty.stream().map(BattlePokemon::getEffectedPokemon).forEach(Pokemon::heal);
        playerParty.stream().map(BattlePokemon::getEffectedPokemon).forEach(pokemon -> pokemon.setLevel(level));
        return new PlayerBattleActor(
                player.getUuid(),
                playerParty
        );
    }
}