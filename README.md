# Dota2 一号位复盘工具

基于 OpenDota 数据，为一号位玩家生成选人轮次、对线期细节与关键错误建议。

## 技术栈

- **后端**：Java 21 + Spring Boot 3.2 + Maven 多模块
- **前端**：Vue 3 + Element Plus
- **缓存**：SQLite（本地持久化）
- **数据源**：OpenDota API
- **录像解析**：Clarity 3.1.3（DEM 文件解析，可选）

## 目录结构

```
├── pom.xml                      # Maven 父 POM
├── dota2-analyzer-core/         # 模型、服务、分析引擎
├── dota2-analyzer-clarity/      # DEM 下载 + Clarity 解析
├── dota2-analyzer-api/          # Spring Boot REST API
└── frontend/                    # Vue 3 前端
```

## 启动

### 后端

```bash
# 首次需要安装依赖
mvn install -DskipTests

# 启动 API 服务
mvn spring-boot:run -pl dota2-analyzer-api
```

默认端口：`http://localhost:5086`

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`http://localhost:5173`

## 功能

- 仅分析一号位对局（可切换为全部位置）
- 对线期分析：净值差、补刀差、经验差、对线组合、击杀/死亡统计
- BP 轮次分析（第 1/2/3 轮选人）
- 关键错误检测（8 类）与改进建议
- 表现评级（星级评分）
- 物品时间轴（合成后散件自动移除）
- 英雄基准分位对比（OpenDota Benchmarks）
- 本地缓存比赛/英雄/物品数据，减少 API 请求
- 数据预拉取：批量缓存最近对局
- 英雄/物品头像由后端缓存并提供本地访问

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| GET | `/api/players/{id}/recent` | 最近对局列表 |
| POST | `/api/players/{id}/analyze-recent` | 复盘分析 |
| GET | `/api/matches/{matchId}/analyze` | 单场分析 |
| POST | `/api/players/{id}/preload` | 批量预拉取 |
| GET | `/api/players/{id}/preload-status` | 预拉取进度 |
| GET | `/api/players/{id}/cached-matches` | 缓存列表 |
| GET | `/api/assets/heroes/{heroId}` | 英雄头像 |
| GET | `/api/assets/items/{itemKey}` | 物品图标 |

## 说明

- OpenDota 需要时间解析回放，未解析对局会显示提示信息
- 默认只统计一号位对局（`onlyPos1=true`）
- 本地缓存路径：`~/.dota2analyzer/match-cache.db`
