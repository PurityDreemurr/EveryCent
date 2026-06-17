# Role Knowledge Dataset

This directory stores role background knowledge that can be embedded and inserted into the role RAG collection.

## `haowei-role-knowledge.jsonl`

Source: `dataset.json`, converted from fine-tuning style instruction/input/output examples into retrieval-oriented knowledge chunks.

Each line is an independent JSON document:

- `id`: stable import id for idempotent ingestion.
- `roleCode`: role profile code, currently `haowei`.
- `title`: short human-readable title.
- `knowledgeType`: one of `BACKGROUND`, `PERSONALITY`, `STYLE`, `BOUNDARY`, `DOMAIN_SKILL`; aligned with the `ai_role_knowledge.knowledge_type` constraint.
- `content`: the text that should be embedded.
- `priority`: suggested ranking priority from 0 to 100.
- `enabled`: whether this chunk should be imported and searchable.
- `tags`: payload tags for filtering/debugging.
- `sourceSampleIds`: 1-based indexes from `dataset.json` used as evidence.
- `retrievalQueries`: example queries that this chunk should match.

Recommended vector payload fields:

```json
{
  "roleCode": "haowei",
  "knowledgeType": "BACKGROUND",
  "title": "皓尾的核心身份",
  "priority": 100,
  "enabled": true,
  "tags": ["身份", "羽龙"],
  "sourceId": "haowei-background-identity"
}
```

Use `content` as the primary embedding text. For better recall, an ingestion job may append `title`, `tags`, and `retrievalQueries` to the embedding input while keeping `content` as the prompt-facing text.
