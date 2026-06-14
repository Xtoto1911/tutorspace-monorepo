# Tutor Calendar Week Grid Design

## Goal

Make the tutor weekly calendar show Monday through Sunday without horizontal
scrolling on a 1440 px desktop viewport and place lessons at their real time,
including lessons beginning at 08:00.

## Current Problems

- The desktop calendar shares its row with a fixed 320 px sidebar, leaving only
  622 px for an 820 px grid.
- The CRM service returns only five fixed labels from 12:00 through 20:00.
- Lesson rows are calculated with a hard-coded `(hour - 12) / 2` formula, so an
  08:00 lesson is rendered in the first visible row instead of at 08:00.
- The CSS grid declares six body rows even though the response currently
  contains five labels.

## Approved Layout

- The weekly calendar occupies the full content width.
- The former right-side context cards move below the weekly calendar in a
  compact responsive row.
- The desktop grid has one narrow time column and seven equal day columns.
- Horizontal scrolling remains an internal fallback for narrow screens only.
- The grid body scrolls vertically inside the calendar card so the page does
  not become excessively tall.
- The day header remains visible while the grid body scrolls.
- Time labels cover 08:00 through 22:00 in one-hour intervals.

## Time And Event Model

- The backend returns the complete hourly scale from 08:00 through 22:00.
- Each event row is derived from its actual start time relative to 08:00.
- Event height is derived from its duration in 60-minute grid units, with a
  minimum visual height that keeps short lessons usable.
- Slots use the same time-to-grid conversion as lessons.
- Events outside the supported 08:00-22:00 display range are clamped to the
  nearest visible boundary rather than being placed using the old noon-based
  formula.

## Responsive Behavior

- At desktop widths, all seven days fit in the card with no horizontal
  scrollbar.
- At tablet and mobile widths, the grid keeps a practical minimum day width
  and scrolls horizontally inside `.tutor-week-scroll`.
- The page itself must not gain horizontal overflow.
- Context cards become two columns when space allows and one column on mobile.

## Scope

This task covers checklist item 11 only. It does not remove the amber
`Связано` statistic or change the pending-request button; those remain separate
checklist items.

## Verification

- Frontend helper tests cover the 08:00-22:00 scale, event placement, duration,
  and clamping.
- CRM tests prove an 08:00 lesson receives the correct first row and the
  response contains the complete scale.
- Frontend tests, typecheck/build, and CRM tests pass.
- Docker Compose rebuilds the stack.
- Browser checks confirm all seven columns fit at 1440 px and internal
  horizontal scrolling is used on mobile.
