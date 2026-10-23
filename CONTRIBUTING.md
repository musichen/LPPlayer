# Contributing to LPPlayer

## Build

Build with Ant from the repo root (see the [README](README.md)):

```bash
ant
```

Or open the NetBeans project (`nbproject/`) and run `gui.LPPlayerGui`, which holds the `main` method.

## Where to change what

- Player engine: `src/objects/` (`MP3Player.java`, `MP3.java`)
- GUI: `src/gui/LPPlayerGui.java` and `src/images/`
- Helpers: `src/utils/` (file filtering, files, skins)

## Pull requests

Create a branch from `master`, keep the change focused, and say how you verified it (built with Ant, ran the app, played an MP3 if playback is affected). Note: this repo has no LICENSE file yet, so say in the PR if your contribution needs licensing discussion.
