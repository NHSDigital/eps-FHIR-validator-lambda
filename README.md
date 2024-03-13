# eps-FHIR-validator-lambda

![Build](https://github.com/NHSDigital/eps-FHIR-validator-lambda/actions/workflows/ci.yml/badge.svg?branch=main)   
![Release](https://github.com/NHSDigital/eps-FHIR-validator-lambda/actions/workflows/release.yml/badge.svg?branch=main)   

## Versions and deployments
Version release history can be found ot https://github.com/NHSDigital/eps-FHIR-validator-lambda/releases.   
We use eslint convention for commit messages for commits to main branch. Descriptions for the types of changes in a release can be found in the [contributing guidelines](./CONTRIBUTING.md)   
Deployment history can be found at https://nhsdigital.github.io/eps-FHIR-validator-lambda/

## Introduction
This contains a cloud formation stack which contains a lambda which can be used to validate FHIR R4 messages against implementation guides on [Simplifier](https://simplifier.net/).

- `scripts/` Utilities helpful to developers of this specification.
- `SAMtemplates/` Contains the SAM templates used to define the stack.
- `.github` Contains github workflows that are used for building and deploying from pull requests and releases.
- `.devcontainer` Contains a dockerfile and vscode devcontainer definition.
- `src` Contains the java source code for the lambda

## Contributing

Contributions to this project are welcome from anyone, providing that they conform to the [guidelines for contribution](https://github.com/NHSDigital/eps-FHIR-validator-lambda/blob/main/CONTRIBUTING.md) and the [community code of conduct](https://github.com/NHSDigital/eps-FHIR-validator-lambda/blob/main/CODE_OF_CONDUCT.md).

### Licensing

This code is dual licensed under the MIT license and the OGL (Open Government License). Any new work added to this repository must conform to the conditions of these licenses. In particular this means that this project may not depend on GPL-licensed or AGPL-licensed libraries, as these would violate the terms of those libraries' licenses.

The contents of this repository are protected by Crown Copyright (C).

## Updates to implementation guide versions

The implementation guides and versions are defined in `src/resources/manifest.json`. When we need to update to a newer version, just update this file and run `make download-dependencies` to download them locally.


## Known limitations
* Resources must use FHIR version R4
* The profiles from the Simplifier packages will only be used in one of the following cases:
  * The resource being validated specifies a profile in the meta field
  * The resource being validated is a message, and a matching message definition in the packages specifies which profile to use for a given resource type
  * A capability statement in the packages specifies which profile to use for a given resource type
* The validator does not use a terminology server, so some code systems, including SNOMED, cannot be validated


## Development

It is recommended that you use visual studio code and a devcontainer as this will install all necessary components and correct versions of tools and languages.  
See https://code.visualstudio.com/docs/devcontainers/containers for details on how to set this up on your host machine.  
There is also a workspace file in .vscode that should be opened once you have started the devcontainer. The workspace file can also be opened outside of a devcontainer if you wish.  
The project uses [SAM](https://aws.amazon.com/serverless/sam/) to develop and deploy the APIs and associated resources.

All commits must be made using [signed commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits)

Once the steps at the link above have been completed. Add to your ~/.gnupg/gpg.conf as below:

```
use-agent
pinentry-mode loopback
```

and to your ~/.gnupg/gpg-agent.conf as below:

```
allow-loopback-pinentry
```

As described here:
https://stackoverflow.com/a/59170001

You will need to create the files, if they do not already exist.
This will ensure that your VSCode bash terminal prompts you for your GPG key password.

You can cache the gpg key passphrase by following instructions at https://superuser.com/questions/624343/keep-gnupg-credentials-cached-for-entire-user-session

### SAM setup and usage

[SAM](https://aws.amazon.com/serverless/sam/) allows rapid local development and deployment to AWS for development and testing.

### Setup

Ensure you have the following lines in the file .envrc

```
export AWS_DEFAULT_PROFILE=prescription-dev
export stack_name=<UNIQUE_NAME_FOR_YOU>
```

UNIQUE_NAME_FOR_YOU should be a unique name for you with no underscores in it - eg anthony-brown-1

Once you have saved .envrc, start a new terminal in vscode and run this command to authenticate against AWS

```
make aws-configure
```

Put the following values in:

```
SSO session name (Recommended): sso-session
SSO start URL [None]: <USE VALUE OF SSO START URL FROM AWS LOGIN COMMAND LINE ACCESS INSTRUCTIONS ACCESSED FROM https://myapps.microsoft.com>
SSO region [None]: eu-west-2
SSO registration scopes [sso:account:access]:
```

This will then open a browser window and you should authenticate with your hscic credentials
You should then select the development account and set default region to be eu-west-2.

You will now be able to use AWS and SAM CLI commands to access the dev account. You can also use the AWS extension to view resources.

When the token expires, you may need to reauthorise using `make aws-login`

### CI Setup

The GitHub Actions require a secret to exist on the repo called "SONAR_TOKEN".
This can be obtained from [SonarCloud](https://sonarcloud.io/)
as described [here](https://docs.sonarsource.com/sonarqube/latest/user-guide/user-account/generating-and-using-tokens/).
You will need the "Execute Analysis" permission for the project (NHSDigital_eps-FHIR-validator-lambda) in order for the token to work.


### Continuous deployment for testing

You can run the following command to deploy the code to AWS for testing

```
make sam-sync
```

This will take a few minutes to deploy - you will see something like this when deployment finishes

```
......
CloudFormation events from stack operations (refresh every 0.5 seconds)
---------------------------------------------------------------------------------------------------------------------------------------------------------------------
ResourceStatus                            ResourceType                              LogicalResourceId                         ResourceStatusReason
---------------------------------------------------------------------------------------------------------------------------------------------------------------------
.....
CREATE_IN_PROGRESS                        AWS::ApiGatewayV2::ApiMapping             HttpApiGatewayApiMapping                  -
CREATE_IN_PROGRESS                        AWS::ApiGatewayV2::ApiMapping             HttpApiGatewayApiMapping                  Resource creation Initiated
CREATE_COMPLETE                           AWS::ApiGatewayV2::ApiMapping             HttpApiGatewayApiMapping                  -
CREATE_COMPLETE                           AWS::CloudFormation::Stack                ab-1                                      -
---------------------------------------------------------------------------------------------------------------------------------------------------------------------


Stack creation succeeded. Sync infra completed.
```

Note - the command will keep running and should not be stopped.


### Make commands

There are `make` commands that are run as part of the CI pipeline and help alias some functionality during development.

#### install targets

- `install-python` installs python dependencies
- `install-hooks` installs git pre commit hooks
- `install-node` installs node dependencies
- `install` runs all install targets

#### SAM targets

These are used to do common commands

- `sam-build` prepares the lambdas and SAM definition file to be used in subsequent steps
- `sam-sync` sync the API and lambda to AWS. This keeps running and automatically uploads any changes to lambda code made locally. Needs AWS_DEFAULT_PROFILE and stack_name environment variables set.
- `sam-validate` validates the main SAM template and the splunk firehose template.
- `sam-deploy-package` deploys a package created by sam-build. Used in CI builds. Needs the following environment variables set
  - artifact_bucket - bucket where uploaded packaged files are
  - artifact_bucket_prefix - prefix in bucket of where uploaded packaged files ore
  - stack_name - name of stack to deploy
  - template_file - name of template file created by sam-package
  - cloud_formation_execution_role - ARN of role that cloud formation assumes when applying the changeset


#### Clean and deep-clean targets

- `clean` clears up any files that have been generated by building or testing locally.
- `deep-clean` runs clean target and also removes any maven and python libraries installed locally.

#### Linting and testing

- `lint` runs lint for all code
- `lint-python` runs lint for python scripts
- `lint-samtemplates` runs lint for SAM templates
- `lint-githubactions` runs lint for github actions
- `lint-githubactions-scriptns` runs lint for github actions scripts
- `test` runs unit tests for all code

#### Compiling

- `compile` compiles all code
- `download-dependencies` downloads files from simplifier

#### Check licenses

- `check-licenses` checks licenses for all packages used - calls check-licenses-java, check-licenses-python, check-licenses-golang
- `check-licenses-java` checks licenses for all java code
- `check-licenses-python` checks licenses for all python code

#### CLI Login to AWS

- `aws-configure` configures a connection to AWS
- `aws-login` reconnects to AWS from a previously configured connection


### Pre-commit hooks

Some pre-commit hooks are installed as part of the install above, to run basic lint checks and ensure you can't accidentally commit invalid changes.
The pre-commit hook uses python package pre-commit and is configured in the file .pre-commit-config.yaml.
A combination of these checks are also run in CI.


### Github pages

We use github pages to display deployment information. The source for github pages is in the gh-pages branch.   
As part of the ci and release workflows, the release tag (either the short commit SHA or release tag) is appended to _data/{environment}_releases.csv so we have a history of releases and replaced in _data/{environment}_latest.csv so we now what the latest released version is.   
There are different makefile targets in this branch. These are
- `run-jekyll` - runs the site locally so changes can be previewed during development
- `sync-main` - syncs common files from main branch to gh-pages branch. You must commit and push after running this
- `install-python` installs python dependencies
- `install-hooks` installs git pre commit hooks
- `install-node` installs node dependencies
- `install-jekyll` installs dependencies to be able to run jekyll locally
- `install` runs all install targets
