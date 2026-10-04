#!/usr/bin/env bash
# Mux the rendered picture with the mix; length = exact voice-over length.
set -euo pipefail
cd "$(dirname "$0")/.."
DUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 assets/vo.mp3)
ffmpeg -v error -y -i output/frames.mp4 -i output/mix.wav \
  -map 0:v -map 1:a -c:v copy -c:a aac -b:a 320k -t "$DUR" -movflags +faststart \
  output/makomadas_teaser_1080x1920.mp4
ffprobe -v error -show_entries format=duration:stream=codec_name,width,height,r_frame_rate -of compact output/makomadas_teaser_1080x1920.mp4
