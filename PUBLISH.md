# Release procedure

First define two environment variables. This will allow you to copy & paste the following commands:

    export RELEASE_VERSION="1.0.0"
    export DEVELOP_VERSION="1.1.0-SNAPSHOT"

Update the version in `pom.xml` and `README.adoc` to match the version you want to release,
then commit and tag the change.

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${RELEASE_VERSION}
    sed -i "s/\(:version_stable: \).*/\1${RELEASE_VERSION}/g" README.adoc
    sed -i "s/\(:version_snapshot: \).*/\1${DEVELOP_VERSION}/g" README.adoc
    git commit -am "Preparing ${RELEASE_VERSION} release"
    git tag -a -m "v${RELEASE_VERSION}" "v${RELEASE_VERSION}"

Now update the version to match the new 'development version':

    mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${DEVELOP_VERSION}
    git commit -am "Preparing for next development iteration"

Push the changes and GitLab will build and publish the release.

    git push --follow-tags

Log into Sonatype OSSRH interface, close and release the staging repository!
