guard-%:
	@ if [ "${${*}}" = "" ]; then \
		echo "Environment variable $* not set"; \
		exit 1; \
	fi


install:
	poetry install

lint:
	poetry run flake8 scripts/*.py --config .flake8
	shellcheck scripts/*.sh

lint-samtemplates:
	poetry run cfn-lint -t SAMtemplates/*.yaml

test: download-dependencies
	mvn test

check-licences:
	scripts/check_python_licenses.sh
	mvn validate

clean-packages:
	rm -f src/main/resources/package/*.tgz

clean: clean-packages
	rm -rf target
	mvn clean
	rm -rf .aws-sam

update-manifest:
	poetry run scripts/update_manifest.py

build: download-dependencies
	mvn package

build-latest: clean-packages update-manifest build

run:
	mvn spring-boot:run

download-dependencies:
	poetry run scripts/download_dependencies.py

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

aws-login:
	aws sso login --sso-session sso-session