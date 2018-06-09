#!/usr/bin/python

import sys
import subprocess
import os
import signal
import time

if len(sys.argv) != 4:
	print "Usage: ./startLogging.py <outputFile> <time to send interrupt> <package name>"
	exit(1)
print "Start logging events and the time allowance is "+sys.argv[2]

out_file = sys.argv[1]
timeLimit = float(sys.argv[2])
pkgName = sys.argv[3]

f = open("temp1.txt",'w')

ep = subprocess.Popen(["adb","shell","getevent", "-tt"], stdout=f)

segs = 5

for i in range(1,segs+1):
	time.sleep(timeLimit/5)
	print "\a"
	print str((segs - i)*timeLimit/5)+"s left"

process = subprocess.Popen("adb shell ps", shell=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)

for line in process.stdout:
	splits = line.split()
	if len(splits) == 9 and splits[8] == pkgName:
		subprocess.call("adb shell kill -s INT "+splits[1], shell = True)
ep.kill()

f = open("temp2.txt",'w')

ep = subprocess.Popen(["adb","shell","getevent", "-tt"], stdout=f)

print "Please play 20s more to wake up the threads"
time.sleep(15)
print "5s left"
time.sleep(5)

ep.kill()

subprocess.call("java -jar reran/translate.jar temp1.txt "+out_file+"1",shell=True)
subprocess.call("java -jar reran/translate.jar temp2.txt "+out_file+"2",shell=True)
