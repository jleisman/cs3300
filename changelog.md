# Changelog
All notable changes to this project will be documented in this file.
Taken from the [keep a changelog project](https://keepachangelog.com/en/1.0.0/).
See their website for proper use of a changelog

TLDR: proper use of this file is at every merge to main branch do the following:

- Add info to changelog in unreleased
- Types of changes:
  - Added
  - Changed
  - Fixed
  - Removed
  - Security

Maintainer will:
- add info to correct version
- Add tag to commit

## [1.0.2] - 2026-04-25
## Added
- ML Python Code
- Final Presentation Documents

## [1.0.1] - 2026-04-21
## Changed
- Interface labels updated to match ML labels

## [1.0.0] - 2026-04-21
## Added
- New ML model
- Interface code module
- Name of programmer in each file
### Changed
- Standardized permission denied screens
- Moved from 4KB app alignment to 16 KB alignment through changing of TLfite to litert imports
- Increased speed of sending bitmaps to ML
- Interface takes bitmaps instead of files
### Removed
- old icon pictures
- old models
- unused imports
- saving of PNGs to phone

## [0.2.0] - 2026-04-11
### Added
- CameraOverlays.kt for text
- Intro screens for first time loading app
- Custom Icon for app
### Changed
- App colors and theming
  - New default colors and handling of dark and light mode
- Reworked CameraScreen.kt to move initial camera permissions to be handled by intro screens
- MainActivity.kt updated with new screens logic and theme logic
- Centralized color codes from camera overlay to Color.kt
- Updated Readme
### Removed
- FaceAlignmentBox and associated code

## [0.1.0] - 2026-03-16
### Added
- Imported and implemented Hilt and KSP.
- Created MyApp.kt.
- Implemented Hilt into existing code.
- Delete files after 1 minute or after success from ML (Whichever if first)
- Log printing for deleting files
- Comments to all files in feature/camera package
### Changed
- name of project and package name to be more concise
- changed from processing image to RGB to greyscale
### Removed
- ImageSaveUtils.kt as all functionality now housed in ImageRepository.kt
### Security
- Remove local.properties from Git history
- Added caching to gradle.properties for faster build times


## [0.0.2] - 2026-03-15
### Added
- testing for tensorflow lite
- tensorflow lite ML model
- Preview of camera view
- Internal saving of PNG files
  - orientation checking and fixed for saved files
  - cropped to 128x128
### Changed
- Gradle Settings
- 
### Fixed
- gitignore

## [0.0.1] - 2026-02-17
### Fixed
- Gradle settings for Android environment
- New documents

## [0.0.0] - 2026-02-12
### Added   
- Initial Android app interface.
- On-device ML model in python.
- Starting Management documents