#!/usr/bin/env bash

cat <<EOF > payload.json
{ 
  "currentTag": "$CURRENT_DEPLOYED_TAG",
  "targetTag": "$DEV_TAG",
  "repoName": "eps-FHIR-validator-lambda",
  "targetEnvironment": "$ENV",
  "productName": "EPS FHIR Validator lambda",
  "releaseNotesPageId": "$PAGE_ID",
  "releaseNotesPageTitle": "Current EPS FHIR Validator lambda - $ENV"
}
EOF
cat payload.json

function_arn=$(aws cloudformation list-exports --query "Exports[?Name=='release-notes:CreateReleaseNotesLambdaArn'].Value" --output text)
aws lambda invoke --function-name "${function_arn}" --cli-binary-format raw-in-base64-out --payload file://payload.json out.txt
cat out.txt
