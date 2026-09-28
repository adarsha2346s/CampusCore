# CampusCore GitHub Pre-Publication Audit

Audit date: 2026-09-29

## 1. Git status

An actual Git status could not be obtained. The installed Git command fails because Apple Command Line Tools are unavailable. `.git/HEAD` refers to `master`, but `.git/index` is absent, so staged, tracked, and untracked status cannot be verified. No commit or push was made.

## 2. Changed and untracked file count

The earlier filesystem inventory contained 219 paths that were candidates under the then-current ignore rules. After the ignore-rule updates described below, **205 filesystem paths do not match an ignore rule**. This is a filesystem/ignore-pattern count, not a verified Git changed or untracked count. Without a usable Git command and index, the exact Git count is unknown.

## 3. Files that will be committed

No files are known to be staged, and no files can be identified as definitely committed by a future `git add` because Git status is unavailable. Suitable public candidates for review are the application source, database schema, backend build files, frontend source and package manifests, project READMEs, `frontend/.env.example`, and `backend/src/main/resources/application-example.properties`.

Review the actual `git add -n .` output before staging or committing.

## 4. Files excluded by `.gitignore`

The root and nested ignore rules cover local environment files, backend `application*.properties` with an exception for `application-example.properties`, frontend dependencies and build output, backend build output, IDE and OS metadata, logs, common temporary files, private-key bundles, and internal development reports.

The relevant local/generated paths were checked against the current ignore rules:

- `backend/src/main/resources/application.properties` — ignored
- `frontend/node_modules/` — ignored
- `frontend/dist/` — ignored
- `backend/target/` — ignored
- `backend/.idea/` — ignored
- `.DS_Store` files — ignored
- Local `.env` files — ignored
- `frontend/.env.example` — visible by design
- `backend/src/main/resources/application-example.properties` — visible by design

Some ignored paths exist in the working directory. Ignore rules prevent adding untracked paths by default; they do not untrack a path that was already committed. Because Git status/index could not be checked, tracked status is not provable here.

## 5. `application.properties` tracking status

`backend/src/main/resources/application.properties` matches the current ignore rule and was not modified. **I cannot confirm from Git metadata that it is untracked**, because `.git/index` is absent and Git status could not run. Verify this after restoring Git command-line tools. Do not stage it.

## 6. Generated and local file tracking status

The listed paths match ignore rules. Their actual tracked status cannot be confirmed without the Git index:

- `.env` and local environment files
- `frontend/node_modules/`
- `frontend/dist/`
- `backend/target/`
- `.idea/`
- `.DS_Store`

## 7. Secret and credential audit

A content scan found configured datasource and JWT values in `backend/src/main/resources/application.properties` and the generated copy under `backend/target/`. **Secret values are intentionally withheld.** These paths are ignored. No other hardcoded secret values, token-shaped JWTs, password hashes, or private-key material were identified by the pattern scan. Password-related field names and password-handling code in application source are expected and are not themselves credentials.

No database commands were run. No credentials were changed. This workspace has no usable Git history to audit for prior exposure; privately review local configuration and rotate any value that may have been published elsewhere.

## 8. Internal reports

The following development reports remain local and are excluded by `.gitignore`:

- `auth-startup-fix-report.md`
- `backend-fix-report.md`
- `backend-integration-review.md`
- `backend-verification-report.md`
- `github-readiness-report.md`
- `codex-inspection-report.md`
- `frontend-implementation-plan.md`
- `frontend-phase1-report.md` through `frontend-phase5-final-report.md`
- `password-reset-review.md`
- `password-reset-execution-report.md`

These documents record internal implementation, testing, and security workflow and add little value to a public portfolio repository. Keep them locally; publish only intentionally maintained, sanitized project documentation. They were not deleted.

## 9. `.gitignore` changes and recommendation

The root `.gitignore` was updated to exclude common private-key and credential-bundle extensions and the internal reports. Existing rules already cover local environment and backend configuration files, generated output, dependencies, IDE/OS files, logs, and temporary files. Example environment/configuration files remain allowed.

No additional `.gitignore` change is currently recommended. After Git is operational, verify the rules with `git check-ignore -v` and ensure no sensitive path is already tracked.

## 10. Public-commit recommendation

**Do not make the first public commit until Git status works and the candidate list has been reviewed.** The ignore rules provide appropriate coverage, and the safe example configuration is included, but this audit cannot verify the exact Git index or tracked state. After fixing the Git tooling, inspect `git status --short --branch`, `git check-ignore -v` for sensitive/generated paths, `git add -n .`, and then review staged names and contents before committing.

No files were committed or pushed. No application functionality, database, or `application.properties` file was changed.
