package kiwiapollo.cobblemontrainerbattle.resulthandler;

import java.util.function.Supplier;

public class SessionBattleResultHandler implements ResultHandler {
    private final Runnable onVictory;
    private final Runnable onDefeat;

    public SessionBattleResultHandler(Runnable onVictory, Runnable onDefeat) {
        this.onVictory = onVictory;
        this.onDefeat = onDefeat;
    }

    @Override
    public void onVictory() {
        onVictory.run();
    }

    @Override
    public void onDefeat() {
        onDefeat.run();
    }
}
