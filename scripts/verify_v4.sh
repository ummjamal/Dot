#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME="${JAVA_HOME:-/usr/local/sdkman/candidates/java/21.0.10-ms}"
if [ -d "$JAVA_HOME/bin" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

./gradlew --stop >/dev/null 2>&1 || true
./gradlew --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs="-Xmx1024m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8" \
  testDebugUnitTest lintDebug assembleDebug

grep -R -nE 'com\.module\.dot|Theme\.Dot|DotApp|FirebaseHandler|FirebaseAuth|FirebaseDatabase|FirebaseStorage|google-services' \
  app/src app/build.gradle build.gradle 2>/dev/null && {
  echo "Legacy reference found." >&2
  exit 1
} || true

git diff --check

echo "V4 verification passed."
