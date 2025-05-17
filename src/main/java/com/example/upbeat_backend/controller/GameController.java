package com.example.upbeat_backend.controller;

import com.example.upbeat_backend.game.dto.request.GameConfigRequest;
import com.example.upbeat_backend.game.dto.response.game.GameCreatedResponseDTO;
import com.example.upbeat_backend.game.dto.response.game.GamePlayerResponseDTO;
import com.example.upbeat_backend.game.dto.response.game.GameStartResponseDTO;
import com.example.upbeat_backend.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @PostMapping("/create")
    public ResponseEntity<GameCreatedResponseDTO> createGame(@Valid @RequestBody GameConfigRequest request) {
        GameCreatedResponseDTO result = gameService.createGame(request.getGameConfig(), request.getMaxPlayers());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/add-player/{gameId}/{playerId}")
    public ResponseEntity<GamePlayerResponseDTO> addPlayerToGame(
            @PathVariable String gameId,
            @PathVariable String playerId) {
        GamePlayerResponseDTO result = gameService.addPlayerToGame(gameId, playerId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/start/{gameId}")
    public ResponseEntity<GameStartResponseDTO> startGame(@PathVariable String gameId) {
        GameStartResponseDTO result = gameService.startGame(gameId);
        return ResponseEntity.ok(result);
    }
}
