#!/usr/bin/env bash

# this script activate synchro git for folder /opt/webdrone, it depends on deploy-github-key.sh wich is triggered at the beginning
set -o errexit

if [[ $(id -u) -ne 0 ]] ; then echo "Please run as root" ; exit 1 ; fi

if ! [ -x "$(command -v git)" ]; then
  apt-get update && apt-get install -y git
fi

if ! [ -x "$(command -v curl)" ]; then
  apt-get update && apt-get install -y curl
fi

if ! [ -x "$(command -v jq)" ]; then
  apt-get update && apt-get install -y jq
fi

currdir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MAIN_GIT_FOLDER="/opt/webdrone/"
# option to clean generated keys on github
delete_existing_host_keys_on_github=true
# option to force the creation of a new key
override_existing_key=true


while [ ! -z "$1" ];do
   case "$1" in
        -o|--git-organisation)
          shift
          git_organisation="$1"
          ;;
        -r|--git-repo)
          shift
          git_repo="$1"
          ;;
        -tk|--gitinit-token)
          shift
          gitinit_token="$1"
          ;;
        -d|--destination-folder)
          shift
          MAIN_GIT_FOLDER="$1"
          ;;
        -gd|--github-delete-gen-keys)
          shift
          delete_existing_host_keys_on_github="$1"
          ;;
        --ssh-force_new-key)
          shift
          override_existing_key="$1"
          ;;
        *)
       echo "Incorrect input provided $1"
   esac
shift
done

REMOTE_URL="git@github.com:${git_organisation}/${git_repo}.git"
mkdir -p ${MAIN_GIT_FOLDER}
cd ${MAIN_GIT_FOLDER}
$currdir/deploy-github-key.sh -go ${git_organisation} -gt ${gitinit_token} -r ${git_repo} -f true -gd delete_existing_host_keys_on_github -f override_existing_key --repository-root-folder "${MAIN_GIT_FOLDER}"
echo "github key success"

# check if current folder is a git repository.
rc=""
is_gitrepo=$(git rev-parse --is-inside-work-tree || rc=$? 2>/dev/null)
if [ "$rc" != "" ] || [ "$is_gitrepo" != "true" ]; then
  echo "init git"
  git init .
  git remote add origin ${REMOTE_URL}
else
  echo "git is initalized"
fi
git config user.name theServer
git config remote.origin.url ${REMOTE_URL}
pwd
echo "pull main repo"
git pull origin main
echo "init keys for submodules"
$currdir/deploy-github-key.sh -go ${git_organisation} -gt ${gitinit_token} -r ${git_repo} -f true -gd delete_existing_host_keys_on_github -f override_existing_key --repository-root-folder "${MAIN_GIT_FOLDER}"
echo "git sync submodule"
git submodule sync
echo "git pull all"
git pull --recurse-submodules origin main


echo "github init success"
cd "$currdir"