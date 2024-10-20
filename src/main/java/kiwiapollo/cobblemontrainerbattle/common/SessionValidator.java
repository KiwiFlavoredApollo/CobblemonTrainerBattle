package kiwiapollo.cobblemontrainerbattle.common;

import kiwiapollo.cobblemontrainerbattle.session.Session;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

public class SessionValidator {
    public static void assertSessionExist(Map<UUID, ? extends Session> sessionRegistry, ServerPlayerEntity player) {
        if (!isSessionExist(sessionRegistry, player)) {
            throw new IllegalStateException();
        }
    }

    public static void assertSessionNotExist(Map<UUID, ? extends Session> sessionRegistry, ServerPlayerEntity player) {
        if (isSessionExist(sessionRegistry, player)) {
            throw new IllegalStateException();
        }
    }

    private static boolean isSessionExist(Map<UUID, ? extends Session> sessionRegistry, ServerPlayerEntity player) {
        return sessionRegistry.containsKey(player.getUuid());
    }
}
