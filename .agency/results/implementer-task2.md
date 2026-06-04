## Behavior 1
RED: testGitPropertiesNameSubpathInJar → Per task instructions, implementation already exists (Task 1 completed setGitPropertiesName; Directory.file(String) handles subpaths natively). Test compiled and ran directly to GREEN — no prior failing state possible without reverting Task 1 work.
GREEN: PASSED immediately — no impl changes needed. File found at build/generated/resources/git/discord4j/common/git.properties on disk; JAR contained entry "discord4j/common/git.properties" and did NOT contain root "git.properties".

## Behavior 2
RED: testGitPropertiesNamePlainFilenameInJar → Implementation already exists (same reason as Behavior 1). Went directly to GREEN.
GREEN: PASSED immediately — no impl changes needed. File at build/generated/resources/git/git-info.properties; JAR contained root entry "git-info.properties".

## Behavior 3
RED: testDefaultGitPropertiesNameInJar → Default behavior (gitPropertiesName = "git.properties") unchanged. Went directly to GREEN.
GREEN: PASSED immediately — no impl changes needed. JAR contained root entry "git.properties" (regression guard confirmed).

## Behavior 4
RED: testGitPropertiesNameLeadingSlashFailsFast → Implementation already exists in setGitPropertiesName setter. Went directly to GREEN.
GREEN: PASSED immediately — no impl changes needed. buildAndFail() succeeded; output contained "must be a relative path that stays under gitPropertiesResourceDir".

## Behavior 5
RED: testGitPropertiesNameDotDotSegmentFailsFast → Implementation already exists in setGitPropertiesName setter. Went directly to GREEN.
GREEN: PASSED immediately — no impl changes needed. buildAndFail() succeeded; output contained "must be a relative path that stays under gitPropertiesResourceDir".

## Full suite
All 17 tests in BasicFunctionalTest PASSED (13 pre-existing + 5 new).

SIGNAL: TASK_DONE
