#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk=${ANDROID_HOME:-"$HOME/Android/Sdk"}
jdk=${JAVA_HOME:-/opt/android-studio/jbr}
tools="$sdk/build-tools/36.0.0"
platform="$sdk/platforms/android-34/android.jar"
out="$PWD/build"
mkdir -p "$out/classes" "$out/dex"
export JAVA_HOME="$jdk"
export PATH="$jdk/bin:$PATH"
"$tools/aapt" package -f -M AndroidManifest.xml -S res -I "$platform" -F "$out/resources.apk"
"$jdk/bin/javac" --release 11 -classpath "$platform" -d "$out/classes" src/*.java
"$tools/d8" --min-api 28 --lib "$platform" --output "$out/dex" "$out/classes/local/petlibrocompanion/"*.class
cp "$out/resources.apk" "$out/unsigned.apk"
(cd "$out/dex" && zip -q "$out/unsigned.apk" classes.dex)
"$tools/zipalign" -f 4 "$out/unsigned.apk" "$out/aligned.apk"
if [[ ! -f "$out/debug.p12" ]]; then
    "$jdk/bin/keytool" -genkeypair -keystore "$out/debug.p12" -storetype PKCS12 \
        -storepass android -keypass android -alias debug -keyalg RSA -validity 10000 \
        -dname 'CN=Feeder Check Local Debug'
fi
"$tools/apksigner" sign --ks "$out/debug.p12" --ks-pass pass:android \
    --v4-signing-enabled false --out "$out/feeder-check.apk" "$out/aligned.apk"
"$tools/apksigner" verify "$out/feeder-check.apk"
