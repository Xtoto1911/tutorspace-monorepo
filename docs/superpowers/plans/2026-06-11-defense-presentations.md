# Defense Presentations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create two coherent 12-slide academic defense presentations as editable native Google Slides files in the supplied Drive folder.

**Architecture:** Author each deck as a local editable PPTX, using a shared visual system and separate backend/frontend accent colors, then import both files to native Google Slides. Verify document structure through the Slides connector and verify every slide visually through fresh thumbnails before moving the files into the target folder.

**Tech Stack:** Presentation authoring runtime, PPTX, Google Drive import, Google Slides API connector, Google Slides thumbnails.

---

## File Structure

- Create: `C:\tmp\tutorspace-defense\backend-defense.pptx` - local backend deck staging file.
- Create: `C:\tmp\tutorspace-defense\frontend-defense.pptx` - local frontend deck staging file.
- Create: `C:\tmp\tutorspace-defense\backend-renders\` - temporary backend render/QA output.
- Create: `C:\tmp\tutorspace-defense\frontend-renders\` - temporary frontend render/QA output.
- Preserve: `C:\JavaMy\newDiploma\docs\superpowers\specs\2026-06-11-defense-presentations-design.md` - approved design source.
- Preserve: all four Google Docs used as source material.

### Task 1: Establish Authoring and Connector Capabilities

**Files:**
- Read: `C:\JavaMy\newDiploma\docs\superpowers\specs\2026-06-11-defense-presentations-design.md`
- Create: `C:\tmp\tutorspace-defense\`

- [ ] **Step 1: Load the presentation-authoring runtime**

Confirm that a presentation authoring tool capable of creating editable PPTX
files is callable. Do not create a blank Google Slides deck directly when the
high-quality PPTX import route is available.

- [ ] **Step 2: Load all required Google Slides workflow references**

Read the connector safety, target guard, discovery, request shape, thumbnail,
new-deck, import, visual-iteration, planning, archetype, and final-pass
references.

- [ ] **Step 3: Confirm the target Drive folder**

List folder:

```text
https://drive.google.com/drive/folders/1F4VYN3Bu7MkoQjgWkBUZpHgOZ5-d6rQN
```

Expected: the two approved speech documents are visible and no destination
presentation with the intended final title already exists.

### Task 2: Build the Shared Academic Visual System

**Files:**
- Create: `C:\tmp\tutorspace-defense\backend-defense.pptx`
- Create: `C:\tmp\tutorspace-defense\frontend-defense.pptx`

- [ ] **Step 1: Configure the 16:9 slide canvas**

Use a light warm-white background, dark neutral text, consistent margins, and
academic title/body hierarchy.

- [ ] **Step 2: Define deck-specific accents**

Use restrained dark green for backend diagrams and restrained dark blue for
frontend diagrams. Use accent colors only for hierarchy, connectors, and key
states.

- [ ] **Step 3: Define reusable slide treatments**

Create consistent title placement, slide numbering, footer placement, diagram
node style, connector style, screenshot-placeholder style, and closing-slide
treatment.

- [ ] **Step 4: Verify baseline readability**

Confirm title text is at least 32 pt, body text is at least 17 pt, and no
single-line title wraps into an unintended second line.

### Task 3: Author the Backend Presentation

**Files:**
- Modify: `C:\tmp\tutorspace-defense\backend-defense.pptx`
- Read: backend thesis and approved backend speech

- [ ] **Step 1: Create slide 1**

Create a minimal title slide containing the exact backend thesis title,
Smirnov K. A., supervisor Kirillova E. S., Kostroma State University, and 2026.

- [ ] **Step 2: Create slide 2**

Draw fragmented tools on the left: marketplace, messenger, calendar/table, and
separate CRM. Connect them to one TutorSpace platform block on the right.

- [ ] **Step 3: Create slide 3**

Show a five-stage chain:

```text
Analysis -> Requirements -> Design -> Implementation -> Verification
```

Place the backend thesis goal above the chain.

- [ ] **Step 4: Create slide 4**

Draw the request lifecycle:

```text
Created -> Accepted -> CRM client
        -> Rejected
        -> Cancelled
        -> Archived
```

Add two timetable rules: no past lessons and no overlapping tutor lessons.

- [ ] **Step 5: Create slide 5**

Draw the backend architecture with the browser/client, Nginx entry point, six
services, Kafka, and service-owned databases.

- [ ] **Step 6: Create slide 6**

Present the six services with one concise responsibility line each:
Auth, Marketplace, CRM, Notification, Admin, and Subscription.

- [ ] **Step 7: Create slide 7**

Use one request-processing example to distinguish synchronous REST calls from
asynchronous Kafka events.

- [ ] **Step 8: Create slide 8**

Draw the security chain:

```text
Credentials -> JWT -> Role check -> Ownership check -> Operation
```

Place audit logging as a side output for critical administrative operations.

- [ ] **Step 9: Create slide 9**

Show one database per service and dashed identifier-based cross-service
references. Explicitly state that physical foreign keys do not cross service
databases.

- [ ] **Step 10: Create slide 10**

Draw the tutor CRM subscription flow:

```text
Tariff selection -> Test YooKassa payment -> Webhook -> Subscription activated
```

Add a note: lesson payments are outside the platform.

- [ ] **Step 11: Create slide 11**

Place the backend implementation stack on the left and a compact checked
scenario matrix on the right.

- [ ] **Step 12: Create slide 12**

Summarize the author's backend contribution in four concise outcomes and add a
short transition to the frontend thesis.

### Task 4: Author the Frontend Presentation

**Files:**
- Modify: `C:\tmp\tutorspace-defense\frontend-defense.pptx`
- Read: frontend thesis and approved frontend speech

- [ ] **Step 1: Create slide 1**

Create a minimal title slide containing the exact frontend thesis title,
Skryabin N. Yu., supervisor Kirillova E. S., Kostroma State University, and
2026.

- [ ] **Step 2: Create slide 2**

Show the frontend thesis scope around four responsibilities: interaction
design, screen/view-model design, client architecture, and implementation.

- [ ] **Step 3: Create slide 3**

Split the slide between interface states and quality criteria. Include loading,
success, empty, error, submitting, and access denied.

- [ ] **Step 4: Create slide 4**

Draw the key user path:

```text
Catalogue -> Tutor profile -> Request -> Tutor accepts -> CRM student -> Lesson
```

- [ ] **Step 5: Create slide 5**

Show the design artefact chain:

```text
Use cases -> Activity diagrams -> Screen data -> View models -> Components
```

- [ ] **Step 6: Create slide 6**

Present React, TypeScript, React Router, Vitest, and Playwright with a concise
reason for each choice.

- [ ] **Step 7: Create slide 7**

Draw the frontend structure:

```text
Pages -> Components
Pages -> Session
Pages -> API modules -> REST API
```

- [ ] **Step 8: Create slide 8**

Separate public and protected routes and show role-directed navigation for
client, tutor, administrator, and moderator.

- [ ] **Step 9: Create slide 9**

Draw the protected request/session flow:

```text
Bearer request -> Success
               -> Auth error -> Refresh -> Retry
                                      -> Failure -> Clear session -> Login
