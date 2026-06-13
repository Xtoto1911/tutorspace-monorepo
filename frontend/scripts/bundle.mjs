import { mkdir, copyFile } from 'node:fs/promises';
import path from 'node:path';
import commonjs from '@rollup/plugin-commonjs';
import nodeResolve from '@rollup/plugin-node-resolve';
import replace from '@rollup/plugin-replace';
import typescript from '@rollup/plugin-typescript';
import { rollup } from 'rollup';
import { cssSource, cssTarget, distDir, indexHtml, rootDir, srcDir, tsconfigBuild } from './shared.mjs';

export async function bundleProject({ mode = 'production', clean = true } = {}) {
  await mkdir(distDir, { recursive: true });
  const viteEnv = Object.fromEntries(
    Object.entries(process.env).filter(([key]) => key.startsWith('VITE_')),
  );

  const bundle = await rollup({
    input: path.join(srcDir, 'main.tsx'),
    plugins: [
      replace({
        preventAssignment: true,
        values: {
          'process.env.NODE_ENV': JSON.stringify(mode),
          'import.meta.env': JSON.stringify(viteEnv),
        },
      }),
      nodeResolve({
        browser: true,
        extensions: ['.mjs', '.js', '.json', '.ts', '.tsx', '.jsx'],
      }),
      commonjs(),
      typescript({
        tsconfig: tsconfigBuild,
        sourceMap: true,
      }),
    ],
    onwarn(warning, warn) {
      if (
        warning.code === 'THIS_IS_UNDEFINED' ||
        warning.code === 'CIRCULAR_DEPENDENCY' ||
        warning.code === 'MODULE_LEVEL_DIRECTIVE'
      ) {
        return;
      }

      warn(warning);
    },
  });

  try {
    await bundle.write({
      dir: distDir,
      format: 'esm',
      inlineDynamicImports: true,
      entryFileNames: 'main.js',
      sourcemap: true,
    });
  } finally {
    await bundle.close();
  }

  await copyFile(indexHtml, path.join(distDir, 'index.html'));
  await copyFile(cssSource, cssTarget);
}
