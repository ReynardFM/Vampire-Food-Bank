# Test Guide

49 tests across 10 classes. This explains what each one checks and why it's worth having.

## Running them

In IntelliJ: right-click `src/test/java` → **Run 'All Tests'**. Or:

```bash
./mvnw test
```

**XAMPP does not need to be running.** The tests use H2, an in-memory database that
lives inside the test process and disappears when it ends. Your real `blood_donation`
data is never touched.

## How the setup works

Two pieces make that possible:

- **`pom.xml`** declares H2 with `<scope>test</scope>`, meaning it's available to tests
  only and never ships with the application.
- **`src/test/resources/application-test.properties`** overrides the MySQL connection
  settings with H2 ones. Any test marked `@ActiveProfiles("test")` picks it up.

The schema is rebuilt from your entity classes each run (`ddl-auto=create-drop`), so
tests always start from empty tables. The seeder is switched off there
(`bloodbank.seed.enabled=false`) so each test creates only the data it needs —
`DatabaseSeederTest` turns it back on just for itself.

## Vocabulary

Enough to read any test in this project:

| Annotation | Meaning |
| --- | --- |
| `@Test` | This method is a test. |
| `@SpringBootTest` | Start the whole application before running it. Needed when the test uses real services and repositories. |
| `@ActiveProfiles("test")` | Use the H2 settings instead of MySQL. |
| `@Transactional` (on a test) | Undo every database change when the test finishes, so tests can't affect each other. |
| `@BeforeEach` | Run this before every test in the class, usually to set up data. |
| `@AutoConfigureMockMvc` | Let the test call URLs without starting a real web server. |
| `@WithMockUser` | Pretend someone is signed in, optionally with a role. |
| `assertThat(x).isEqualTo(y)` | Fail the test unless `x` equals `y`. |

## The tests

### `BloodBankApplicationTests` — 1 test

Starts the application and does nothing else. It sounds trivial, but it's the test most
likely to catch a broken configuration: a bad annotation, a missing bean, a malformed
property. If this fails, nothing else will run either.

### `BloodGroupTest` — 10 tests

Checks `BloodGroup.compatibleDonors()` against the real transfusion rules. This is the
most valuable file here, because these rules are medical facts that are easy to get
subtly wrong and impossible to spot by reading:

- O− can give to every group, and can only receive from O−
- AB+ can receive from every group
- A+ accepts A+, A−, O+ and O−
- **Rh-negative patients are never offered Rh-positive blood** — checked across all groups
- **A and B never cross**, except for AB recipients who can take both
- Every group can always receive its own

`compatibleRecipients()` answers the same question from the donor's side, and gets three
more tests. The important one walks all 64 pairs and asserts the two directions agree —
donor search reads one, recording a donation reads the other, and a disagreement would
mean offering a donor the next screen then refuses.

### `UrgencyLevelTest` — 2 tests

Confirms `LOW < MEDIUM < HIGH < CRITICAL` by severity, and — the interesting one —
proves that severity order is *different* from alphabetical order. That's the whole
reason `urgencySeverity` exists: sorting the stored text gives CRITICAL, HIGH, LOW,
MEDIUM, which puts LOW in the middle.

### `DonationRequestTest` — 2 tests

`setUrgencyLevel()` copies the severity into a separate column so the admin queue can
sort by it. These tests confirm the copy happens, and that it still happens when the
urgency is changed a second time — the case where a stale value would silently break
sorting.

### `UserServiceTest` — 4 tests

- Deactivating a donor works
- **Deactivating an administrator is refused.** Nothing in the app can create or
  re-enable an admin, so this would be a one-way door out of the admin area
- Deactivated accounts disappear from the account listing
- Registration stores a BCrypt hash, not the plain password, and assigns `USER`

### `DonationServiceTest` — 10 tests

- Recording a donation updates the donor's `lastDonationDate`
- **A backdated donation does not drag `lastDonationDate` backwards.** Donations are often
  entered days after collection, so a later entry can carry an earlier date; the field has
  to hold the most recent donation, not the most recently typed one
- **Linking a donation to an APPROVED request marks that request FULFILLED** — the step
  that closes the request lifecycle
- **Linking to a request that isn't APPROVED is refused**, and the request's status is
  left untouched
- **Linking to a request this donor's blood cannot serve is refused.** An A+ donation must
  not close out a request raised for an O− patient
- A donor with no blood group on file cannot link to anything, since nothing is known
  about who they can give to
- **A partial donation leaves the request APPROVED.** A request can ask for five units
  while one person gives one, so closing it on the first donation recorded the request as
  served with the patient still needing blood
- **The donation that completes a request fulfils it** — two units then one, against a
  three-unit request
- Over-collecting still fulfils the request rather than leaving it open forever
- **A request cannot be fulfilled by the person who raised it.** Somebody who needs blood
  is not in a position to give it, and allowing it would close the request as served while
  the patient still needed blood

### `ReportServiceTest` — 9 tests

The daily reports count two different things — when a request *arrived* and when it was
*decided* — and confusing those is exactly the bug that had the dashboard reporting old
requests as today's work. These pin the separation down:

- **A request raised on one day and approved on another counts once in each day's report**,
  not twice in either
- **A request with no decision time is never counted as decided.** Pending requests, and
  rows that predate the `decided_at` column, must stay out of every day's figures
- **A decision at 23:59 belongs to that day, not the next.** The day window runs to the
  last instant before midnight; comparing on the date alone would drop everything after 00:00
- Donations and their unit totals are both counted
- Quiet days are left out of the list rather than listed as rows of zeros
- The list is newest first
- **A day outside the 30-day window is absent from the list but still opens** with its real
  figures, since any date should be reachable
- A day with nothing on it reports itself as empty
- The detail page gets the rows behind each figure, not just the counts

### `DatabaseSeederTest` — 3 tests

The seeder runs on every application start, so the important property is that running it
twice changes nothing. This test runs it a second time and asserts the row counts are
identical. It also checks an administrator exists, and that every blood group has at
least one donor so search is never empty.

### `AdminSortingTest` — 5 tests

`Sort.by()` accepts any text, and an unknown field name throws an exception — so
`/admin/donors?sort=password` would be a 500 error without the whitelists in the
controllers. These tests confirm valid sorts work, invalid ones quietly fall back to a
default, urgency maps to its severity column, and donors get 403 on admin pages.

### `ProfileDateBindingTest` — 3 tests

Regression cover for a real bug. Without `@DateTimeFormat`, Spring renders a date in a
locale-specific format like `8/3/26`, and `<input type="date">` only accepts
`yyyy-MM-dd` — so the field showed up **empty** even though the date was stored fine.

- The edit form renders `value="1994-03-12"` in the HTML — this asserts on the actual
  page output, because the bug was in rendering, not in the data
- Submitting an ISO date saves correctly
- Submitting a blank date is rejected instead of wiping the stored value

## What isn't covered

Worth knowing so you don't over-trust a green run:

- No test renders the donation-recording form or the dashboard
- Pagination isn't tested
- Nothing checks CSS or page layout
- The compatibility rules are tested, but not the search query that uses them

A passing suite means the domain rules and the guards still hold. It does not mean every
page looks right.
