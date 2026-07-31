# Status Bar Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the latest dictionary lookup outcome and query errors in a Local Dictionary status bar widget.

**Architecture:** `DictionaryStatus` holds the latest display state and publishes changes through the application message bus. A `StatusBarWidgetFactory` creates a project widget that subscribes to these changes. `DictionaryHoverService` reports successful, missing, and failed lookups without depending on the widget implementation.

**Tech Stack:** Kotlin, IntelliJ Platform StatusBarWidget API, IntelliJ MessageBus, JUnit 4.

---

### Task 1: Model dictionary status changes

**Files:**
- Create: `src/main/kotlin/com/github/acecode0/localdictionary/status/DictionaryStatus.kt`
- Create: `src/test/kotlin/com/github/acecode0/localdictionary/status/DictionaryStatusTest.kt`

- [ ] **Step 1: Write the failing state test.**

```kotlin
@Test
fun reportsLatestDefinition() {
    val status = DictionaryStatus()
    status.reportDefinition("cache", "n. 高速缓存")
    assertEquals("cache: n. 高速缓存", status.current().text)
}
```

- [ ] **Step 2: Verify failure.**

Run: `cmd /c ".\\gradlew.bat test --tests *.DictionaryStatusTest"`

Expected: FAIL because `DictionaryStatus` does not exist.

- [ ] **Step 3: Implement the minimal model.**

```kotlin
data class DictionaryStatusSnapshot(val text: String, val tooltip: String, val hasError: Boolean)

class DictionaryStatus {
    fun reportDefinition(word: String, translation: String)
    fun reportMissing()
    fun reportError(error: Throwable)
    fun current(): DictionaryStatusSnapshot
}
```

Use the exact initial snapshot `("Local Dictionary", "Local Dictionary", false)`, missing snapshot `("未找到", "未找到词典释义", false)`, and error snapshot `("错误", error.message ?: error.javaClass.simpleName, true)`.

- [ ] **Step 4: Add tests for missing and error states; run them.**

Run: `cmd /c ".\\gradlew.bat test --tests *.DictionaryStatusTest"`

Expected: PASS.

- [ ] **Step 5: Commit.**

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/status src/test/kotlin/com/github/acecode0/localdictionary/status; git commit -m "feat: 添加词典状态模型"`

### Task 2: Register and render the status bar widget

**Files:**
- Create: `src/main/kotlin/com/github/acecode0/localdictionary/status/LocalDictionaryStatusBarWidgetFactory.kt`
- Create: `src/main/kotlin/com/github/acecode0/localdictionary/status/LocalDictionaryStatusBarWidget.kt`
- Modify: `src/main/resources/META-INF/plugin.xml`

- [ ] **Step 1: Write the failing widget-presentation test.**

```kotlin
@Test
fun rendersCurrentStatusText() {
    val status = DictionaryStatus().apply { reportDefinition("cache", "n. 高速缓存") }
    assertEquals("cache: n. 高速缓存", LocalDictionaryStatusBarWidget(status).text)
}
```

- [ ] **Step 2: Verify failure.**

Run: `cmd /c ".\\gradlew.bat test --tests *.LocalDictionaryStatusBarWidgetTest"`

Expected: FAIL because the widget does not exist.

- [ ] **Step 3: Implement and register the widget.**

```kotlin
class LocalDictionaryStatusBarWidget(private val status: DictionaryStatus) : StatusBarWidget, StatusBarWidget.TextPresentation {
    override fun ID() = "LocalDictionaryStatusBarWidget"
    override fun getPresentation() = this
    override fun getText() = status.current().text
    override fun getTooltipText() = status.current().tooltip
}
```

Register `statusBarWidgetFactory` with implementation `com.github.acecode0.localdictionary.status.LocalDictionaryStatusBarWidgetFactory` in `plugin.xml`. The error click consumer opens `PathManager.getLogPath()` through `BrowserUtil.browse` and returns `null` for non-error states.

- [ ] **Step 4: Verify and commit.**

Run: `cmd /c ".\\gradlew.bat test --tests *.LocalDictionaryStatusBarWidgetTest"`

Expected: PASS.

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/status src/main/resources/META-INF/plugin.xml src/test/kotlin/com/github/acecode0/localdictionary/status; git commit -m "feat: 添加词典状态栏组件"`

### Task 3: Publish hover lookup outcomes

**Files:**
- Modify: `src/main/kotlin/com/github/acecode0/localdictionary/hover/DictionaryHoverService.kt`
- Test: `src/test/kotlin/com/github/acecode0/localdictionary/hover/HoverDelayControllerTest.kt`

- [ ] **Step 1: Write a failing test that a successful lookup reports its last definition.**

```kotlin
@Test
fun reportsLookupOutcomeToStatus() {
    val status = DictionaryStatus()
    status.reportDefinition("local", "adj. 本地的")
    assertEquals("local: adj. 本地的", status.current().text)
}
```

- [ ] **Step 2: Update `DictionaryHoverService`.**

After a nonempty definitions list, call `status.reportDefinition(word, translation)` for its first result. Call `status.reportMissing()` for an empty list. Wrap `DictionaryDatabase.openBundled()` and lookup in `runCatching`; call `status.reportError(exception)` before logging the exception and returning.

- [ ] **Step 3: Run full verification.**

Run: `cmd /c ".\\gradlew.bat test verifyPlugin buildPlugin --no-daemon --console=plain -Pkotlin.compiler.execution.strategy=in-process"`

Expected: exit code `0` and `build/distributions/Local Dictionary-0.0.1.zip` exists.

- [ ] **Step 4: Commit.**

Run: `git add src/main/kotlin/com/github/acecode0/localdictionary/hover/DictionaryHoverService.kt src/test/kotlin/com/github/acecode0/localdictionary/hover; git commit -m "feat: 在状态栏显示查询结果"`
