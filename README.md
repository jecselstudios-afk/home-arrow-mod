# Home Arrow (Fabric, Minecraft 26.3)

Client-side mod. No server install needed.

- **H** - set your current position as home (one home per world/server, saved in `config/homearrow.properties`)
- **N** - toggle an on-screen arrow that points to your home, with the distance underneath

Both keys can be changed in Options > Controls > Key Binds > Home Arrow.

## Build
Needs JDK 25.

    ./gradlew build

The jar is in `build/libs/` (use the one without `-sources`).

## GitHub
Push to GitHub and the included workflow (`.github/workflows/build.yml`) builds it on every push.
Download the jar from the run's **Artifacts** section (Actions tab).
