package kiwiapollo.trainerbattle.common;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kiwiapollo.trainerbattle.TrainerBattle;
import kiwiapollo.trainerbattle.exceptions.InvalidPokemonStatsException;
import kiwiapollo.trainerbattle.exceptions.NotCobblemonMoveNameException;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class TrainerFileParser {
    public Trainer parse(Path resourcePath) {
        try {
            InputStream resourceInputStream = new FileInputStream(resourcePath.toString());
            InputStreamReader reader = new InputStreamReader(resourceInputStream);

            BufferedReader bufferedReader = new BufferedReader(reader);
            JsonArray trainerJsonObject = new Gson().fromJson(bufferedReader, JsonArray.class);
            bufferedReader.close();

            return new Trainer(
                    resourcePath.getFileName().toString().replace(".json", ""),
                    getPokemons(trainerJsonObject)
            );

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Pokemon> getPokemons(JsonArray trainer) {
        List<JsonObject> pokemonJsonObjects = trainer.getAsJsonArray().asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(this::isExistPokemon).toList();

        return pokemonJsonObjects.stream().map(this::createPokemon).toList();
    }

    private boolean isExistPokemon(JsonObject pokemon) {
        Identifier identifier = Identifier.of(
                "cobblemon", pokemon.get("species").getAsString().toLowerCase());
        return PokemonSpecies.INSTANCE.getByIdentifier(identifier) != null;
    }

    private Pokemon createPokemon(JsonObject jsonObject) {
        Identifier identifier = Identifier.of(
                "cobblemon", jsonObject.get("species").getAsString().toLowerCase());
        Pokemon pokemon = PokemonSpecies.INSTANCE.getByIdentifier(identifier).create(30);

        if (jsonObject.get("evs") != null && !jsonObject.get("evs").isJsonNull()) {
            setPokemonStats(pokemon::setEV, jsonObject.get("evs").getAsJsonObject());
        }

        if (jsonObject.get("ivs") != null && !jsonObject.get("ivs").isJsonNull()) {
            setPokemonStats(pokemon::setIV, jsonObject.get("ivs").getAsJsonObject());
        }

        if (jsonObject.get("nature") != null && !jsonObject.get("nature").isJsonNull()) {
            setPokemonNature(pokemon, jsonObject.get("nature").getAsString());
        }

        if (jsonObject.get("gender") != null && !jsonObject.get("gender").isJsonNull()) {
            setPokemonGender(pokemon, jsonObject.get("gender").getAsString());
        }

        if (jsonObject.get("moves") != null && !jsonObject.get("moves").isJsonNull()) {
            setPokemonMoveSet(pokemon, jsonObject.get("moves").getAsJsonArray());
        }

        if (jsonObject.get("item") != null && !jsonObject.get("item").isJsonNull()) {
            setPokemonHeldItem(pokemon, jsonObject.get("item").getAsString());
        }

        if (jsonObject.get("ability") != null && !jsonObject.get("ability").isJsonNull()) {
            setPokemonAbility(pokemon, jsonObject.get("ability").getAsString());
        }

        return pokemon;
    }

    private void setPokemonAbility(Pokemon pokemon, String ability) {
        pokemon.updateAbility(Abilities.INSTANCE.getOrException(
                ability.replace(" ", "").toLowerCase()).create(false));
    }

    private void setPokemonStats(BiConsumer<Stats, Integer> consumer, JsonObject stats) {
        try {
            assertValidStats(stats);

            consumer.accept(Stats.HP, stats.get("hp").getAsInt());
            consumer.accept(Stats.ATTACK, stats.get("atk").getAsInt());
            consumer.accept(Stats.DEFENCE, stats.get("def").getAsInt());
            consumer.accept(Stats.SPECIAL_ATTACK, stats.get("spa").getAsInt());
            consumer.accept(Stats.SPECIAL_DEFENCE, stats.get("spd").getAsInt());
            consumer.accept(Stats.SPEED, stats.get("spe").getAsInt());

        } catch (InvalidPokemonStatsException ignored) {

        }
    }

    private void assertValidStats(JsonObject stats) throws InvalidPokemonStatsException {
        try {
            List<String> statNames = List.of("hp", "atk", "def", "spa", "spd", "spe");

            boolean isNull = statNames.stream()
                    .map(stats::get)
                    .anyMatch(Objects::isNull);

            boolean isJsonNull = statNames.stream()
                    .map(stats::get)
                    .anyMatch(JsonElement::isJsonNull);

            if (isNull || isJsonNull) {
                throw new InvalidPokemonStatsException();
            }

            statNames.forEach(name -> stats.get(name).getAsInt());

        } catch (NullPointerException | UnsupportedOperationException e) {
            throw new InvalidPokemonStatsException();
        }
    }

    private void setPokemonNature(Pokemon pokemon, String natureName) {
        Nature nature = Natures.INSTANCE.getNature(
                Identifier.of("cobblemon", natureName.toLowerCase()));

        if (Natures.INSTANCE.all().contains(nature)) {
            pokemon.setNature(nature);
        }
    }

    private void setPokemonGender(Pokemon pokemon, String gender) {
        switch (gender) {
            case "M" -> pokemon.setGender(Gender.MALE);
            case "F" -> pokemon.setGender(Gender.FEMALE);
            case "" -> pokemon.setGender(Gender.GENDERLESS);
            default -> {}
        }
    }

    private void setPokemonHeldItem(Pokemon pokemon, String itemName) {
        Item item = Registries.ITEM.get(Identifier.of("cobblemon", itemName.replace(" ", "_").toLowerCase()));

        pokemon.swapHeldItem(new ItemStack(item), false);
    }

    private void setPokemonMoveSet(Pokemon pokemon, JsonArray moveSet) {
        pokemon.getMoveSet().clear();
        moveSet.getAsJsonArray().asList().stream()
                .map(JsonElement::getAsString)
                .map(this::toCobblemonMoveName)
                .forEach(name -> pokemon.getMoveSet().add(Moves.INSTANCE.getByNameOrDummy(name).create()));
    }

    private String toCobblemonMoveName(String moveName) {
        try {
            String cobblemonMoveName = moveName.replace(" ", "")
                    .replace("-", "")
                    .toLowerCase();
            assertCobblmonMoveName(cobblemonMoveName);
            return cobblemonMoveName;

        } catch (NotCobblemonMoveNameException e) {
            return getExceptionalCobblemonMoveName(moveName);
        }
    }

    private String getExceptionalCobblemonMoveName(String moveName) {
        Map<String, String> moveNameExceptions = Map.of(
                "Drain Kiss", "drainingkiss"
        );

        if (!moveNameExceptions.containsKey(moveName)) {
            TrainerBattle.LOGGER.error(String.format("Failed to get Cobblemon move name for %s", moveName));
            TrainerBattle.LOGGER.error("Falling back to Tackle");
            return "tackle";
        } else {
            return moveNameExceptions.get(moveName);
        }
    }

    private void assertCobblmonMoveName(String moveName) throws NotCobblemonMoveNameException {
        if (!Moves.INSTANCE.names().contains(moveName)) {
            throw new NotCobblemonMoveNameException();
        }
    }
}