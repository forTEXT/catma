# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

CATMA (Computer Assisted Text Markup and Analysis) — a web application for text annotation and analysis. It is a **single Maven WAR module** (`de.catma:catma`, Java 21) built on **Vaadin 8** (server-side UI + a GWT widgetset for custom client widgets), packaged for a servlet container (Jetty).

CATMA has **no database of its own for project data**: a self-managed **GitLab server is the backend**. Projects, documents, annotations and tagsets live in Git repositories on GitLab; SQLite is used only for a small amount of auxiliary application state.

## Prerequisites & one-time setup

- JDK 21, Maven.
- A dedicated self-managed GitLab server (<v19) with admin access. Not needed to compile, but needed to run or to execute the GitLab-backed tests.
- **A forked dependency must be installed into the local Maven repo or the build will fail** (`org.vaadin:elements 0.2.3-CATMA`). Download URL and the exact `mvn install:install-file` command are in `doc/DEVELOPMENT.md`. `gitlab4j-api` uses the stock release (5.x — the 6.x line is Jakarta-based and incompatible with the javax stack).

Full setup instructions: `doc/DEVELOPMENT.md`. Self-hosting: `doc/SELF-HOSTING.md`. Docker/standalone: `docker/README.md`.

## Common commands

```bash
# Full build (includes the GWT widgetset + theme compile — slow, several minutes)
mvn clean compile package -DskipTests=true

# Rebuild skipping the GWT widgetset compile (much faster; safe when no client-side code changed)
mvn package -DskipTests=true -Dgwt.compiler.skip=true

# Run locally on Jetty (http://localhost:8080/)
mvn jetty:run

# Run against an alternative properties file (see "Configuration" below)
mvn -Dprop=catma_local-dev.properties package jetty:run

# Tests
mvn test
mvn test -Dtest=TagsetDefinitionTest
mvn test -Dtest=AuthServiceTest#methodName
```

`mvn dependency:tree` / `dependency:analyze` are used often here — the POM contains many deliberate version pins and exclusions (Jersey, Tika, commons-logging, xalan, activation), and `maven-enforcer-plugin` bans several artifacts. Read the comments in `pom.xml` before changing a dependency.

## Configuration

All runtime settings are enum constants in `de.catma.properties.CATMAPropertyKey`, read from a properties file via `CATMAProperties.INSTANCE`. The template is `src/main/resources/catma.properties`.

- The file is selected by the **`prop` system property**, defaulting to `catma.properties`.
- For the web app, `PropertiesInitializerServlet` resolves it against the **servlet context real path**, so the file must sit in the deployed webapp root (`src/main/webapp/` for `mvn jetty:run`, `target/catma-x.x-SNAPSHOT/` for an exploded artifact).
- For tests, `de.catma.PropertiesHelper.load()` resolves it relative to the **working directory**.
- Concrete `*.properties` files are gitignored and `.aiignore`d — never commit one, and don't expect to find real values in the repo.
- `SQLITE_DB_BASE_PATH` must contain a copy of `src/main/resources/catma.db` or the app won't start.

## Architecture

### Entry points (`src/main/webapp/WEB-INF/web.xml`)

1. `PropertiesInitializerServlet` — loads config, `load-on-startup 1`.
2. `de.catma.ui.CatmaApplicationServlet` → `de.catma.ui.CatmaApplication` (the Vaadin `UI`, mapped at `/*`) — the interactive application, with Vaadin Push over WebSocket/XHR.
3. A Jersey `ServletContainer` at `/api/v1/*` → `de.catma.api.v1.ApiApplication` — the read/export REST API, documented via Swagger annotations.

`HazelCastInitializerServlet` + `de.catma.hazelcast` provide cross-session messaging and caching (signup tokens, project invitation codes).

### Persistence model — the important part

`de.catma.repository.git` implements the whole storage layer on top of GitLab + JGit:

- `GitProjectsManager` (`ProjectsManager`) creates/lists/forks projects. `GraphWorktreeProject` (`Project`/`IndexedProject`) is the object a logged-in user works with.
- **Each user works on their own Git branch** named after their user identifier; `GitProjectHandler.synchronizeWithRemote()` pushes that branch and drives a **GitLab merge request** into `master` to publish changes. Conflict resolution happens in GitLab's MR UI. `DEV_PREVENT_PUSH` and `MIN_TIME_BETWEEN_SYNCHRONIZATIONS_SECONDS` guard this in development.
- Local working copies live under `GIT_REPOSITORY_BASE_PATH/<user-identifier>/<project>`; the API has its own separate clone area (`API_GIT_REPOSITORY_BASE_PATH`).
- Repository layout inside a project: `tagsets/`, `collections/`, `documents/` (constants at the top of `GitProjectHandler`). Annotation collections are stored as **paged** JSON-LD Web Annotation files, capped by `MAX_ANNOTATION_PAGE_FILE_SIZE_BYTES` so that diffs stay viewable in GitLab.
- `GitlabManagerPrivileged` (admin token) vs `GitlabManagerRestricted` (per-user token) — the split matters for permissions; `de.catma.rbac` maps GitLab roles onto `RBACRole`/`RBACPermission`.
- `repository/git/resource/provider` selects *which* revision a project is read at (`SynchronizedResourceProvider` for the user's own branch, `LatestContributionsResourceProvider` for viewing others' unmerged work).

