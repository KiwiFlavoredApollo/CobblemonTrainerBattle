package kiwiapollo.cobblemontrainerbattle;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.google.gson.JsonObject;
import kiwiapollo.cobblemontrainerbattle.battlefactory.BattleFactorySession;
import kiwiapollo.cobblemontrainerbattle.commands.*;
import kiwiapollo.cobblemontrainerbattle.common.*;
import kiwiapollo.cobblemontrainerbattle.economies.Economy;
import kiwiapollo.cobblemontrainerbattle.entities.TrainerEntity;
import kiwiapollo.cobblemontrainerbattle.events.BattleVictoryEventHandler;
import kiwiapollo.cobblemontrainerbattle.events.LootDroppedEventHandler;
import kiwiapollo.cobblemontrainerbattle.events.TrainerSpawnEventHandler;
import kiwiapollo.cobblemontrainerbattle.groupbattle.GroupBattleSession;
import kiwiapollo.cobblemontrainerbattle.groupbattle.GroupFile;
import kiwiapollo.cobblemontrainerbattle.trainerbattle.TrainerFile;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CobblemonTrainerBattle implements ModInitializer {
	public static final String NAMESPACE = "cobblemontrainerbattle";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
	public static final Config CONFIG = ConfigLoader.load();
	public static final Economy ECONOMY = EconomyFactory.create(CONFIG.economy);
	public static final String GROUP_CONFIG_DIR = "groups";
	public static final String TRAINER_CONFIG_DIR = "trainers";
	public static final String ARCADE_CONFIG_DIR = "arcades";
	public static final EntityType<TrainerEntity> TRAINER_ENTITY_TYPE =
			EntityType.Builder.create(TrainerEntity::new, SpawnGroup.CREATURE)
					.setDimensions(0.6f, 1.8f)
					.build("trainer");

	public static Map<UUID, TrainerPokemonBattle> trainerBattles = new HashMap<>();
	public static JsonObject defaultTrainerConfiguration = new JsonObject();
	public static Map<String, TrainerFile> trainerFiles = new HashMap<>();
	public static Map<String, GroupFile> groupFiles = new HashMap<>();
	public static JsonObject battleFactoryConfiguration = new JsonObject();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(new TrainerBattleCommand());
			dispatcher.register(new TrainerBattleFlatCommand());
			dispatcher.register(new GroupBattleCommand());
			dispatcher.register(new GroupBattleFlatCommand());
			dispatcher.register(new BattleFactoryCommand());
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

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerUuid = handler.getPlayer().getUuid();
			if (!trainerBattles.containsKey(playerUuid)) {
				return;
			}

			trainerBattles.get(playerUuid).end();

			if (trainerBattles.get(playerUuid).getSession() instanceof GroupBattleSession session) {
                session.isDefeated = true;
			}

			if (trainerBattles.get(playerUuid).getSession() instanceof BattleFactorySession session) {
                session.isDefeated = true;
			}

			trainerBattles.remove(playerUuid);
		});

		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ResourceReloadListener());

		Registry.register(Registries.ENTITY_TYPE, Identifier.of(NAMESPACE, "trainer"), TRAINER_ENTITY_TYPE);
		FabricDefaultAttributeRegistry.register(TRAINER_ENTITY_TYPE, TrainerEntity.createMobAttributes());
		ServerTickEvents.END_WORLD_TICK.register(TrainerSpawnEventHandler::onEndWorldTick);
	}
}