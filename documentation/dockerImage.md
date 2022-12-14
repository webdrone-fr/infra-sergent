# Docker image

## Create a developer image

To build a deveveloper sergent docker image go on [github actions workflow](https://github.com/webdrone-fr/infra-sergent/actions/workflows/docker-ci.yml), and run a new workflow with these parameters :
- `Branch` : dev
- `Git branch or tag` : dev
- `Docker image tag` : dev-latest

To pull the image up in the server, run this command:

```sh
docker login
# wddeploy
# wddeploy password
docker pull webdronesas/sergent:dev-latest
```

## Release a new sergent image

To build a new release of sergent docker image go on [github actions workflow](https://github.com/webdrone-fr/infra-sergent/actions/workflows/docker-ci.yml), and run a new workflow with these parameters :
- `Branch` : main
- `Git branch or tag` : main
- `Docker image tag` : vX.X

WIP