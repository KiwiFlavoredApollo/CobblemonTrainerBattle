package kiwiapollo.cobblemontrainerbattle.common;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.exceptions.*;
import kiwiapollo.cobblemontrainerbattle.parsers.SmogonPokemonParser;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.stream.Stream;

public class PlayerValidator {
    private final ServerPlayerEntity player;

    public PlayerValidator(ServerPlayerEntity player) {
        this.player = player;
    }

    public void assertNotEmptyPlayerParty()
            throws EmptyPlayerPartyException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);

        if (playerPartyStore.toGappyList().stream().allMatch(Objects::isNull)) {
            throw new EmptyPlayerPartyException();
        }
    }

    public void assertNotFaintPlayerParty()
            throws FaintedPlayerPartyException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        Stream<Pokemon> pokemons = playerPartyStore.toGappyList().stream().filter(Objects::nonNull);

        if (pokemons.allMatch(Pokemon::isFainted)) {
            throw new FaintedPlayerPartyException();
        }
    }

    public void assertPlayerNotBusyWithPokemonBattle()
            throws BusyWithPokemonBattleException {
        if (Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null) {
            throw new BusyWithPokemonBattleException();
        }
    }

    public void assertPlayerPartyAtOrAboveRelativeLevelThreshold()
            throws BelowRelativeLevelThresholdException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        Stream<Pokemon> pokemons = playerPartyStore.toGappyList().stream().filter(Objects::nonNull);

        if (pokemons.map(Pokemon::getLevel).allMatch(level -> level < SmogonPokemonParser.RELATIVE_LEVEL_THRESHOLD)) {
            throw new BelowRelativeLevelThresholdException();
        }
    }

    public void assertSatisfiedBattleCondition(Identifier identifier)
            throws BattleConditionException {
        assertSatisfiedMinimumLevelBattleCondition(identifier);
        assertSatisfiedMaximumLevelBattleCondition(identifier);
    }

    private void assertSatisfiedMaximumLevelBattleCondition(Identifier identifier)
            throws BattleConditionException {
        try {
            Trainer trainer = CobblemonTrainerBattle.trainers.get(identifier);
            PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
            boolean isAtOrBelowMaximumPartyLevel = playerPartyStore.toGappyList().stream()
                    .filter(Objects::nonNull)
                    .map(Pokemon::getLevel)
                    .allMatch(level -> level <= trainer.condition.maximumPartyLevel);

            if (!isAtOrBelowMaximumPartyLevel) {
                throw new BattleConditionException(
                        BattleConditionType.MAXIMUM_PARTY_LEVEL,
                        trainer.condition.maximumPartyLevel
                );
            }

        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException ignored) {

        }
    }

    private void assertSatisfiedMinimumLevelBattleCondition(Identifier identifier)
            throws BattleConditionException {
        try {
            Trainer trainer = CobblemonTrainerBattle.trainers.get(identifier);
            PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
            boolean isAtOrAboveMinimumPartyLevel = playerPartyStore.toGappyList().stream()
                    .filter(Objects::nonNull)
                    .map(Pokemon::getLevel)
                    .allMatch(level -> level >= trainer.condition.minimumPartyLevel);

            if (!isAtOrAboveMinimumPartyLevel) {
                throw new BattleConditionException(
                        BattleConditionType.MINIMUM_PARTY_LEVEL,
                        trainer.condition.minimumPartyLevel
                );
            }

        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException ignored) {

        }
    }
}