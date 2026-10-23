# LPPlayer

Java desktop MP3 player app with a Swing GUI.

## Structure

- `src/gui/LPPlayerGui.java` - the main window; its `main` method is the entry point
- `src/objects/` - the player engine (`MP3Player.java`, `MP3.java`)
- `src/utils/` - helpers for files, skins, and MP3 file filtering
- `src/images/` - GUI images and skins

## Build and run

The project ships with an Ant build file (`build.xml`) and a NetBeans project (`nbproject/`):

```bash
ant
```

Then run the built app from the `dist/` output, or open the project in NetBeans and run `gui.LPPlayerGui`. A prebuilt copy is kept under `build/` and `dist/`.

## Notes

There is no LICENSE file in this repo yet, so the code is all rights reserved by default until one is added.
