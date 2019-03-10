Currently releases of Socomo are done by Wojciech Gdela. Here's the checklist for a release:

1. `./mvnw versions:set -DgenerateBackupPoms=false -DnewVersion=2.X.0`
2. `./mvnw verify -P release`
3. `./dogfood.sh`
4. `./catfood.sh`
5. commit changes and push to github
6. publish new release on github with tag `v2.X.0`
7. check in browser the `socomo.html` from `socomo-core` module
8. `./mvnw deploy -P release`
9. close staging repository on [sonatype manager](https://oss.sonatype.org/)
10. after ten minutes check [central repository](https://repo1.maven.org/maven2/pl/gdela/socomo-parent/)
11. `./mvnw versions:set -DgenerateBackupPoms=false -DnewVersion=2.Y-SNAPSHOT`
12. `./dogfood.sh`
13. commit changes and push to github
