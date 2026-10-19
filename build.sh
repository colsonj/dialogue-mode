#!/usr/bin/env bash
# Builds and signs build/dialogue-mode.apk using raw SDK tools (no Gradle).
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-$HOME/android-sdk}
BT=${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)}
JAR=$SDK/platforms/android-34/android.jar

rm -rf build && mkdir -p build/res build/gen build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res/res.zip
"$BT/aapt2" link -I "$JAR" --manifest AndroidManifest.xml -o build/base.apk \
    --java build/gen build/res/res.zip

javac -source 11 -target 11 -Xlint:-options -classpath "$JAR" -d build/classes \
    $(find src build/gen -name '*.java')
"$BT/d8" --min-api 28 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')

cp build/base.apk build/unsigned.apk
python3 -c "import zipfile; zipfile.ZipFile('build/unsigned.apk', 'a').write('build/dex/classes.dex', 'classes.dex')"
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk

KS=keystore.jks
[ -f $KS ] || keytool -genkeypair -keystore $KS -storepass android -keypass android \
    -alias dm -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Dialogue Mode" >/dev/null
"$BT/apksigner" sign --ks $KS --ks-pass pass:android --out build/dialogue-mode.apk build/aligned.apk
echo "Built build/dialogue-mode.apk"
