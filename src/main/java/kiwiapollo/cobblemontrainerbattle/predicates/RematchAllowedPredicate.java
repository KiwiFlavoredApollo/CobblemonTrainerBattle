package kiwiapollo.cobblemontrainerbattle.predicates;

import kiwiapollo.cobblemontrainerbattle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.parser.history.PlayerHistoryManager;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RematchAllowedPredicate implements MessagePredicate<PlayerBattleParticipant> {
    private final Identifier opponent;
    private final boolean isRematchAllowed;

    public RematchAllowedPredicate(Identifier opponent, boolean isRematchAllowed) {
        this.opponent = opponent;
        this.isRematchAllowed = isRematchAllowed;
    }

    @Override
    public MutableText getMessage() {
        return Text.translatable("command.cobblemontrainerbattle.condition.is_rematch_allowed_after_victory.trainerbattle");
    }

    @Override
    public boolean test(PlayerBattleParticipant player) {
        return isRematchAllowed && !PlayerHistoryManager.get(player.getUuid()).isOpponentDefeated(opponent);
    }
}