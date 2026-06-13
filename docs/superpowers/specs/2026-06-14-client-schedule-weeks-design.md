# Client Schedule Week Navigation Design

## Goal

Make `/app/client/schedule` a real seven-day calendar that can move backward and
forward by week while displaying CRM lessons for the selected date range.

## Backend Contract

`GET /api/client/schedule` accepts optional ISO date query parameters:

```text
from=YYYY-MM-DD
to=YYYY-MM-DD
```

The parameters must be supplied together. The range must start on Monday and end
on Sunday exactly six days later. Without parameters, the service selects the
current Monday-to-Sunday week, except that an existing future lesson may remain
the initial focus when the current week has no visible lessons.

The response keeps the current `ClientSchedulePageDto` shape. `period`,
`weekDays`, `events`, time-slot rows, and week metrics are calculated from the
selected seven-day range. `upcomingLessons` remains global so the side panel
continues to show the next actionable lesson even when the selected week is
empty.

## Frontend Behavior

The page stores the selected Monday as state and requests the matching
Monday-to-Sunday range. The toolbar provides:

- previous week;
- current week;
- next week;
- manual refresh of the selected week.

The response period remains authoritative after every request. Navigation is
disabled while loading to prevent overlapping changes.

The calendar renders seven day columns. The selected week may be empty without
hiding the rest of the page: the grid area shows a local empty state while the
global next-lesson and reschedule information remains available.

## Calendar Layout

At desktop widths all seven days fit inside the calendar card. Day columns use
`minmax(0, 1fr)` so long labels do not expand the grid. Event cards wrap or
truncate internal text without changing column width.

At narrower widths the calendar becomes horizontally scrollable with a stable
minimum column width. The rest of the page remains responsive and does not
create document-level horizontal overflow.

Time rows are derived from real lessons and reschedule requests in the selected
week. Existing backend fallback rows remain available for an empty week.

## Verification

- CRM service unit tests cover explicit ranges, seven returned days, weekend
  lessons, invalid ranges, and global upcoming lessons.
- Frontend API tests cover encoded `from` and `to` parameters.
- Frontend helper tests cover Monday normalization and previous/current/next
  week calculations.
- Frontend build and complete Vitest suite pass.
- CRM service Maven tests pass.
- `docker compose up --build -d` succeeds.
- Browser verification covers previous, current, and next week navigation at
  desktop width and confirms that weekend columns fit.

