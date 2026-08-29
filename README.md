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

## Accounts & email setup (GUI)
Launching the GUI (`com.dnd.ui.GuiApp`) now requires logging in before the Player/DM
choice is shown. Accounts, registration codes, and password-reset codes are stored
locally at `data/auth/users.json` (never committed - see `.gitignore`).

Verification and password-recovery emails are sent over SMTP once you fill in
`data/auth/smtp-config.json` (auto-created with placeholders on first run):

```json
{
  "host": "smtp.gmail.com",
  "port": 587,
  "username": "you@gmail.com",
  "password": "an-app-password",
  "fromAddress": "you@gmail.com",
  "useTls": true,
  "enabled": true
}
```

Until `enabled` is set to `true` with real credentials, the app falls back to showing
the one-time verification/reset code directly in a dialog, so registration and
password recovery remain fully usable without any email provider configured.
