@echo off
echo Starting build process...

:: 1. Create input and dist folders if they don't exist
if not exist "input" mkdir "input"
if not exist "installer" mkdir "installer"

:: 2. Copy *.jar files from target to input
echo Copying JAR files...
for %%F in ("target\file-indexer-*.jar") do (
    copy /Y "%%F" "input\indexer.jar"
)
copy "src\main\resources\application-prod.yml" "input\" /Y

:: 3. Run the jpackage command
echo Running jpackage...

jpackage  ^
  --type exe  ^
  --name Indexer ^
  --app-version 1.0.0  ^
  --vendor "MOHammad" ^
  --input input ^
  --main-jar indexer.jar  ^
  --java-options "--enable-native-access=ALL-UNNAMED"  ^
  --java-options "-Dspring.profiles.active=prod"  ^
  --java-options '-Dspring.config.additional-location=optional:file:${user.home}/.indexer/'  ^
  --icon src\main\resources\indexer.ico ^
  --win-console  ^
  --win-menu  ^
  --win-shortcut  ^
  --win-dir-chooser  ^
  --win-upgrade-uuid "b3c1f2a0-5d7e-4c8a-9f21-0a6e4d8b7c11"  ^
  --dest installer

echo Done!
@REM pause