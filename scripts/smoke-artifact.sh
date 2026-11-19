#!/usr/bin/env bash
# Smoke test of the PUBLISHED artifact, offline from any OnlineSim host:
#  1. publishToMavenLocal into build/m2 (never ~/.m2);
#  2. check the publication: jar + sources + javadoc (Dokka) + POM + Gradle module, POM fields Maven Central requires,
#     the dependencies (Java SDK, coroutines, stdlib only), Java 11 in the module metadata, every class file major 55;
#  3. resolve it (plus the Java SDK from Maven Central) from an empty Gradle project and run the README "Testing"
#     example against MockOnlineSim.
# SMOKE_JAVA_HOME (optional) runs the example on another JDK, e.g. 11; the build itself needs JDK 17+.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
version="$(sed -n 's/^version = "\(.*\)"$/\1/p' build.gradle.kts)"
kotlin="$(sed -n 's/^ *kotlin("jvm") version "\(.*\)"$/\1/p' build.gradle.kts)"
repo="$root/build/m2"
app="$root/build/smoke-app"
fail() { echo "smoke: $*" >&2; exit 1; }

rm -rf "$repo/ru/onlinesim/onlinesim-kotlin" "$app"
./gradlew publishToMavenLocal "-Dmaven.repo.local=$repo" --no-daemon --quiet

dir="$repo/ru/onlinesim/onlinesim-kotlin/$version"
base="$dir/onlinesim-kotlin-$version"
for f in "$base.jar" "$base-sources.jar" "$base-javadoc.jar" "$base.pom" "$base.module"; do
  [[ -s "$f" ]] || fail "missing $f"
done
# grep without -q: an early exit would SIGPIPE unzip under pipefail.
unzip -l "$base-javadoc.jar" | grep 'ru.onlinesim.kotlin.mock/-mock-online-sim/index.html' >/dev/null || fail "javadoc jar lacks the mock"
unzip -l "$base-sources.jar" | grep 'ru/onlinesim/kotlin/mock/MockOnlineSim.kt' >/dev/null || fail "sources jar lacks the mock"

pom="$base.pom"
for tag in '<name>' '<description>' '<url>' '<licenses>' '<license>' '<scm>' '<connection>' '<developers>' '<developer>'; do
  grep -q "$tag" "$pom" || fail "POM lacks $tag"
done
deps="$(grep -o '<artifactId>[^<]*</artifactId>' "$pom" | sed 's/<[^>]*>//g' | grep -v '^onlinesim-kotlin$' | sort -u | tr '\n' ' ')"
[[ "$deps" == "kotlin-stdlib kotlinx-coroutines-core-jvm onlinesim-api " ]] || fail "POM dependencies are '$deps'"
grep -q '"org.gradle.jvm.version": 11' "$base.module" || fail "module metadata does not say Java 11"
echo "publication $version: jar, sources, javadoc, pom, module ok; POM complete; deps: $deps"

classes=0
while IFS= read -r cls; do
  major="$(unzip -p "$base.jar" "$cls" | od -An -j6 -N2 -tu1 | awk 'NF {print $1 * 256 + $2}')"
  [[ "$major" == 55 ]] || fail "$cls has class file major $major, expected 55"
  classes=$((classes + 1))
done < <(unzip -Z1 "$base.jar" | grep '\.class$')
echo "class files: $classes, all major 55"

mkdir -p "$app/src/main/kotlin"
cat > "$app/settings.gradle.kts" <<'EOF'
rootProject.name = "smoke-app"
EOF
cat > "$app/build.gradle.kts" <<EOF
plugins {
    kotlin("jvm") version "$kotlin"
    application
}
repositories {
    maven { url = uri("$repo") }
    mavenCentral()
}
dependencies { implementation("ru.onlinesim:onlinesim-kotlin:$version") }
java { targetCompatibility = JavaVersion.VERSION_11 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
application { mainClass.set("MainKt") }
EOF
# The first kotlin block after "## Testing" in README.md.
awk '/^## Testing/ {t=1} t && /^```kotlin/ {b=1; next} b && /^```/ {exit} b {print}' README.md \
  > "$app/src/main/kotlin/Main.kt"
grep -q 'fun main' "$app/src/main/kotlin/Main.kt" || fail "README Testing example not found"

./gradlew -p "$app" installDist --no-daemon --quiet
JAVA_HOME="${SMOKE_JAVA_HOME:-${JAVA_HOME:-}}" "$app/build/install/smoke-app/bin/smoke-app"
echo "README \"Testing\" example from the artifact: ok ($("${SMOKE_JAVA_HOME:-${JAVA_HOME:-/usr}}/bin/java" -version 2>&1 | head -1))"
