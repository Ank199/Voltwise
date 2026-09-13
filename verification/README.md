# Voltwise X battery monitoring

Open Voltwise once to start its foreground monitor. Its ongoing notification keeps the monitor visible and ready for charger connections. Android/OEM force-stop and background restrictions can still interrupt monitoring; reopening resumes it and closes any interrupted session at its last saved sample. No measurements are synthesized during a gap.

## Data

- ACTION_BATTERY_CHANGED provides level, status, plugged source, health, technology, temperature (tenths Celsius), and voltage (millivolts).
- CURRENT_NOW follows Android's microamp contract. The UI displays its absolute magnitude divided by 1000; raw sign is retained. Zero and integer unsupported sentinels produce an unsupported state. Small currents are not automatically reinterpreted as milliamps. The physically verified realme RMX2151 firmware exception reports mA; that exact manufacturer/model uses a disclosed conversion override. Saved samples normalize this to microamps.
- Invalid battery voltage (outside 1000–20000 mV) is unsupported. A known physical RMX2151 reported EXTRA_VOLTAGE=4. This is not silently turned into 4 V. Battery power is unavailable without valid voltage and current.
- Power is an estimate of battery-side power, not USB input power or an adapter rating.
- Samples are recorded approximately every five seconds while connected. Room stores session boundaries, sample means, maximum temperature, stability and time connected at 100%. Duration and percentage gained are derived from saved boundaries.
- Timeline records observed transitions and keeps a rolling 24-hour view. “Overcharge” means connected at 100%, not proof of an electrical overcharge.
- The charger test requires 600 seconds of real observations without gaps longer than 30 seconds. Its heuristic score combines percentage gain per hour, temperature rise, overheat threshold crossings (45 C), and coefficients of variation for supported electrical sensors. Missing components are disclosed. Device workload and charge taper affect the score; it cannot independently diagnose a cable.
- Version 1 and 2 databases migrate without deleting records. Legacy sessions are retained with sensorVersion=0 (or the intermediate uncorrected sensorVersion=1) but excluded from operational views because their old collection path used substitute readings and incorrect aggregates.

## Build and test

Windows: `./gradlew clean assembleDebug`

Offline unit tests using JUnit already bundled with Gradle 8.2:

`./gradlew testDebugUnitTest -I verification/test-dependencies.gradle --offline`

The test-only dependency script does not affect app runtime or include fixture values in the APK.

## Physical checks

1. Open Command and expand Sensor debug to compare raw readings with Android's battery service.
2. Unplug, then reconnect. Verify the status changes immediately, a new session starts, and Timeline contains both transitions.
3. Start the 10-minute charger test in Lab. Leave the phone plugged in, including with the app in the background. Verify progress, the saved result, and the Timeline result event.
4. Disconnect during a second test to verify interruption rather than a fabricated score.
5. Verify History and Analytics after disconnecting. Missing current or voltage must stay unsupported.
6. Stop the app, reopen it, and verify saved history remains and interrupted monitoring is marked partial.


## Verified on 2026-09-12

- `clean assembleDebug testDebugUnitTest -I verification/test-dependencies.gradle --offline`: BUILD SUCCESSFUL; 12 tests, zero failures.
- Installed debug APK on connected realme RMX2151 and successfully launched MainActivity.
- Upgraded the phone's existing version-1 database to version 3 without clearing app data.
- Screen showed 96%, Charging, USB, 38.90 C, and a changing corrected current (86 mA in one captured frame).
- Room snapshot confirmed a sensorVersion=2 active USB session with `endTime=null`; consecutive samples were approximately 5005 milliseconds apart. Normalized current samples included -86000 and -250000 microamps.
- Invalid voltage was stored as NULL, and average voltage/wattage remained NULL rather than fabricated values.
- Connection and charging-start events were present in Room.
- Physical unplug/reconnect and completing the button-started ten-minute test remain manual checks. OEM firmware denies ADB INJECT_EVENTS; no battery readings were spoofed to substitute for those checks.
