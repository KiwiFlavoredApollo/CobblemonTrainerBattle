package kiwiapollo.cobblemontrainerbattle.groupbattle;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battlefactory.BattleFactory;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.FlatBattleParticipantFactory;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.NormalBattleParticipantFactory;
import kiwiapollo.cobblemontrainerbattle.battleparticipant.BattleParticipantFactory;
import kiwiapollo.cobblemontrainerbattle.common.*;
import kiwiapollo.cobblemontrainerbattle.exception.*;
import kiwiapollo.cobblemontrainerbattle.resulthandler.GenericResultHandler;
import kiwiapollo.cobblemontrainerbattle.exception.BattleStartException;
import kiwiapollo.cobblemontrainerbattle.resulthandler.ResultHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;
import java.util.*;

public class GroupBattle {
    public static final int FLAT_LEVEL = 100;
    public static Map<UUID, GroupBattleSession> sessions = new HashMap<>();

    public static int startNormalGroupBattleSession(CommandContext<ServerCommandSource> context) {
        return startSession(context, new NormalBattleParticipantFactory());
    }

    public static int startFlatGroupBattleSession(CommandContext<ServerCommandSource> context) {
        return startSession(context, new FlatBattleParticipantFactory(FLAT_LEVEL));
    }

    private static int startSession(CommandContext<ServerCommandSource> context, BattleParticipantFactory battleParticipantFactory) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();

            String groupResourcePath = StringArgumentType.getString(context, "group");
            Identifier identifier = Identifier.of(CobblemonTrainerBattle.NAMESPACE, groupResourcePath);

            ResourceValidator.assertTrainerGroupExist(identifier);
            ResourceValidator.assertTrainerGroupValid(identifier);
            SessionValidator.assertSessionNotExist(sessions, player);

            TrainerGroup trainerGroup = CobblemonTrainerBattle.trainerGroupRegistry.get(identifier);
            List<Identifier> trainersToDefeat = trainerGroup.trainers.stream()
                    .map(trainerResourcePath -> Identifier.of(CobblemonTrainerBattle.NAMESPACE, trainerResourcePath)).toList();

            ResultHandler resultHandler = new GenericResultHandler(
                    player,
                    trainerGroup.onVictory,
                    trainerGroup.onDefeat
            );

            GroupBattleSession session = new GroupBattleSession(
                    player,
                    trainersToDefeat,
                    resultHandler,
                    battleParticipantFactory
            );

            sessions.put(player.getUuid(), session);

            player.sendMessage(Text.translatable("command.cobblemontrainerbattle.groupbattle.startsession.success"));
            CobblemonTrainerBattle.LOGGER.info("Started group battle session: {}", player.getGameProfile().getName());

            return Command.SINGLE_SUCCESS;

        } catch (IllegalStateException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            MutableText message = Text.translatable("command.cobblemontrainerbattle.groupbattle.common.valid_session_exist");
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;

        } catch (FileNotFoundException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            String groupResourcePath = StringArgumentType.getString(context, "group");
            MutableText message = Text.translatable("command.cobblemontrainerbattle.common.resource.not_found", groupResourcePath);
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;

        } catch (IllegalArgumentException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            String groupResourcePath = StringArgumentType.getString(context, "group");
            MutableText message = Text.translatable("command.cobblemontrainerbattle.common.resource.cannot_be_read", groupResourcePath);
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;
        }
    }

    public static int stopSession(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();

            SessionValidator.assertSessionExist(sessions, player);
            PlayerValidator.assertPlayerNotBusyWithPokemonBattle(player);

            GroupBattleSession session = sessions.get(player.getUuid());
            session.onSessionStop();

            BattleFactory.sessions.remove(player.getUuid());

            player.sendMessage(Text.literal("command.cobblemontrainerbattle.groupbattle.stopsession.success"));
            CobblemonTrainerBattle.LOGGER.info("Stopped group battle session: {}", player.getGameProfile().getName());

            return Command.SINGLE_SUCCESS;

        } catch (IllegalStateException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            MutableText message = Text.translatable("command.cobblemontrainerbattle.groupbattle.common.valid_session_not_exist");
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;

        } catch (BusyWithPokemonBattleException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            MutableText message = Text.translatable("command.cobblemontrainerbattle.trainerbattle.busy_with_pokemon_battle");
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;
        }
    }

    public static int startBattle(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();

            SessionValidator.assertSessionExist(sessions, player);

            GroupBattleSession session = sessions.get(player.getUuid());
            session.startBattle();

            return Command.SINGLE_SUCCESS;

        } catch (IllegalStateException e) {
            ServerPlayerEntity player = context.getSource().getPlayer();

            MutableText message = Text.translatable("command.cobblemontrainerbattle.groupbattle.common.valid_session_not_exist");
            player.sendMessage(message.formatted(Formatting.RED));

            return 0;

        } catch (BattleStartException e) {
            return 0;
        }
    }
}
