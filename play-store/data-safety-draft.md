# Google Play Data Safety Draft

Use this as a starting point in Play Console. Verify every answer before submitting.

## Does your app collect or share user data?
Recommended answer based on current app behavior: No data is shared with third parties. Battery and app data is processed/stored locally on device.

If Play Console treats local-only battery history as collection, declare it as app activity / diagnostics collected for app functionality and stored on device only. Do not say data is transmitted if the app does not transmit it.

## Data types likely applicable
- App activity / app interactions: optional, if declaring local alert/history use
- Diagnostics / device or other IDs: avoid unless you add analytics/crash reporting later
- Health and fitness: likely no, because this is device battery health, not user health
- Location, contacts, camera, microphone, files, financial info: no

## Data sharing
No.

## Data processed ephemerally?
Live battery readings are processed to display monitoring and alerts. Some battery history is stored locally for charts and insights.

## Is data encrypted in transit?
Not applicable if no data leaves the device.

## Can users request data deletion?
Yes. Users can clear recorded history in app settings or clear app storage/uninstall through Android settings.

## Purpose
App functionality: live battery monitoring, alerts, graphs, charging history and insights.

## Ads
No ads.

## Analytics
No analytics currently found in dependencies.

## Account creation
No account required.
