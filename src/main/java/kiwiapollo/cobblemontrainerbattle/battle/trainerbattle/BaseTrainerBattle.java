package kiwiapollo.cobblemontrainerbattle.battle.trainerbattle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.trainer.TrainerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.*;
import kiwiapollo.cobblemontrainerbattle.exception.*;
import kiwiapollo.cobblemontrainerbattle.parser.pokemon.ShowdownPokemonParser;
import kotlin.Unit;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BaseTrainerBattle implements TrainerBattle {
    private final PlayerBattleParticipant player;
    private final TrainerBattleParticipant trainer;

    private UUID battleId;

    public BaseTrainerBattle(
            PlayerBattleParticipant player,
            TrainerBattleParticipant trainer
    ) {
        this.player = player;
        this.trainer = trainer;
    }

    @Override
    public void start() throws BattleStartException {
        List<MessagePredicate<PlayerBattleParticipant>> predicates = List.of(
                new PlayerNotBusyPredicate.PlayerBattleParticipantPredicate(),
                new PlayerPartyNotEmptyPredicate(),
                new PlayerPartyNotFaintedPredicate(),
                new MinimumPartySizePredicate.PlayerPredicate(trainer.getBattleFormat()),
                new MinimumPartyLevelPredicate(ShowdownPokemonParser.MAXIMUM_RELATIVE_LEVEL)
        );

        for (MessagePredicate<PlayerBattleParticipant> predicate : predicates) {
            if (!predicate.test(player)) {
                player.sendErrorMessage(predicate.getErrorMessage());
                throw new BattleStartException();
            }
        }

        Cobblemon.INSTANCE.getStorage()
                .getParty(player.getPlayerEntity()).toGappyList().stream()
                .filter(Objects::nonNull)
                .forEach(Pokemon::recall);

        Cobblemon.INSTANCE.getBattleRegistry().startBattle(
                trainer.getBattleFormat(),
                new BattleSide(player.createBattleActor()),
                new BattleSide(trainer.createBattleActor()),
                false
        ).ifSuccessful(pokemonBattle -> {
            battleId = pokemonBattle.getBattleId();

            ((PlayerBattleActor) pokemonBattle.getActor(player.getPlayerEntity())).setBattleTheme(CobblemonSounds.PVN_BATTLE);

            player.sendInfoMessage(Text.translatable("command.cobblemontrainerbattle.success.trainerbattle", trainer.getName()));
            CobblemonTrainerBattle.LOGGER.info("Started trainer battle : {} versus {}", player.getName(), trainer.getIdentifier());

            return Unit.INSTANCE;
        });
    }

    @Override
    public void onPlayerVictory() {

    }

    @Override
    public void onPlayerDefeat() {

    }

    @Override
    public UUID getBattleId() {
        return battleId;
    }

    @Override
    public PlayerBattleParticipant getPlayer() {
        return player;
    }

    @Override
    public TrainerBattleParticipant getTrainer() {
        return trainer;
    }
}
