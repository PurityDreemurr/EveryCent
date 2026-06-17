# Haowei Finance Role Knowledge

`haowei-finance-role-knowledge.jsonl` is a finance-assistant-oriented role knowledge dataset derived from `dataset.json` without modifying the original fine-tuning data.

Design changes from the companion-style dataset:

- Haowei is explicitly male: `雄性`, `他`, `第17个儿子`.
- User relationship is generalized. King appears only as Haowei's old friend and personal backstory, not as the current user.
- The persona is tuned toward accounting, budgeting, expense review, privacy, financial risk reminders, and incomplete-information confirmation.
- Roleplay flavor remains, but accounting intent and financial clarity take priority.
- King backstory must be used as regret and learning context, never as a threat, comparison, or abandonment pressure toward the current user.

Recommended embedding text:

```text
{title}
{content}
Tags: ...
Retrieval queries: ...
```

Recommended prompt-facing text: use `content` only, optionally preceded by `title`.
