package kiwiapollo.cobblemontrainerbattle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kiwiapollo.cobblemontrainerbattle.commands.BattleFrontierCommand;
import kiwiapollo.cobblemontrainerbattle.commands.TrainerBattleCommand;
import kiwiapollo.cobblemontrainerbattle.commands.TrainerBattleFlatCommand;
import kiwiapollo.cobblemontrainerbattle.events.BattleVictoryEventHandler;
import kiwiapollo.cobblemontrainerbattle.events.LootDroppedEventHandler;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CobblemonTrainerBattle implements ModInitializer {
	public static final String NAMESPACE = "cobblemontrainerbattle";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
	public static final Set<PokemonBattle> TRAINER_BATTLES = new HashSet<>();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(new TrainerBattleCommand());
			dispatcher.register(new TrainerBattleFlatCommand());
			dispatcher.register(new BattleFrontierCommand());
		});

		CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, battleVictoryEvent -> {
			// BATTLE_VICTORY event fires even if the player loses
			LOGGER.info("BATTLE_VICTORY event");
			new BattleVictoryEventHandler().run(battleVictoryEvent);
			return Unit.INSTANCE;
        });

		CobblemonEvents.LOOT_DROPPED.subscribe(Priority.HIGHEST, lootDroppedEvent -> {
			// LOOT_DROPPED event fires before BATTLE_VICTORY event
			// Cobblemon Discord, Hiroku: It's only used if the player kills the pokemon by hand, not by battle
			// However Pokemons drop loot when defeated in battles, at least on 1.5.1
			LOGGER.info("LOOT_DROPPED event");
			new LootDroppedEventHandler().run(lootDroppedEvent);
			return Unit.INSTANCE;
        });
	}
}