package com.dnd.ui;

import com.dnd.cli.core.CliSession;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.JsonMappers;
import com.dnd.ui.scenes.*;
import com.dnd.ui.scenes.player.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

public class SceneRouter {
    private final UiSession uiSession;
    private final Stage stage;

    public SceneRouter(UiSession uiSession, Stage stage) {
        this.uiSession = uiSession;
        this.stage = stage;
    }

    /** Scenes reachable without being logged in - everything else redirects to LOGIN. */
    private static boolean isPublic(SceneType type) {
        return type == SceneType.LOGIN || type == SceneType.REGISTER || type == SceneType.VERIFY_EMAIL
            || type == SceneType.FORGOT_PASSWORD || type == SceneType.RESET_PASSWORD;
    }

    public void goTo(SceneType type) {
        if (!isPublic(type) && !uiSession.isLoggedIn()) {
            type = SceneType.LOGIN;
        }
        if (type == SceneType.DM_MENU) {
            autoPush();
        }
        Scene scene = buildScene(type);
        stage.setScene(scene);
    }

    private Scene buildScene(SceneType type) {
        switch (type) {
            case LOGIN: return new com.dnd.ui.scenes.LoginScene(uiSession).build();
            case REGISTER: return new com.dnd.ui.scenes.RegisterScene(uiSession).build();
            case VERIFY_EMAIL: return new com.dnd.ui.scenes.VerifyEmailScene(uiSession).build();
            case FORGOT_PASSWORD: return new com.dnd.ui.scenes.ForgotPasswordScene(uiSession).build();
            case RESET_PASSWORD: return new com.dnd.ui.scenes.ResetPasswordScene(uiSession).build();
            case LANDING: return new LandingScene(uiSession).build();
            case CAMPAIGN_SELECTION: return new CampaignSelectionScene(uiSession).build();
            case CREATE_CAMPAIGN: return new CreateCampaignScene(uiSession).build();
            case DM_MENU: return new DmMenuScene(uiSession).build();
            case DM_ONLINE_SESSION: return new OnlineSessionScene(uiSession).build();
            case PLAYER_CAMPAIGN_SELECTION: return new PlayerCampaignSelectionScene(uiSession).build();
            case PLAYER_CHARACTER_SELECTION: return new PlayerCharacterSelectionScene(uiSession).build();
            case PLAYER_HOME: return new PlayerHomeScene(uiSession).build();
            case PLAYER_ONLINE_SESSION: return new PlayerOnlineSessionScene(uiSession).build();
            case ENTITY_LIST: return new com.dnd.ui.scenes.EntityListScene(uiSession).build();
            case ENTITY_DETAIL: return new com.dnd.ui.scenes.EntityDetailScene(uiSession).build();
            case MAP_VIEW: return new com.dnd.ui.scenes.MapViewScene(uiSession).build();
            case MAP_EDITOR: return new com.dnd.ui.scenes.MapEditorScene(uiSession).build();
            case CHARACTER_PROFILE: return new com.dnd.ui.scenes.player.CharacterProfileScene(uiSession).build();
            case STORYLINE: return new com.dnd.ui.scenes.StorylineScene(uiSession).build();
            default: return new LandingScene(uiSession).build();
        }
    }

    private void autoPush() {
        CliSession session = uiSession.getSession();
        if (!session.isOnline() || session.getCampaignContext() == null) return;
        try {
            ObjectMapper mapper = JsonMappers.create();
            CampaignRepositories repos = new CampaignRepositories(session.getCampaignContext().getPath());
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("campaign", session.getCampaignContext().getName());
            snapshot.put("players", repos.players().list());
            snapshot.put("maps", repos.maps().list());
            snapshot.put("npcs", repos.npcs().list());
            snapshot.put("monsters", repos.monsters().list());
            session.getFirebaseSync().push(mapper.writeValueAsString(snapshot));
        } catch (Exception ignored) {}
    }
}
