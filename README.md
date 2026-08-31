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

### Troubleshooting email

Use the **Test email settings** button on the login screen. It sends a real message to
`fromAddress` and reports the exact failure instead of a generic "not configured" notice.
The same real reason is now shown in the code-fallback dialog whenever a send fails.

Common causes:

- **`host` must be a mail server, not an address** - `smtp.gmail.com`, not `you@gmail.com`.
- **Gmail requires an App Password**, not your account password. Enable 2-Step Verification,
  then create one at <https://myaccount.google.com/apppasswords>. Google shows it as
  `abcd efgh ijkl mnop`; the spaces are display-only and are stripped automatically.
- **Connection timed out / no route to host** - your network is blocking outbound SMTP.
  Office, university, and VPN networks very commonly block ports 587, 465 and 25. This is
  *not* a configuration error: the credentials can be perfectly correct and the connection
  still never opens. Try port `465` (implicit SSL, selected automatically when you set
  `"port": 465`), or run the app from a home network or phone hotspot. Nothing in the app
  can work around a blocked port.
- With email unavailable for any reason, the in-app code dialog still lets you finish
  registering or resetting a password.

## Storyline (DM menu)

The Storyline section pairs a file tree of session notes (`<campaign>/storyline/`) with a
horizontally scrollable story-arc timeline. Sessions are the text files; folders become
half-circle arcs spanning their sessions, so the root folder arcs across the whole campaign.

- **Ordering** is explicit and persisted per folder in a hidden `.storyline-order` file.
  Drag items in the tree, or use the arrow buttons, to reorder. The arc timeline reads the
  same order, so the two views never disagree.
- **Editor** opens maximized and fills the window. Its toolbar has:
  - `Insert Info` - a cascading menu of every campaign catalog (NPCs, monsters, items,
    spells, races, ...); hover a category to get a filterable, scrollable list of its
    entries, and click one to insert a formatted block about it at the caret.
  - `Insert Block` - templates for a scene, encounter, NPC beat, skill check, loot, or
    session recap.
  - `Read-Aloud` / `DM Note` - wrap the selection in `[READ ALOUD]` or `[DM NOTE]` markers,
    keeping player-facing prose separate from private prep.
  - `Player View` - a large-type, read-only window showing only the read-aloud passages,
    for reading straight to the table.
  - Heading, bullets, checklist, divider, find, and `Ctrl+S` to save.
