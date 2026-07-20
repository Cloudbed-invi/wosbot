# Porting Notes: old `wosbot` to Frostguard

Generated on 2026-07-20 for the semantic port from `/home/sli/dev/wosbot` into this repository.

## Baseline Facts

- New repository reset root: `d159a07facfbb7636adf8ab5da0d3550440ecd44` (`2026-05-13T11:41:11+05:30`, `Initial commit: Repository reset`).
- Old repository divergence marker: `b59fcac51bab9018f3c6ff3c15466ed6743d0065` (`2026-05-12T23:10:31+02:00`, Selso, Life Essence back-tap fix).
- Old repository branch inspected: `fix/custom_exploration`, aligned with `origin/main`.
- Current new-repo dirty state before this note: untracked `AGENTS.md` and `fg-engine/lib/`. Do not overwrite or remove them while porting.
- Porting assumption: include `b59fcac` itself in the initial port set.

## Refactor Map

The repositories do not share useful Git ancestry after the reset, so direct cherry-picks are unsafe. Replay behavior into the Frostguard layout:

| Old project area | New project area | Notes |
| --- | --- | --- |
| `wos-ot` DTO/config classes | `fg-api` | Use `dev.frostguard.api.configs` and `dev.frostguard.api.domain`. |
| `wos-persitence` | `fg-data` | Keep persistence entities/services in the data module. |
| `wos-utiles` image/OCR utilities | `fg-vision` or `fg-engine` helpers | Vision primitives belong in `fg-vision`; task-facing facades belong in `fg-engine`. |
| `wos-serv` services, scheduler, tasks | `fg-engine` and `fg-tasks` | Infrastructure/services in `fg-engine`; routines in `fg-tasks`. |
| `wos-hmi` JavaFX UI | `fg-app` | Controllers now live under `dev.frostguard.app.panel.*`. |
| `wos-tgwatcher` | `fg-watcher` | Preserve watcher packaging as the canonical shaded watcher JAR. |
| root `lib` assets | `tools` or packaged resources | Runtime tools are staged by `fg-app` packaging. |

Package rename: `cl.camodev.wosbot...` became `dev.frostguard...`.

Important `fg-vision` refactor: do not reintroduce the old OCR provider interface. The new OCR integration point is `ResilientOcrExecutor.TextExtractor`, documented in `fg-vision/REFACTORING_NOTES.md`.

## Design, Rename, and Deletion Notes

Use this section before porting old diffs. It records semantic replacements that are easy to miss when file paths no longer match.

### Module and Package Renames

- Root Maven modules use the `fg-*` names; do not recreate old `wos-*` modules.
- Old `cl.camodev.wosbot.ot` DTOs and enums now live under `dev.frostguard.api.domain` and `dev.frostguard.api.configs`.
- Old `cl.camodev.wosbot.serv.task.impl` task classes map either to `fg-tasks` routines or to runtime custom task examples under root `custom_tasks/`.
- Old HMI controllers under `cl.camodev.wosbot.*.view` map to `fg-app/src/main/java/dev/frostguard/app/panel/*` controllers and `/layout` or `/styles` resources.
- Old watcher code maps to `fg-watcher`; do not move watcher behavior into the desktop app module.

### Architecture Replacements

- `ServScheduler` responsibilities are split between `ScheduleService`, `TaskDispatcher`, and per-profile `TaskQueue`.
- `ServTaskManager` behavior is represented by `TaskManagementService` and `TaskStateData`.
- Runtime custom tasks are managed by `CustomTaskService`; live/startup scheduling should go through `ScheduleService.scheduleCustomTask(...)`.
- Template search from task code should use `TemplateSearchHelper` and `TemplatesEnum`, not old file-path-only helpers unless a new classpath template enum is impossible.
- Task Builder persistence uses `TaskBuilderService` JSON definitions plus generated Java sidecars in `custom_tasks/`.

### Assets and Runtime Files

- Template PNGs belong in `fg-vision/src/main/resources/templates` and should be exposed through `fg-api/src/main/resources/config/templates.properties`.
- Runtime binaries and tools are staged from `tools/` by `fg-app` packaging. Generated or cached native files under `fg-engine/lib/` are not commit candidates.
- Desktop bundle contents are controlled by `fg-app/src/main/assembly/zip.xml`.

### Intentional Skips and Deletions

- Version-only bumps from the old repository are skipped unless Frostguard release policy asks for them.
- Old `AGENTS.md` content is not ported over the existing repository guide.
- Old Windows/autostart documentation is not part of this pass unless it maps cleanly to Frostguard docs.
- Do not revive the old OCR provider interface removed by the `fg-vision` refactor.
- Do not restore the Life Essence double-back navigation; the intended behavior is to avoid that extra back sequence.

## Commit Inventory

Status values:

