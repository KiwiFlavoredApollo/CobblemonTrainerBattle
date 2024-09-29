package kiwiapollo.cobblemontrainerbattle.trainerbattle;

import java.nio.file.Path;
import java.util.Random;

public class InclementEmeraldRandomTrainerFactory {
    TrainerFileScanner trainerFileScanner;

    public InclementEmeraldRandomTrainerFactory() {
        this.trainerFileScanner = new InclementEmeraldTrainerFileScanner();
    }

    public Trainer create() {
        return new TrainerFileParser().parse(getRandomTrainerFile());
    }

    private Path getRandomTrainerFile() {
        int random = new Random().nextInt(trainerFileScanner.getTrainerFiles().size() - 1);
        return trainerFileScanner.getTrainerFiles().get(random);
    }
}