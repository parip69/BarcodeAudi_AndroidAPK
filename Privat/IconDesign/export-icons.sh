#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
source_icon=Privat/IconDesign/barcode-gloss-master.png
export_icon() {
  local destination=$1 size=$2 scale=${3:-100}
  local inner=$((size * scale / 100))
  convert "$source_icon" -resize "${inner}x${inner}" -background black -gravity center -extent "${size}x${size}" -alpha off "PNG24:$destination"
}
export_icon app/src/main/assets/icons/icon-192.png 192
export_icon app/src/main/assets/icons/icon-512.png 512
export_icon app/src/main/assets/icons/apple-touch-icon.png 180
export_icon app/src/main/assets/icons/icon-maskable-192.png 192 82
export_icon app/src/main/assets/icons/icon-maskable-512.png 512 82
export_icon app/src/main/res/drawable/ic_launcher_foreground.png 512 60
for density_size in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density=${density_size%:*}
  size=${density_size#*:}
  directory="app/src/main/res/mipmap-$density"
  mkdir -p "$directory"
  export_icon "$directory/ic_launcher.png" "$size"
  export_icon "$directory/ic_launcher_round.png" "$size" 82
done
