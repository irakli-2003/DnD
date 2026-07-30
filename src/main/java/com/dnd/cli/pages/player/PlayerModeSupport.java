package com.dnd.cli.pages.player;

import com.dnd.cli.core.CliSession;
import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.PlayerCharacter;

/**
 * Small helpers shared by the player-mode pages: resolving the active
 * campaign's repositories and the currently selected {@link PlayerCharacter}
 * from the session-scoped active player character id
 * ({@link CliSession#getActivePlayerCharacterId()}).
 */
final class PlayerModeSupport {
    private PlayerModeSupport() {
    }

    /** @return repositories for the session's active campaign, or {@code null} if none is selected. */
    static CampaignRepositories repositoriesFor(CliSession session) {
        if (session.getCampaignContext() == null) {
            return null;
        }
        return new CampaignRepositories(session.getCampaignContext().getPath());
    }

    /** @return the currently selected {@link PlayerCharacter}, or {@code null} if none is available. */
    static PlayerCharacter activeCharacter(CliSession session, CampaignRepositories repositories) {
        if (repositories == null || session.getActivePlayerCharacterId() == null) {
            return null;
        }
        return repositories.players().getById(session.getActivePlayerCharacterId());
    }
}

