# Dota2 一号位复盘工具

本项目为前后端分离：
- 后端：C# ASP.NET Core Web API（OpenDota 数据源）
- 前端：Vue 3 + Element Plus

## 目录结构
- `src/Dota2Analyzer.Api` 后端 API
- `src/Dota2Analyzer.Core` 核心分析逻辑
- `frontend` 前端应用

## 功能概览
- 仅分析一号位对局（可切换）
- 对线、BP 轮次、表现评估与建议
- 物品时间轴（合成后散件自动移除）
- 本地缓存比赛/英雄/物品常量，降低 OpenDota 请求
- 数据拉取页面：预拉取最近对局并显示缓存状态
- 英雄/物品头像由后端缓存并提供本地静态访问

## 启动后端
```powershell
cd C:\tools\dota2analyzer\src\Dota2Analyzer.Api
dotnet run
```
默认地址：`http://localhost:5086`

## 启动前端
```powershell
cd C:\tools\dota2analyzer\frontend
npm install
npm run dev
```
默认地址：`http://localhost:5173`

## 说明
- OpenDota 需要时间解析回放，未解析对局会显示提示信息。
- 默认只统计你的一号位对局（`onlyPos1=true`）。
- 可在前端切换“只看一号位”。
- 本地缓存路径：`%LocalAppData%\Dota2Analyzer\match-cache.db`

## 关键接口
- `GET /api/players/{accountId}/recent`
- `POST /api/players/{accountId}/analyze-recent`
- `GET /api/matches/{matchId}/analyze`
- `POST /api/players/{accountId}/preload`
- `GET /api/players/{accountId}/preload-status`
- `GET /api/players/{accountId}/cached-matches`
- `GET /api/assets/heroes/{heroId}`
- `GET /api/assets/items/{itemKey}`
