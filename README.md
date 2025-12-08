# starWarsLuceneSearch

Command‑line/IDE demo that uses Apache Lucene to index and search Star Wars transcription JSON files. It supports:
- Exact phrase search (Boolean/Phrase queries)
- Phonetic search for misheard/misspelled words (Double Metaphone via lucene‑analyzers‑phonetic)
- Spell suggestions (lucene‑suggest)
- Ranking of results aggregated into bookmark tag IDs with scores

## Prerequisites
- Java: 21+ (project currently targets Java 25 in `pom.xml`)
- Maven: 3.9+
- OS: macOS/Windows/Linux

## Getting started
1) Clone
```
git clone https://github.com/<your-username>/LuceneJsonSearch.git
cd LuceneJsonSearch
```

2) Build dependencies and compile
```
mvn clean package -DskipTests
```
This will download dependencies and compile sources into `target/classes`.

3) Run
You can run from an IDE or via Maven Exec Plugin. The entry point is now:
```
public static void main(String[] args) throws IOException, ParseException
```

- Option A — IntelliJ IDEA (recommended)
  - Open as a Maven project
  - Open `org.jsonsearch.lucene.StarWarsTester`
  - Run the class (Run ▶ StarWarsTester)

- Option B — Maven Exec Plugin (simple CLI run)
  1. Add the following plugin to your `pom.xml` under `<build><plugins>` (see snippet below).
  2. Run:
     ```
     mvn -q -DskipTests exec:java
     ```
     Or explicitly:
     ```
     mvn -q -DskipTests exec:java -Dexec.mainClass=org.jsonsearch.lucene.StarWarsTester
     ```

- Option C — Plain `java` (manual classpath)
  Ensure all deps are on the classpath, then run:
  - macOS/Linux:
    ```
    java -cp "target/classes:$(mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.1:classpath -Dexec.classpathScope=runtime -Dexec.executable=echo -Dexec.args=\"%classpath\")" org.jsonsearch.lucene.StarWarsTester
    ```
  - Windows (PowerShell):
    ```
    java -cp "target/classes;$(mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.1:classpath -Dexec.classpathScope=runtime -Dexec.executable=echo -Dexec.args=\"%classpath\")" org.jsonsearch.lucene.StarWarsTester
    ```
  Using the Maven Exec Plugin (Option B) is simpler.

## Usage
At startup you’ll be prompted:
1) Whether to build indexes (y/n)
   - Data path for JSON files (default: `src/main/resources`)
   - Output path for exact match index (default: `src/test/indexExactWord`)
   - Output path for phonetic index (default: `src/test/indexPhonetic`)
2) Enter the phrase to search (e.g., `hyper space`)

What happens under the hood:
- Exact index is built over the JSON `text` field
- Phonetic index is built to enable Double Metaphone matching
- Spell suggestions are generated using Lucene’s `SpellChecker` over the exact index’s dictionary
- Results are merged and ranked into final bookmark tag IDs with scores

## Configuration
Key constants are defined in `src/main/java/org/jsonsearch/lucene/LuceneConstants.java`:
- `MAX_SEARCH` — maximum number of results
- `MIN_OCCUR` — minimum occurrences of a phrase to count as a hit (default 2)
- `PHRASE_QUERY_SLOP` — slop for phrase queries (default 2)
- Field names: `CONTENTS`, `BOOKMARK_TAG`, etc.

Default directories (can be overridden at runtime):
- Data: `src/main/resources`
- Exact index: `src/test/indexExactWord`
- Phonetic index: `src/test/indexPhonetic`
- Spellchecker dictionary index base: `src/test/dictionaryIndex`

## Input data
Two example JSON files are provided:
- `src/main/resources/starwars-trans.json`
- `src/main/resources/starwars-trans2.json`

Each JSON object is parsed and indexed, focusing on the `text` field.

## Dependencies
Managed by Maven. Key libraries (aligned to `pom.xml`):
- `org.apache.lucene:lucene-core:10.3.1`
- `org.apache.lucene:lucene-queryparser:10.3.1`
- `org.apache.lucene:lucene-suggest:10.3.1` (for `SpellChecker`)
- `org.apache.lucene:lucene-analyzers-phonetic:8.11.4` (Double Metaphone)
- `commons-codec:commons-codec:1.20.0`
- `com.googlecode.json-simple:json-simple:1.1.1`

You can adjust versions in `pom.xml` if needed.

## Screenshots
User prompts and results:
- User prompt to enter search and index paths, then phrase
  - `screenshots/intellij_output_user1124.png`
- Result overview
  - `screenshots/intellij_output_user21124.png`
- Spellchecker and bookmark tag IDs ranked by total score
  - `screenshots/intellij_output_bookmarkscores.png`
- Implementation with Boolean query
  - `screenshots/intellij_output_1121.png`
- Earlier implementation without Boolean query
  - `screenshots/intellij_output_1120.png`

Example markdown embedding if desired:
```
![User Prompt in Terminal](screenshots/intellij_output_user1124.png)
![Search Results in Terminal](screenshots/intellij_output_user21124.png)
![Bookmark Tag Scores](screenshots/intellij_output_bookmarkscores.png)
![With Boolean Query](screenshots/intellij_output_1121.png)
![Without Boolean Query](screenshots/intellij_output_1120.png)
```

## Troubleshooting
- No results found: try lowering `MIN_OCCUR` in `LuceneConstants` or use a broader phrase
- Spell suggestions empty: ensure you built the exact index; suggestions are generated from its terms
- Path issues: when prompted, provide absolute paths if your working directory differs
- Mixed Java versions: the project targets Java 25 in `pom.xml`. If building with Java 21, set `maven.compiler.source/target` accordingly

## Project structure
- `src/main/java/org/jsonsearch/lucene/` — core classes: `Indexer`, `Searcher`, `MyPhoneticAnalyzer`, `StarWarsTester`, etc.
- `src/main/resources/` — sample Star Wars transcripts
- `src/test/` — default output locations for indexes used during interactive runs
- `screenshots/` — images used in this README

## Maven Exec Plugin snippet
Add this to your `pom.xml` under `<build><plugins>` to enable `mvn exec:java`:
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.5.1</version>
  <configuration>
    <mainClass>org.jsonsearch.lucene.StarWarsTester</mainClass>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>java</goal>
      </goals>
    </execution>
  </executions>
  
</plugin>
```

## License
Add your preferred license here (e.g., MIT). If none is specified, this section can be removed.


