# EveryCent Qdrant 向量数据库运行指南

## 1. 用途

本配置只负责搭建本地 Qdrant 向量数据库，并初始化两个 collection：

- `everycent_ai_memory`：用户长期记忆向量。
- `everycent_ai_role_knowledge`：角色设定知识向量。

MySQL 仍负责保存业务元数据；Qdrant 只保存向量索引和检索 payload。

## 2. 启动

在项目根目录执行：

```bash
docker compose -f src/main/docker/qdrant.yml up -d
```

首次启动会拉起两个服务：

- `everycent-qdrant`：Qdrant 服务，HTTP 端口默认 `6333`，gRPC 端口默认 `6334`。
- `qdrant-init`：一次性初始化容器，负责创建两个 collection，执行完会退出。

查看状态：

```bash
docker compose -f src/main/docker/qdrant.yml ps
```

查看日志：

```bash
docker compose -f src/main/docker/qdrant.yml logs -f qdrant
docker compose -f src/main/docker/qdrant.yml logs qdrant-init
```

## 3. 停止与清理

停止容器但保留向量数据：

```bash
docker compose -f src/main/docker/qdrant.yml down
```

停止并删除向量数据卷：

```bash
docker compose -f src/main/docker/qdrant.yml down -v
```

## 4. 配置项

可通过环境变量覆盖默认值：

```bash
QDRANT_HTTP_PORT=6333
QDRANT_GRPC_PORT=6334
QDRANT_VECTOR_SIZE=1024
QDRANT_DISTANCE=Cosine
QDRANT_MEMORY_COLLECTION=everycent_ai_memory
QDRANT_ROLE_KNOWLEDGE_COLLECTION=everycent_ai_role_knowledge
```

当前文档约定 embedding 维度为 `1024`，距离函数为 `Cosine`。如果以后更换 embedding 模型并改变维度，必须新建 collection 或清空旧 collection 后重建。

## 5. 健康检查

```bash
curl http://127.0.0.1:6333/healthz
curl http://127.0.0.1:6333/collections/everycent_ai_memory
curl http://127.0.0.1:6333/collections/everycent_ai_role_knowledge
```

运行验证测试：

```bash
./mvnw -Dskip.installnodenpm=true -Dskip.npm=true -Dtest=QdrantVectorStoreIT test
```

如果 Qdrant 不在本机默认端口：

```bash
QDRANT_TEST_BASE_URL=http://你的服务器:6333 ./mvnw -Dskip.installnodenpm=true -Dskip.npm=true -Dtest=QdrantVectorStoreIT test
```

## 6. 注意事项

- 两个 collection 不能混用：用户长期记忆写入 `everycent_ai_memory`，角色设定知识写入 `everycent_ai_role_knowledge`。
- 用户长期记忆检索必须带 `userId` filter，避免跨用户召回。
- 角色设定检索必须带 `roleProfileId` 和 `enabled=true` filter，避免召回其他角色或停用知识。
- Qdrant 默认没有开启鉴权；生产环境请放在内网、防火墙后，或按 Qdrant 官方方式启用 API key/TLS。
- 向量数据保存在 Docker volume `everycent-vector_everycent-qdrant-storage` 中，执行 `down -v` 会删除。
- 本次只搭建向量数据库和验证测试，未接入业务聊天、embedding 或 RAG 服务实现。
