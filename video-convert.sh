#!/bin/bash
#
# This script converts movie files into gifs.
# It is used when creating instruction videos for quests.
#
# How to create a quest instruction videos:
#
# 0. Install dependencies:
#
#       on Mac: brew install ffmpeg gifsicle
#
# 1. Capture a video
#
#      on Mac: you can use Quicktime Player: File > New Screen Recording
#
# 2. Convert the video to a gif using the script:
#
#      ./video-convert.sh my-video.mov
#
#      (this creates a my-video.gif)
#
# 3. Move the gif to /resource/public/images/quests/
#
# 4. Add the image path to `braid.client.quests.list`
#
# For Linux, consider using byzanz: http://linux.die.net/man/1/byzanz-record
#
# Code based on http://blog.pkh.me/p/21-high-quality-gif-with-ffmpeg.html

INFILE=$1
outfile="${INFILE%%.mov}.gif"

filters="fps=15,scale=800:-1:flags=lanczos"
palette="/tmp/palette.png"

ffmpeg -v warning -i $1 -vf "$filters,palettegen" -y $palette
ffmpeg -v warning -i $1 -r 10 -i $palette -lavfi "$filters [x]; [x][1:v] paletteuse" -f gif - | gifsicle --optimize=3 --delay=4 > $outfile

