package com.rassix.randomNumberGenerator.repository;

import com.rassix.randomNumberGenerator.constant.GamePhase;
import com.rassix.randomNumberGenerator.exception.GameMissingException;
import com.rassix.randomNumberGenerator.exception.PlayerExistsException;
import com.rassix.randomNumberGenerator.repository.model.Game;
import com.rassix.randomNumberGenerator.repository.model.Game.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;

@Slf4j
public class GameRepository {

    private Map<String, Game> games = new HashMap<>();

    public String createGame(Game game) {
        String gameId = UUID.randomUUID().toString();
        games.put(gameId, game);
        return gameId;
    }

    public Game getGame(String gameId) {
        return Optional.ofNullable(games.get(gameId))
                .orElseThrow(() -> new GameMissingException("Game with the id \"" + gameId + "\" does not exist"));
    }

    public Map<String, Game> getAllGames() {
        return games;
    }

    public synchronized void addPlayer(String gameId, String playerName, Player player) throws PlayerExistsException {
        Game game = games.get(gameId);
        if(!isNull(game.getPlayers().get(playerName))) {
            throw new PlayerExistsException("Player " + playerName + " already exists in game");
        }
        game.getPlayers().put(playerName, player);
    }

    public void changeGamePhase(String gameId, GamePhase newPhase) {
        Game game = games.get(gameId);
        game.setGamePhase(newPhase);
    }

}
