package kiwiapollo.cobblemontrainerbattle.parser.profile;

import com.cobblemon.mod.common.battles.BattleFormat;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import kiwiapollo.cobblemontrainerbattle.CobblemonTrainerBattle;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.BattleFormatFactory;
import kiwiapollo.cobblemontrainerbattle.battle.predicates.MinimumPartySizePredicate;
import kiwiapollo.cobblemontrainerbattle.parser.pokemon.ShowdownPokemon;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerOption;
import kiwiapollo.cobblemontrainerbattle.battle.trainerbattle.TrainerProfile;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.List;
import java.util.Map;

public class TrainerProfileLoader implements SimpleSynchronousResourceReloadListener {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SoundEvent .class, new BattleThemeDeserializer())
            .registerTypeAdapter(ItemStack.class, new HeldItemDeserializer())
            .create();

    @Override
    public Identifier getFabricId() {
        return Identifier.of(CobblemonTrainerBattle.MOD_ID, "trainer_profile_loader");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        TrainerProfileStorage.getProfileRegistry().clear();
        for (Map.Entry<Identifier, TrainerResource> entry : new TrainerResourceMapFactory(resourceManager).create().entrySet()) {
            try {
                Identifier identifier = entry.getKey();
                List<ShowdownPokemon> team = readTrainerTeamResource(entry.getValue().team());
                TrainerOption option = readTrainerOptionResource(entry.getValue().option());

                TrainerProfile profile = new TrainerProfile(team, option);

                BattleFormat format = new BattleFormatFactory(profile.battleFormat).create();
                if (!new MinimumPartySizePredicate.TrainerProfilePredicate(format).test(profile)) {
                    CobblemonTrainerBattle.LOGGER.error("Illegal party size : {}", identifier);
                    throw new IllegalStateException();
                }

                TrainerProfileStorage.getProfileRegistry().put(identifier, profile);

            } catch (IllegalStateException | JsonParseException | IOException e) {
                Identifier identifier = entry.getKey();
                CobblemonTrainerBattle.LOGGER.error("Error occurred while loading {}", identifier);
            }
        }
    }

    private List<ShowdownPokemon> readTrainerTeamResource(Resource resource) throws IOException, JsonParseException {
        try (BufferedReader reader = resource.getReader()) {
            return GSON.fromJson(reader, new TypeToken<List<ShowdownPokemon>>(){}.getType());
        }
    }

    private TrainerOption readTrainerOptionResource(Resource resource) throws IOException {
        try (BufferedReader reader = resource.getReader()) {
            return GSON.fromJson(reader, TrainerOption.class);
        }
    }
}
