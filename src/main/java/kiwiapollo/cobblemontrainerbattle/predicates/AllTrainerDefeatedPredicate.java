package kiwiapollo.cobblemontrainerbattle.predicates;

import kiwiapollo.cobblemontrainerbattle.session.Session;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class AllTrainerDefeatedPredicate<T extends Session> implements MessagePredicate<T> {
    @Override
    public MutableText getErrorMessage() {
        return Text.translatable("predicate.cobblemontrainerbattle.error.all_trainer_defeated");
    }

    @Override
    public boolean test(T session) {
        return session.isAllTrainerDefeated();
    }
}
