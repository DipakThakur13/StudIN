# StudIN design sources

Vector sources for the brand and in-app illustrations. **Edit the SVGs here**, then regenerate
the Android VectorDrawables:

```bash
python3 design/svg2vd.py design/logo/studin_mark.svg app/src/main/res/drawable/ic_studin_mark.xml --width 48
python3 design/svg2vd.py design/logo/ic_launcher_foreground.svg app/src/main/res/drawable/ic_launcher_foreground.xml
python3 design/svg2vd.py design/logo/ic_launcher_background.svg app/src/main/res/drawable/ic_launcher_background.xml
python3 design/svg2vd.py design/logo/ic_launcher_monochrome.svg app/src/main/res/drawable/ic_launcher_monochrome.xml
python3 design/svg2vd.py design/logo/splash_mark.svg app/src/main/res/drawable/splash_mark.xml
python3 design/svg2vd.py design/logo/ic_stat_studin.svg app/src/main/res/drawable/ic_stat_studin.xml --width 24
for f in design/illustrations/ill_*.svg; do python3 design/svg2vd.py "$f" "app/src/main/res/drawable/$(basename "$f" .svg).xml" --width 200; done
```

`svg2vd.py` supports the subset used here: path/circle/ellipse/rect/line, groups with
translate/scale/rotate, solid colours with opacity, and linear/radial gradients.

## Logo – "Rising Sun Book"
An open book with a sun rising from its spine: **study** (book), **focus** (the sun's
single point of light) and **growth** (rising). Forest-green tile `#243F32 → #355846`,
burnt-orange cover and sun `#E66B38 → #F3A16D`, ivory pages `#FAFAF7`.

| File | Use |
|---|---|
| `logo/studin_mark.svg` | Master mark (in-app header, docs, legacy launcher PNGs) |
| `logo/ic_launcher_*.svg` | Adaptive icon layers (+ monochrome for themed icons) |
| `logo/splash_mark.svg` | Android 12+ splash icon (fits the circular mask) |
| `logo/wordmark*.svg` | Mark + "StudIN" wordmark for docs/store listing |

## Illustrations
Flat, warm, object-based scenes (no people) drawn on a 320×240 grid. They contain no
backdrop – the app draws the backdrop with theme colours so the same art works in light and
dark mode.

| File | Story |
|---|---|
| `ill_welcome` | Your study desk: phone timer, books, plant |
| `ill_focus` | Focus timer + shield; distracting apps crossed out |
| `ill_recall` | Flashcards and an MCQ with the right answer |
| `ill_ai_tutor` | StudIN's on-device tutor mascot |
| `ill_insights` | Progress chart, streak flame, consistency heatmap |
| `ill_privacy` | Locked phone, on-device chip, no cloud upload |
