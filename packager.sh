#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "Starting build process..."

# 1. Create input and installer folders if they don't exist
mkdir -p input
mkdir -p installer

# 2. Copy JAR files and application properties to input
echo "Copying JAR files..."
# Find and copy matching jar files (takes the first match if multiple exist)
for f in target/file-indexer-*.jar; do
    if [ -f "$f" ]; then
        cp -f "$f" "input/indexer.jar"
        break
    fi
done

if [ -f "src/main/resources/application-prod.yml" ]; then
    cp -f "src/main/resources/application-prod.yml" "input/"
fi

# 3. Run the jpackage command
echo "Running jpackage..."

jpackage \
  --type app-image \
  --name Indexer \
  --app-version 1.0.0 \
  --vendor "MOHammad" \
  --input input \
  --main-jar indexer.jar \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --java-options "-Dspring.profiles.active=prod" \
  --java-options "-Dspring.config.additional-location=optional:file:\${user.home}/.indexer/" \
  --icon src/main/resources/indexer.png \
  --dest installer

echo "Done!"