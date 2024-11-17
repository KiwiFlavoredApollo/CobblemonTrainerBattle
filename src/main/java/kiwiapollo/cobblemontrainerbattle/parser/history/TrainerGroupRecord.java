package kiwiapollo.cobblemontrainerbattle.parser.history;

import net.minecraft.nbt.NbtCompound;

import java.time.Instant;

public class TrainerGroupRecord implements PlayerHistoryRecord, BattleRecord {
    private int victory;
    private int defeat;
    private Instant timestamp;

    public TrainerGroupRecord() {
        this.victory = 0;
        this.defeat = 0;
        this.timestamp = Instant.now();
    }

    @Override
    public int getVictoryCount() {
        return victory;
    }

    @Override
    public void setVictoryCount(int count) {
        victory = count;
    }

    @Override
    public int getDefeatCount() {
        return defeat;
    }

    @Override
    public void setDefeatCount(int count) {
        defeat = count;
    }

    @Override
    public void updateTimestamp() {
        timestamp = Instant.now();
    }

    @Override
    public NbtCompound writeToNbt(NbtCompound nbt) {
        nbt.putLong("timestamp", timestamp.toEpochMilli());
        nbt.putInt("victory", victory);
        nbt.putInt("defeat", defeat);

        return nbt;
    }

    @Override
    public TrainerGroupRecord readFromNbt(NbtCompound nbt) {
        timestamp = Instant.ofEpochMilli(nbt.getLong("timestamp"));
        victory = nbt.getInt("victory");
        defeat = nbt.getInt("defeat");

        return this;
    }
}