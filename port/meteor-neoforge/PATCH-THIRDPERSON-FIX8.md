# XiaohSense ThirdPerson (fix8)

Minecraft 1.21.1 / NeoForge 21.1.265 / JDK 21.

Render category: Thirdperson (search thirdperson).
Enabling from first person selects vanilla rear third person. F5 remains the vanilla rear/front/first-person cycle. Disabling restores the perspective from activation.

- Thirdperson Distance: default 4 blocks, range 0–64; preserves vanilla camera wall collision and entity-scale distance factor.
- Thirdperson Camera Height: default 0, range -16–16 blocks; world-vertical offset from the vanilla eye-height anchor, before camera collision checks.
- Thirdperson Fov: default 70 degrees, range 30–110; base world FOV before vanilla dynamic effects and Zoom's multiplier. Does not change the game's saved FOV option.

Settings apply only while this module is enabled and the perspective is third person. Freecam takes precedence. ThirdPerson takes precedence over CameraTweaks distance/clip while applying; other camera modifications retain their existing behavior. User config paths and branding remain unchanged.

Build: port/meteor-neoforge/gradlew.bat -p port/meteor-neoforge --no-daemon shadowJar sourceBundle
Validation: tools/verify-jar.ps1; tools/run-release-test.ps1 -WithBaritone
Tests use the isolated release-instance single-player fixture, not user worlds. Results in meteor-selftest.txt.
