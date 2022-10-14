#!/usr/bin/env bash

set -o errexit

# default parameters related to github
github_token_file=/root/.github_access_token
github_owner=webdrone-infra
github_parent_ssh_host=github.com

# ssh key & config file path
SSH_DIR=/root/.ssh

# option to clean generated keys on github
delete_existing_host_keys_on_github=false
# option to force the creation of a new key
override_existing_key=false
repository_root_folder=""

while [ ! -z "$1" ];do
   case "$1" in
        -gt|--github_access_token)
          shift
          github_access_token="$1"
          ;;
        -go|--github_owner)
          shift
          github_owner="$1"
          ;;
        -sd|--ssh-dir)
          shift
          SSH_DIR="$1"
          ;;
        -r|--repo-name)
          shift
          reponame="$1"
          ;;
        -gd|--github-delete-gen-keys)
          shift
          delete_existing_host_keys_on_github="$1"
          ;;
        -f|--ssh-force_new-key)
          shift
          override_existing_key="$1"
          ;;
        -t|--repository-root-folder)
          shift
          repository_root_folder="$1"
          ;;		  
        *)
       echo "Incorrect input provided $1"
   esac
shift
done



# ssh key & config file path
configfile=$SSH_DIR/config

# get hostname
hostname=`hostname`

# The parent directory is supposed to be the root directory of github repository.
currdir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ "$repository_root_folder" = "" ]; then
  repository_root_folder="$(dirname "$currdir")"
fi
if [ ! -d "$repository_root_folder" ]; then
  echo "ERROR , repository_root_folder: $repository_root_folder not found."
  exit 1
fi
cd "$repository_root_folder"

# github_access_token file should exist, or if it doesn't exists, the token should be given as a parameter
if [ -f "$github_token_file" ]; then
  github_access_token=$(cat $github_token_file)
elif [ -z "$github_access_token" ]; then
  echo
  echo "ERROR : $github_token_file not found."
  exit 1
fi
 echo "TEST"
 

echo "add github's server key to knownhosts"
curl --silent https://api.github.com/meta \
  | jq --raw-output '"github.com "+.ssh_keys[]' >> /root/.ssh/known_hosts
 
# check if current folder is a git repository.
rc=""
is_gitrepo=$(git rev-parse --is-inside-work-tree || rc=$? 2>/dev/null)
if [ "$rc" = "" ] && [ "$is_gitrepo" = "true" ]; then
  echo "The parent folder is a git repository"
  is_gitrepo="true";
  git_url="$(git config --get remote.origin.url)"
  reponame_dot_git="$(basename $git_url)"
  reponame=${reponame_dot_git%.*}

  git remote set-url origin $github_parent_ssh_host:$github_owner/$reponame_dot_git

elif [ -z "$reponame" ]; then
  echo "ERROR : The parent folder is not a git repository, and reponame is not defined"
  exit 1
else
  is_gitrepo="false;"
  git_url="https://github.com/${github_owner}/${reponame}"
  reponame_dot_git="${reponame}.git"
  echo "The parent folder is not a repository. Using parameters : ${git_url}  ${reponame}"
fi

  keyfile=$SSH_DIR/github-id_rsa
  echo
  echo "+ generate a key: $keyfile"
  if [ ! -f "$keyfile" ]  || [ "$override_existing_key" = true ]; then
    echo -e 'y\n' | ssh-keygen -t rsa \
      -f $keyfile \
      -C $reponame_dot_git\
      -N ''\
      -q 1>/dev/null
  else
    echo "Key file already exists."
  fi

  echo
  echo "+ configure a key: $configfile"
  if [ -e "$configfile" ] && grep -q "Host $github_parent_ssh_host" "$configfile"; then
    echo "Key already configured in this server."
  else
    cat << EOF >> $configfile
Host $github_parent_ssh_host
  HostName github.com
  User git
  IdentitiesOnly yes
  IdentityFile $keyfile
