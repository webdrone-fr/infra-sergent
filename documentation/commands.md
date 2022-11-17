# Sergent commands
The role of sergent is to affer an endpoint that allow to execute management commands on the server

## Get commands
Path : `server-domain-name.fr:8001/sergent?command=<command-name>`  
Parameters are given with path params

### list

to list all available, use `list` command name  
`https://<server-domain>/sergent?command=list`

### dockerpull

to execute [dockerpull.sh](https://github.com/webdrone-infra/infra-common/blob/main/dockerpull.sh), use `dockerpull` command name  
`https://<server-domain>/sergent?command=dockerpull`

### gitpull

to execute [gitpull.sh](https://github.com/webdrone-infra/infra-common/blob/main/gitpull.sh), use `gitpull` command name  
`https://<server-domain>/sergent?command=gitpull`

### install

to execute [install.sh](https://github.com/webdrone-infra/infra-common/blob/main/install.sh), use `install` command name  
`https://<server-domain>/sergent?command=install`

### deploy-kc-theme

to execute [deploy-kc-theme.sh](https://github.com/webdrone-infra/infra-common/blob/main/deploy-kc-theme.sh), use `deploy-kc-theme` command name  
you can add parameters with path params  
`https://<server-domain>/sergent?command=deploy-kc-theme&params=params`

### deploy

Work in progress

### docker-status

to get information on docker container state, use `docker-status` command name  
`https://<server-domain>/sergent?command=docker-status`  
The command will execute a `docker ps --format "table {{.Statud}}\t{{.Names}}"`

## Post commands
Path : `server-domain-name.fr:8001/sergent?command=<command-name>`  
Parameters are given with the body in json

### update-module

to update all modules, use `update-module` command name  
Parameters :
- `id` : The uuid of the server action
- `gitCredentials` :
    - `username` : username of the github account
    - `password` : token of the github account
- `stackName` : Name of the stack. Blank for default (stackName of .env file)
- `serviceName` : Name of the service. Blank for default (meveo)
- `serviceWebContext` : Name of the service web context. Blank for default (meveo)  

`https://<server-domain>/sergent?command=update-module`  
The command will execute the script [gitpull.sh](https://github.com/webdrone-infra/infra-common/blob/main/gitpull.sh). Then it will execute a docker command to get and install module dependencies.

### setup-git

to setup git on a server and clone a repository on it, use `setup-git` command  
Parameters :
- `git-organisation` : the github organization where is the github repository
- `git-repo` : the github repository used to init git
- `gitinit-token` : the github token used to manage the repository  

`https://<server-domain>/sergent?command=setup-git`  
The command will check if the folder /opt/webdrone/common exist. If yes, it will execute the script [setup-git.sh](https://github.com/webdrone-infra/infra-common/blob/main/setup-git.sh). If no, it will create [setup-git.sh](https://github.com/webdrone-infra/infra-common/blob/main/setup-git.sh) and [deploy-github-key.sh](https://github.com/webdrone-infra/infra-common/blob/main/deploy-github-key.sh), execute them and delete them at the end.

### setup-docker

to setup docker on a server, use `setup-docker` command  
Parameters :
- `username` : the account name of docker
- `password` : the password of the docker account  

`https://<server-domain>/sergent?command=setup-docker`  
The command will execute [setup-docker.sh](https://github.com/webdrone-infra/infra-common/blob/main/setup-docker.sh)

### restart-docker

to restart one or all docker container, use `restart-docker` command*
Parameter :
- `container` : container name or nothing to restart all container  

`https://<server-domain>/sergent?command=restart-docker`  
The command will execute a `docker-compose down && docker-compose up -d`, if no container name are provided. And `docker restart <container-name>`, if a docker container is provided

### manage-sergent

to execute [sergent.sh](pushOnInfra), use `manage-sergent` command name  
Parameter :
- `install` : true for installing ; false for updating  

`https://<server-domain>/sergent?command=manage-sergent`  
