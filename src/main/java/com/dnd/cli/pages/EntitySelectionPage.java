package com.dnd.cli.pages;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.Page;
import com.dnd.cli.pages.entity.EntityCrudService;
import com.dnd.data.CampaignPaths;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.IdHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin CLI page that routes create/edit/delete/open commands for a chosen
 * entity type to {@link EntityCrudService}. All reflection-based form
 * building, type resolution, and CRUD logic live outside of this class.
 */
public class EntitySelectionPage implements Page {
    public enum Operation {
        CREATE("Create Content", "Choose what to create."),
        EDIT("Edit Content", "Choose what to edit."),
        DELETE("Delete Content", "Choose what to delete."),
        OPEN("Open Content", "Choose what to view.");

        private final String title;
        private final String body;

        Operation(String title, String body) {
            this.title = title;
            this.body = body;
        }

        public String getTitle() {
            return title;
        }

        public String getBody() {
            return body;
        }
    }

    private final Operation operation;
    private final EntityCrudService crudService;
    private Page parent;

    // Repositories/IdHandler are cheap value holders over a campaign directory,
    // but there is no need to reconstruct them on every keystroke. Cache by
    // campaign path and rebuild only when the active campaign changes.
    private Path cachedCampaignPath;
    private CampaignRepositories cachedRepositories;
    private IdHandler cachedIdHandler;

    public EntitySelectionPage(Operation operation, Page parent) {
        this(operation, parent, new EntityCrudService());
    }

    public EntitySelectionPage(Operation operation, Page parent, EntityCrudService crudService) {
        this.operation = operation;
        this.parent = parent;
        this.crudService = crudService;
    }

    public void setParent(Page parent) {
        this.parent = parent;
    }

    @Override
    public Page getParent() {
        return parent;
    }

    @Override
    public String getTitle() {
        return operation.getTitle();
    }

    @Override
    public String getBody() {
        return operation.getBody();
    }

    @Override
    public List<CommandSpec> getCommands() {
        List<CommandSpec> commands = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            commands.add(new CommandSpec(type.getKey(), type.getLabel(), selectedSession -> handleEntity(selectedSession, type)));
        }
        return commands;
    }

    private Page handleEntity(CliSession session, EntityType type) {
        if (session.getCampaignContext() == null) {
            session.getConsole().println("No campaign selected. Choose a campaign first.");
            return this;
        }

        Path campaignPath = session.getCampaignContext().getPath();
        ensureRepositoriesFor(campaignPath);

        switch (operation) {
            case CREATE:
                crudService.create(session, cachedRepositories, cachedIdHandler, type);
                break;
            case EDIT:
                crudService.edit(session, cachedRepositories, type);
                break;
            case DELETE:
                crudService.delete(session, cachedRepositories, cachedIdHandler, type);
                break;
            case OPEN:
                Page openTarget = crudService.open(session, cachedRepositories, type, this);
                if (openTarget != null) {
                    return openTarget;
                }
                break;
            default:
                break;
        }
        return this;
    }

    private void ensureRepositoriesFor(Path campaignPath) {
        if (campaignPath.equals(cachedCampaignPath)) {
            return;
        }
        cachedCampaignPath = campaignPath;
        cachedRepositories = new CampaignRepositories(campaignPath);
        CampaignPaths paths = new CampaignPaths(campaignPath);
        cachedIdHandler = new IdHandler(paths.idRegistryFile());
    }
}
