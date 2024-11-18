package kiwiapollo.cobblemontrainerbattle.economy;

import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import net.minecraft.server.network.ServerPlayerEntity;

public class NullEconomy implements Economy {
    public NullEconomy() {
        CobblemonTrainerBattle.LOGGER.info("Loaded NullEconomy");
    }

    @Override
    public double getBalance(ServerPlayerEntity player) {
        return 0;
    }

    @Override
    public void addBalance(ServerPlayerEntity player, double amount) {

    }

    @Override
    public void removeBalance(ServerPlayerEntity player, double amount) {

    }

    @Override
    public boolean isExistEnoughBalance(ServerPlayerEntity player, double amount) {
        return true;
    }

    @Override
    public String getNotEnoughBalanceMessage() {
        return "";
    }
}