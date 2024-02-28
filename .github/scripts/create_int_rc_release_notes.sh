#!/usr/bin/env bash

cat <<EOF > payload.json
{ 
  "currentTag": "$CURRENT_DEPLOYED_TAG",
  "targetTag": "$RELEASE_TAG",
  "repoName": "eps-FHIR-validator-lambda",
  "targetEnvironment": "INT",
  "productName": "EPS FHIR Validator lambda",
  "releaseNotesPageId": "$PAGE_ID",
  "releaseNotesPageTitle": "EPS-FHIR-Validator-$RELEASE_TAG - Deployed to [INT] on $(date +'%d-%m-%y')",
  "createReleaseCandidate": "true",
  "releasePrefix": "EPS-FHIR-Validator-"
}
EOF
cat payload.json

function_arn=$(aws cloudformation list-exports --query "Exports[?Name=='release-notes:CreateReleaseNotesLambdaArn'].Value" --output text)
aws lambda invoke --function-name "${function_arn}" --cli-binary-format raw-in-base64-out --payload file://payload.json out.txt
cat out.txt
