import path from 'node:path';
import { fileURLToPath } from 'node:url';

export const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
export const srcDir = path.join(rootDir, 'src');
export const distDir = path.join(rootDir, 'dist');
export const indexHtml = path.join(rootDir, 'index.html');
export const cssSource = path.join(srcDir, 'styles', 'global.css');
export const cssTarget = path.join(distDir, 'main.css');
export const tsconfigBuild = path.join(rootDir, 'tsconfig.build.json');
