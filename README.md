# DnD
Structured environment for running Dungeons and dragons campaign

## CLI quick start
Build and run the CLI entry point:

```powershell
mvn test
mvn package
java -jar target\dnd-campaign-manager-1.0.0.jar
```

### Campaign storage
- `src/main/resources/data/default-campaign/` holds the template campaign (read-only in CLI).
- `src/main/resources/data/custom-campaigns/` stores one folder per custom campaign.

### Commands
- `dm` or `player` at the landing page.
- `create` in campaign selection to create a new campaign.
- `back` or `b` to go back.
- `exit` to quit.