**Serialization must be byte-stable across writes** so that Git diffs stay minimal. `SerializationHelper` builds a GSON instance around `SortedReflectiveTypeAdapterFactory`, which sorts fields deterministically. This requires the locally-added `com.google.gson.internal.bind.SortedFieldTypeAdapterWrapper` in `src/main/java/com/google/gson/internal/` — a class placed in Gson's own package on purpose. Similarly, `src/main/java/org/gitlab4j/api/Extended*.java` patch gitlab4j from inside its package. Don't "clean these up" into `de.catma`.

### In-memory graph & indexing

`repository/git/graph` defines fine-grained provider interfaces (`DocumentProvider`, `CollectionsProvider`, `TagsetsProvider`, …); `graph/lazy/LazyGraphProjectHandler` is the current implementation, backed by Guava `LoadingCache`s that materialize documents/collections from Git on demand. `LazyGraphProjectIndexer` implements `de.catma.indexer.Indexer`.

`de.catma.indexer` does tokenization and term extraction with Lucene analyzers — `WhitespaceAndPunctuationAnalyzer` plus custom tokenizers that honour user-defined *unseparable character sequences* (a `CharTree` of sequences that must not be split) — and provides KWIC (`KwicProvider`). Term positions carry character offsets so that annotations can be anchored back into the source text.

### Query language

CATMA has its own query language. Grammars are ANTLR **3** sources under `grammars/` (`ast`, `parser`, `tree`, `pointertarget`), but there is **no ANTLR plugin in the build** — the generated lexer/parser/walker in `de/catma/queryengine/parser/` are **checked in**. If you change a `.g` file you must regenerate with an ANTLR 3 tool and commit the output. `de.catma.queryengine` holds the query node types (`Phrase`, `TagQuery`, `CollocQuery`, `FreqQuery`, `SimilQuery`, refinements, …) executed as a `QueryJob`.

### UI

`de.catma.ui.module` mirrors the product's modules: `dashboard`, `project`, `tags`, `annotate`, `analyze`, `account`, plus `main` (shell/nav/error handling).

Navigation is **event-bus driven, not URL-routed**: Guava `EventBus` carries `RouteTo*Event`s from `de.catma.ui.events.routing`, and views implement `de.catma.ui.CatmaRouter` (`@Subscribe` handlers + `isNewTarget()`). Domain changes propagate the same way via `de.catma.project.event` (`DocumentChangeEvent`, `CollectionChangeEvent`, `CommentChangeEvent`, …).

Long-running work goes through `de.catma.backgroundservice` (`BackgroundService` / `ProgressCallable` / `ExecutionListener`), with `UIBackgroundService` marshalling results back onto the Vaadin UI thread. Because push mode is `MANUAL`, UI updates from background threads need an explicit push.

`de.catma.ui.client.ui` is **GWT client-side code** compiled into the `CleaWidgetset` — most notably `tagger` (the annotation editor: selection handling, comment bubbles, tag highlighting) and `visualization`. Changing anything under `ui/client` (or `CleaWidgetset.gwt.xml`) requires a widgetset recompile. Note `CleaWidgetset.gwt.xml` sets `user.agent` to `safari` (WebKit/Blink) only — deliberate, to keep compile times down.

`analyze/visualization` wraps third-party JS shipped in `src/main/webapp/VAADIN/` (`doubletreejs`, `vega`).

UI strings go through `de.catma.ui.i18n.Messages` / `src/main/resources/de/catma/ui/i18n/messages.properties`.

### Import/export

`de.catma.serialization.tei` reads/writes TEI (the historical CATMA interchange format); `serialization/intrinsic` handles intrinsic markup extracted from source documents; `api/v1/serialization` produces the JSON project export. Document ingestion uses Apache Tika (`tika-parsers-standard-package` + language detection).

## Tests

- Only `src/test/java` is compiled. **`src/test/de` and `src/test/helpers` are legacy trees outside the Maven test source root** — they are dead code kept for reference; do not add to them and don't assume they compile.
- Tests split into two kinds:
  - **Self-contained**: `api/v1/service/*Test` (Jersey Test Framework + Grizzly + Mockito), `TagsetDefinitionTest`, `SortedReflectiveTypeAdapterFactoryTest`.
  - **GitLab-backed integration tests**: everything in `repository/git/` (`GitProjectsManagerTest`, `GitProjectHandlerTest`, `GitLabServerManagerTest`, …). These create real data on a live GitLab server, then clean up. They are slow and require a configured properties file (`-Dprop=...`) pointing at your dev GitLab. The documented build skips them (`-DskipTests=true`).
- JUnit 5 (Jupiter) is the API for `src/test/java`.

## Conventions

- Logging in application code is `java.util.logging` throughout (`slf4j-simple` is present only as a provider for third-party libraries). Don't introduce SLF4J calls in `de.catma`.
- Existing Java sources are indented with **tabs**; lines run long (the `.editorconfig` — a large IDEA export — sets `max_line_length = 160` but declares spaces at the root level, which the codebase does not follow). Match the surrounding file.
- `.aiignore` marks files that should not be fed to AI tooling (all `*.properties`, `doc/`, `.run/`, `testdocs/`, build output).