package kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.standalone;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.trainer.TrainerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.exception.BattleStartException;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.MessagePredicate;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.BaseTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerBattle;
import kiwiapollo.cobblemontrainerbattle.parser.history.BattleRecord;
import kiwiapollo.cobblemontrainerbattle.parser.history.PlayerHistoryManager;

import java.util.UUID;

public class StandaloneTrainerBattle implements TrainerBattle {
    private final BaseTrainerBattle battle;

    public StandaloneTrainerBattle(
            PlayerBattleParticipant player,
            TrainerBattleParticipant trainer
    ) {
        this.battle = new BaseTrainerBattle(player, trainer);
    }

    @Override
    public void start() throws BattleStartException {
        for (MessagePredicate<PlayerBattleParticipant> predicate : getTrainer().getPredicates()) {
            if (!predicate.test(getPlayer())) {
                getPlayer().sendErrorMessage(predicate.getErrorMessage());
                throw new BattleStartException();
            }
        }

        battle.start();

        setBattleTheme();
    }

    private void setBattleTheme() {
        if (getTrainer().getBattleTheme().isPresent()) {
            PokemonBattle battle = Cobblemon.INSTANCE.getBattleRegistry().getBattle(getBattleId());
            PlayerBattleActor player = (PlayerBattleActor) battle.getActor(getPlayer().getPlayerEntity());

            player.setBattleTheme(getTrainer().getBattleTheme().get());
        }
    }

    @Override
    public void onPlayerVictory() {
        getPlayer().onVictory();
        getTrainer().onDefeat();
        updateVictoryRecord();
    }

    @Override
    public void onPlayerDefeat() {
        getPlayer().onDefeat();
        getTrainer().onVictory();
        updateDefeatRecord();
    }

    private BattleRecord getBattleRecord() {
        return (BattleRecord) PlayerHistoryManager.getPlayerHistory(getPlayer().getUuid()).getOrCreateRecord(getTrainer().getIdentifier());
    }

    private void updateVictoryRecord() {
        getBattleRecord().setVictoryCount(getBattleRecord().getVictoryCount() + 1);
    }

    private void updateDefeatRecord() {
        getBattleRecord().setDefeatCount(getBattleRecord().getDefeatCount() + 1);
    }

    @Override
    public UUID getBattleId() {
        return battle.getBattleId();
    }

    @Override
    public PlayerBattleParticipant getPlayer() {
        return battle.getPlayer();
    }

    @Override
    public TrainerBattleParticipant getTrainer() {
        return battle.getTrainer();
    }
}
