#!/usr/bin/env bash
# Lokalny build bez Android SDK: aapt + javac + dx + apksigner.
# Wymaga: android.jar (np. robolectric android-all) w ANDROID_JAR.
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_JAR:?ustaw ANDROID_JAR}"
OUT=build-local; rm -rf $OUT; mkdir -p $OUT/gen $OUT/classes
M=app/src/main
# manifest z wersjami (w Gradle są w build.gradle)
sed 's|<manifest xmlns:android="http://schemas.android.com/apk/res/android">|<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="pl.cisnienie.kalendarz" android:versionCode="1" android:versionName="1.0"><uses-sdk android:minSdkVersion="26" android:targetSdkVersion="34"/>|' $M/AndroidManifest.xml > $OUT/AndroidManifest.xml
aapt package -f -m -J $OUT/gen -M $OUT/AndroidManifest.xml -S $M/res -I "$ANDROID_JAR" -F $OUT/res.apk
javac -encoding UTF-8 --release 8 -nowarn -classpath "$ANDROID_JAR" -d $OUT/classes \
  $(find $M/java $OUT/gen -name '*.java')
dalvik-exchange --dex --min-sdk-version=26 --output=$OUT/classes.dex $OUT/classes
cp $OUT/res.apk $OUT/unsigned.apk
(cd $OUT && zip -q unsigned.apk classes.dex)
zipalign -f 4 $OUT/unsigned.apk $OUT/aligned.apk
KS=signing/cisnienie.keystore

apksigner sign --ks $KS --ks-pass pass:android --out $OUT/Cisnienie.apk $OUT/aligned.apk
echo "OK: $OUT/Cisnienie.apk"
