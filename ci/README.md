# Continuous Integration

Frostguard builds on GitHub Actions from
[`.github/workflows/daily-windows-bundle.yml`](../.github/workflows/daily-windows-bundle.yml).
No manual activation step is needed — the workflow is live once it is on `main`.

## When it runs

| Trigger | Purpose |
|---|---|
| `schedule` (03:17 UTC daily) | Publishes a nightly Windows bundle for testers |
| `pull_request` | Guards `pom.xml`, `src/`, `tools/` and workflow changes |
| `push` to `ci/**` | Lets CI changes be iterated on a branch |
| `workflow_dispatch` | On-demand build from the Actions tab |

## What the pipeline does

1. Checks out the repository **with Git LFS**, then asserts that every LFS asset
   (the OpenCV `.dll`, `adb.exe`, and the `.traineddata` OCR models) was really
   materialised. A pointer stub that slipped through would otherwise yield a
   bundle that fails only at runtime on a user's machine.
2. Sets up **Temurin JDK 21** with a Maven dependency cache.
3. Installs `libtesseract` / `libleptonica`, which tess4j binds at runtime for
   the OCR regression tests. OpenCV needs no system package — the
   `org.openpnp:opencv` artifact ships the Linux native image.
4. Runs `mvn clean install -Djavafx.platform=win`. This **cross-builds the
   Windows desktop bundle from Linux** while still executing the full JUnit 5
   suite, including the vision and OCR saved-frame tests.
5. Verifies the produced ZIP: Windows JavaFX runtime present, Linux JavaFX
   runtime absent, launcher/watcher JARs, bundled `adb`/OCR assets, template
   sprites, `custom_tasks/`, and a floor on the number of staged runtime JARs.
6. Cross-checks that **every `Class-Path` entry in the app manifest actually
   exists in the ZIP** via [`verify_bundle_manifest.py`](verify_bundle_manifest.py).
7. Uploads the bundle (version-tagged, no re-compression) and the Surefire
   test reports.

## Why `-Djavafx.platform=win`

JavaFX artifacts are platform-classified. Without this flag a Linux runner
resolves the `-linux` classifier and produces a bundle that cannot start on
Windows. The flag forces the `-win` classifier so the artifact is usable by the
Windows userbase, and step 5 asserts the substitution really took effect.

## Notes for maintainers

- Tests are **not** skipped. `OpenCvPatternLocator.loadNativeLibrary()` selects
  the native image per platform, so the vision suites run on Linux runners and
  on Windows developer machines alike.
- The bundle is ~220 MB, mostly the OpenCV and JavaFX runtimes. It is uploaded
  with `compression-level: 0` because a ZIP does not recompress usefully.
- Reproduce a CI failure locally with:

  ```sh
  mvn clean install -Djavafx.platform=win
  ```
