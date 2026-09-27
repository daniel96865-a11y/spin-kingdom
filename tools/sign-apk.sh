#!/usr/bin/env bash
# Signs an unsigned release APK (e.g. the CI artifact) locally with the Spin Kingdom key (APK Signature Scheme v2 + v3).
# Usage: tools/sign-apk.sh <unsigned.apk> <out.apk>
# The keystore lives OUTSIDE the repository: /workspace/.spinkingdom-signing/spinkingdom-release.jks (+ pass.txt)
set -euo pipefail
IN=$1; OUT=$2
KS_DIR=${SPINKINGDOM_KS_DIR:-/workspace/.spinkingdom-signing}
BT=${ANDROID_BUILD_TOOLS:-/workspace/android-sdk/build-tools/34.0.0}
mkdir -p "$(dirname "$OUT")"
TMP=$(mktemp --suffix=.apk)
"$BT/zipalign" -f -p 4 "$IN" "$TMP"
"$BT/apksigner" sign --ks "$KS_DIR/spinkingdom-release.jks" --ks-key-alias spinkingdom \
  --ks-pass file:"$KS_DIR/pass.txt" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$OUT" "$TMP"
rm -f "$TMP"
"$BT/apksigner" verify --verbose --print-certs "$OUT"
sha256sum "$OUT"
