# Account and data erasure

MusFit is local-first. It has no MusFit cloud account store, analytics store, or
server-side retention queue. The Settings screen exposes two destructive,
confirmed operations:

- **Delete this account** removes the active identity and every Room row owned
  by it. Other local accounts remain and the most recently updated account
  becomes active. If none remains, MusFit creates a blank local account.
- **Delete all MusFit data** removes every local identity and owned Room row,
  clears AI coach encrypted preferences, deletes the shared Android Keystore
  key, and creates one blank local account so the app remains usable.

Room deletion and fallback-session creation run in one transaction. Account
foreign-key cascades remove Food, Training, profile, goal, coach, and Health
cache rows. A failed transaction leaves the original account and session
intact.

The confirmation dialog can also delete records MusFit authored in Health
Connect. This is opt-in. Health cleanup runs before local deletion so the
authored-record ledger remains available for retry. A partial, unavailable, or
failed Health result stops local erasure and reports the failure. Records from
other apps are never targeted.

AI coach credentials are removed before local account deletion. Per-account
erasure removes only that account's encrypted credential. All-data erasure also
deletes the shared Keystore entry after clearing all encrypted preferences.

Encrypted export files previously saved through Data transfer live outside
MusFit's private app storage and are not deleted by either operation. They are
user-controlled backups and can restore the exported data until the user
deletes those files. Android or device-level backups are governed by the
platform backup policy, not by an in-app erasure request.

Cancellation or dismissing the confirmation dialog performs no repository
call and changes no data.
