#!/bin/sh

for f in *.apk; do
	adb install $f
done
