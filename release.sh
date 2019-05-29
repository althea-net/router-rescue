#!/bin/bash

# This script assumes you have router-rescue-signing-key.jks in the project root

RELEASE_DIR=app/build/outputs/apk/release
rm $RELEASE_DIR/app-release-unsigned-aligned.apk
zipalign -v -p 4 $RELEASE_DIR/app-release-unsigned.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
apksigner sign --ks router-rescue-signing-key.jks --out $RELEASE_DIR/app-signed-release.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
