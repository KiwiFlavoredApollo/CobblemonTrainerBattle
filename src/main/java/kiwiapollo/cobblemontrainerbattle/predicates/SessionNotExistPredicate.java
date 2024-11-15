package kiwiapollo.cobblemontrainerbattle.predicates;

import kiwiapollo.cobblemontrainerbattle.session.Session;
import kiwiapollo.cobblemontrainerbattle.session.SessionRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class SessionNotExistPredicate implements MessagePredicate<ServerPlayerEntity> {
    private final SessionRegistry<? extends Session> registry;

    public SessionNotExistPredicate(SessionRegistry<? extends Session> registry) {
        this.registry = registry;
    }

    @Override
    public MutableText getMessage() {
        return Text.translatable("command.cobblemontrainerbattle.groupbattle.common.valid_session_not_exist");
    }

    @Override
    public boolean test(ServerPlayerEntity player) {
        return !registry.containsKey(player.getUuid());
    }
}
