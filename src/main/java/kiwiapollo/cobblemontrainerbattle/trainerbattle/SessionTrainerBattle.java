package kiwiapollo.cobblemontrainerbattle.trainerbattle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonSounds;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.trainer.TrainerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.exception.*;
import kiwiapollo.cobblemontrainerbattle.predicates.MessagePredicate;
import kiwiapollo.cobblemontrainerbattle.session.Session;

import java.util.UUID;

public class SessionTrainerBattle implements TrainerBattle {
    private final BaseTrainerBattle battle;
    private final Session session;

    public SessionTrainerBattle(
            PlayerBattleParticipant player,
            TrainerBattleParticipant trainer,
            Session session
    ) {
        this.battle = new BaseTrainerBattle(player, trainer);
        this.session = session;
    }

    @Override
    public void start() throws BattleStartException {
        for (MessagePredicate<PlayerBattleParticipant> predicate : session.getBattlePredicates()) {
            if (!predicate.test(getPlayer())) {
                getPlayer().sendErrorMessage(predicate.getErrorMessage());
                throw new BattleStartException();
            }
        }

        battle.start();

        setBattleTheme();
    }

    private void setBattleTheme() {
        try {
            PokemonBattle battle =  Cobblemon.INSTANCE.getBattleRegistry().getBattle(getBattleId());
            PlayerBattleActor player = (PlayerBattleActor) battle.getActor(getPlayer().getPlayerEntity());
            player.setBattleTheme(CobblemonSounds.PVN_BATTLE);
        } catch (NullPointerException ignored) {

        }
    }

    @Override
    public void onPlayerVictory() {
        getPlayer().onVictory();
        session.onBattleVictory();
    }

    @Override
    public void onPlayerDefeat() {
        getPlayer().onDefeat();
        session.onBattleDefeat();
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
