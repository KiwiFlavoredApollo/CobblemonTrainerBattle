package kiwiapollo.cobblemontrainerbattle.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerBattleStorage;
import kiwiapollo.cobblemontrainerbattle.common.GlobalRandomTrainerFactory;
import kiwiapollo.cobblemontrainerbattle.exception.BattleStartException;
import kiwiapollo.cobblemontrainerbattle.parser.profile.TrainerProfileStorage;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.MessagePredicate;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.ProfileExistPredicate;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.standalone.StandaloneFlatTrainerBattle;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

public class TrainerBattleFlatOtherCommand extends LiteralArgumentBuilder<ServerCommandSource> {
    public TrainerBattleFlatOtherCommand() {
        super("trainerbattleflatother");

        List<String> permissions = List.of(
                String.format("%s.%s.%s", CobblemonTrainerBattle.MOD_ID, getLiteral(), "trainer"),
                String.format("%s.%s.%s", CobblemonTrainerBattle.MOD_ID, getLiteral(), "random")
        );

        this.requires(new MultiCommandSourcePredicate(permissions.toArray(String[]::new)))
                .then(getSelectedTrainerBattleCommand())
                .then(getRandomTrainerBattleCommand());
    }

    private ArgumentBuilder<ServerCommandSource, ?> getSelectedTrainerBattleCommand() {
        String permission = String.format("%s.%s.%s", CobblemonTrainerBattle.MOD_ID, getLiteral(), "trainer");
        return RequiredArgumentBuilder.<ServerCommandSource, EntitySelector>argument("player", EntityArgumentType.player())
                .requires(new MultiCommandSourcePredicate(permission))
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("trainer", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            TrainerProfileStorage.getProfileRegistry().keySet().stream()
                                    .map(Identifier::toString)
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::makePlayerStartBattleWithSelectedTrainer));
    }

    private ArgumentBuilder<ServerCommandSource, ?> getRandomTrainerBattleCommand() {
        String permission = String.format("%s.%s.%s", CobblemonTrainerBattle.MOD_ID, getLiteral(), "random");
        return RequiredArgumentBuilder.<ServerCommandSource, EntitySelector>argument("player", EntityArgumentType.player())
                .requires(new MultiCommandSourcePredicate(permission))
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("random")
                        .executes(this::makePlayerStartBattleWithRandomTrainer));
    }

    private int makePlayerStartBattleWithSelectedTrainer(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            Identifier trainer = new Identifier(StringArgumentType.getString(context, "trainer"));

            MessagePredicate<Identifier> isTrainerProfileExist = new ProfileExistPredicate(TrainerProfileStorage.getProfileRegistry());
            if (!isTrainerProfileExist.test(trainer)) {
                player.sendMessage(isTrainerProfileExist.getErrorMessage());
                return 0;
            }

            TrainerBattle trainerBattle = new StandaloneFlatTrainerBattle(player, trainer);
            trainerBattle.start();

            TrainerBattleStorage.getTrainerBattleRegistry().put(player.getUuid(), trainerBattle);

            return Command.SINGLE_SUCCESS;

        } catch (CommandSyntaxException | BattleStartException e) {
            return 0;
        }
    }

    private int makePlayerStartBattleWithRandomTrainer(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
            Identifier trainer = new GlobalRandomTrainerFactory().create();

            MessagePredicate<Identifier> isTrainerProfileExist = new ProfileExistPredicate(TrainerProfileStorage.getProfileRegistry());
            if (!isTrainerProfileExist.test(trainer)) {
                player.sendMessage(isTrainerProfileExist.getErrorMessage());
                return 0;
            }

            TrainerBattle trainerBattle = new StandaloneFlatTrainerBattle(player, trainer);
            trainerBattle.start();

            TrainerBattleStorage.getTrainerBattleRegistry().put(player.getUuid(), trainerBattle);

            return Command.SINGLE_SUCCESS;

        } catch (CommandSyntaxException | BattleStartException e) {
            return 0;
        }
    }
}
