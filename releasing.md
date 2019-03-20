Currently releases of Socomo are done by Wojciech Gdela. Here's the checklist for a release:

1. `./mvnw versions:set -DgenerateBackupPoms=false -DnewVersion=2.X.0`
2. `./mvnw clean verify -P release`
3. `./dogfood.sh`
4. `./catfood.sh`
5. commit changes and push to github
6. publish a new release on github with tag `v2.X.0`
7. attach `socomo-standalone-2.x.0.jar` to the github release
7. check in browser the `socomo.html` from `socomo-core` module
8. `./mvnw clean deploy -P release`
9. close staging repository on [sonatype manager](https://oss.sonatype.org/)
10. click the release button
11. after ten minutes check [central repository](https://repo1.maven.org/maven2/pl/gdela/socomo-parent/)
12. `./mvnw versions:set -DgenerateBackupPoms=false -DnewVersion=2.Y-SNAPSHOT`
13. `./dogfood.sh`
14. commit changes and push to github
