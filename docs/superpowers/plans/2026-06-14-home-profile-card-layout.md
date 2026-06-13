# Home Profile Card Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the home profile card label, tutor name, and subjects on separate lines without overflowing the card.

**Architecture:** Preserve the existing React markup and three-column card grid. Add a focused layout rule for the details wrapper and wrapping safeguards for its text children.

**Tech Stack:** React 19, TypeScript, CSS, Vitest, Docker Compose

---

### Task 1: Protect the home profile card layout

**Files:**
- Create: `frontend/src/styles/home-profile-card-layout.test.ts`
- Modify: `frontend/src/styles/global.css:16330`
- Modify: `UI_FIX_CHECKLIST.md`

- [ ] **Step 1: Write the failing test**

Create a Vitest test that reads `global.css` and asserts that
`.home-desk-card--profile > div:nth-child(2)` uses `display: grid`,
`min-width: 0`, and a vertical gap, and that its text children use
`overflow-wrap: anywhere`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `npm test -- src/styles/home-profile-card-layout.test.ts`

Expected: FAIL because the focused details-wrapper declarations do not exist.

- [ ] **Step 3: Write the minimal implementation**

Add these focused rules near `.home-desk-card--profile`:

```css
.home-desk-card--profile > div:nth-child(2) {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.home-desk-card--profile > div:nth-child(2) > * {
  min-width: 0;
  overflow-wrap: anywhere;
}
```

- [ ] **Step 4: Run focused and project verification**

Run:

```powershell
npm test -- src/styles/home-profile-card-layout.test.ts
npm run build
docker compose up --build -d
```

Expected: the test and build exit successfully, and all containers start.

- [ ] **Step 5: Verify in the browser**

At `http://localhost:4173/`, inspect `.home-desk-card--profile` at desktop
width. Confirm the label, name, and subject render on separate lines and the
card has no horizontal overflow.

- [ ] **Step 6: Mark and merge**

Change the first checklist item in `UI_FIX_CHECKLIST.md` from `[ ]` to `[x]`,
commit the scoped files, switch to `dev`, and merge
`codex/fix-home-profile-card`.
