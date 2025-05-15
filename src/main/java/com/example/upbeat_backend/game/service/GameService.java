package com.example.upbeat_backend.game.service;

import com.example.upbeat_backend.game.dto.reids.GameConfigDTO;
import com.example.upbeat_backend.game.dto.reids.GameInfoDTO;
import com.example.upbeat_backend.game.dto.response.event.ExecutionResult;
import com.example.upbeat_backend.game.dto.response.event.GameEvent;
import com.example.upbeat_backend.game.dto.response.game.GameResultNotificationDTO;
import com.example.upbeat_backend.game.exception.state.GameException;
import com.example.upbeat_backend.game.model.enums.GameStatus;
import com.example.upbeat_backend.game.plans.parser.Parser;
import com.example.upbeat_backend.game.plans.parser.ParserImpl;
import com.example.upbeat_backend.game.plans.tokenizer.Tokenizer;
import com.example.upbeat_backend.game.plans.tokenizer.TokenizerImpl;
import com.example.upbeat_backend.game.runtime.GameEnvironment;
import com.example.upbeat_backend.game.runtime.GameEnvironmentImpl;
import com.example.upbeat_backend.game.state.GameState;
import com.example.upbeat_backend.game.state.GameStateImpl;
import com.example.upbeat_backend.game.state.player.Player;
import com.example.upbeat_backend.game.state.region.Region;
import com.example.upbeat_backend.repository.RedisGameStateRepository;
import com.example.upbeat_backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class GameService {
    private final RedisGameStateRepository repository;
    private final UserService userService;
    private final SimpMessagingTemplate template;

    public String createGame(GameConfigDTO config, int maxPlayers) {
        String gameId = UUID.randomUUID().toString();
        repository.saveGameConfig(gameId, config);
        repository.saveTerritorySize(gameId, config.getRows(), config.getCols());
        repository.initializeGameInfo(gameId, maxPlayers);
        return gameId;
    }

    public void addPlayerToGame(String gameId, String playerId) {
        GameInfoDTO gameInfo = validateGameExists(gameId);
        int countPlayer = repository.getGamePlayers(gameId).size();
        if (countPlayer >= gameInfo.getMaxPlayers()) {
            throw new GameException.GameIsFull(gameId);
        }
        repository.addPlayerToGame(gameId, playerId);
    }

    public void startGame(String gameId) {
        GameInfoDTO gameInfo = validateGameExists(gameId);
        if (gameInfo.getGameStatus() != GameStatus.WAITING_FOR_PLAYERS) {
            throw new GameException.GameAlreadyStarted(gameId);
        }
        repository.updateGameStatus(gameId, GameStatus.IN_PROGRESS);
        initializeGameState(gameId);
    }

    private void initializeGameState(String gameId) {
        GameInfoDTO gameInfo = validateGameExists(gameId);
        if (gameInfo.getGameStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException.InvalidGameState(gameId, gameInfo.getGameStatus().name(), GameStatus.IN_PROGRESS.name());
        }
        GameStateImpl gameState = new GameStateImpl(gameId, repository, userService);
        gameState.initialize();
    }

    public ExecutionResult executePlayerPlan(String gameId, String playerId) {
        String plan = repository.getPlayerPlan(gameId, playerId);
        if (plan == null) {
            throw new GameException.PlanNotFound(playerId);
        }
        return executeConstructionPlan(gameId, playerId, plan);
    }

    public ExecutionResult executePlayerPlan(String gameId, String playerId, String plan) {
        if (plan == null) {
            throw new GameException.PlanNotFound(playerId);
        }

        GameConfigDTO gameConfig = repository.getGameConfig(gameId);
        Player player = validatePlayerExists(gameId, playerId);

        if (player.getBudget() < gameConfig.getRevCost()) {
            throw new GameException.NotEnoughBudget(playerId);
        }
        repository.incrementPlayerBudget(gameId, playerId, - gameConfig.getRevCost());
        repository.savePlayerPlan(gameId, playerId, plan);

        return executeConstructionPlan(gameId, playerId, plan);
    }

    private ExecutionResult executeConstructionPlan(String gameId, String playerId, String plan) {
        GameState gameState = new GameStateImpl(gameId, repository, userService);
        GameEnvironment environment = new GameEnvironmentImpl(repository, gameId, gameState, playerId);

        Map<String, Region> startState = gameState.getTerritory();

        Tokenizer tokenizer = new TokenizerImpl(plan);
        Parser parser = new ParserImpl(tokenizer);
        parser.parse().evaluate(environment);

        boolean isGameFinished = checkGameResult(gameId);

        String nextPlayerId = isGameFinished ? null : nextTurn(gameId);
        GameInfoDTO gameInfo = validateGameExists(gameId);
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

    private boolean checkGameResult(String gameId) {
        List<String> remainingPlayers = repository.getPlayersWithCityCenters(gameId);
        boolean isGameFinished = false;

        if (remainingPlayers.size() == 1) {
            String winnerId = remainingPlayers.getFirst();
            repository.setGameWinner(gameId, winnerId);
            isGameFinished = true;
        } else if (remainingPlayers.isEmpty()) {
            repository.setGameWinner(gameId, "draw");
            isGameFinished = true;
        }

        if (isGameFinished) {
            notifyGameFinished(gameId);
        }

        return isGameFinished;
    }

    private void notifyGameFinished(String gameId) {
        GameInfoDTO gameInfo = validateGameExists(gameId);
        String winnerId = gameInfo.getWinner();
        boolean isDraw = "draw".equals(winnerId);

        GameResultNotificationDTO notification = GameResultNotificationDTO.builder()
                .gameStatus(gameInfo.getGameStatus())
                .gameId(gameId)
                .isDraw(isDraw)
                .winnerId(winnerId)
                .build();

        List<String> players = repository.getGamePlayers(gameId);
        for (String playerId : players) {
            boolean isWinner = !isDraw && playerId.equals(winnerId);
            notification.setWinner(isWinner);
            template.convertAndSendToUser(playerId, "/queue/game-result", notification);
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

    private GameInfoDTO validateGameExists(String gameId) {
        GameInfoDTO gameInfo = repository.getGameInfo(gameId);
        if (gameInfo == null) {
            throw new GameException.GameNotFound(gameId);
        }
        return gameInfo;
    }

    private Player validatePlayerExists(String gameId, String playerId) {
        Player player = repository.getPlayer(gameId, playerId);
        if (player == null) {
            throw new GameException.PlayerNotFound(playerId);
        }
        return player;
    }

}
