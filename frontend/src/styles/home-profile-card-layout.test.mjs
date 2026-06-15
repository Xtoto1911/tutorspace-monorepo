import { readFileSync } from 'node:fs';

import { describe, expect, it } from 'vitest';

const globalCss = readFileSync(new URL('./global.css', import.meta.url), 'utf8');

describe('home profile card layout', () => {
  it('keeps tutor details stacked and contained in the text column', () => {
    expect(globalCss).toMatch(
      /\.home-desk-card--profile > div:nth-child\(2\)\s*{[^}]*min-width:\s*0;[^}]*display:\s*grid;[^}]*gap:\s*2px;[^}]*}/s,
    );
    expect(globalCss).toMatch(
      /\.home-desk-card--profile > div:nth-child\(2\) > \*\s*{[^}]*min-width:\s*0;[^}]*overflow-wrap:\s*anywhere;[^}]*}/s,
    );
  });
});
