#!/system/bin/sh
kill -s INT $1
#sleep 1
#kill -9 $1
/system/xbin/opcontrol --stop
