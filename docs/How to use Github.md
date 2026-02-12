
# GitHub Team Workflow Checklist
Scope: Android app with on‑device ML. This checklist covers creating a branch, adding code,
rebasing with main, opening a pull request (PR), and ensuring documentation is updated.

If you are using an IDE, these steps will be incorporated in the git/GitHub interface.
Follow the steps in this document using the buttons and commands in your IDE.

## 1) Create a New Branch
Pull the latest main:
```terminaloutput
git checkout main
git pull origin main
```

Create and switch to a feature branch:
```terminaloutput
git checkout -b feature/<short-description>
```

### Naming convention:
```
feature/<short-description>
bugfix/<short-description>
model/<model-version-change>
refactor/<short-description>
```


## 2) Commit Your Work

Commit in small, logical increments.  
Use descriptive commit messages (type: summary):
```
add: implement camera preview
model: update emotion model to v4
fix: handle null buffer in inference wrapper
change: adjust normalization for model v4
```

Push the branch:
```terminaloutput
git push -u origin feature/<short-description>
```

## 3) Keep Your Branch Up to Date with Main (use rebase)
Before opening a PR, sync your branch with the latest main using rebase to maintain a linear history.

Fetch main:
```
git fetch origin
```
 
Rebase your branch onto the latest main:
```
git checkout feature/<short-description>
git rebase origin/
```

If conflicts occur:

- Resolve conflicts in files.
- Mark resolved files:  
  ``git add <file1> <file2>``
- Continue rebase:  
  ``git rebase --continue``
- If you rebased after you already pushed, force‑push the updated branch:  
   ``git push --force-with-lease``

## 4) Update the Changelog

- Edit CHANGELOG.md and add entries under **[Unreleased]** relevant sections.
- Keep entries short, specific, and focused on user‑visible changes.

Example:
```Markdown
## [Unreleased]
### Added
- Camera interface for live emotion detection.
### Changed
- Updated emotion_detection.tflite from v3 to v4; improved accuracy and reduced latency.Show more lines
```

## 5) Update Documentation (when applicable)
   Update docs in the same Pull Request if:
- ML model version changes
- Preprocessing pipeline changes
- UI/UX or behavior changes
- New modules/features introduced

Common files (if applicable):
- README.md
- docs/ pages
- ml/ notes (e.g., ml/MODEL_NOTES.md)
- CHANGELOG.md

## 6) Open a Pull Request to Main

- Target branch: main
- PR Title: short and descriptive  
Example: Add camera pipeline for on-device ML inference
- PR Description should include:
  - What changed and why 
  - Impacted areas (Android UI, ML model, preprocessing, performance)
  - Model version changes and any input/output shape or normalization changes
  - New dependencies or configuration changes
  - Confirmation that CHANGELOG.md and docs were updated

## 7) Pre‑Merge Checks

- Branch is rebased on origin/main (no merge commits)
- CI/build passes
- No leftover debug code or large unintended files
- Only relevant files changed
- Reviewer assigned (should be other teammate in respective team)

## 8) After Approval

- Merge method: project‑preferred (Squash and merge or Rebase and merge)
- Ensure merge target is main
- Optionally delete the branch after release is confirmed stable

## 9) Tagging Releases (maintainers - probably Joshua)

- Move items from **[Unreleased]** to a new version section in CHANGELOG.md:  
``Markdown## [1.3.0] - 2026-02-12``

- Create and push a tag:
```
git tag -a v1.3.0 -m "Release 1.3.0"
git push origin v1.3.0
```
- Draft a GitHub Release:
  - Select the tag
  - Paste the corresponding CHANGELOG.md section as notes
  - Publish