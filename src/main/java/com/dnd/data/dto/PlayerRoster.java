package com.dnd.data.dto;

import com.dnd.model.character.PlayerCharacter;

import java.util.List;

public class PlayerRoster {
    private List<PlayerCharacter> players;

    public PlayerRoster() {
    }

    public List<PlayerCharacter> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerCharacter> players) {
        this.players = players;
    }
}

