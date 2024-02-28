guard-%:
	@ if [ "${${*}}" = "" ]; then \
		echo "Environment variable $* not set"; \
		exit 1; \
	fi

# install targets
install: install-python install-hooks

install-python:
	poetry install

install-hooks: install-python
	poetry run pre-commit install --install-hooks --overwrite

# lint targets
lint: lint-samtemplates lint-python lint-githubactions lint-githubaction-scripts

lint-python:
	poetry run flake8 scripts/*.py --config .flake8

lint-samtemplates:
	poetry run cfn-lint -t SAMtemplates/*.yaml

lint-githubactions:
	actionlint

lint-githubaction-scripts:
	shellcheck .github/scripts/*.sh

# test targets

test: download-dependencies
	mvn test

check-licenses: check-licenses-python check-licenses-java

check-licenses-python:
	scripts/check_python_licenses.sh

check-licenses-java:
	mvn validate	

# clean targets
clean-packages:
	rm -f src/main/resources/package/*.tgz

clean: clean-packages
	rm -rf target
	mvn clean
	rm -rf .aws-sam

deep-clean: clean
	rm -rf .venv
	rm -rf .idea
	rm -rf .mvn
	rm -rf src/main/main.iml
	rm -rf src/test/test.iml

# build targets
compile: download-dependencies
	mvn package

download-dependencies:
	poetry run scripts/download_dependencies.py

# SAM targets
sam-validate: 
	sam validate --template-file SAMtemplates/main_template.yaml --region eu-west-2
	sam validate --template-file SAMtemplates/lambda_resources.yaml --region eu-west-2

sam-build: sam-validate download-dependencies
	sam build --template-file SAMtemplates/main_template.yaml --region eu-west-2

sam-sync: guard-AWS_DEFAULT_PROFILE guard-stack_name download-dependencies
	sam sync \
		--stack-name $$stack_name \
		--watch \
		--template-file SAMtemplates/main_template.yaml

sam-deploy-package: guard-artifact_bucket guard-artifact_bucket_prefix guard-stack_name guard-template_file guard-cloud_formation_execution_role guard-LOG_LEVEL guard-LOG_RETENTION_DAYS
	sam deploy \
		--template-file $$template_file \
		--stack-name $$stack_name \
		--capabilities CAPABILITY_NAMED_IAM CAPABILITY_AUTO_EXPAND \
		--region eu-west-2 \
		--s3-bucket $$artifact_bucket \
		--s3-prefix $$artifact_bucket_prefix \
		--config-file samconfig_package_and_deploy.toml \
		--no-fail-on-empty-changeset \
		--role-arn $$cloud_formation_execution_role \
		--no-confirm-changeset \
		--force-upload \
		--tags "version=$$VERSION_NUMBER" \
		--parameter-overrides \
			  EnableSplunk=true \
			  LogLevel=$$LOG_LEVEL \
			  LogRetentionDays=$$LOG_RETENTION_DAYS 

aws-configure:
	aws configure sso --region eu-west-2

aws-login:
	aws sso login --sso-session sso-session