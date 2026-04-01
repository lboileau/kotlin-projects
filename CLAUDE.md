# Kotlin Projects

## Skills
- /build-feature: .claude/skills/build-feature/SKILL.md
- /fix-bug: .claude/skills/fix-bug/SKILL.md
- /create-acceptance-tests: .claude/skills/create-acceptance-tests/SKILL.md
- /create-project: .claude/skills/create-project/SKILL.md
- /db-manager: .claude/skills/db-manager/SKILL.md
- /service-manager: .claude/skills/service-manager/SKILL.md
- /web-manager: .claude/skills/web-manager/SKILL.md
- /linear-ticket: .claude/skills/linear-ticket/SKILL.md
- /run-dev: .claude/skills/run-dev/SKILL.md

## Agent Team
Specialized agents in `.claude/agents/` power the build workflow:
- **orchestrator** — Runs workflow, spawns teammates, manages PR stack (used by /build-feature and /create-project)
- **architect** — Creates plans, defines contracts, knows all coding patterns
- **kotlin-dev** — Implements clients, services, libs per plan
- **web-dev** — Implements React + TypeScript frontend features per plan
- **db-dev** — Implements schemas, migrations, seeds per plan
- **test-engineer** — Creates unit, integration, and acceptance tests
- **code-reviewer** — Reviews implementation PRs against plan + architecture patterns
- **test-reviewer** — Reviews tests for quality, coverage, and real-scenario authenticity
- **doc-updater** — Updates CLAUDE.md, READMEs, and skills from retrospective
