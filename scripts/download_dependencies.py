#!/usr/bin/env python

import json
import requests


def download_dependencies(manifest_file):
    with open(f"src/main/resources/{manifest_file}", "r") as manifestFile:
        manifestStr = manifestFile.read()
        manifest = json.loads(manifestStr)
        for entry in manifest:
            packageName = entry["packageName"]
            version = entry["version"]
            packageData = requests.get(f"https://packages.simplifier.net/{packageName}/{version}").content
            with open(f"src/main/resources/{packageName}-{version}.tgz", "wb") as packageFile:
                packageFile.write(packageData)


download_dependencies("nhs_digital.manifest.json")
download_dependencies("uk_core.manifest.json")
