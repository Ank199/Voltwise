# Play Store Release Checklist

## Required before upload
- [ ] Replace support email placeholders in store-listing.md and privacy-policy.md
- [ ] Host privacy policy online on a public URL
- [ ] Create 512 x 512 PNG app icon
- [ ] Create 1024 x 500 PNG feature graphic
- [ ] Capture at least 2 phone screenshots
- [ ] Create release keystore
- [ ] Build signed app-release.aab
- [ ] Complete Data Safety form
- [ ] Complete Content Rating questionnaire
- [ ] Select category: Tools
- [ ] Target audience: general/adults, not child-directed
- [ ] Ads declaration: no ads
- [ ] App access: no login/restricted access
- [ ] Explain foreground service use if Play asks: live battery monitoring, charging status, temperature and alerts

## Current project notes
- Package name: com.voltwise
- Version: 1.0, versionCode 1
- Login: none
- Ads: none found
- Cloud sync: none found
- Notifications: foreground monitoring plus user battery alerts
- Play target SDK updated to 36

## Risk to review carefully
The app uses FOREGROUND_SERVICE_SPECIAL_USE. In Play Console, explain this clearly as live battery monitoring while charging/discharging. If Google rejects special use, you may need to change foreground service type and reduce background behavior.
