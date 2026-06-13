import ts from 'typescript';
import { describe, expect, it } from 'vitest';

const sourceRoot = ts.sys.resolvePath('src');

describe('production API sources', () => {
  it('contain no runtime mock modules or fallback API arguments', () => {
    const violations: string[] = [];
    const sources = ts.sys.readDirectory(sourceRoot, ['.ts', '.tsx'])
      .filter((path) => !/\.test\.(ts|tsx)$/.test(path));

    for (const path of sources) {
      const source = ts.sys.readFile(path) ?? '';
      const file = path.slice(sourceRoot.length + 1);
      const ast = ts.createSourceFile(path, source, ts.ScriptTarget.Latest, true);

      if (file.replace(/\\/g, '/') === 'shared/api/mock-data.ts') {
        violations.push(`${file}: runtime mock module exists`);
      }

      function visit(node: ts.Node) {
        if (ts.isImportDeclaration(node) && ts.isStringLiteral(node.moduleSpecifier)
          && node.moduleSpecifier.text.includes('mock-data')) {
          violations.push(`${file}: imports ${node.moduleSpecifier.text}`);
        }

        if (ts.isCallExpression(node) && ts.isIdentifier(node.expression)
          && ['apiRequest', 'authRequest', 'liveApiRequest'].includes(node.expression.text)
          && node.arguments.length > 2) {
          violations.push(`${file}: ${node.expression.text} has a fallback argument`);
        }

        ts.forEachChild(node, visit);
      }

      visit(ast);
    }

    expect(violations).toEqual([]);
  });
});
