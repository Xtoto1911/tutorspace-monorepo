# Tutor Profile Complaints Design

## Goal

Users with tutor or client roles can report a public tutor profile, and moderators/admins can act on complaints by hiding reviews, hiding tutor profiles, or preventing a user from publishing future reviews.

## Scope

- Add profile complaints from the public tutor profile page.
- Keep review complaints working for clients and tutors.
- Add a marketplace-owned review publication restriction keyed by `userId`.
- Expose internal marketplace moderation endpoints for admin-service actions.
- Extend admin-service complaint moderation so complaint decisions can call marketplace actions.
- Update the moderator complaints UI to show profile complaints and review restriction actions.

## Backend Design

`marketplace-service` remains the source of truth for tutor profiles and reviews. It will store active review restrictions and check them in `ReviewService.create` before publishing a review. This avoids making review publication depend on admin-service at runtime.

Profile complaints are created through marketplace so the service can resolve the target tutor profile, reported tutor user id, and profile snapshot. Marketplace forwards the complaint to admin-service through the existing internal complaint client.

`admin-service` remains the complaint queue and decision owner. A moderation request can include a concrete action:

- `HIDE_REVIEW`: approve complaint and set review status to `REJECTED`.
- `RESTRICT_REVIEW_PUBLICATION`: approve complaint and create a marketplace restriction for the reported user.
- `HIDE_TUTOR_PROFILE`: approve complaint and set tutor profile status to `HIDDEN`.
- `DISMISS`: reject complaint without side effects.

## Frontend Design

The public tutor profile page gets an inline "Пожаловаться" action in the booking card. Clients and tutors can use it. Anonymous users see the button as a link to sign in.

The moderator complaints page will select complaints from the queue, show target type-specific context, and send exact moderation actions to the backend. Review complaints offer deleting the review and restricting review publication. Tutor profile complaints offer hiding the profile.

## Error Handling

- Creating a profile complaint for a missing tutor profile returns not found.
- A restricted user receives a clear business error when attempting to publish a review.
- Failed admin-service sync while reporting returns the existing service-unavailable path.
- Moderator actions keep the complaint decision in admin-service even if downstream marketplace calls log an error, matching the existing client behavior.

## Verification

- Marketplace unit tests for review restriction and profile complaint payload.
- Admin-service unit tests for moderation action side effects.
- Frontend API tests for profile complaint and moderator action endpoints.
- Frontend typecheck/build.
- Targeted Maven tests for marketplace-service and admin-service.
