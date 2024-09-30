package kiwiapollo.cobblemontrainerbattle.battlefrontier;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.PokemonStats;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battleactors.player.BattleFrontierPlayerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.battleactors.trainer.BattleFrontierNameTrainerBattleActorFactory;
import kiwiapollo.cobblemontrainerbattle.exceptions.*;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.ThreePokemonTotalRandomTrainerFactory;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.Trainer;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.TrainerBattle;
import kotlin.Unit;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class BattleFrontier {
    public static final int LEVEL = 100;
    public static Map<UUID, BattleFrontierSession> SESSIONS = new HashMap<>();

    public static void startSession(CommandContext<ServerCommandSource> context) {
        try {
            assertNotExistValidSession(context.getSource().getPlayer());

            BattleFrontier.SESSIONS.put(context.getSource().getPlayer().getUuid(), new BattleFrontierSession());

            context.getSource().getPlayer().sendMessage(Text.literal("Battle Frontier session is started"));
            showPartyPokemons(context);
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Started Battle Frontier session",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (ValidBattleFrontierSessionExistException e) {
            context.getSource().getPlayer().sendMessage(Text.literal("Battle Frontier session already exists"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session exists",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    public static void stopSession(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());

            BattleFrontier.SESSIONS.remove(context.getSource().getPlayer().getUuid());

            context.getSource().getPlayer().sendMessage(Text.literal("Battle Frontier session is stopped"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Stopped Battle Frontier session",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have active Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    public static void startBattle(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertNotPlayerDefeated(context.getSource().getPlayer());
            TrainerBattle.assertNotExistPlayerParticipatingPokemonBattle(context.getSource().getPlayer());

            Trainer trainer = new ThreePokemonTotalRandomTrainerFactory().create(context.getSource().getPlayer());
            Cobblemon.INSTANCE.getBattleRegistry().startBattle(
                    BattleFormat.Companion.getGEN_9_SINGLES(),
                    new BattleSide(new BattleFrontierPlayerBattleActorFactory().create(context.getSource().getPlayer())),
                    new BattleSide(new BattleFrontierNameTrainerBattleActorFactory().create(trainer)),
                    false
            ).ifSuccessful(pokemonBattle -> {
                CobblemonTrainerBattle.TRAINER_BATTLES.put(context.getSource().getPlayer().getUuid(), pokemonBattle);
                UUID playerUuid = context.getSource().getPlayer().getUuid();
                BattleFrontier.SESSIONS.get(playerUuid).battleUuid = pokemonBattle.getBattleId();

                context.getSource().getPlayer().sendMessage(Text.literal("Battle Frontier Pokemon Battle started"));
                CobblemonTrainerBattle.LOGGER.info(String.format("%s: versus %s",
                        context.getSource().getPlayer().getGameProfile().getName(), trainer.name));

                return Unit.INSTANCE;
            });

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You must create Battle Frontier session first"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (BattleFrontierDefeatedPlayerException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You are defeated. Please create another Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Battle Frontier session expired due to defeat",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (PlayerParticipatingPokemonBattleExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You cannot start Pokemon battle while on another"));
            CobblemonTrainerBattle.LOGGER.error("Error occurred while starting trainer battle");
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Already participating in another Pokemon battle",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    private static void assertNotPlayerDefeated(ServerPlayerEntity player) throws BattleFrontierDefeatedPlayerException {
        BattleFrontierSession session = SESSIONS.get(player.getUuid());
        if (session.isDefeated) {
            throw new BattleFrontierDefeatedPlayerException();
        }
    }

    private static void assertExistValidSession(ServerPlayerEntity player)
            throws ValidBattleFrontierSessionNotExistException {
        if (!isExistValidSession(player)) {
            throw new ValidBattleFrontierSessionNotExistException();
        }
    }

    private static void assertNotExistValidSession(ServerPlayerEntity player)
            throws ValidBattleFrontierSessionExistException {
        if (isExistValidSession(player)) {
            throw new ValidBattleFrontierSessionExistException();
        }
    }

    private static boolean isExistValidSession(ServerPlayerEntity player) {
        if (!SESSIONS.containsKey(player.getUuid())) {
            return false;
        }

        BattleFrontierSession session = SESSIONS.get(player.getUuid());
        return Instant.now().isBefore(session.timestamp.plus(Duration.ofHours(24)));
    }

    public static void tradePokemon(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertNotPlayerDefeated(context.getSource().getPlayer());
            assertExistDefeatedTrainer(context.getSource().getPlayer());
            assertNotPlayerTradedPokemon(context.getSource().getPlayer());

            int playerslot = IntegerArgumentType.getInteger(context, "playerslot");
            int trainerslot = IntegerArgumentType.getInteger(context, "trainerslot");

            BattleFrontierSession session = SESSIONS.get(context.getSource().getPlayer().getUuid());
            Trainer lastDefeatedTrainer = session.defeatedTrainers.get(session.defeatedTrainers.size() - 1);
            Pokemon trainerPokemon = lastDefeatedTrainer.pokemons.get(trainerslot - 1);
            Pokemon playerPokemon = session.partyPokemons.get(playerslot - 1);

            session.partyPokemons = new ArrayList<>(session.partyPokemons);
            session.partyPokemons.set(playerslot - 1, trainerPokemon.clone(true, true));
            session.isTradedPokemon = true;

            context.getSource().getPlayer().sendMessage(
                    Text.literal("Traded ")
                            .append(playerPokemon.getDisplayName())
                            .append(Text.literal(" for "))
                            .append(trainerPokemon.getDisplayName())
                            .formatted(Formatting.YELLOW));

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have active Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (BattleFrontierDefeatedPlayerException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You are defeated. Please create another Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Battle Frontier session expired due to defeat",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (DefeatedTrainerNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have any defeated trainers"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Defeated trainers do not exist",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (PlayerTradedPokemonException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You already traded your Pokemon"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Already traded Pokemon",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    private static void assertNotPlayerTradedPokemon(ServerPlayerEntity player)
            throws PlayerTradedPokemonException {
        if (SESSIONS.get(player.getUuid()).isTradedPokemon) {
            throw new PlayerTradedPokemonException();
        }
    }

    public static void showTradeablePokemons(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertExistDefeatedTrainer(context.getSource().getPlayer());

            BattleFrontierSession session = SESSIONS.get(context.getSource().getPlayer().getUuid());
            Trainer lastDefeatedTrainer = session.defeatedTrainers.get(session.defeatedTrainers.size() - 1);
            printPokemons(context, lastDefeatedTrainer.pokemons);

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have active Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (DefeatedTrainerNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have any defeated trainers"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Defeated trainers do not exist",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    public static void showPartyPokemons(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            printPokemons(context, SESSIONS.get(context.getSource().getPlayer().getUuid()).partyPokemons);

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have active Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));
        }
    }

    private static void printPokemons(CommandContext<ServerCommandSource> context, List<Pokemon> pokemons) {
        for (int i = 0; i < pokemons.size(); i++) {
            Pokemon pokemon = pokemons.get(i);

            context.getSource().getPlayer().sendMessage(
                    Text.literal("[" + (i + 1) + "] ").append(pokemon.getDisplayName()).formatted(Formatting.YELLOW));
            context.getSource().getPlayer().sendMessage(
                    Text.literal("Ability ").append(Text.translatable(pokemon.getAbility().getDisplayName())));
            context.getSource().getPlayer().sendMessage(
                    Text.literal("Nature ").append(Text.translatable(pokemon.getNature().getDisplayName())));
            context.getSource().getPlayer().sendMessage(
                    Text.literal("MoveSet ").append(getPokemonMoveSetReport(pokemon.getMoveSet())));
            context.getSource().getPlayer().sendMessage(
                    Text.literal("EVs ").append(Text.literal(getPokemonStatsReport(pokemon.getEvs()))));
            context.getSource().getPlayer().sendMessage(
                    Text.literal("IVs ").append(Text.literal(getPokemonStatsReport(pokemon.getIvs()))));
        }
    }

    private static Text getPokemonMoveSetReport(MoveSet moveSet) {
        MutableText moveSetReport = Text.literal("");
        for (Move move : moveSet.getMoves()) {
            if (moveSetReport.equals(Text.literal(""))) {
                moveSetReport.append(move.getDisplayName());
            } else {
                moveSetReport.append(Text.literal(" / ")).append(move.getDisplayName());
            }
        }
        return moveSetReport;
    }

    private static String getPokemonStatsReport(PokemonStats stats) {
        return String.format("HP %d / ATK %d / DEF %d / SPA %d / SPD %d / SPE %d",
                stats.get(Stats.HP), stats.get(Stats.ATTACK), stats.get(Stats.DEFENCE),
                stats.get(Stats.SPECIAL_ATTACK), stats.get(Stats.SPECIAL_DEFENCE), stats.get(Stats.SPEED));
    };

    public static void rerollPokemons(CommandContext<ServerCommandSource> context) {
        try {
            assertExistValidSession(context.getSource().getPlayer());
            assertNotExistDefeatedTrainers(context.getSource().getPlayer());

            BattleFrontierSession session = SESSIONS.get(context.getSource().getPlayer().getUuid());

            session.partyPokemons = new RandomPartyPokemonsFactory().create();
            context.getSource().getPlayer().sendMessage(Text.literal("Rerolled Pokemons"));
            showPartyPokemons(context);

        } catch (ValidBattleFrontierSessionNotExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You do not have active Battle Frontier session"));
            CobblemonTrainerBattle.LOGGER.error(String.format("%s: Valid Battle Frontier session does not exists",
                    context.getSource().getPlayer().getGameProfile().getName()));

        } catch (DefeatedTrainerExistException e) {
            context.getSource().getPlayer().sendMessage(
                    Text.literal("You cannot reroll Pokemons after battling trainers"));
        }
    }

    private static void assertExistDefeatedTrainer(ServerPlayerEntity player)
            throws DefeatedTrainerNotExistException {
        if (!isExistDefeatedTrainers(player)) {
            throw new DefeatedTrainerNotExistException();
        }
    }

    private static void assertNotExistDefeatedTrainers(ServerPlayerEntity player)
            throws DefeatedTrainerExistException {
        if (isExistDefeatedTrainers(player)) {
            throw new DefeatedTrainerExistException();
        }
    }

    private static boolean isExistDefeatedTrainers(ServerPlayerEntity player) {
        return !SESSIONS.get(player.getUuid()).defeatedTrainers.isEmpty();
    }
}
