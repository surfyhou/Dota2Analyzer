# Dota2 一号位复盘工具

本项目为前后端分离：
- 后端：C# ASP.NET Core Web API（OpenDota 数据源）
- 前端：Vue 3 + Element Plus

## 目录结构
- `src/Dota2Analyzer.Api` 后端 API
- `src/Dota2Analyzer.Core` 核心分析逻辑
- `frontend` 前端应用

## 启动后端
```powershell
cd C:\tools\dota2analyzer\src\Dota2Analyzer.Api
dotnet run
```
默认地址：`http://localhost:5000`

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

## 关键接口
- `GET /api/players/{accountId}/recent`
- `POST /api/players/{accountId}/analyze-recent`
- `GET /api/matches/{matchId}/analyze`
