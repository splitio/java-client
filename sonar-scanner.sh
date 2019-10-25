#/bin/bash -e

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  mvn --batch-mode sonar:sonar -DskipTests \
    -Dsonar.pullrequest.provider='GitHub' \
    -Dsonar.pullrequest.github.repository='splitio/java-client' \
    -Dsonar.pullrequest.key=$TRAVIS_PULL_REQUEST \
    -Dsonar.pullrequest.branch=$TRAVIS_PULL_REQUEST_BRANCH \
    -Dsonar.pullrequest.base=$TRAVIS_BRANCH
else
  if [ "$TRAVIS_BRANCH" == 'development' ]; then
    TARGET_BRANCH='master'
  else
    TARGET_BRANCH='development'
  fi
  mvn --batch-mode sonar:sonar -DskipTests \
    -Dsonar.branch.name=$TRAVIS_BRANCH \
    -Dsonar.branch.target=$TARGET_BRANCH
fi
