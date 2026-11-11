# 2h NPC & Object Customizer

> This is the full description of 2h NPC & Object Customizer - every row, menu and setting. The short version,
> with pictures, is the plugin's page on the Plugin Hub (the repository's README).

Record a stretch of play, or look a monster up by name, and the sidebar lists every NPC and object as a row with a
picture drawn from the game's own model. Any named NPC or object can then be outlined or hidden from its row. The
Changes tab lists what you changed, and Undo puts it back.

## Recording

Press **Record** at the top of the sidebar and play. While it records, the top line reads
*Recording 0:42 - 87 things*: how long it has run and how many different things it has seen. No row is drawn while
it records; the list appears when you press **Stop**. A recording also stops when you log out, hop worlds or turn
the plugin off.

What a recording catches:

| Section | What is in it |
|---|---|
| NPCs | every NPC in the scene when you start, and every NPC that appears after |
| Objects | objects that appeared while you recorded |
| Scenery nearby | named objects within 15 tiles of you, looked at when you start and every 10 game ticks |
| Effects | graphics and projectiles, with the NPCs a graphic was seen on |
| Animations | NPC animations, with the NPCs they were seen on |
| Your spells | the spells you cast by clicking; only the spell's name is kept, never its target |

Other players are never recorded or listed. Autocast spells are not caught. A thing that appears many times is one
row with a count, not many rows. Each section has a limit (300 named NPCs, 300 named objects, 500 scenery, 500
animations, 300 effects, 100 spells); past it the section ends with *and N more not listed*.

## The list

From the top: the Record button and the status line, the look-up bar, the session picker, and **List | Changes**.

- **The session picker** shows the recording or look-up on screen; press it to pick another. The last five
  recordings and the last five look-ups are kept until you close the client. They are never saved to disk.
- **Filter** narrows every section to the rows whose name holds what you type.
- **A section's header** shows its name and count; press it to fold the section away or open it again.

**A row** is a picture, the name in bold, and a grey line under it:

- an NPC or object: its ids, *seen n* and when it was first seen (minutes and seconds from the start);
- an effect or animation: *on* and the NPCs it was seen on, the count and the time;
- a row from the wiki: *From the wiki* and its ids.

A thing with no game name is listed as *Unnamed NPC 1234* or *Unnamed object 1234*. Effects and animations are
listed by id, as *Graphic 2576* or *Animation 866*. A picture can take a moment: some keep filling in shortly after
Stop, and a row whose picture could not be drawn stays blank.

## The look-up

Type a monster's name in the bar that reads *Look up a monster*. From three letters on, up to eight names from the
OSRS wiki are offered under it; pick one. The list then holds:

- **This monster**: one row per name, with the wiki's picture, its ids and its right-click options as a grey line;
- **Linked on its wiki page**: one row for each monster its wiki page links to, marked *From the wiki*.

These rows work like recorded NPC rows. The wiki gives the names, ids and pictures; the plugin uses the game's own
name for each id it knows. No wiki text is copied or shown. While it asks, the bar says *Looking...*; a name with no
monster says *No monster by that name*; if the wiki does not answer it says so. Escape clears the bar.

## A row's menu

Press **...** at the right of a row. The menu opens with the thing's name.

| Row | Menu |
|---|---|
| A named NPC | Outline, Colour, Width, Hide, Copy id |
| A named object | Outline here, Colour, Width, Show / Hide here / Hide everywhere, Copy id |
| A row from the wiki | the NPC's menu and *Remove from this list* |
| Anything else | one grey line saying why it cannot be changed, and Copy id |

- **Outline** draws RuneLite's own outline round the thing. **Colour** opens RuneLite's colour picker (cyan at
  first); **Width** is 1 to 6 (2 at first).
- **An NPC** is outlined or hidden wherever an NPC of that name appears, also when it changes into a form with that
  name.
- **An object** is outlined at the places it was seen in the recording (up to 16). **Hide here** hides it at those
  places; **Hide everywhere** hides every object of that name. **Show** undoes the hide.
- **A badge** after the row's name shows its outline colour, that it is hidden, or both.
- **Copy id** puts the row's ids on your clipboard, separated by commas.

The grey line says one of: *No name, so it can't be changed*; *The Plugin Hub doesn't allow changing this* (a few
objects other hiders also leave alone); *Shown only* (effects, animations and spells).

Up to 40 outlines are drawn at once, the nearest first. There are no right-click edits in this version: every change
is made from the sidebar, and the game's own menus are left as they are.

## Changes

**Changes**, beside List, has one row per change: whether it is an NPC or an object, the name, what it does
(*Outline, width 3*, *Outline here, width 2*, *Hidden*, *Hidden here*, *Hidden everywhere*) and an **Undo** link. **Undo all** at the bottom
asks once before it removes them all. A thing comes back as soon as its change is undone.

Changes are saved in RuneLite's settings and come back after a restart. Up to 500 are kept.

## Settings

| Setting | What it does |
|---|---|
| Use the OSRS wiki | Lets the look-up bar ask the OSRS wiki. On by default. Off, the bar reads *Wiki look-up is off* and sends nothing. |
| Sidebar icon | Which of ten pictures the plugin's sidebar button shows. *Three tiles* at first. |

Both are in RuneLite's settings and in the panel's own menu, behind the settings icon at the top right of the sidebar.
In that menu **Sidebar icon** opens the ten pictures, each shown beside its name; pick one and the button changes at
once. The menu also shows the plugin's version, links to the 2hBuilds Discord, X and GitHub pages and this plugin's
GitHub page, and **Reset to default**, which puts both settings back. Reset does not touch your changes.

## What it sends and saves

- **Sent**: only the look-up. The name you type, and the titles of the pages it finds, go to
  `oldschool.runescape.wiki`, and the pictures come from there. Nothing about you or your account is sent.
- **Saved**: your changes and the two settings, in RuneLite's settings (config group `scene-customizer`). The plugin
  writes no files.
- **Not saved**: recordings and look-ups, which live in memory until you close the client.

## Limits

- Only NPCs and objects with a game name can be outlined or hidden. Things with no name are listed but cannot be
  changed.
- Effects, animations and spells are shown only.
- An object's outline applies at the places it was seen, not to every object of that name.
- There are no right-click (menu) edits in this version.

## Licence

BSD 2-Clause. See `LICENSE`.
