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

test:
	poetry run scripts/download_dependencies.py
	mvn test

check-licences:
	scripts/check_python_licenses.sh
	mvn validate

clean-packages:
	rm -f src/main/resources/*.tgz

clean: clean-packages
	rm -rf target
	mvn clean

update-manifest:
	poetry run scripts/update_manifest.py

build:
	poetry run scripts/download_dependencies.py
	mvn package

build-latest: clean-packages update-manifest build

run:
	mvn spring-boot:run

sam-validate: 
	sam validate --template-file SAMtemplates/main_template.yaml --region eu-west-2
	sam validate --template-file SAMtemplates/lambda_resources.yaml --region eu-west-2


sam-build: sam-validate
	sam build --template-file SAMtemplates/main_template.yaml --region eu-west-2

sam-sync: guard-AWS_DEFAULT_PROFILE guard-stack_name
	sam sync \
		--stack-name $$stack_name \
		--watch \
		--template-file SAMtemplates/main_template.yaml

aws-login:
	aws sso login --sso-session sso-session