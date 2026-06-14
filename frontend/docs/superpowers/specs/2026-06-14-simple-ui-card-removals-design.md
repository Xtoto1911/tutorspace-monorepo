# Simple UI Card Removals Design

## Goal

Remove four low-value interface blocks without changing page data contracts or unrelated page behavior:

1. `tutor-subscription-stats` from `/app/tutor/subscription`.
2. `tutor-checkout-summary` from `/app/tutor/subscription/checkout/result`.
3. The amber `tutor-calendar-stat` labelled `Связано` from `/app/tutor/calendar`.
4. The `Уведомления`, `Модерация`, and `Что улучшить` cards from `/app/tutor/profile`.

## Scope

Each checklist item is implemented in its own `codex/*` branch and merged into
`dev` only after its focused verification succeeds. The existing uncommitted
checklist addition remains outside these commits.

The changes remove only page markup and imports or local values that become
unused. API response types and backend page-view payloads remain unchanged
because other consumers may still rely on those fields.

## Page Behavior

### Tutor Subscription

The tariff cards, current-plan information, payment history, and subscription
actions remain unchanged. Only the decorative statistics strip is removed.

### Checkout Result

The result status, explanatory text, and navigation actions remain unchanged.
Only the payment summary card is removed.

### Tutor Calendar

The lesson count and available-window statistics remain. The amber `Связано`
card is removed, and the remaining statistics use the existing responsive grid.

### Tutor Profile

Profile editing, publication state, preview, subjects, pricing, experience, and
other active controls remain. The three informational cards named in the
checklist are removed without replacement.

## Verification

For each branch:

- Add or update a focused source-structure regression test before changing the
  page, and verify that it fails for the targeted block.
- Remove the targeted markup and verify the focused test passes.
- Run `npm test` and `npm run build`.
- Run `docker compose up --build -d`.
- Inspect the affected local page in the in-app browser.
- Mark only the corresponding checklist item as completed.
- Commit, merge into `dev`, rerun the relevant frontend verification, and delete
  the feature branch.

After all four merges, run the frontend tests and production build once more and
confirm the Docker Compose services are running.
