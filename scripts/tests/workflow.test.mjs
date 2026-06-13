import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const root = resolve(import.meta.dirname, "../..");

function read(relativePath) {
  try {
    return readFileSync(resolve(root, relativePath), "utf8");
  } catch {
    throw new Error(`${relativePath} does not exist`);
  }
}

function assertMatch(value, pattern, message) {
  if (!pattern.test(value)) {
    throw new Error(message);
  }
}

const workflow = read(".github/workflows/deploy.yml");
const productionCompose = read("docker-compose.prod.yml");

assertMatch(workflow, /push:\s*\n\s*branches:\s*\[\s*main\s*\]/, "workflow must deploy main");
assertMatch(workflow, /workflow_dispatch:/, "workflow must support manual dispatch");
assertMatch(workflow, /permissions:\s*\n\s*contents:\s*read/, "workflow permissions must be read-only");
assertMatch(workflow, /cancel-in-progress:\s*true/, "workflow must serialize deployments");
assertMatch(workflow, /npm ci/, "frontend dependencies must use npm ci");
assertMatch(workflow, /npm test/, "frontend tests are missing");
assertMatch(workflow, /npm run build/, "frontend build is missing");

for (const service of [
  "auth-service",
  "marketplace-service",
  "crm-service",
  "notification-service",
  "admin-service",
  "subscription-service",
]) {
  assertMatch(workflow, new RegExp(service), `backend matrix is missing ${service}`);
}

assertMatch(workflow, /docker compose[\s\S]*config --quiet/, "Compose validation is missing");
assertMatch(workflow, /needs:\s*\[[^\]]*frontend[^\]]*backend[^\]]*compose[^\]]*\]/, "deploy must depend on all CI jobs");
assertMatch(workflow, /vars\.DEPLOY_ENABLED\s*==\s*'true'/, "deployment must have an explicit enable switch");
assertMatch(workflow, /DEPLOY_KNOWN_HOSTS/, "known_hosts secret is missing");
assertMatch(workflow, /StrictHostKeyChecking=yes/, "strict SSH host verification is required");
assertMatch(workflow, /github\.sha/, "deployment must pass the tested GitHub SHA");
assertMatch(workflow, /sudo \/usr\/local\/sbin\/tutorspace-deploy/, "workflow must use the sudo deployment wrapper");

assertMatch(productionCompose, /gateway:[\s\S]*ports:\s*!override\s*\n\s*-\s*"80:80"/, "gateway must publish port 80");
const publishedPortBlocks = productionCompose.match(/^\s+ports:/gm) ?? [];
if (publishedPortBlocks.length !== 1) {
  throw new Error("production override must not publish backend or infrastructure ports");
}
assertMatch(productionCompose, /restart:\s*unless-stopped/, "production services need restart policies");

console.log("PASS: workflow and production Compose contract");
