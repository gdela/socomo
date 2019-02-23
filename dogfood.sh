#!/usr/bin/env bash
# Script to visualize dependencies using SoCoMo in the SoCoMo project itself
set -o errexit -o pipefail -o noclobber -o nounset

# read the version of socomo that is being developed here
SOCOMO_VERSION=`grep -o '<version>.*</version>' -m 1 pom.xml | sed 's/<[^>]*>//g'`

echo '~~~ Installing SoCoMo to local maven repository ~~~'
./mvnw install -q -Dmaven.test.redirectTestOutputToFile=true

echo '~~~ Using SoCoMo on the SoCoMo project itself ~~~'
./mvnw pl.gdela:socomo-maven-plugin:${SOCOMO_VERSION}:analyze -e
