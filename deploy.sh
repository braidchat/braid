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

PROJECT_NAME="chat"
SERVER="chat"
DATE=$(date +"%Y-%m-%d_%H%M%S")
JAR_NAME=${PROJECT_NAME}-${DATE}.jar
VERSION="0.0.1"
lein uberjars
scp target/${PROJECT_NAME}-${VERSION}-standalone.jar $SERVER:/www/deploys/${PROJECT_NAME}/${JAR_NAME}.jar
ssh flexdb "cd /www/deploys/${PROJECT_NAME}/ && ln -s ${JAR_NAME} ${PROJECT_NAME}.jar"
git tag "${DATE}"
echo "Update supervisord on remote now"
