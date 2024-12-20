package kiwiapollo.cobblemontrainerbattle.battle.groupbattle;

import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.factory.SessionBattleParticipantFactory;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerBattleStorage;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.session.SessionTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.parser.history.BattleRecord;
import kiwiapollo.cobblemontrainerbattle.parser.history.PlayerHistoryManager;
import kiwiapollo.cobblemontrainerbattle.parser.profile.TrainerGroupProfileStorage;
import kiwiapollo.cobblemontrainerbattle.battle.postbattle.DefeatActionSetHandler;
import kiwiapollo.cobblemontrainerbattle.battle.postbattle.VictoryActionSetHandler;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.AnyTrainerNotDefeatedPredicate;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.MessagePredicate;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.PlayerNotDefeatedPredicate;
import kiwiapollo.cobblemontrainerbattle.exception.BattleStartException;
import kiwiapollo.cobblemontrainerbattle.battle.session.Session;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class GroupBattleSession implements Session {
    private final ServerPlayerEntity player;
    private final Identifier group;
    private final SessionBattleParticipantFactory factory;

    private final VictoryActionSetHandler sessionVictoryHandler;
    private final DefeatActionSetHandler sessionDefeatHandler;
    private final SoundEvent battleTheme;

    private TrainerBattle lastTrainerBattle;
    private int streak;
    private boolean isPlayerDefeated;

    public GroupBattleSession(ServerPlayerEntity player, Identifier group, SessionBattleParticipantFactory factory) {
        this.player = player;
        this.group = group;
        this.factory = factory;

        TrainerGroupProfile profile = TrainerGroupProfileStorage.getProfileRegistry().get(group);
        this.sessionVictoryHandler = new VictoryActionSetHandler(player, profile.onVictory);
        this.sessionDefeatHandler = new DefeatActionSetHandler(player, profile.onDefeat);
        this.battleTheme = profile.battleTheme;

        this.streak = 0;
        this.isPlayerDefeated = false;
    }

    @Override
    public void startBattle() throws BattleStartException {
        List<MessagePredicate<GroupBattleSession>> predicates = List.of(
                new PlayerNotDefeatedPredicate<>(),
                new AnyTrainerNotDefeatedPredicate<>()
        );

        for (MessagePredicate<GroupBattleSession> predicate: predicates) {
            if (!predicate.test(this)) {
                player.sendMessage(predicate.getErrorMessage().formatted(Formatting.RED));
                throw new BattleStartException();
            }
        }

        TrainerBattle trainerBattle = new SessionTrainerBattle(
                factory.createPlayer(this),
                factory.createTrainer(this),
                this
        );
        trainerBattle.start();

        TrainerBattleStorage.getTrainerBattleRegistry().put(player.getUuid(), trainerBattle);

        this.lastTrainerBattle = trainerBattle;
    }

    @Override
    public void onBattleVictory() {
        streak += 1;
    }

    @Override
    public void onBattleDefeat() {
        isPlayerDefeated = true;
    }

    @Override
    public void onSessionStop() {
        if (isAllTrainerDefeated()) {
            sessionVictoryHandler.run();
            updateVictoryRecord();
        } else {
            sessionDefeatHandler.run();
            updateDefeatRecord();
        }
    }

    private BattleRecord getBattleRecord() {
        return (BattleRecord) PlayerHistoryManager.getPlayerHistory(player.getUuid()).getOrCreateRecord(group);
    }

    private void updateVictoryRecord() {
        getBattleRecord().setVictoryCount(getBattleRecord().getVictoryCount() + 1);
    }

    private void updateDefeatRecord() {
        getBattleRecord().setDefeatCount(getBattleRecord().getDefeatCount() + 1);
    }

    @Override
    public int getStreak() {
        return streak;
    }

    @Override
    public List<MessagePredicate<PlayerBattleParticipant>> getBattlePredicates() {
        return List.of();
    }

    @Override
    public boolean isPlayerDefeated() {
        return isPlayerDefeated;
    }

    @Override
    public boolean isAllTrainerDefeated() {
        try {
            factory.createTrainer(this);
            return false;

        } catch (IllegalStateException e ) {
            return true;
        }
    }

    @Override
    public boolean isAnyTrainerDefeated() {
        return streak > 0;
    }

    @Override
    public Optional<SoundEvent> getBattleTheme() {
        return Optional.ofNullable(battleTheme);
    }
}
