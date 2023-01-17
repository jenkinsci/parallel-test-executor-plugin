#!/usr/bin/env bash
set -euxo pipefail
cd $(dirname $0)
export TOKEN=$(cat target/gitea_token.txt)
pushd target/repo > /dev/null
  export BRANCH_NAME=experiment-$(openssl rand -hex 6)
  export TARGET_BRANCH=main
  git checkout -b "$BRANCH_NAME"
  git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit --allow-empty -m "Empty commit"
  git push
  curl -X 'POST' \
    'http://localhost:3000/api/v1/repos/jenkins/demo/pulls' \
    -H "Authorization: token $TOKEN" \
    -H 'accept: application/json' \
    -H 'Content-Type: application/json' \
    -d "{
    \"base\": \"$TARGET_BRANCH\",
    \"title\": \"A pull request from $BRANCH_NAME\",
    \"body\": \"Some description\",
    \"head\": \"$BRANCH_NAME\"
  }"
popd > /dev/null
