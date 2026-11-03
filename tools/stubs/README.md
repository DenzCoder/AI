# Test-only stubs

`androidx/room/*.java` mimic the few Room annotations used by `data/db/Entitas.kt` so that
`tools/jvm-check.sh` can compile the entities and the entity<->domain mappers on a plain JVM.
They are never part of the Android build (Gradle only reads `app/src`). The real Room artifacts
are used there.
