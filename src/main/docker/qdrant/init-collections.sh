#!/bin/sh
set -eu

create_collection() {
  collection_name="$1"
  echo "Ensuring Qdrant collection exists: ${collection_name}"
  curl -fsS -X PUT "${QDRANT_URL}/collections/${collection_name}" \
    -H 'Content-Type: application/json' \
    --data "{\"vectors\":{\"size\":${QDRANT_VECTOR_SIZE},\"distance\":\"${QDRANT_DISTANCE}\"}}"
  echo
}

create_collection "${QDRANT_MEMORY_COLLECTION}"
create_collection "${QDRANT_ROLE_KNOWLEDGE_COLLECTION}"

echo "Qdrant collections are ready."
