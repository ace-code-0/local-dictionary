# Local Dictionary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an offline IntelliJ Platform dictionary that supplies ECDICT definitions for words in editor identifiers.

**Architecture:** Identifier splitting and SQLite lookup are standalone Kotlin units. A project service listens to editor mouse movement, schedules lookup after 250ms of pointer inactivity, and renders a hint. A Documentation Target API adapter exposes the same rendered definition to standard Quick Documentation without controlling its delay. The bundled SQLite resource is copied to the IDE system cache before JDBC opens it read-only; its SHA-256 is compared on every initialization so upgraded bundled dictionaries replace stale caches.

**Tech Stack:** Kotlin, IntelliJ Platform 2025.2, `org.xerial:sqlite-jdbc`, JUnit 4, ECDICT SQLite.

---

## File Structure

- `build.gradle.kts`: SQLite JDBC runtime dependency.
- `src/main/resources/dictionary/ecdict.db`: bundled ECDICT database.
- `src/main/kotlin/com/github/acecode0/localdictionary/dictionary/IdentifierWords.kt`: identifier splitter.
- `src/main/kotlin/com/github/acecode0/localdictionary/dictionary/DictionaryDatabase.kt`: extracted-resource lookup service.
- `src/main/kotlin/com/github/acecode0/localdictionary/hover/DictionaryHoverService.kt`: 250ms editor-hover listener and hint lifecycle.
- `src/main/kotlin/com/github/acecode0/localdictionary/documentation/LocalDictionaryDocumentationTargetProvider.kt`: 2023.1+ Quick Documentation adapter.
- `src/main/resources/META-INF/plugin.xml`: service registration and template-extension removal.

### Task 1: Add SQLite runtime and data

**Files:** Modify `build.gradle.kts`; create `src/main/resources/dictionary/ecdict.db`, `src/test/resources/dictionary/test-ecdict.db`, and `THIRD-PARTY-NOTICES`.

- [ ] **Step 1: Add the SQLite JDBC runtime.**

```kotlin
dependencies { implementation("org.xerial:sqlite-jdbc:3.50.3.0") }
```

- [ ] **Step 2: Download the ECDICT SQLite release, copy it as `src/main/resources/dictionary/ecdict.db`, and add its MIT copyright and license to `THIRD-PARTY-NOTICES`.**

Run: `Invoke-WebRequest -Uri 'https://github.com/skywind3000/ECDICT/releases/latest/download/ecdict-sqlite-28.zip' -OutFile "$env:TEMP\ecdict-sqlite.zip"; Expand-Archive -Force "$env:TEMP\ecdict-sqlite.zip" "$env:TEMP\ecdict-sqlite"`

Expected: a nonempty production database resource and license notice.

- [ ] **Step 3: Create a test database with `stardict(word TEXT PRIMARY KEY, translation TEXT)` and rows `cache/n. 高速缓存` and `local/adj. 本地的`.**

Run: `sqlite3 src/test/resources/dictionary/test-ecdict.db "CREATE TABLE stardict (word TEXT PRIMARY KEY, translation TEXT); INSERT INTO stardict VALUES ('cache','n. 高速缓存'), ('local','adj. 本地的');"`

- [ ] **Step 4: Verify resolution and commit.**

Run: `./gradlew.bat dependencies --configuration runtimeClasspath`

Expected: `org.xerial:sqlite-jdbc:3.50.3.0` appears.

Run: `git add build.gradle.kts src/main/resources/dictionary src/test/resources/dictionary THIRD-PARTY-NOTICES; git commit -m "feat: 引入本地词典数据"`

### Task 2: Split identifier words

**Files:** Create `src/main/kotlin/com/github/acecode0/localdictionary/dictionary/IdentifierWords.kt` and `src/test/kotlin/com/github/acecode0/localdictionary/dictionary/IdentifierWordsTest.kt`.

- [ ] **Step 1: Write the failing test.**

```kotlin
fun testSplitsCamelPascalAndSnakeCase() {
    assertEquals(listOf("http", "cache", "key"), IdentifierWords.split("HTTP_CacheKey"))
}
```

- [ ] **Step 2: Verify it fails.** Run: `./gradlew.bat test --tests "*.IdentifierWordsTest"`. Expected: FAIL because `IdentifierWords` is absent.

- [ ] **Step 3: Implement the splitter.**

```kotlin
object IdentifierWords {
    private val boundaries = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])|_+")
    fun split(identifier: String): List<String> = identifier.split(boundaries).map { it.lowercase() }.filter { it.matches(Regex("[a-z]+")) }
}
```

