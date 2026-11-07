#!/usr/bin/env bash
# JVM-only logic check. Compiles the pure-Kotlin `domain/` package and its unit tests with the
# Kotlin compiler (2.0.21) and JUnit 4 that ship inside a Gradle distribution, then runs the tests.
# Also compiles data/db/Entitas.kt + Pemetaan.kt against tiny Java stubs of the Room annotations,
# and data/ai (the Gemini HTTP client, tested against a local loopback server).
# This does NOT replace the Gradle/AGP build: it never touches Android, Compose, Room or DataStore,
# and it uses an older Kotlin compiler than the project. It only proves the domain logic.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GRADLE_BIN="$(readlink -f "$(command -v gradle)")"
LIB="$(dirname "$GRADLE_BIN")/../lib"
OUT="${TMPDIR:-/tmp}/asisten-jvm-check"
rm -rf "$OUT" && mkdir -p "$OUT/main" "$OUT/test"

jar() { ls "$LIB"/$1 | head -1; }
COMPILER_CP="$(jar 'kotlin-compiler-embeddable-*.jar'):$(jar 'kotlin-stdlib-*.jar'):$(jar 'kotlin-reflect-*.jar'):$(jar 'kotlin-script-runtime-*.jar'):$(jar 'kotlin-daemon-embeddable-*.jar'):$(jar 'trove4j*.jar' 2>/dev/null || true):$(jar 'annotations-*.jar'):$(jar 'kotlinx-coroutines-core-jvm-*.jar')"
STDLIB="$(jar 'kotlin-stdlib-*.jar')"
JUNIT="$(jar 'junit-4*.jar'):$(jar 'hamcrest-core-*.jar')"
COROUTINES="$(jar 'kotlinx-coroutines-core-jvm-*.jar')"

kc() { java -cp "$COMPILER_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect "$@"; }

# Pure domain code plus the Room entities and their mappers (annotations come from tools/stubs).
mapfile -t MAIN < <(find "$ROOT/app/src/main/kotlin/id/asisten/app/domain" -name '*.kt'; \
  find "$ROOT/app/src/main/kotlin/id/asisten/app/data/ai" -name '*.kt' 2>/dev/null; \
  find "$ROOT/app/src/main/kotlin/id/asisten/app/data/db" \( -name 'Entitas.kt' -o -name 'Pemetaan.kt' \) 2>/dev/null)
mkdir -p "$OUT/stubs"
javac -d "$OUT/stubs" $(find "$ROOT/tools/stubs" -name '*.java')
mapfile -t TEST < <(find "$ROOT/app/src/test/kotlin" -name '*.kt')
echo "Kompilasi ${#MAIN[@]} file domain dan ${#TEST[@]} file tes (Kotlin $(basename "$(jar 'kotlin-stdlib-*.jar')"))"

kc -cp "$STDLIB:$COROUTINES:$OUT/stubs" -d "$OUT/main" "${MAIN[@]}"
kc -cp "$STDLIB:$COROUTINES:$JUNIT:$OUT/stubs:$OUT/main" -d "$OUT/test" "${TEST[@]}"

mapfile -t CLASSES < <(cd "$OUT/test" && find . -name '*Test.class' | sed 's|^\./||; s|\.class$||; s|/|.|g' | sort)
java -cp "$STDLIB:$COROUTINES:$JUNIT:$OUT/stubs:$OUT/main:$OUT/test" org.junit.runner.JUnitCore "${CLASSES[@]}"
