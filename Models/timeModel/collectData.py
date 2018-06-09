#! /usr/bin/python

import sys
import re

def extractNumbers(line):
	return [long(s) for s in re.split(" |,|\)|\(",line.strip()) if s.isdigit()]

if len(sys.argv) < 2:
	print "Usage: ./collectData.py <logcat file>"
	exit(1)

file = open(sys.argv[1], "r")

icMap = dict()
mcMap = dict()
ttMap = dict()
jtMap = dict()
tcMap = dict()

for line in file:
	if line.find("TIME MODEL") != -1:
		print line
		numbers = extractNumbers(line)	
		key = str(numbers[0])+"-"+str(numbers[1])+"-"+str(numbers[2])
		if key not in icMap:
			icMap[key] = numbers[5]
			mcMap[key] = numbers[6]
			tcMap[key] = 1
			if numbers[2] == 2:
				ttMap[key] = numbers[7]
			else:
				jtMap[key] = numbers[7]
				ttMap[key] = numbers[8]
		else:
			icMap[key] += numbers[5]
			mcMap[key] += numbers[6]
			tcMap[key] +=1
			if numbers[2] == 2:
				ttMap[key] += numbers[7]
			else:
				jtMap[key] += numbers[7]
				ttMap[key] += numbers[8]
template = "{0:20}{1:20}{2:20}{3:20}{4:20}{5:20}"
print template.format("pid-uid-feature","instruction counts","method counts","total time","java time", "thread count")
for key in icMap:
	if key.endswith("2"):
		print template.format(key,str(icMap[key]),str(mcMap[key]),str(ttMap[key]),"-",str(tcMap[key]))
	else:
		print template.format(key,str(icMap[key]),str(mcMap[key]),str(ttMap[key]),str(jtMap[key]),str(tcMap[key]))


