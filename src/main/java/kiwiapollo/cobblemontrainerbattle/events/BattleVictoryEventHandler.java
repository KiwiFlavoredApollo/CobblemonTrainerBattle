package kiwiapollo.cobblemontrainerbattle.events;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battlefrontier.BattleFrontier;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.Trainer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class BattleVictoryEventHandler {
    public void run(BattleVictoryEvent battleVictoryEvent) {
        handleTrainerBattleVictoryEvent(battleVictoryEvent);
        handleBattleFrontierBattleVictoryEvent(battleVictoryEvent);
    }

    private void handleTrainerBattleVictoryEvent(BattleVictoryEvent battleVictoryEvent) {
        List<UUID> battleIds = CobblemonTrainerBattle.TRAINER_BATTLES.values().stream().map(PokemonBattle::getBattleId).toList();
        if (!battleIds.contains(battleVictoryEvent.getBattle().getBattleId())) return;

        ServerPlayerEntity player = battleVictoryEvent.getBattle().getPlayers().get(0);
        BattleActor playerBattleActor = battleVictoryEvent.getBattle().getActor(player);
        String trainerName = StreamSupport.stream(battleVictoryEvent.getBattle().getActors().spliterator(), false)
                .filter(battleActor -> !battleActor.isForPlayer(player))
                .findFirst().get().getName().getString();

        if (battleVictoryEvent.getWinners().contains(playerBattleActor)) {
            CobblemonTrainerBattle.ECONOMY.addBalance(player, CobblemonTrainerBattle.CONFIG.victoryCurrencyAmount);
            CobblemonTrainerBattle.LOGGER.info(String.format(
                    "%s: Victory against %s", player.getGameProfile().getName(), trainerName));

        } else {
            CobblemonTrainerBattle.ECONOMY.addBalance(player, CobblemonTrainerBattle.CONFIG.defeatCurrencyAmount);
            CobblemonTrainerBattle.LOGGER.info(String.format(
                    "%s: Defeated by %s", player.getGameProfile().getName(), trainerName));
        }

        CobblemonTrainerBattle.TRAINER_BATTLES.remove(battleVictoryEvent.getBattle());
    }

    private void handleBattleFrontierBattleVictoryEvent(BattleVictoryEvent battleVictoryEvent) {
        boolean isBattleFrontierBattle = BattleFrontier.SESSIONS.values().stream()
                .map(session -> session.battleUuid)
                .anyMatch(uuid -> uuid == battleVictoryEvent.getBattle().getBattleId());
        if (!isBattleFrontierBattle) return;

        ServerPlayerEntity player = battleVictoryEvent.getBattle().getPlayers().get(0);
        BattleActor playerBattleActor = battleVictoryEvent.getBattle().getActor(player);

        if (battleVictoryEvent.getWinners().contains(playerBattleActor)) {
            handleBattleFrontierPlayerVictoryEvent(battleVictoryEvent);

        } else {
            handleBattleFrontierPlayerDefeatEvent(battleVictoryEvent);
        }
    }

    private void handleBattleFrontierPlayerDefeatEvent(BattleVictoryEvent battleVictoryEvent) {
        ServerPlayerEntity player = battleVictoryEvent.getBattle().getPlayers().get(0);

        BattleFrontier.SESSIONS.get(player.getUuid()).isDefeated = true;
    }

    private void handleBattleFrontierPlayerVictoryEvent(BattleVictoryEvent battleVictoryEvent) {
        ServerPlayerEntity player = battleVictoryEvent.getBattle().getPlayers().get(0);

        BattleFrontier.SESSIONS.get(player.getUuid()).battleCount += 1;
        BattleFrontier.SESSIONS.get(player.getUuid()).defeatedTrainers
                .add(getDefeatedTrainer(battleVictoryEvent));
        BattleFrontier.SESSIONS.get(player.getUuid()).isTradedPokemon = false;
    }

    private Trainer getDefeatedTrainer(BattleVictoryEvent battleVictoryEvent) {
        BattleActor defeatedTrainerBattleActor =
                StreamSupport.stream(battleVictoryEvent.getBattle().getActors().spliterator(), false)
                        .filter(battleActor -> battleActor instanceof TrainerBattleActor).toList().get(0);

        String defeatedTrainerName = defeatedTrainerBattleActor.getName().toString();

        List<Pokemon> defeatedTrainerPokemons = defeatedTrainerBattleActor.getPokemonList().stream()
                .map(BattlePokemon::getOriginalPokemon).toList();

        return new Trainer(
                defeatedTrainerName,
                defeatedTrainerPokemons
        );
    }
}
