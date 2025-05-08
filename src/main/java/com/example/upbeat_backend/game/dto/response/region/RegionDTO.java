package com.example.upbeat_backend.game.dto.response.region;

public record RegionDTO(
        long deposit,
        int row,
        int col,
        String ownerName
) {
    public static RegionDTO from(com.example.upbeat_backend.game.state.region.Region region) {
        return new RegionDTO(
                region.getDeposit(),
                region.getRow(),
                region.getCol(),
                region.getOwner() != null ? region.getOwner() : null
        );
    }
}