EOF
  fi



  #read submodules names, append to parent's basename (in array)
  REPO_NAME_ARRAY=()
  if [ "$is_gitrepo" = "true" ]; then
    OLDIFS=$IFS
    IFS='
	'
    STATUS_ARRAY=($(git submodule status))
    for ITERATE_STATUS in "${STATUS_ARRAY[@]}";   do ITERATE_NAME=$(echo $ITERATE_STATUS | sed  -E -e 's/^.*\s([^\s)]+)( \(.*\))?$/\1/m');echo $ITERATE_NAME;REPO_NAME_ARRAY+=($ITERATE_NAME);done
    IFS=$OLDIFS
	git submodule init 
  fi
  REPO_NAME_ARRAY+=($reponame);
  echo "reponames : ${REPO_NAME_ARRAY[@]}"

  ## FOR EACH REPO (PARENT AND SUBMODULES)
  for ITERATE_REPO_NAME in "${REPO_NAME_ARRAY[@]}";
  do
      echo "iterate $ITERATE_REPO_NAME"
      ITERATE_KEY_FILE=$keyfile
      ITERATE_GIT_OWNER=$github_owner
	  ITERATE_REPO_PATH=${reponame}
	  if [ "${ITERATE_REPO_NAME}" != "${reponame}" ]; then
		  tmp=$(git config --get submodule.${ITERATE_REPO_NAME}.url);
		  ITERATE_REPO_PATH=$(basename $tmp);ITERATE_REPO_PATH=${ITERATE_REPO_PATH%.*};
		  ITERATE_GIT_OWNER=$(echo "$tmp"| sed -E "s@.*[/:]([^\\/]*)/[^\\/]*\\.git\$@\\1@g");
		  ITERATE_KEY_FILE=$SSH_DIR/github-id_rsa-${ITERATE_REPO_PATH}
		  echo
		  echo "+ generate a key: $ITERATE_KEY_FILE"
		  if [ ! -f "$ITERATE_KEY_FILE" ]  || [ "$override_existing_key" = true ]; then
			echo -e 'y\n' | ssh-keygen -t rsa \
			  -f $ITERATE_KEY_FILE \
			  -C ${ITERATE_REPO_PATH}.git\
			  -N ''\
			  -q 1>/dev/null
		  else
			echo "Key file already exists."
		  fi

		  echo
		  echo "+ configure a key: $configfile"
		  if [ -e "$configfile" ] && grep -q "Host github_${ITERATE_REPO_PATH}" "$configfile"; then
			echo "Key already configured in this server(github_${ITERATE_REPO_PATH})."
		  else
			cat << EOF >> $configfile
Host github_${ITERATE_REPO_PATH}
  HostName github.com
  User git
  IdentitiesOnly yes
  IdentityFile ${ITERATE_KEY_FILE}
EOF
		fi
	  fi
	  
	  api_url=https://api.github.com/repos/${ITERATE_GIT_OWNER}/${ITERATE_REPO_PATH}/keys
	  key_title="$hostname $ITERATE_KEY_FILE.pub"

	  # delete all existing deploy keys with the same title
	  if [ "$delete_existing_host_keys_on_github" = true ]; then
		echo "delete keys on repo ${ITERATE_REPO_PATH} of ${ITERATE_GIT_OWNER}"
		curl \
		  -H "Authorization: token $github_access_token" \
		  -H "Accept: application/vnd.github.v3+json" \
		  $api_url 2>/dev/null \
		  | jq ".[] | select(.title==\"${key_title}\") | .id " | \
		  while read _id; do
			echo "- delete key: $_id"
			rc=""
			curl \
			  -X "DELETE" \
			  -H "Authorization: token $github_access_token" \
			  -H "Accept: application/vnd.github.v3+json" \
			  $api_url/$_id || rc=$? 2>/dev/null
			if [ "$rc" != "" ]; then
			   echo "failed to delete key: $_id"
			fi
		  done
	  fi

	  # add the ITERATE_KEY_FILE to github
	  echo
	  echo "+ deploy a key on repo ${ITERATE_REPO_PATH} of ${ITERATE_GIT_OWNER}"
	  echo -n ">> "
	  status_code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null \
		-X POST \
		-H "Authorization: token $github_access_token" \
		-H "Accept: application/vnd.github.v3+json" \
		$api_url \
		--data @- << EOF
		{
		  "title" : "$key_title",
		  "key" : "$(cat $ITERATE_KEY_FILE.pub)",
		  "read_only" : true
		}
EOF
  )
	  if [ "$status_code" -eq "201" ] ; then
		echo "successfully deployed."
	  elif [ "$status_code" -eq "422" ] ; then
		echo "key already deployed."
	  else
		echo "ERROR : Failed to deploy a key. HTTP response is $status_code"
		exit 1
	  fi
	  if [ ${ITERATE_REPO_NAME} != ${reponame} ]; then
		 echo "force remote url of sub-module ${ITERATE_GIT_OWNER}/${ITERATE_REPO_PATH} (ssh url, based on ssh host defined in /root/.ssh/config)"
		 echo "git config --file=.gitmodules submodule.${ITERATE_REPO_NAME}.url github_${ITERATE_REPO_PATH}:${ITERATE_GIT_OWNER}/${ITERATE_REPO_PATH}.git"
		 git config --file=.gitmodules submodule.${ITERATE_REPO_NAME}.url github_${ITERATE_REPO_PATH}:${ITERATE_GIT_OWNER}/${ITERATE_REPO_PATH}.git
      fi
  done
  

  echo
  echo "local key:"
  ssh-keygen -lf $keyfile

  echo
  echo "config:"
  cat $configfile


  # remove the github_access_token file
if [ -f "$github_token_file" ]; then
  rm -f $github_token_file
fi