## Behavior 0
RED: testSetGitPropertiesNamePlainFilename → NOTE: Groovy auto-generates a setter from the field, so `setGitPropertiesName("my.properties")` already works without any explicit method. Test passed immediately (no true RED possible for a plain acceptance test when no validation exists). Tracer bullet confirmed working.
GREEN: No implementation needed — Groovy field setter handles plain filenames natively. Confirmed PASSED.

## Behavior 1
RED: testSetGitPropertiesNameSubpath → Same as Behavior 0: Groovy auto-setter handles subpaths with no validation in place. Test passed immediately. Acceptance-only test, no validation needed.
GREEN: Confirmed PASSED — subpath `"discord4j/common/git.properties"` stored correctly.

## Behavior 2
RED: testSetGitPropertiesNameNullRejected → FAILED (expected IllegalArgumentException, but Groovy setter accepted null and set field to null). BUILD FAILED confirmed.
GREEN: Added `setGitPropertiesName(String name)` method with null check:
  - if name == null → throw IllegalArgumentException("gitPropertiesName must not be null")
  - else → this.gitPropertiesName = name
Confirmed PASSED.

## Behavior 3
RED: testSetGitPropertiesNameLeadingSlashRejected → FAILED (expected IllegalArgumentException, but null check only impl accepted "/git.properties"). BUILD FAILED confirmed.
GREEN: Added leading `/` check inside setGitPropertiesName:
  - if name.startsWith("/") → throw IllegalArgumentException("gitPropertiesName must be a relative path that stays under gitPropertiesResourceDir, got: '${name}'")
Confirmed PASSED.

## Behavior 4
RED: testSetGitPropertiesNameDotDotPrefixRejected → FAILED (expected IllegalArgumentException, but impl only checked for leading slash). BUILD FAILED confirmed.
GREEN: Replaced separate leading-slash check with combined check using segment split:
  - split on `[/\\]`, collect segments
  - if name.startsWith("/") OR segments.contains("..") → throw IllegalArgumentException (same message)
Confirmed PASSED.

## Behavior 5
RED: testSetGitPropertiesNameDotDotInMiddleRejected → Passed immediately — Behavior 4's impl (`segments.contains("..")`) already handles `..` anywhere in the path, including the middle. No additional implementation needed.
GREEN: Confirmed PASSED — `"some/../other/git.properties"` correctly rejected.

## Full suite
All GitPropertiesPluginExtensionTest tests PASSED (no regressions).

SIGNAL: TASK_DONE
