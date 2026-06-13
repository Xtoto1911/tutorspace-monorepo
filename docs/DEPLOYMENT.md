# TutorSpace deployment

This guide deploys TutorSpace to Ubuntu 22.04 at:

`http://176.109.106.132`

The setup intentionally uses an IP address without TLS. It is suitable for a
temporary demonstration only. Do not use real personal data or reuse real
passwords because browser traffic is not encrypted.

## Deployment model

- Pushes to `main` run frontend, backend, and Compose checks.
- Deployment starts only when all checks pass.
- GitHub Actions connects as `kirill`.
- `/usr/local/sbin/tutorspace-deploy` checks out the exact tested commit.
- Docker Compose exposes only the gateway on host port 80.
- PostgreSQL data remains in the `postgres_data` Docker volume.

## 1. Create the server GitHub deploy key

Run as `root` on the server:

```bash
install -d -m 700 /root/.ssh
ssh-keygen -t ed25519 \
  -C tutorspace-server \
  -f /root/.ssh/tutorspace_github \
  -N ''

ssh-keyscan github.com >> /root/.ssh/known_hosts
chmod 600 /root/.ssh/known_hosts

cat >/root/.ssh/config <<'EOF'
Host github.com
  HostName github.com
  User git
  IdentityFile /root/.ssh/tutorspace_github
  IdentitiesOnly yes
EOF
chmod 600 /root/.ssh/config

cat /root/.ssh/tutorspace_github.pub
```

Copy the displayed public key. In GitHub open:

`tutorspace-monorepo → Settings → Deploy keys → Add deploy key`

Use the title `TutorSpace production server`. Leave write access disabled.

Verify from the server:

```bash
ssh -T git@github.com
```

GitHub should report successful authentication.

## 2. Create the GitHub Actions SSH key

Run on the local Windows machine in PowerShell:

```powershell
ssh-keygen -t ed25519 `
  -C "tutorspace-github-actions" `
  -f "$HOME\.ssh\tutorspace_actions" `
  -N '""'
```

This creates:

- Private key: `$HOME\.ssh\tutorspace_actions`
- Public key: `$HOME\.ssh\tutorspace_actions.pub`

Never commit either file. The private key goes only into a GitHub Actions
secret.

## 3. Bootstrap the server

After the CI/CD branch is merged into `main`, run as `root`:

```bash
git clone git@github.com:Xtoto1911/tutorspace-monorepo.git /tmp/tutorspace-bootstrap
```

Copy the complete single-line public key from
`$HOME\.ssh\tutorspace_actions.pub`, then run:

```bash
bash /tmp/tutorspace-bootstrap/scripts/bootstrap-server.sh \
  git@github.com:Xtoto1911/tutorspace-monorepo.git \
  'ssh-ed25519 REPLACE_WITH_COMPLETE_PUBLIC_KEY tutorspace-github-actions'

rm -rf /tmp/tutorspace-bootstrap
```

The bootstrap script:

- clones the repository to `/opt/tutorspace`;
- adds the CI public key to `kirill` with an OpenSSH forced command;
- installs the root-owned deployment wrapper;
- adds a sudo rule for that wrapper only.

The CI key cannot open an interactive shell, forward ports, or execute arbitrary
commands. It can request only a TutorSpace deployment for a full commit SHA.

## 4. Create the production environment

Run as `root`:

```bash
cd /opt/tutorspace

JWT_SECRET="$(openssl rand -base64 48 | tr -d '\n')"
POSTGRES_ADMIN_PASSWORD="$(openssl rand -hex 24)"
AUTH_DB_PASSWORD="$(openssl rand -hex 24)"
MARKETPLACE_DB_PASSWORD="$(openssl rand -hex 24)"
CRM_DB_PASSWORD="$(openssl rand -hex 24)"
NOTIFICATION_DB_PASSWORD="$(openssl rand -hex 24)"
ADMIN_DB_PASSWORD="$(openssl rand -hex 24)"
SUBSCRIPTION_DB_PASSWORD="$(openssl rand -hex 24)"
ADMIN_SEED_PASSWORD="$(openssl rand -base64 18 | tr -d '=+/')"

cat >.env <<EOF
JWT_SECRET=$JWT_SECRET
JWT_ACCESS_TOKEN_TTL=15m
JWT_REFRESH_TOKEN_TTL=24h

