package kiwiapollo.cobblemontrainerbattle.battle.predicates;

import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.PlayerBattleParticipant;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ForbiddenHeldItemNotExistPredicate extends HeldItemPredicate {
    private final List<ItemStack> forbidden;
    private Text error;

    public ForbiddenHeldItemNotExistPredicate(List<ItemStack> forbidden) {
        this.forbidden = forbidden.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public MutableText getErrorMessage() {
        return Text.translatable("predicate.cobblemontrainerbattle.error.forbidden_held_item_not_exist", error);
    }

    @Override
    public boolean test(PlayerBattleParticipant player) {
        Set<ItemStack> party = player.getParty().toGappyList().stream()
                .filter(Objects::nonNull)
                .map(Pokemon::heldItem)
                .collect(Collectors.toSet());

        for (ItemStack f : forbidden) {
            if (containsItemStack(party, f)) {
                error = f.getName();
                return false;
            }
        }
        return true;
    }
}
