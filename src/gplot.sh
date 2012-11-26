#!/bin/bash

for x; do
gnuplot -persist <<EOF
plot "$x" using 1:2 with lines
EOF
done
