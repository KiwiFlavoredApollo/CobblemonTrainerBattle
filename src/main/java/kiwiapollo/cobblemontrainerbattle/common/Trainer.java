package kiwiapollo.cobblemontrainerbattle.common;

import kiwiapollo.cobblemontrainerbattle.parsers.SmogonPokemon;

import java.util.List;

public class Trainer {
    public List<SmogonPokemon> pokemons;
    public PostBattleAction onVictory;
    public PostBattleAction onDefeat;
    public BattleCondition condition;

    public Trainer(
            List<SmogonPokemon> pokemons,
            PostBattleAction onVictory,
            PostBattleAction onDefeat,
            BattleCondition condition
    ) {
        this.pokemons = pokemons;
        this.onVictory = onVictory;
        this.onDefeat = onDefeat;
        this.condition = condition;
    }
}