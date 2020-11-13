#!/usr/bin/env bash
# Script to visualize dependencies using Socomo in random selection of foreign projects
set -o errexit -o pipefail -o noclobber -o nounset

mkdir -p catfood; cd catfood

if [[ "$*" = "init" ]]; then
	echo '~~~ Setting up ancient maven for foreign projects ~~~'
	maven_version=$(grep -o '<maven.version>.*</maven.version>' -m 1 ../socomo-maven/pom.xml | sed 's/<[^>]*>//g')
	../mvnw -q -N io.takari:maven:wrapper -Dmaven=${maven_version}
	echo '~~~ Downloading some projects to use Socomo on them ~~~'
	[[ -d guava ]] || git clone https://github.com/google/guava.git
	[[ -d commons-lang ]] || git clone http://git-wip-us.apache.org/repos/asf/commons-lang.git
	[[ -d assertj-core ]] || git clone https://github.com/joel-costigliola/assertj-core.git
	[[ -d fun-timekeeper ]] || git clone https://github.com/gdela/fun-timekeeper.git
	[[ -d all-things-cqrs ]] || git clone https://github.com/ddd-by-examples/all-things-cqrs.git
	echo "Hint: you can add more projects to the './catfood' directory"
	echo "Hint: run '$0 compile socomo' now to compile and run socomo on those projects"
	exit 0
fi

if [[ -z "$(ls */pom.xml 2>/dev/null)" ]]; then
	echo "Hint: run '$0 init' first to download some foreign projects"
	exit 1
fi

# read the version of socomo that is being developed here
SOCOMO_VERSION=$(grep -o '<version>.*</version>' -m 1 ../pom.xml | sed 's/<[^>]*>//g')
# get goals specified in script arguments or set default goals
MAVEN_GOALS=${*:-socomo}
MAVEN_GOALS=${MAVEN_GOALS/socomo/pl.gdela:socomo-maven:${SOCOMO_VERSION}:analyze}

echo '~~~ Installing Socomo to local maven repository ~~~'
../mvnw -f ../pom.xml install -Dmaven.test.redirectTestOutputToFile=true | grep -v '^\[ERROR\]\s*$'
echo ''

echo '~~~ Using Socomo on foreign projects ~~~'
mkdir -p logs; rm logs/*.log || true
echo "Will do 'mvn $MAVEN_GOALS' on projects"
echo "Logs will be stored in './catfood/logs' directory"
for pom in */pom.xml; do
	project=$(dirname ${pom})
	echo -n "Working on $project..."
	mvn=$([[ -x ${project}/mvnw ]] && echo "${project}/mvnw" || echo "./mvnw")
	${mvn} -f ${pom} ${MAVEN_GOALS} -V -e > logs/${project}.log || {
		echo "FAILED"
		continue
	}
	echo "OK"
	grep -o 'analysis took.*' logs/${project}.log || true
done
