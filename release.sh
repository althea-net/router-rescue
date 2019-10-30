#!/bin/bash

# This script assumes you have router-rescue-signing-key.jks in the project root

cp ~/Downloads/openwrt-ipq40xx-linksys_ea6350v3-squashfs-sysupgrade.bin app/src/main/assets/ea6350.bin 
cp ~/Downloads/openwrt-ipq40xx-glinet_gl-b1300-squashfs-sysupgrade.bin app/src/main/assets/glb1300.bin 
cp ~/Downloads/openwrt-ar71xx-generic-mynet-n750-squashfs-sysupgrade.bin app/src/main/assets/n750.bin 
cp ~/Downloads/openwrt-ar71xx-generic-mynet-n600-squashfs-sysupgrade.bin app/src/main/assets/n600.bin 
cp ~/Downloads/openwrt-mvebu-cortexa9-linksys-wrt3200acm-squashfs-sysupgrade.bin app/src/main/assets/wrt3200acm.bin 
cp ~/Downloads/openwrt-mvebu-cortexa9-linksys-wrt32x-squashfs-sysupgrade.bin app/src/main/assets/wrt32x.bin 

./gradlew build
RELEASE_DIR=app/build/outputs/apk/release
rm $RELEASE_DIR/app-release-unsigned-aligned.apk
zipalign -v -p 4 $RELEASE_DIR/app-release-unsigned.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
apksigner sign --ks router-rescue-signing-key.jks --out $RELEASE_DIR/app-signed-release.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
