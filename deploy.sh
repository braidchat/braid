#!/bin/bash

set -euo pipefail

set +e
if ! git diff-files --quiet --ignore-submodules ; then
  echo "Uncommited changes; stash or commit before deploying"
  exit 1
fi
if ! git diff-index --cached --quiet HEAD --ignore-submodules ; then
  echo "Staged but uncommited changes; commit before deploying"
  exit 2
fi
set -e

git fetch
git checkout origin/master

PROJECT_NAME="chat"
SERVER="chat"
DATE=$(date +"%Y-%m-%d_%H%M%S")
JAR_NAME=${PROJECT_NAME}-${DATE}.jar
VERSION="0.0.1"
git clean -idx -e profiles.clj
lein clean
lein with-profile +lp uberjar
scp "target/${PROJECT_NAME}-${VERSION}-standalone.jar" "$SERVER:/www/deploys/${PROJECT_NAME}/${JAR_NAME}"
ssh $SERVER "cd /www/deploys/${PROJECT_NAME} && ln -sf ${JAR_NAME} ${PROJECT_NAME}.jar"
git tag "${DATE}"
git push --tags
echo "Update supervisord on remote now"
