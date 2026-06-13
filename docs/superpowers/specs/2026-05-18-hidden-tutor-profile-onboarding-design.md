# Hidden Tutor Profile Onboarding Design

## Goal

New tutor accounts start with a hidden public card. The tutor is warned on first entry and receives a short, practical onboarding path for filling the profile and opening the card when ready.

## Backend Behavior

- CRM remains the source for tutor profile editing and visibility.
- New CRM tutor settings default to `isHidden = true`.
- The first CRM-to-marketplace sync creates the marketplace profile with `TutorProfileStatus.HIDDEN`.
- Direct marketplace tutor profile creation also starts as `HIDDEN`.
- Existing tutors keep their current visibility unless they explicitly toggle it.

## Frontend Behavior

- Tutor registration still writes the initial CRM profile after auth registration, but includes `isHidden: true`.
- After successful tutor registration, the app navigates to `/app/tutor/profile?welcome=1`.
- The tutor profile workspace shows an onboarding notice when `welcome=1`, when the profile is hidden, or when completion is below 100%.
- The notice explains that the card is hidden and gives a compact checklist: fill name/city, description, subjects, documents, then open visibility.
- The existing visibility switch remains the actual control for opening or hiding the card.

## Error Handling

- If initial CRM sync fails during registration, the existing registration error flow remains in place.
- If visibility toggle fails, the UI keeps the previous state and shows the existing error message.

## Testing

- Marketplace service test proves new profile creation starts hidden.
- CRM service test proves default tutor settings are hidden and workspace DTO reports hidden visibility.
- Frontend public API test proves tutor registration sends `isHidden: true`.
- Frontend helper or component-level test proves hidden/incomplete profile should show onboarding.
