package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.response.event.ExecutionResult;
import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.plans.parser.Parser;
import com.example.upbeat_backend.game.plans.parser.ParserImpl;
import com.example.upbeat_backend.game.plans.tokenizer.Tokenizer;
import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import com.example.upbeat_backend.game.runtime.GameEnvironmentImpl;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.game.state.GameStateImpl;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import com.example.upbeat_backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class GameService {
    private final RedisGameStateRepository repository;
    private UserService userService;

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
        GameStateImpl gameState = new GameStateImpl(gameId, repository, userService);
        gameState.initialize();
    }

    public ExecutionResult executePlayerPlan(String gameId, String playerId) {
        String plan = repository.getPlayerPlan(gameId, playerId);
        if (plan == null) {
            throw new IllegalArgumentException("Plan cannot be null");
        }
        return executePlayerPlan(gameId, playerId, plan);
    }

    public ExecutionResult executePlayerPlan(String gameId, String playerId, String plan) {
        repository.savePlayerPlan(gameId, playerId, plan);

        GameState gameState = new GameStateImpl(gameId, repository, userService);
        GameEnvironmentImpl environment = new GameEnvironmentImpl(repository, gameId, gameState, playerId);

        Map<String, Region> startState = gameState.getTerritory();

        Tokenizer tokenizer = new TokenizerImpl(plan);
        Parser parser = new ParserImpl(tokenizer);
        parser.parse().evaluate(environment);

        checkGameResult(gameId);

        String nextPlayerId = nextTurn(gameId);
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        GameStatus gameStatus = gameInfo.getGameStatus();
        List<GameEvent> events = environment.getEvents();
        Map<String, Region> finalState = gameState.getTerritory();

        return ExecutionResult.builder()
                .gameId(gameId)
                .playerId(playerId)
                .nextPlayerId(nextPlayerId)
                .gameStatus(gameStatus)
                .events(events)
                .startState(startState)
                .finalState(finalState)
                .build();
    }

    private void checkGameResult(String gameId) {
        List<String> remainingPlayers = repository.getPlayersWithCityCenters(gameId);
        if (remainingPlayers.size() == 1) {
            repository.setGameWinner(gameId, remainingPlayers.getFirst());
        }
    }

    private String nextTurn(String gameId) {
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

        return nextPlayerId;
    }

    private void calculateInterest(String gameId) {
        GameStateImpl gameState = new GameStateImpl(gameId, repository, userService);
        gameState.calculateInterest();
    }

}
