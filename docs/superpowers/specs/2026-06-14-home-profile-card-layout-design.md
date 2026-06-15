# Home Profile Card Layout Design

## Problem

At desktop widths, the label "Подходит по цели" and the tutor name in
`.home-desk-card--profile` render on the same line. Long tutor names or subject
values can also make the text column overflow its card.

## Design

Keep the existing three-column card layout: avatar, tutor details, and rating.
Turn the tutor-details wrapper into a vertical grid with a small internal gap
and `min-width: 0`. Make its text children wrap within the available column
instead of increasing the card width.

No React markup or data-flow changes are required. Mobile behavior remains the
same: the rating moves below the two main columns at the existing breakpoint.

## Verification

- Add a source-level regression test for the required CSS declarations.
- Run the focused Vitest test and the frontend build.
- Rebuild the local stack with `docker compose up --build -d`.
- Inspect the card at desktop width and confirm the three text values appear on
  separate lines without horizontal overflow.

