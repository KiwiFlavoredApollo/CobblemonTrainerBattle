package kiwiapollo.cobblemontrainerbattle.predicates;

import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.player.PlayerBattleParticipant;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Objects;

public class MaximumPartyLevelPredicate implements MessagePredicate<PlayerBattleParticipant> {
    private final int maximum;

    public MaximumPartyLevelPredicate(int maximum) {
        this.maximum = maximum;
    }

    @Override
    public MutableText getMessage() {
        return Text.translatable("command.cobblemontrainerbattle.condition.maximum_party_level", maximum);
    }

    @Override
    public boolean test(PlayerBattleParticipant player) {
        return player.getParty().toGappyList().stream()
                .filter(Objects::nonNull)
                .map(Pokemon::getLevel)
                .allMatch(level -> level <= maximum);
    }
}