POSTGRES_ADMIN_USER=tutorspace_admin
POSTGRES_ADMIN_PASSWORD=$POSTGRES_ADMIN_PASSWORD
AUTH_DB_PASSWORD=$AUTH_DB_PASSWORD
MARKETPLACE_DB_PASSWORD=$MARKETPLACE_DB_PASSWORD
CRM_DB_PASSWORD=$CRM_DB_PASSWORD
NOTIFICATION_DB_PASSWORD=$NOTIFICATION_DB_PASSWORD
ADMIN_DB_PASSWORD=$ADMIN_DB_PASSWORD
SUBSCRIPTION_DB_PASSWORD=$SUBSCRIPTION_DB_PASSWORD

ADMIN_SEED_ENABLED=true
ADMIN_SEED_EMAIL=admin@tutorspace.local
ADMIN_SEED_PASSWORD=$ADMIN_SEED_PASSWORD

YOOKASSA_SHOP_ID=
YOOKASSA_SECRET_KEY=
YOOKASSA_RETURN_URL=http://176.109.106.132/app/tutor/subscription/checkout/result
YOOKASSA_WEBHOOK_SECRET=
YOOKASSA_TEST_MODE=true
EOF

chmod 600 .env
printf 'Demo administrator: admin@tutorspace.local\n'
printf 'Demo password: %s\n' "$ADMIN_SEED_PASSWORD"
```

Store the displayed administrator password locally. PostgreSQL passwords should
not be changed after the database volume is initialized unless the database
roles are updated at the same time.

## 5. Configure the firewall

Run as `root`:

```bash
ufw allow OpenSSH
ufw allow 80/tcp
ufw --force enable
ufw status
```

If the hosting provider has a separate network firewall, allow inbound TCP 22
and TCP 80 there as well.

## 6. Configure GitHub Actions

Open:

`tutorspace-monorepo → Settings → Secrets and variables → Actions`

Create repository secrets:

| Secret | Value |
| --- | --- |
| `DEPLOY_HOST` | `176.109.106.132` |
| `DEPLOY_USER` | `kirill` |
| `DEPLOY_SSH_KEY` | Complete contents of `$HOME\.ssh\tutorspace_actions` |
| `DEPLOY_KNOWN_HOSTS` | Server host-key line generated below |

Generate the pinned host-key line on the server:

```bash
read -r key_type key_value _ </etc/ssh/ssh_host_ed25519_key.pub
printf '176.109.106.132 %s %s\n' "$key_type" "$key_value"
```

Copy the complete output into `DEPLOY_KNOWN_HOSTS`.

Create a repository variable:

| Variable | Value |
| --- | --- |
| `DEPLOY_ENABLED` | `true` |

Until this variable is set to `true`, CI runs but deployment is skipped.

## 7. Run the first deployment

Open `Actions → Test and deploy → Run workflow` and select `main`.

On the server, monitor:

```bash
cd /opt/tutorspace
docker compose \
  --env-file .env \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  ps
```

Verify:

```bash
curl -i http://127.0.0.1/health
curl -I http://176.109.106.132
```

Then open `http://176.109.106.132` in a browser.

## Operations

Show logs:

```bash
cd /opt/tutorspace
docker compose \
  --env-file .env \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  logs --tail=200
```

Show one service:

```bash
docker logs --tail=200 tutorspace-auth-service
```

Restart the stack without rebuilding:

```bash
cd /opt/tutorspace
docker compose \
  --env-file .env \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  restart
```

Deploy or roll back to a commit that belongs to `main`:

```bash
sudo /usr/local/sbin/tutorspace-deploy FULL_40_CHARACTER_COMMIT_SHA
```

Do not run `docker compose down -v`; `-v` deletes the PostgreSQL volume.

## Troubleshooting

Check the gateway:

```bash
curl -v http://127.0.0.1/health
docker logs --tail=200 tutorspace-gateway
```

Check all containers:

```bash
docker ps -a
```

Check deployment permissions:

```bash
sudo -l -U kirill
visudo -cf /etc/sudoers.d/tutorspace-deploy
```

Test the CI key from the local machine. An interactive shell must be rejected:

```powershell
ssh -i "$HOME\.ssh\tutorspace_actions" kirill@176.109.106.132
```
