install:
	bundle install

run-jekyll:
	bundle exec jekyll serve

sync-main:
	git checkout main .tool-versions
	git checkout main .pre-commit-config.yaml
	git checkout main .gitignore
	git checkout main .devcontainer
