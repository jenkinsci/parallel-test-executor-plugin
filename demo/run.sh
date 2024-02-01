#!/usr/bin/env bash
set -euo pipefail

function gitea_create_admin_user() {
  local username; username="${1:?}"
  local email; email="${2:?}"
  mkdir -p target
  [ -f target/gitea_output.txt ] || docker compose exec -u 1000:1000 gitea gitea admin user create --admin --username "$username" --random-password --email "$email" > target/gitea_output.txt
}

function gitea_generate_access_token() {
  local username; username="${1:?}"
  mkdir -p target
  [ -f target/gitea_token.txt ] || docker compose exec -u 1000:1000 gitea gitea admin user generate-access-token --username "$username" --raw > target/gitea_token.txt
}

function gitea_token() {
  cat target/gitea_token.txt
}

function gitea_repository_exists() {
    local token; token="$(gitea_token)"
    [ "$(curl -s -o /dev/null -w "%{http_code}" -X 'GET' \
      'http://localhost:3000/api/v1/repos/jenkins/demo' \
      -H "Authorization: token $token")" = "200" ]

}

function gitea_create_repository() {
  local token; token="$(gitea_token)"
  curl -X 'POST' \
    'http://localhost:3000/api/v1/user/repos' \
    -H "Authorization: token $token" \
    -H 'accept: application/json' \
    -H 'Content-Type: application/json' \
    -d '{
    "auto_init": true,
    "default_branch": "main",
    "name": "demo"
  }'
}

function gitea_init_repository() {
  git clone http://jenkins:$(gitea_token)@localhost:3000/jenkins/demo.git target/repo
  cp -R repo target/
  pushd target/repo
    bash ../../gen.sh
    git add .
    git -c user.email=demo@jenkins-ci.org -c user.name="Parallel Test Executor Demo" commit -m "Initial commit"
    git push origin -u
  popd
}

function jenkins_download_cli() {
  [ -f target/jenkins-cli.jar ] || wget -O target/jenkins-cli.jar -o /dev/null http://localhost:8080/jnlpJars/jenkins-cli.jar
}

function jenkins_update_credentials() {
  local credentialsId; credentialsId="${1:?}"
  local token; token="$(gitea_token)"
  jenkins_download_cli
  sed -e "s/SECRET/$token/" credentials.xml | java -jar target/jenkins-cli.jar -s http://localhost:8080/ update-credentials-by-xml "SystemCredentialsProvider::SystemContextResolver::jenkins" "(global)" "$credentialsId"
}

function readme() {
  local gitea_username; gitea_username=$(grep "New user" < target/gitea_output.txt | cut -f 2 -d "'")
  local gitea_password; gitea_password=$(grep password < target/gitea_output.txt | cut -f 2 -d "'")

  echo "Demo initialized"
  echo "Gitea is available on http://localhost:3000 using $gitea_username $gitea_password"
  echo "Jenkins is available on  http://localhost:8080"
  echo "The demo git repo is available in target/repo and can be accessed at http://localhost:3000/jenkins/demo"
  open http://localhost:8080/
}

docker compose up -d --wait
gitea_create_admin_user jenkins demo@jenkins.io
gitea_generate_access_token jenkins
jenkins_update_credentials gitea
if ! gitea_repository_exists; then
  gitea_create_repository
  gitea_init_repository
fi
readme
