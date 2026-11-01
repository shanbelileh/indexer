# File Indexer

A command line tool that indexes the text files on your drives and lets you search them fast.
Built with **Java 25**, **Spring Boot 4.1.1**, **Spring Shell 4**, **Apache Lucene 10** and **H2**.

- Pick drives from the command line, nothing else to configure per run
- Indexes only files with the extensions you list in a YAML file
- Re-indexing is incremental: only new or changed files are read again, deleted files are removed
- Shows every file while it is being indexed
- Two search modes: regular (Lucene query syntax) and regex

## Contents

- [Requirements](#requirements)
- [Build and run](#build-and-run)
- [Commands](#commands)
- [Configuration](#configuration)
- [Where data is stored](#where-data-is-stored)
- [How it works](#how-it-works)
- [Windows installer](#windows-installer)
- [Troubleshooting](#troubleshooting)

## Requirements

- JDK 25
- Maven 3.9+
- For the installer only: the WiX Toolset (used by `jpackage` on Windows)

## Build and run

```
mvn clean package
java --enable-native-access=ALL-UNNAMED -jar target/file-indexer-0.1.0.jar
```

This opens an interactive prompt (`shell:>`). `--enable-native-access=ALL-UNNAMED` only hides a harmless
JLine warning on Java 25.

To use a Spring profile, pass it as a JVM option (not as a program argument, because Spring Shell treats
program arguments as commands):

```
java -Dspring.profiles.active=prod --enable-native-access=ALL-UNNAMED -jar target/file-indexer-0.1.0.jar
```

## Commands

Run a command at the prompt, or pass it directly on the command line to run it once and exit.

### `index`

Indexes the drives you select. Without options it asks which drives to use.

```
index                   # asks for the drives
index -d C,D            # drives by letter
index -d all            # every readable drive
```

Each file is printed as it is processed:

```
== Scanning F:\ ==
[NEW]     F:\work\notes.md
[UPDATED] F:\work\todo.txt
[REMOVED] F:\work\old.txt
== Done F:\: 120 new, 3 updated, 1 removed, 4000 unchanged, 20 skipped, 0 failed ==
```

| Marker      | Meaning                                              |
|-------------|------------------------------------------------------|
| `[NEW]`     | File was not indexed before                          |
| `[UPDATED]` | Size or modified time changed since the last run     |
| `[REMOVED]` | File no longer exists and was dropped from the index |
| `[FAILED]`  | File could not be read                               |

Files that are too large, or look binary, are skipped.

### `search`

Asks for the search type, then the drives, then the text. Everything can also be given as options:

```
search                                   # fully interactive
search -t regular -d F -q "invoice 2026"
search -t regex   -d C,D -q "inv[a-z]+ce"
```

| Option            | Meaning                         |
|-------------------|---------------------------------|
| `-t`, `--type`    | `regular` or `regex`            |
| `-d`, `--drives`  | Drives to search, or `all`      |
| `-q`, `--text`    | Text or regular expression      |

**Regular search** uses Lucene query syntax: words are combined with AND, and you can use
`"exact phrase"`, wildcards (`inv*`, `te?t`), `OR` and `NOT`. Text that is not valid query syntax is
searched literally.

**Regex search** uses Lucene regular expressions and matches against single indexed words, which are
lower-cased. So use lower-case patterns and `[0-9]` instead of `\d`. A pattern cannot span several words.

Results show the file path and a relevance score, up to `max-results` entries.

### Other commands

`help` lists everything Spring Shell provides, `exit` or `quit` leaves the prompt.

## Configuration

Settings live in `src/main/resources/application.yml`. Profile files such as `application-prod.yml`
override only the values they contain.

```yaml
indexer:
  extensions: [txt, md, java, xml, yml, json, sql, csv, log]  # which files are indexed
  excluded-dirs: [.git, node_modules, target, build, Windows] # directories never entered
  lucene-dir: ${user.home}/.indexer/lucene
  max-file-size-mb: 10                                        # larger files are skipped
  max-results: 50
```

Extensions are case-insensitive and written without a dot. Changing the extension list does not remove
files that were already indexed, so delete the data folder if you want a clean index.

## Where data is stored

Indexing state is kept outside the install folder so the app works from `Program Files`:

| Item                         | Location                                |
|------------------------------|-----------------------------------------|
| H2 database (`indexed_file`) | `~/.indexer/fileindex.mv.db`        |
| Lucene index                 | `~/.indexer/lucene/`                |
| Shell history                | `~/.indexer/spring-shell.log`       |

To start from scratch, delete the `.indexer` folder in your home directory.

## How it works

1. **Walk**: the selected drive is scanned, skipping excluded directories and files with other extensions.
2. **Compare**: each file's size and last-modified time are compared with the `indexed_file` H2 table.
   Matching files are left alone.
3. **Index**: new and changed files are added to Lucene and recorded in H2. Files seen in the table but
   missing on disk are deleted from both.
4. **Search**: the query runs against Lucene, filtered to the selected drives.

The whole known-file list of a drive is loaded into memory during indexing. That is fine for hundreds of
thousands of files. For many millions, look up one file at a time in H2 instead.

## Windows installer

Build the jar, then create an installer with `jpackage` (WiX Toolset must be installed):

```powershell
mvn clean package

jpackage `
  --type exe `
  --name "Indexer" `
  --app-version 0.1.0 `
  --vendor "YourName" `
  --icon installer\app.ico `
  --input target `
  --main-jar file-indexer-0.1.0.jar `
  --java-options "--enable-native-access=ALL-UNNAMED" `
  --java-options "-Dspring.profiles.active=prod" `
  --win-console --win-menu --win-shortcut --win-dir-chooser `
  --win-upgrade-uuid "<your-own-fixed-guid>" `
  --dest installer
```

Notes:

- `--win-console` is required, otherwise no console window opens for the prompt.
- Create the upgrade GUID once (`[guid]::NewGuid()`) and keep it for every release.
- The installer does not change `PATH`. To run it as a command, create a small wrapper:

  ```powershell
  mkdir C:\Tools
  Set-Content C:\Tools\indexer.cmd '@"C:\Program Files\Indexer\Indexer.exe" %*'
  ```

  and add `C:\Tools` to your user `PATH`. Then `indexer index -d F` works from any terminal.

## Troubleshooting

**The app starts and exits immediately with code 0.** The shell runner was created but not started.
Add an explicit starter:

```java
@Configuration
class ShellStarterConfig {
    @Bean
    ApplicationRunner fileIndexerShellRunner(ShellRunner shellRunner) {
        return args -> shellRunner.run(args.getSourceArgs());
    }
}
```

**`AccessDeniedException` for a file in `C:\Program Files\...`.** Something is writing to a relative path.
Keep all writable files under `~/.indexer` (see the configuration above).

**Regex finds nothing.** Patterns match single lower-cased words. Use lower-case letters and `[0-9]`.

**Nothing is found at all.** Run `index` for the same drives you search, and check that the file
extension is listed in `indexer.extensions`.

**Colors or arrow keys look wrong in IntelliJ.** Enable *Emulate terminal in console* in the run
configuration, or run from Windows Terminal or PowerShell.