```

- [ ] **Step 10: Create slide 10**

Create three editable screenshot placeholders:

```text
Catalogue: filters and tutor cards
Client cabinet: requests and statuses
Tutor cabinet: students, calendar, or lesson creation
```

- [ ] **Step 11: Create slide 11**

Show unit and E2E test layers plus the interface states verified by the tests.

- [ ] **Step 12: Create slide 12**

Summarize the author's frontend contribution and practical significance in
four concise outcomes.

### Task 5: Render and Inspect Local PPTX Files

**Files:**
- Read: `C:\tmp\tutorspace-defense\backend-defense.pptx`
- Read: `C:\tmp\tutorspace-defense\frontend-defense.pptx`
- Create: `C:\tmp\tutorspace-defense\backend-renders\`
- Create: `C:\tmp\tutorspace-defense\frontend-renders\`

- [ ] **Step 1: Render every backend slide**

Expected: 12 readable slide images.

- [ ] **Step 2: Inspect backend contact sheets and individual slides**

Fix every overlap, clipped label, small annotation, ambiguous connector, or
unbalanced layout.

- [ ] **Step 3: Render every frontend slide**

Expected: 12 readable slide images.

- [ ] **Step 4: Inspect frontend contact sheets and individual slides**

Verify that all three screenshot placeholders are obvious, editable, and state
exactly what future screenshots must show.

- [ ] **Step 5: Re-render after corrections**

Expected: 24 clean slides with no unresolved visual issue.

### Task 6: Import Both Decks as Native Google Slides

**Files:**
- Upload: `C:\tmp\tutorspace-defense\backend-defense.pptx`
- Upload: `C:\tmp\tutorspace-defense\frontend-defense.pptx`

- [ ] **Step 1: Import backend PPTX**

Import with native Google Slides conversion and title:

```text
ВКР — серверная часть TutorSpace — Смирнов К.А.
```

- [ ] **Step 2: Read back the backend presentation**

Expected: native Google Slides presentation with 12 editable slides.

- [ ] **Step 3: Import frontend PPTX**

Import with native Google Slides conversion and title:

```text
ВКР — пользовательский интерфейс TutorSpace — Скрябин Н.Ю.
```

- [ ] **Step 4: Read back the frontend presentation**

Expected: native Google Slides presentation with 12 editable slides.

- [ ] **Step 5: Move both presentations to the supplied Drive folder**

Preserve unrelated parents and verify both files are listed directly in the
target folder.

### Task 7: Verify Native Slides Structure and Appearance

**Files:**
- Read: imported backend Google Slides deck
- Read: imported frontend Google Slides deck

- [ ] **Step 1: Confirm target identities**

Verify presentation IDs, titles, URLs, and 12-slide counts.

- [ ] **Step 2: Inspect every backend slide through connector readback**

Confirm slide order, substantive text, editable diagrams, and the absence of
template prompts.

- [ ] **Step 3: Request and curl a fresh large thumbnail for every backend slide**

Inspect every thumbnail and correct all clipping, overlap, or import drift.

- [ ] **Step 4: Inspect every frontend slide through connector readback**

Confirm slide order, substantive text, editable diagrams, and all screenshot
placeholder labels.

- [ ] **Step 5: Request and curl a fresh large thumbnail for every frontend slide**

Inspect every thumbnail and correct all clipping, overlap, or import drift.

- [ ] **Step 6: Complete final folder verification**

Expected: both verified native Google Slides decks are present in the supplied
folder and the four source Google Docs remain unchanged.

### Task 8: Clean Staging Files and Hand Off

**Files:**
- Delete after successful verification:
  `C:\tmp\tutorspace-defense\backend-defense.pptx`
- Delete after successful verification:
  `C:\tmp\tutorspace-defense\frontend-defense.pptx`
- Delete after successful verification:
  `C:\tmp\tutorspace-defense\backend-renders\`
- Delete after successful verification:
  `C:\tmp\tutorspace-defense\frontend-renders\`

- [ ] **Step 1: Remove temporary local staging artifacts**

Keep the Google Slides files as the only user-facing deliverables.

- [ ] **Step 2: Return verified links**

Provide the observed Google Slides links for the backend and frontend
presentations and state that both contain 12 slides.
