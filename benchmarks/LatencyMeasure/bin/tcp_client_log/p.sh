#!/bin/bash

for f in $(< ./logs_to_process.txt); do
	cat $f | awk '{print $3","$4/100000000.0}' > csv/$f.csv
done
