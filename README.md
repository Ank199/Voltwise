# Voltwise

Voltwise is an Android battery intelligence app with live charging monitoring, charging speed insights, battery graphs, health guidance, and user-controlled battery percentage alerts.

## Play Store policy files

- Privacy policy: docs/privacy-policy.md
- Store listing draft: play-store/store-listing.md
- Data Safety draft: play-store/data-safety-draft.md
- Release checklist: play-store/release-checklist.md

## Build

```powershell
./gradlew.bat :app:compileDebugKotlin
./gradlew.bat :app:bundleRelease
```

The release bundle is generated locally at:

```text
app/build/outputs/bundle/release/app-release.aab
```

Build outputs and local SDK settings are intentionally ignored by Git.
