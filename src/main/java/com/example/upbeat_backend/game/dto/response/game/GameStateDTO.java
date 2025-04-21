package com.example.upbeat_backend.game.dto.response.game;

import com.example.upbeat_backend.game.dto.response.player.PlayerDTO;
import com.example.upbeat_backend.game.dto.response.region.RegionDTO;

import java.util.List;

public record GameStateDTO(
        List<PlayerDTO> players,
        List<RegionDTO> regions,
        String currentPlayerId
) {}
