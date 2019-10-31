#!/usr/bin/env bash

find ./output/ -type f -name "*-crop.pdf" -exec rm '{}' \;
find ./output/ -type f -name "*.pdf" -exec pdfcrop --hires '{}' \;
