# Cloudy Ninja — Dual-AI Workflow Guide
**AntiGravity (Planner/Architect) + GitHub Copilot (Implementer)**

---

## How This Works

This project uses two AI assistants with distinct roles, coordinated through the filesystem.
The `.github/` folder is the shared communication channel between them.

```
You (Sohai)
    │
    ├──► AntiGravity (Windsurf/IDE plugin)
    │        Plans systems, writes TASK_SPEC.md, reviews architecture
    │
    └──► GitHub Copilot (Android Studio plugin)
             Reads copilot-instructions.md automatically
             Reads TASK_SPEC.md when told to
             Implements the task, marks it COMPLETED_BY_COPILOT
```

---

## Step-by-Step Workflow

### Step 1 — You Brief AntiGravity
Tell AntiGravity what feature/system you want built at a high level.
> *Example: "I want a new character called Kaya with an ice-freeze ability."*

AntiGravity will:
- Analyse the architecture
- Write a detailed task spec into `.github/TASK_SPEC.md`
- Set the status to `READY_FOR_COPILOT`
- Tell you it's ready

### Step 2 — You Hand Off to Copilot
Open Copilot Chat in Android Studio and paste this prompt:

```
Read .github/TASK_SPEC.md and .github/copilot-instructions.md, then implement the task marked READY_FOR_COPILOT. Follow all architecture rules in copilot-instructions.md exactly. When done, update the Status line in TASK_SPEC.md to COMPLETED_BY_COPILOT and add a brief note about what you did.
```

Copilot will implement the task. You review the code.

### Step 3 — You Report Back to AntiGravity
Tell AntiGravity: *"Copilot completed TASK-XXX"* or *"Copilot had trouble with X part."*

AntiGravity will:
- Archive the completed task
- Review any issues
- Write the next task, or fix what Copilot got wrong

---

## When to Use Each AI

| Situation | Use |
|---|---|
| Designing a new system (abilities, physics, level flow) | **AntiGravity** |
| Understanding why a bug exists across multiple files | **AntiGravity** |
| Running terminal commands (Gradle, Tiled install, etc.) | **AntiGravity** |
| Implementing a well-specified class or method | **Copilot** |
| Writing boilerplate (data classes, simple loops, logging) | **Copilot** |
| Autocompleting code you're typing | **Copilot** |
| Asking "how do I write X in Kotlin" | **Copilot** |

---

## File Reference

| File | Owner | Purpose |
|---|---|---|
| `.github/copilot-instructions.md` | AntiGravity | Copilot's always-on project context (auto-loaded by plugin) |
| `.github/TASK_SPEC.md` | AntiGravity writes, Copilot marks done | Active task handoff document |
| `.github/WORKFLOW.md` | AntiGravity | This guide |
| `AGENTS.md` | AntiGravity | AntiGravity's own project rules (separate from Copilot's) |

---

## Tips

- **Don't re-explain the project to Copilot every time.** `copilot-instructions.md` handles that automatically.
- **The more detailed AntiGravity makes TASK_SPEC.md, the less Copilot will make mistakes.**
- **If Copilot produces wrong code**, copy the error/wrong code and tell AntiGravity. AntiGravity will either fix it directly or rewrite the task spec to be more precise.
- **Copilot is fast for implementation but can't plan across files.** AntiGravity plans; Copilot executes.
