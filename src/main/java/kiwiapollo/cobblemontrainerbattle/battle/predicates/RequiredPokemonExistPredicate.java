package kiwiapollo.cobblemontrainerbattle.battle.predicates;

import com.cobblemon.mod.common.pokemon.Pokemon;
import kiwiapollo.cobblemontrainerbattle.battle.battleparticipant.player.PlayerBattleParticipant;
import kiwiapollo.cobblemontrainerbattle.parser.ShowdownPokemon;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Objects;

public class RequiredPokemonExistPredicate implements MessagePredicate<PlayerBattleParticipant> {
    private final List<ShowdownPokemon> required;
    private ShowdownPokemon error;

    public RequiredPokemonExistPredicate(List<ShowdownPokemon> required) {
        this.required = required.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public MutableText getErrorMessage() {
        return Text.translatable("predicate.cobblemontrainerbattle.error.required_pokemon_exist", toPokemonDescriptor(error));
    }

    @Override
    public boolean test(PlayerBattleParticipant player) {
        List<Pokemon> party = player.getParty().toGappyList().stream().filter(Objects::nonNull).toList();
        for (ShowdownPokemon r : required) {
            if (!containsPokemon(party, r)) {
                error = r;
                return false;
            }
        }
        return true;
    }

    private boolean containsPokemon(List<Pokemon> party, ShowdownPokemon required) {
        for (Pokemon p : party) {
            boolean isSpeciesEqual = normalize(p.getSpecies().getName()).equals(normalize(required.species));
            boolean isFormEqual = p.getForm().getName().equals(required.form);

            if (isSpeciesEqual && isFormEqual) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String species) {
        String normalized = species;
        normalized = normalized.replaceAll("^cobblemon:", "");
        normalized = normalized.replaceAll("[-\\s]", "");
        normalized = normalized.toLowerCase();
        return normalized;
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split("[^a-zA-Z0-9]+");
        StringBuilder pascalCased = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                pascalCased.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase());
            }
        }

        return pascalCased.toString();
    }

    private String toPokemonDescriptor(ShowdownPokemon pokemon) {
        return String.format("%s %s", toPascalCase(normalize(pokemon.species)), pokemon.form);
    }
}
