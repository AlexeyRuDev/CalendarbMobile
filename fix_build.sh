#!/usr/bin/env bash
# fix_build.sh — repair the Gradle failure:
#
#   Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
#   > Could not create task ':app:kaptDebugKotlin'.
#      > 'org.gradle.api.file.FileCollection org.gradle.api.artifacts.Configuration.fileCollection(org.gradle.api.specs.Spec)'
#
# ROOT CAUSE
# ----------
# Configuration.fileCollection(Spec) was added in Gradle 7.4. The error means an
# old Kotlin Gradle Plugin (< 1.6.20, i.e. its kapt sub-plugin) is running on a
# Gradle distribution older than 7.4 (or the buildscript classpath pins an old
# kotlin-gradle-plugin that overrides the newer plugin declared elsewhere).
#
# FIX
# ---
# Align the toolchain to a mutually compatible set (verified by reproduction):
#   Gradle >= 7.5  +  Android Gradle Plugin >= 7.4.x  +  Kotlin Gradle Plugin >= 1.7.20
# This script upgrades the Gradle wrapper and the KGP version in place.
#
# Usage:
#   ./fix_build.sh [project-dir] [gradle-version] [kotlin-version] [agp-version]
#   defaults: project-dir = current dir, gradle 7.6.4, kotlin 1.8.22, agp 7.4.2
set -euo pipefail

DIR="${1:-.}"
GRADLE_VERSION="${2:-7.6.4}"
KOTLIN_VERSION="${3:-1.8.22}"
AGP_VERSION="${4:-7.4.2}"

WRAP_PROPS="$DIR/gradle/wrapper/gradle-wrapper.properties"

echo "==> Project: $DIR"
echo "==> Target versions: Gradle $GRADLE_VERSION | Kotlin $KOTLIN_VERSION | AGP $AGP_VERSION"

# 1) Upgrade the Gradle wrapper distribution (>= 7.4 is required for
#    Configuration.fileCollection(Spec); 7.6.4+ also works with JDK 17).
if [[ -f "$WRAP_PROPS" ]]; then
    echo "==> Patching $WRAP_PROPS"
    sed -i -E "s|^distributionUrl=.*|distributionUrl=https\\\\://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip|" "$WRAP_PROPS"
else
    echo "!!  $WRAP_PROPS not found — if you run Gradle from your system install,"
    echo "    make sure it is >= ${GRADLE_VERSION} (check: gradle --version)."
fi

# 2) Bump the Kotlin Gradle Plugin everywhere it is pinned.
for f in $(find "$DIR" -maxdepth 3 \( -name "build.gradle" -o -name "build.gradle.kts" \) -not -path "*/build/*"); do
    # Groovy buildscript form: classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:X.Y.Z"
    sed -i -E "s|(org\.jetbrains\.kotlin:kotlin-gradle-plugin:)[0-9][0-9a-zA-Z.\-]*|\1${KOTLIN_VERSION}|" "$f"
    # plugins DSL / version-catalog-style string form: id("org.jetbrains.kotlin...") version "X.Y.Z"
    sed -i -E "s|(org\.jetbrains\.kotlin[^\"']*(jvm|android|kapt|mpp)[^\"']*\"[[:space:]]+version[[:space:]]+\"[\"']?)[0-9][0-9a-zA-Z.\-]*(\")?|\1${KOTLIN_VERSION}\3|" "$f"
done

# 3) Bump the Android Gradle Plugin if it is pinned below 7.4.
for f in $(find "$DIR" -maxdepth 3 \( -name "build.gradle" -o -name "build.gradle.kts" \) -not -path "*/build/*"); do
    sed -i -E "s|(com\.android\.tools\.build:gradle:)[0-9]+\.[0-9]+\.[0-9]+|\1${AGP_VERSION}|" "$f"
done

# 4) Make sure AndroidX flag exists (needed once AGP >= 7 is used).
if [[ -f "$DIR/app/build.gradle" || -f "$DIR/app/build.gradle.kts" ]] && ! grep -q "android.useAndroidX" "$DIR/gradle.properties" 2>/dev/null; then
    echo "android.useAndroidX=true" >> "$DIR/gradle.properties"
fi

echo "==> Done. Clean stale caches and rebuild:"
echo "    rm -rf ~/.gradle/caches/transforms-* \$DIR/build \$DIR/app/build"
echo "    ./gradlew --stop"
echo "    ./gradlew :app:assembleDebug"
