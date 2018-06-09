#!/usr/bin/python

import sys
import subprocess
import os
import signal
import time

if len(sys.argv) != 4:
	print "Usage: ./replay.py <input> <time to send interrupt> <package name>"
	exit(1)
print "Start replay events and the time allowance is "+sys.argv[2]

out_file = sys.argv[1]
timeLimit = float(sys.argv[2])
pkgName = sys.argv[3]

subprocess.call("adb push "+out_file+"1 /data/local/translatedEvents.txt", shell = True)

subprocess.Popen("adb shell /data/local/./replay.exe /data/local/translatedEvents.txt",shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)

print "Now, sleep for "+sys.argv[2]+"s and wait the events to be replay"

time.sleep(timeLimit)

process = subprocess.Popen("adb shell ps", shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)

for line in process.stdout:
	splits = line.split()
	if len(splits) == 9 and splits[8] == pkgName:
		subprocess.call("adb shell kill -s INT "+splits[1], shell = True)

subprocess.call("adb push "+out_file+"2 /data/local/translatedEvents.txt", shell = True)

subprocess.Popen("adb shell /data/local/./replay.exe /data/local/translatedEvents.txt",shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)


print "sleep for 20s more to wait for the output"
time.sleep(20)

