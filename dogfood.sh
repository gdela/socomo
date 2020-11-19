#!/usr/bin/env bash
# Script to visualize dependencies using Socomo in the Socomo project itself
set -o errexit -o pipefail -o noclobber -o nounset

# read the version of socomo that is being developed here
SOCOMO_VERSION=$(grep -o '<version>.*</version>' -m 1 pom.xml | sed 's/<[^>]*>//g')

echo '~~~ Installing Socomo to local maven repository ~~~'
./mvnw install -Dmaven.test.redirectTestOutputToFile=true | grep -v '^\[ERROR\]\s*$'
echo ''

echo '~~~ Using Socomo standalone on the Socomo project itself ~~~'
java -jar socomo-core/target/socomo-standalone-${SOCOMO_VERSION}.jar \
	--output-for-html socomo-core --output-for-data socomo-core/target \
	socomo-core/target/socomo-core-${SOCOMO_VERSION}.jar
echo ''

echo '~~~ Using Socomo maven plugin on the Socomo project itself ~~~'
./mvnw pl.gdela:socomo-maven:${SOCOMO_VERSION}:analyze -e