- `pending`: not ported yet.
- `check`: inspect for existing equivalent before coding.
- `required`: confirmed desired if missing/incomplete here.
- `ported`: applied or confirmed present in Frostguard.
- `platform`: inspected; remaining work is platform/build documentation or runtime support tracked separately.
- `marker`: merge/grouping commit; inspect only for merge-resolution-only changes.
- `skip`: do not port unless policy changes.

| Status | Old hash | Date | Purpose | New target |
| --- | --- | --- | --- | --- |
| ported | `b59fcac` | 2026-05-12 | Life Essence: remove extra back tap after collection. | `fg-tasks` Life Essence routine. |
| marker | `6f6c26f` | 2026-06-13 | Groups janeistaken macOS integration branch. | Context only; see Linux foundation notes. |
| marker | `e4e79f9` | 2026-06-15 | Merges `mymain` fixes into 2.0.0 line. | Inspect for merge-only docs/Life Essence resolution. |
| platform | `8a4c3f1` | 2026-06-16 | Linux build/docs, packaging, Life Essence carry-over. | Life Essence covered; Linux docs/scripts remain platform follow-up. |
| marker | `1d84a8d` | 2026-06-18 | Groups Linux/build-doc follow-up. | Inspect for merge-only changes. |
| ported | `7b40567` | 2026-06-19 | Add scheduled shield custom task. | `fg-engine` custom task service/scheduler, `fg-app` custom task UI, examples. |
| ported | `34df727` | 2026-06-29 | Shield task settings, embedded templates, template asset path helper. | `fg-engine`, `fg-app`, `fg-vision` templates, `fg-app` assembly. |
| ported | `a7a21c5` | 2026-06-30 | Add expert idle exploration custom task example. | Custom task examples and packaging. |
| ported | `c6574a3` | 2026-06-30 | Old AGENTS update and custom task folder packaging. | Kept current `AGENTS.md`; ported custom task folder packaging. |
| ported | `e436118` | 2026-07-01 | Task Builder capture cross offset CSS fix. | `fg-app` task builder CSS/layout. |
| ported | `e4852ec` | 2026-07-01 | Add name field to Task Builder node and generated code. | `fg-api` automation model, `fg-app` task builder, `fg-engine` code generator. |
| marker | `7cc0785` | 2026-07-06 | Groups scheduled shield/custom-task branch. | Inspect for merge-only changes after leaf commits. |
| ported | `ccadf48` | 2026-07-14 | Task file / JSON support. | `fg-engine` `TaskBuilderService`, `fg-app` task builder UI, examples. |
| ported | `5bb54c3` | 2026-07-14 | Exploration claim disabled detection and image-search result size. | `fg-api` `ImageSearchResultData`, `fg-vision` locator result population, `fg-tasks` exploration routines. |
| skip | `8d7a2fa` | 2026-07-15 | Version-only release bump to old 2.0.3. | Skip unless Frostguard version policy requires it. |
| ported | `8b2c735` | 2026-07-19 | Exploration combat locked fix and exploration docs. | `fg-tasks` `DoExplorationRoutine`, docs if still relevant. |
| ported | `e0afe85` | 2026-07-19 | Longer exploration window. | `fg-tasks` `DoExplorationRoutine`. |

## Remaining Porting Work

The semantic port inspection pass is complete. The completed work covers Life Essence, exploration claim/result-size handling, Do Exploration timing, the Linux OpenCV loader, Task Builder JSON/name/crosshair fixes, the custom task examples, and the scheduled shield task/settings work.

Remaining non-platform leaf commits: none currently known.

Required inspection completed on 2026-07-20:

- `8a4c3f1`: Life Essence carry-over was already ported. The remaining differences are Linux/Windows docs, quick-build helper behavior, docs packaging, and version metadata; keep only the Linux-relevant parts in the platform follow-up list below.
- `e4e79f9`: merge-resolution diff adds old install/autostart docs and repeats the Life Essence back-tap removal. No additional Frostguard code port is required.
- `1d84a8d`: merge-resolution diff contains Linux README/docs/quick-build/packaging material only. No additional non-platform code port is required.
- `7cc0785`: merge-resolution diff groups the custom-task branch already ported in `5593ea3`; old `AGENTS.md` and version bump changes remain intentionally skipped.

## Linux Foundation From macOS Branch

Do not full-port macOS support in this pass. The macOS branch is useful for Linux builds, but full support is larger because it includes platform lifecycle classes, BlueStacks Air support, native loading, ADB screenshot behavior, and emulator profile settings.

Port only the foundations that remain useful after inspection:

- shell script ideas from `quick_build.sh`, `run-mac.sh`, and `start-watcher.sh` where they generalize to Linux;
- runtime-relative path resolution for staged assets;
- Git LFS asset verification from `scripts/verify-lfs-assets.sh`;
- Linux build documentation from `docs/LINUX.md`;
- packaging changes that make runtime assets available outside Windows.

