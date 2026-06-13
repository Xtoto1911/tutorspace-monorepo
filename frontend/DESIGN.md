# Design

## Theme

TutorSpace uses a warm daylight workspace theme: cream paper surfaces, moss-green primary actions, muted clay accents, and soft ink text. The physical scene is a user working from a laptop in a calm evening environment, choosing a tutor or managing CRM tasks without visual noise.

## Color

Use OKLCH tokens in CSS. Neutrals are warm, never pure black or pure white. Primary green is reserved for main actions, active navigation, and important selected states. Clay and amber are supporting tones for human, educational details.

## Typography

Use a tuned sans-serif stack with `Onest` first when available, then system UI fallbacks. Keep product labels compact and readable. Public headings can be larger, but workspace headings should stay practical.

## Layout

Use clear shells, generous rhythm, and fewer nested cards. Public pages can use editorial spacing and asymmetric support panels. Workspace pages should keep familiar topbar, sidebar, page content, tables, and forms.

## Components

Buttons are consistent across public and app surfaces: moss primary, paper secondary, quiet ghost. Cards use tinted surfaces, soft borders, and subtle shadows. Inputs have visible focus rings and avoid harsh gray borders.

## Motion

Use short 150-250ms transitions for hover, focus, and state changes. Respect reduced motion. Avoid decorative page-load choreography.
