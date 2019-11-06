#!/bin/bash

# Remember to bump the app versionCode and versionName in app/build.grade before running this
# This script assumes you have router-rescue-signing-key.jks in the project root

assets=$(curl https://api.github.com/repos/althea-net/althea-firmware/releases | jq '.[0].assets_url');
assets="${assets%\"}"
assets="${assets#\"}"
curl $assets | jq '.[].browser_download_url' | xargs -I {} wget {};

mv openwrt-ipq40xx-linksys_ea6350v3-squashfs-sysupgrade.bin app/src/main/assets/ea6350-prerelease.bin 
mv openwrt-ipq40xx-glinet_gl-b1300-squashfs-sysupgrade.bin app/src/main/assets/glb1300-prerelease.bin 
mv openwrt-ar71xx-generic-mynet-n750-squashfs-sysupgrade.bin app/src/main/assets/n750-prerelease.bin 
mv openwrt-ar71xx-generic-mynet-n600-squashfs-sysupgrade.bin app/src/main/assets/n600-prerelease.bin 
mv openwrt-mvebu-cortexa9-linksys-wrt3200acm-squashfs-sysupgrade.bin app/src/main/assets/wrt3200acm-prerelease.bin 
mv openwrt-mvebu-cortexa9-linksys-wrt32x-squashfs-sysupgrade.bin app/src/main/assets/wrt32x-prerelease.bin 

assets=$(curl https://api.github.com/repos/althea-net/althea-firmware/releases/latest | jq '.assets_url');
assets="${assets%\"}"
assets="${assets#\"}"
curl $assets | jq '.[].browser_download_url' | xargs -I {} wget {};

mv openwrt-ipq40xx-linksys_ea6350v3-squashfs-sysupgrade.bin app/src/main/assets/ea6350.bin 
mv openwrt-ipq40xx-glinet_gl-b1300-squashfs-sysupgrade.bin app/src/main/assets/glb1300.bin 
mv openwrt-ar71xx-generic-mynet-n750-squashfs-sysupgrade.bin app/src/main/assets/n750.bin 
mv openwrt-ar71xx-generic-mynet-n600-squashfs-sysupgrade.bin app/src/main/assets/n600.bin 
mv openwrt-mvebu-cortexa9-linksys-wrt3200acm-squashfs-sysupgrade.bin app/src/main/assets/wrt3200acm.bin 
mv openwrt-mvebu-cortexa9-linksys-wrt32x-squashfs-sysupgrade.bin app/src/main/assets/wrt32x.bin 
rm openwrt*

./gradlew build
RELEASE_DIR=app/build/outputs/apk/release
rm $RELEASE_DIR/app-release-unsigned-aligned.apk
zipalign -v -p 4 $RELEASE_DIR/app-release-unsigned.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
apksigner sign --ks router-rescue-signing-key.jks --out $RELEASE_DIR/app-signed-release.apk $RELEASE_DIR/app-release-unsigned-aligned.apk
