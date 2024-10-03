package kiwiapollo.cobblemontrainerbattle.groupbattle;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonElement;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battleactors.player.FlatLevelFullHealthPlayerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.battleactors.player.StatusQuoPlayerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.battleactors.trainer.FlatLevelFullHealthTrainerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.battleactors.trainer.TrainerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.common.InvalidBattleSessionState;
import kiwiapollo.cobblemontrainerbattle.common.InvalidPlayerState;
import kiwiapollo.cobblemontrainerbattle.common.InvalidResourceState;
import kiwiapollo.cobblemontrainerbattle.exceptions.InvalidBattleSessionStateException;
import kiwiapollo.cobblemontrainerbattle.exceptions.InvalidPlayerStateException;
import kiwiapollo.cobblemontrainerbattle.exceptions.InvalidResourceStateException;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.SpecificTrainerFactory;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.Trainer;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.TrainerFileParser;
import kotlin.Unit;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public class GroupBattle {
    public static final int FLAT_LEVEL = 100;
    public static Map<UUID, GroupBattleSession> SESSIONS = new HashMap<>();

    public static int quickStartBattleWithStatusQuo(CommandContext<ServerCommandSource> context) {
        try {
            assertNotExistValidSession(context.getSource().getPlayer());
            startSession(context);
            return startBattleWithStatusQuo(context);

        } catch (InvalidBattleSessionStateException e) {
            return startBattleWithStatusQuo(context);
        }
    }

    public static int quickStartBattleWithFlatLevelAndFullHealth(CommandContext<ServerCommandSource> context) {
        try {
            assertNotExistValidSession(context.getSource().getPlayer());
            startSession(context);
            return startBattleWithFlatLevelAndFullHealth(context);

        } catch (InvalidBattleSessionStateException e) {
            return startBattleWithFlatLevelAndFullHealth(context);
        }
    }

    public static int startSession(CommandContext<ServerCommandSource> context) {
        try {
            assertNotExistValidSession(context.getSource().getPlayer());

            String groupFilePath = StringArgumentType.getString(context, "group");
            assertValidGroupFile(groupFilePath);

            GroupBattle.SESSIONS.put(context.getSource().getPlayer().getUuid(), new GroupBattleSession(groupFilePath));

            context.getSource().getPlayer().sendMessage(Text.literal("Battle group session has started"));
            CobblemonTrainerBattle.LOGGER.info(String.format("Started battle group session: %s",
                    context.getSource().getPlayer().getGameProfile().getName()));

            return Command.SINGLE_SUCCESS;

        } catch (InvalidBattleSessionStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidBattleSessionStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;

        } catch (InvalidResourceStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidResourceStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;
        }
    }

    public static int stopSession(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());

            GroupBattle.SESSIONS.remove(context.getSource().getPlayer().getUuid());

            context.getSource().getPlayer().sendMessage(Text.literal("Battle group session has stopped"));
            CobblemonTrainerBattle.LOGGER.info(String.format("Stopped battle group session: %s",
                    context.getSource().getPlayer().getGameProfile().getName()));

            return Command.SINGLE_SUCCESS;

        } catch (InvalidBattleSessionStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidBattleSessionStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;
        }
    }

    public static int startBattleWithStatusQuo(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertNotPlayerDefeated(context.getSource().getPlayer());
            assertNotPlayerBusyWithAnotherPokemonBattle(context.getSource().getPlayer());
            assertNotEmptyPlayerParty(context.getSource().getPlayer());
            assertNotFaintPlayerParty(context.getSource().getPlayer());
            assertPlayerPartyAtOrAboveRelativeLevelThreshold(context.getSource().getPlayer());

            String nextTrainerResourcePath = getNextTrainerResourcePath(context.getSource().getPlayer());
            Trainer trainer = new SpecificTrainerFactory().create(context.getSource().getPlayer(), nextTrainerResourcePath);

            Cobblemon.INSTANCE.getBattleRegistry().startBattle(
                    BattleFormat.Companion.getGEN_9_SINGLES(),
                    new BattleSide(new StatusQuoPlayerBattleActorFactory().create(context.getSource().getPlayer())),
                    new BattleSide(new TrainerBattleActorFactory().create(trainer)),
                    false

            ).ifSuccessful(pokemonBattle -> {
                UUID playerUuid = context.getSource().getPlayer().getUuid();
                CobblemonTrainerBattle.trainerBattles.put(playerUuid, pokemonBattle);
                GroupBattle.SESSIONS.get(playerUuid).battleUuid = pokemonBattle.getBattleId();

                context.getSource().getPlayer().sendMessage(
                        Text.literal(String.format("Trainer battle has started against %s", trainer.name)));
                CobblemonTrainerBattle.LOGGER.info(String.format("battlegroup: %s versus %s",
                        context.getSource().getPlayer().getGameProfile().getName(), trainer.name));

                return Unit.INSTANCE;
            });

            return Command.SINGLE_SUCCESS;

        } catch (InvalidBattleSessionStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidBattleSessionStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;

        } catch (InvalidResourceStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidResourceStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;

        } catch (InvalidPlayerStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidPlayerStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;
        }
    }

    public static int startBattleWithFlatLevelAndFullHealth(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertNotPlayerDefeated(context.getSource().getPlayer());
            assertNotPlayerBusyWithAnotherPokemonBattle(context.getSource().getPlayer());
            assertNotEmptyPlayerParty(context.getSource().getPlayer());

            String nextTrainerResourcePath = getNextTrainerResourcePath(context.getSource().getPlayer());
            Trainer trainer = new SpecificTrainerFactory().create(context.getSource().getPlayer(), nextTrainerResourcePath);

            Cobblemon.INSTANCE.getBattleRegistry().startBattle(
                    BattleFormat.Companion.getGEN_9_SINGLES(),
                    new BattleSide(new FlatLevelFullHealthPlayerBattleActorFactory().create(context.getSource().getPlayer(), FLAT_LEVEL)),
                    new BattleSide(new FlatLevelFullHealthTrainerBattleActorFactory().create(trainer, FLAT_LEVEL)),
                    false

            ).ifSuccessful(pokemonBattle -> {
                UUID playerUuid = context.getSource().getPlayer().getUuid();
                CobblemonTrainerBattle.trainerBattles.put(playerUuid, pokemonBattle);
                GroupBattle.SESSIONS.get(playerUuid).battleUuid = pokemonBattle.getBattleId();

                context.getSource().getPlayer().sendMessage(
                        Text.literal(String.format("Flat level trainer battle has started against %s", trainer.name)));
                CobblemonTrainerBattle.LOGGER.info(String.format("battlegroup: %s versus %s",
                        context.getSource().getPlayer().getGameProfile().getName(), trainer.name));

                return Unit.INSTANCE;
            });

            return Command.SINGLE_SUCCESS;

        } catch (InvalidBattleSessionStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidBattleSessionStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;

        } catch (InvalidResourceStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidResourceStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;

        } catch (InvalidPlayerStateException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal(getInvalidPlayerStateErrorMessage(e)).formatted(Formatting.RED));
            CobblemonTrainerBattle.LOGGER.error(e.getMessage());
            return -1;
        }
    }

    public static String getNextTrainerResourcePath(ServerPlayerEntity player)
            throws InvalidResourceStateException, InvalidBattleSessionStateException {
        try {
            GroupBattleSession session = SESSIONS.get(player.getUuid());
            int defeatedTrainersCount = session.defeatedTrainers.size();
            GroupFile groupFile = CobblemonTrainerBattle.groupFiles.get(session.groupResourcePath);

            if (groupFile.configuration.get("trainers").getAsJsonArray().isEmpty()) {
                throw new InvalidResourceStateException(
                        String.format("Group has no trainers: %s", session.groupResourcePath),
                        InvalidResourceState.CONTAINS_INVALID_VALUE,
                        session.groupResourcePath
                );
            }

            return groupFile.configuration.get("trainers").getAsJsonArray()
                    .get(defeatedTrainersCount).getAsString();

        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException e) {
            GroupBattleSession session = SESSIONS.get(player.getUuid());
            GroupFile groupFile = CobblemonTrainerBattle.groupFiles.get(session.groupResourcePath);
            throw new InvalidResourceStateException(
                    String.format("Unreadable resource: %s", groupFile),
                    InvalidResourceState.UNREADABLE,
                    session.groupResourcePath
            );

        } catch (IndexOutOfBoundsException e) {
            throw new InvalidBattleSessionStateException(
                    String.format("Player has defeated all trainers: %s", player.getGameProfile().getName()),
                    InvalidBattleSessionState.ALL_TRAINER_DEFEATED
            );
        }
    }

    private static void assertValidGroupFile(String groupFilePath) throws InvalidResourceStateException {
        try {
            if (!CobblemonTrainerBattle.groupFiles.get(groupFilePath).configuration
                    .get("trainers").getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsString)
                    .allMatch(CobblemonTrainerBattle.trainerFiles::containsKey)) {
                throw new InvalidResourceStateException(
                        String.format("One or more trainer files are not loaded: %s", groupFilePath),
                        InvalidResourceState.CONTAINS_INVALID_VALUE,
                        groupFilePath);
            };
        } catch (NullPointerException | IllegalStateException | UnsupportedOperationException | ClassCastException e) {
            throw new InvalidResourceStateException(
                    String.format("Unreadable resource: %s", groupFilePath),
                    InvalidResourceState.UNREADABLE,
                    groupFilePath
            );
        }
    }

    private static void assertNotPlayerDefeated(ServerPlayerEntity player) throws InvalidBattleSessionStateException {
        GroupBattleSession session = SESSIONS.get(player.getUuid());
        if (session.isDefeated) {
            throw new InvalidBattleSessionStateException(
                    String.format("Player is defeated: %s", player.getGameProfile().getName()),
                    InvalidBattleSessionState.DEFEATED_TO_TRAINER
            );
        }
    }

    private static void assertExistValidSession(ServerPlayerEntity player)
            throws InvalidBattleSessionStateException {
        if (!isExistValidSession(player)) {
            throw new InvalidBattleSessionStateException(
                    String.format("Valid battle session does not exists: %s", player.getGameProfile().getName()),
                    InvalidBattleSessionState.SESSION_NOT_EXISTS
            );
        }
    }

    private static void assertNotExistValidSession(ServerPlayerEntity player)
            throws InvalidBattleSessionStateException {
        if (isExistValidSession(player)) {
            throw new InvalidBattleSessionStateException(
                    String.format("Valid battle session exists: %s", player.getGameProfile().getName()),
                    InvalidBattleSessionState.SESSION_EXISTS
            );
        }
    }

    private static boolean isExistValidSession(ServerPlayerEntity player) {
        if (!SESSIONS.containsKey(player.getUuid())) {
            return false;
        }

        GroupBattleSession session = SESSIONS.get(player.getUuid());
        return Instant.now().isBefore(session.timestamp.plus(Duration.ofHours(24)));
    }

    private static void assertNotPlayerBusyWithAnotherPokemonBattle(ServerPlayerEntity player)
            throws InvalidPlayerStateException {
        if (Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null) {
            throw new InvalidPlayerStateException(
                    String.format("Player is busy with another Pokemon battle: %s",
                            player.getGameProfile().getName()),
                    InvalidPlayerState.BUSY_WITH_ANOTHER_POKEMON_BATTLE
            );
        }
    }

    private static void assertNotEmptyPlayerParty(ServerPlayerEntity player) throws InvalidPlayerStateException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        if (playerPartyStore.toGappyList().stream().allMatch(Objects::isNull)) {
            throw new InvalidPlayerStateException(
                    String.format("Player has no Pokemon: %s", player.getGameProfile().getName()),
                    InvalidPlayerState.EMPTY_POKEMON_PARTY
            );
        }
    }

    private static void assertPlayerPartyAtOrAboveRelativeLevelThreshold(ServerPlayerEntity player)
            throws InvalidPlayerStateException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        Stream<Pokemon> pokemons = playerPartyStore.toGappyList().stream().filter(Objects::nonNull);
        if (pokemons.map(Pokemon::getLevel).allMatch(level -> level < TrainerFileParser.RELATIVE_LEVEL_THRESHOLD)) {
            throw new InvalidPlayerStateException(
                    String.format("Pokemon levels are below relative level threshold", player.getGameProfile().getName()),
                    InvalidPlayerState.POKEMON_PARTY_BELOW_RELATIVE_LEVEL_THRESHOLD
            );
        }
    }

    private static void assertNotFaintPlayerParty(ServerPlayerEntity player) throws InvalidPlayerStateException {
        PlayerPartyStore playerPartyStore = Cobblemon.INSTANCE.getStorage().getParty(player);
        Stream<Pokemon> pokemons = playerPartyStore.toGappyList().stream().filter(Objects::nonNull);
        if (pokemons.allMatch(Pokemon::isFainted)) {
            throw new InvalidPlayerStateException(
                    String.format("Pokemons are all fainted: %s", player.getGameProfile().getName()),
                    InvalidPlayerState.FAINTED_POKEMON_PARTY
            );
        }
    }

    private static String getInvalidBattleSessionStateErrorMessage(InvalidBattleSessionStateException e) {
        if (e.getInvalidBattleSessionState().equals(InvalidBattleSessionState.SESSION_EXISTS)) {
            return "Active group battle session exist";
        }

        if (e.getInvalidBattleSessionState().equals(InvalidBattleSessionState.SESSION_NOT_EXISTS)) {
            return "Active group battle session does not exist";
        }

        if (e.getInvalidBattleSessionState().equals(InvalidBattleSessionState.ALL_TRAINER_DEFEATED)) {
            return "You have defeated all trainers";
        }

        if (e.getInvalidBattleSessionState().equals(InvalidBattleSessionState.DEFEATED_TO_TRAINER)) {
            return "You cannot continue group battle session due to being defeated";
        }

        throw new RuntimeException(e);
    }

    private static String getInvalidResourceStateErrorMessage(InvalidResourceStateException e) {
        if (e.getInvalidResourceState().equals(InvalidResourceState.UNREADABLE)) {
            return String.format("An error occurred while reading %s", e.getResourcePath());
        }

        if (e.getInvalidResourceState().equals(InvalidResourceState.CONTAINS_INVALID_VALUE)) {
            return String.format("Invalid values found in %s", e.getResourcePath());
        }

        throw new RuntimeException(e);
    }

    private static String getInvalidPlayerStateErrorMessage(InvalidPlayerStateException e) {
        if (e.getInvalidPlayerState().equals(InvalidPlayerState.EMPTY_POKEMON_PARTY)) {
            return "You have no Pokemon";
        }

        if (e.getInvalidPlayerState().equals(InvalidPlayerState.BUSY_WITH_ANOTHER_POKEMON_BATTLE)) {
            return "You cannot start trainer battle while on another";
        }

        if (e.getInvalidPlayerState().equals(InvalidPlayerState.FAINTED_POKEMON_PARTY)) {
            return "Your Pokemons are all fainted";
        }

        if (e.getInvalidPlayerState().equals(InvalidPlayerState.POKEMON_PARTY_BELOW_RELATIVE_LEVEL_THRESHOLD)) {
            return String.format("Pokemon levels should be above %d", TrainerFileParser.RELATIVE_LEVEL_THRESHOLD);
        }

        throw new RuntimeException(e);
    }
}
