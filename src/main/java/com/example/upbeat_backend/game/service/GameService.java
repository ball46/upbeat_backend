package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.plans.parser.Parser;
import com.example.upbeat_backend.game.plans.parser.ParserImpl;
import com.example.upbeat_backend.game.plans.tokenizer.Tokenizer;
import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import com.example.upbeat_backend.game.runtime.GameEnvironmentImpl;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.game.state.GameStateImpl;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class GameService {
    private final RedisGameStateRepository repository;

    public String createGame(GameConfigDTO config, int maxPlayers) {
        String gameId = UUID.randomUUID().toString();
        repository.saveGameConfig(gameId, config);
        repository.saveTerritorySize(gameId, config.getRows(), config.getCols());
        repository.initializeGameInfo(gameId, maxPlayers);
        return gameId;
    }

    public void addPlayerToGame(String gameId, String playerId) {
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        int countPlayer = repository.getGamePlayers(gameId).size();
        if (countPlayer >= gameInfo.getMaxPlayers()) {
            throw new IllegalStateException("Game is full");
        }
        repository.addPlayerToGame(gameId, playerId);
    }

    public void startGame(String gameId) {
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        if (gameInfo.getGameStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game cannot be started");
        }
        repository.updateGameStatus(gameId, GameStatus.IN_PROGRESS);
        initializeGameState(gameId);
    }

    private void initializeGameState(String gameId) {
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        if (gameInfo.getGameStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Game cannot be initialized");
        }
        GameStateImpl gameState = new GameStateImpl(gameId, repository);
        gameState.initialize();
    }

    public List<GameEvent> executePlayerPlan(String gameId, String playerId) {
        String plan = repository.getPlayerPlan(gameId, playerId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
        return executePlayerPlan(gameId, playerId, plan);
    }

    public List<GameEvent> executePlayerPlan(String gameId, String playerId, String plan) {
        repository.savePlayerPlan(gameId, playerId, plan);

        GameState gameState = new GameStateImpl(gameId, repository);
        GameEnvironmentImpl environment = new GameEnvironmentImpl(repository, gameId, gameState, playerId);

        Tokenizer tokenizer = new TokenizerImpl(plan);
        Parser parser = new ParserImpl(tokenizer);
        parser.parse().evaluate(environment);

        checkGameResult(gameId);

        nextTurn(gameId);

        return environment.getEvents();
    }

    private void checkGameResult(String gameId) {
        List<String> remainingPlayers = repository.getPlayersWithCityCenters(gameId);
        if (remainingPlayers.size() == 1) {
            repository.setGameWinner(gameId, remainingPlayers.getFirst());
        }
    }

    private void nextTurn(String gameId) {
        List<String> players = repository.getGamePlayers(gameId);
        String currentPlayerId = repository.getCurrentState(gameId).getCurrentPlayerId();

        int currentIndex = players.indexOf(currentPlayerId);
        int nextIndex = (currentIndex + 1) % players.size();
        String nextPlayerId = players.get(nextIndex);

        boolean isNewRound = nextIndex == 0;
        if (isNewRound) {
            calculateInterest(gameId);
            repository.incrementTurn(gameId);
        }

        repository.updateCurrentPlayer(gameId, nextPlayerId);
    }

    private void calculateInterest(String gameId) {
        GameStateImpl gameState = new GameStateImpl(gameId, repository);
        gameState.calculateInterest();
    }

}
