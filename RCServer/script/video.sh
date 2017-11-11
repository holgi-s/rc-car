#!/bin/sh

while true
do
  raspivid -n -ih -vf -hf -w 640 -h 480 -fps 20 -t 0 -l  -o tcp://0.0.0.0:5001
  sleep 1
done
