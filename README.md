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
- `src/main/resources/data/srd/` holds the standard-rules dataset that new campaigns are
  seeded from (see below).

### Standard rules dataset
Creating a campaign without ticking **Start blank** seeds it from the bundled standard-rules
catalogues in `src/main/resources/data/srd/`, so you can start playing with conventional
D&D-style content instead of an empty world:

| Catalogue | Entries | Contents |
| --- | --- | --- |
| `races.json` | 21 | The core ancestries plus their common subraces |
| `classes.json` | 12 | Hit die, primary abilities and saving-throw bonuses per class |
| `items.json` | 94 | 41 weapons (incl. 5 magic weapons), 21 armours and shields, 12 potions and oils, 20 pieces of adventuring gear |
| `spells.json` | 67 | Cantrips through 9th level, with school, casting method, range and mana cost |
| `monsters.json` | 59 | CR 0 to CR 24, from goblins to an ancient red dragon |
| `beasts.json` | 26 | Animals and mounts by habitat |
| `npcs.json` | 22 | Ready-to-use townsfolk, patrons and villains |
| `places.json` | 18 | Settlements, dungeons and landmarks, most linked to a battle map |
| `maps.json` | 12 | Grid battle maps (see below) |
| `effects.json` | 34 | Conditions and ongoing effects, including the six the damage engine looks up by id |
| `damage-types.json` | 15 | Physical and elemental damage types |
| `languages.json` | 18 | Each with its primer book and study time |
| `books.json` | 26 | 18 language primers plus 8 pieces of in-world lore |
| `alchemy-ingredients.json` | 24 | Reagents for brewing |
| `dice.json` | 7 | d4 through d100 |
| `players.json` | 6 | A pregenerated level 3 party, already equipped |

Bundled battle maps: the Gilded Stag tavern (common room and guest rooms), Greenhollow's
village square, the Ironhand smithy, the temple of the Dawnfather, Stonekeep's ground floor,
a standing-stone forest clearing, the Barrow Crypt, Blackfen Cave, a roadside bandit camp,
Magister Orin's tower, and a blank open-ground map for improvised fights. Impassable cells
(walls, furniture, deep water) render as black in the map editor.

A default campaign also gets a starter `storyline/` chapter with three session files that
demonstrate the `[READ ALOUD]` / `[DM NOTE]` markers and map links.

Ticking **Start blank** still produces empty catalogues for every one of these files.

The dataset is original text written for this project, describing rules concepts from the
System Reference Document (CC-BY-4.0); no SRD wording is reproduced verbatim.

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
  - `Insert Map` - drops a `[map:<id>|<label>]` link for any map in the campaign. Double-click
    a link (or put the caret in it and press `Open Map Link`) to launch the battle map.
  - A session timer that counts up from zero or down from a set number of minutes.
  - Heading, bullets, checklist, divider, find, and `Ctrl+S` to save.

Read-aloud markers and map links are stripped from the Player View, so the table only ever
sees the prose.

## Battle map

The battle map is the playground where encounters are run. Open it from a `[map:...]` link in
a session file, or from `Run Battle` in the map viewer.

- **Turn order.** `Roll Initiative` rolls d20 + DEX for every creature on the map and sorts
  them; `Next Turn` cycles forever, skipping the dead and bumping the round counter on each
  wrap. The roster shows the current combatant highlighted with the next few queued behind it.
- **Token detail.** Click any token to swap the left panel into its sheet - portrait, stats,
  health and mana bars, coins, items, spells, and abilities - with `← Back` to return to the
  roster. Click empty space or press `Esc` to deselect.
- **Vitals.** Damage, healing, max HP/mana, coins, and initiative are all editable in place.
  Dropping to zero hit points knocks a creature down rather than killing it; from there
  `Failed Save` counts 3 -> 2 -> 1 on the token in red, and the third failure marks it dead
  with a red cross. `Made Save` and healing above zero bring it back.
- **Ranges.** Hovering a spell or ability draws its range as a disc around the token (5 ft per
  cell) and rings every creature inside it, so you can see legal targets before committing.
- **Map control.** Add and remove tokens, drag them between cells, toggle a cell impassable,
  show or hide health bars, zoom, and save the map back to the campaign. A timer is available
  here too.