Current Ubuntu native status:

- OpenCV is fixed for Linux builds/runtime startup by using `org.openpnp:opencv` native loading first, with the bundled DLL retained only as a Windows fallback.
- Tesseract is not bundled as Linux native binaries. This Ubuntu machine already has `libtesseract.so.5` and `liblept.so.5`, and a runtime probe through `TesseractOcrProvider` successfully read `123` from a generated image using `tools/tesseract` tessdata.

Remaining Linux build/runtime actions:

- Document Ubuntu prerequisites for OCR: `tesseract-ocr`, `libtesseract-dev`, and `libleptonica-dev` for fresh machines.
- Decide whether the Linux package should depend on system Tesseract or bundle Linux `libtesseract`/`liblept` binaries.
- Port or replace Windows-only ADB packaging; current staged tools include `adb.exe` and Windows DLLs, not Linux `adb`.
- Add a Linux runtime smoke check that loads OpenCV, locates tessdata, and performs a minimal OCR call.

## Per-Commit Porting Protocol

For each non-marker commit:

1. In the old repo, inspect `git show --find-renames --stat <hash>` and then the full diff.
2. In this repo, search for semantic equivalents before editing.
3. Classify the commit as `already present`, `obsolete`, `port`, or `blocked`.
4. Port behavior into the new module names and package names; do not copy old package paths.
5. Update this table or add a note under "Port Results" before moving to the next commit.

For marker merge commits:

1. Compare the merge commit to its first parent.
2. Ignore changes already covered by leaf commits.
3. Port only unique merge-resolution changes that are still relevant.

## Known Current Checks

- `EXPLORATION_CLAIM_DISABLED` already exists in `TemplatesEnum`.
- `ExplorationRoutine` already checks enabled and disabled claim templates.
- `ImageSearchResultData` now stores optional matched template size, populated by `OpenCvPatternLocator` hits.
- `CustomTaskService`, `TaskBuilderService`, and custom-task scheduler hooks already exist in the new architecture; custom task commits should be adapted rather than copied.

## Test Expectations

- Notes-only changes: no Maven build required.
- Task-only behavior ports: run the affected module test, typically `mvn -pl fg-tasks test`.
- Engine/service/model ports: run `mvn -pl fg-engine test` and any affected downstream module tests.
- API or cross-module ports: run `mvn clean install`.
- Add focused tests when behavior changes:
  - image-search result size compatibility;
  - exploration enabled/disabled claim handling;
  - task builder JSON/task-file loading;
  - custom task scheduling/settings behavior.

## Port Results

Record completed work here as commits are replayed.

- `b59fcac`: behavior was already present in `LifeEssenceRoutine`; removed stale disabled back-button blocks and navigation comments from both Life Essence routines so the current code matches the intended flow.
- `5bb54c3`: disabled exploration claim detection was already present; added optional `SizeData` to `ImageSearchResultData`, populated template size from OpenCV hit paths, and changed exploration reward claiming to tap within the matched claim-button bounds when size is available.
- `8b2c735` and `e0afe85`: rewrote `DoExplorationRoutine` around named tap areas, a bounded 2-minute fighting window, 15-second result-start delay, 25-second result detection window, victory/defeat statistics, detected explore-button tapping, and adapted task/design docs for Frostguard paths. Skipped the old version bump and legacy `AGENTS.md` path notes.
- Linux OpenCV foundation: `mvn clean install package` initially failed on Linux because both the app and evidence tests loaded the bundled Windows-only `opencv_java4110.dll`. Ported the old repo's cross-platform `org.openpnp` native loading path into `OpenCvPatternLocator.loadOpenCvNative()`, kept the DLL as a Windows fallback, and updated app startup/tests to use the new loader. Ubuntu now runs the OpenCV evidence tests instead of skipping them.
- `e436118`: ported the Task Builder preview alignment fix so crosshair coordinates match the top-aligned preview image.
- `e4852ec`: added Task Builder node names to `AutomationStep`, bound the existing UI name field, displayed names on node cards, and emitted node names as generated Java comments.
- `ccadf48`: added Task Builder JSON import/save support, generated Java sidecar output, current `custom_tasks` directory resolution, and a focused save/load test.
- `a7a21c5` and `c6574a3`: added the adapted `expert_idle_exploration` custom task example and included `custom_tasks` in the desktop bundle while leaving the existing `AGENTS.md` untouched.
- `7b40567` and `34df727`: added the adapted `shield` custom task, configurable custom-task settings (`firstExecutionUtc`, `followUpDelayHours`), persisted settings for disabled imported tasks, startup/live scheduling through a shared `ScheduleService.scheduleCustomTask` path, safe completion handling for tasks that clear their next schedule, the embedded `SHIELD_MY_CITY` template asset, and desktop bundle inclusion through the existing `custom_tasks` packaging.