- [ ] **Step 4: Verify and commit.** Run: `./gradlew.bat test --tests "*.IdentifierWordsTest"`. Expected: PASS.

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/dictionary/IdentifierWords.kt src/test/kotlin/com/github/acecode0/localdictionary/dictionary/IdentifierWordsTest.kt; git commit -m "feat: 支持标识符单词拆分"`

### Task 3: Query extracted SQLite resources

**Files:** Create `src/main/kotlin/com/github/acecode0/localdictionary/dictionary/DictionaryDatabase.kt` and `src/test/kotlin/com/github/acecode0/localdictionary/dictionary/DictionaryDatabaseTest.kt`.

- [ ] **Step 1: Write a failing query test.**

```kotlin
fun testFindsTranslationIgnoringCase() = DictionaryDatabase(testResourcePath).use { database -> assertEquals("n. 高速缓存", database.lookup("CACHE")) }
```

- [ ] **Step 2: Verify failure.** Run: `./gradlew.bat test --tests "*.DictionaryDatabaseTest"`. Expected: FAIL because `DictionaryDatabase` is absent.

- [ ] **Step 3: Implement lookup with a parameterized statement.**

```kotlin
fun lookup(word: String): String? = connection.prepareStatement("SELECT translation FROM stardict WHERE lower(word) = ? LIMIT 1").use { statement -> statement.setString(1, word.lowercase()); statement.executeQuery().use { results -> if (results.next()) results.getString(1) else null } }
```

Hash the bundled `dictionary/ecdict.db` and cached file with SHA-256 on initialization. Copy the resource to a sibling temporary file and atomically replace `PathManager.getSystemPath()/local-dictionary/ecdict.db` when the hashes differ; then open it using `jdbc:sqlite:file:<path>?mode=ro` and close its connection in `close()`.

- [ ] **Step 4: Verify and commit.** Run: `./gradlew.bat test --tests "*.DictionaryDatabaseTest"`. Expected: PASS for case-insensitive hits and unknown-word `null`.

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/dictionary/DictionaryDatabase.kt src/test/kotlin/com/github/acecode0/localdictionary/dictionary/DictionaryDatabaseTest.kt; git commit -m "feat: 支持离线词典查询"`

### Task 4: Display definitions after a fixed 250ms editor hover

**Files:** Create `src/main/kotlin/com/github/acecode0/localdictionary/hover/DictionaryHoverService.kt`, `src/test/kotlin/com/github/acecode0/localdictionary/hover/DictionaryHoverServiceTest.kt`, `src/main/kotlin/com/github/acecode0/localdictionary/documentation/LocalDictionaryDocumentationTargetProvider.kt`, and `src/test/kotlin/com/github/acecode0/localdictionary/documentation/LocalDictionaryDocumentationTargetProviderTest.kt`; modify `src/main/resources/META-INF/plugin.xml`.

- [ ] **Step 1: Write the failing hover-delay test.**

```kotlin
fun testSchedulesLookupAfter250Milliseconds() {
    val scheduler = RecordingScheduler()
    DictionaryHoverController(scheduler, dictionary).mouseMoved(editor, point)
    assertEquals(250, scheduler.lastDelayMillis)
}
```

- [ ] **Step 2: Verify failure.** Run: `./gradlew.bat test --tests "*.DictionaryHoverServiceTest"`. Expected: FAIL because the hover service is absent.

- [ ] **Step 3: Implement and register the listener service.**

```kotlin
override fun mouseMoved(event: EditorMouseEvent) {
    alarm.cancelAllRequests()
    alarm.addRequest({ showDefinition(event) }, 250)
}
```

Use `EditorFactory.getInstance().eventMulticaster.addEditorMouseMotionListener(this, disposable)` and register `DictionaryHoverService` as an application service. `showDefinition` obtains the word under the pointer, queries the dictionary on a background read action, and shows a `HintManager` popup only if the pointer has not moved.

Also implement and register `com.intellij.platform.backend.documentation.targetProvider` using `DocumentationTargetProvider`; its target renders the same definition for Quick Documentation but has no scheduling responsibility.

- [ ] **Step 4: Verify and commit.** Run: `./gradlew.bat test --tests "*.DictionaryHoverServiceTest"`. Expected: PASS for the 250ms delay, cancellation on movement, and a nonempty definition result.

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/hover src/main/kotlin/com/github/acecode0/localdictionary/documentation src/main/resources/META-INF/plugin.xml src/test/kotlin/com/github/acecode0/localdictionary/hover src/test/kotlin/com/github/acecode0/localdictionary/documentation; git commit -m "feat: 在编辑器显示本地释义"`

### Task 5: Remove template code and verify plugin

**Files:** Delete `services/MyProjectService.kt`, `startup/MyProjectActivity.kt`, `toolWindow/MyToolWindowFactory.kt`, and `MyPluginTest.kt`; modify `plugin.xml` and `messages/MyBundle.properties`.

- [ ] **Step 1: Remove the Tool Window and startup registrations, their source files, test, and unused messages.**

- [ ] **Step 2: Set plugin name to `Local Dictionary` and replace the template description with an offline dictionary description.**

- [ ] **Step 3: Run full verification.** Run: `./gradlew.bat test verifyPlugin buildPlugin`. Expected: exit `0`, all tests pass, and `build/distributions` contains the plugin ZIP.

- [ ] **Step 4: Commit and inspect state.** Run: `git add src/main src/test; git commit -m "refactor: 移除插件模板代码"; git diff HEAD~5..HEAD --check; git status --short`. Expected: no whitespace errors and an empty worktree.
