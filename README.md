# AI E-Commerce Platform · Java Fullstack (电商协同管理系统 · Java 版)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.x-blue.svg)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.x-purple.svg)](https://vitejs.dev/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](BEIKESHOP-LICENSE.txt)

基于 **Spring Boot 3 + Java 21 + Vue 3 + TypeScript** 研发的全栈电商管理中台与商城系统（参照 BeikeShop 业务架构体系设计）。支持商户后台全流程管理、多维度实时数据大盘、商品规格属性（SKU）动态配置、订单履约、售后工单（RMA）流转及客户分级折扣策略。

---

## 🌟 核心特性与技术亮点

- **现代 Java 21 技术栈**：基于 JDK 21 LTS 与 Spring Boot 3 构建，全面采用 Java 21 `Record` 特性进行 DTO/VO 领域建模，提升实体不可变性并减少冗余样板代码。
- **双存储模式灵活架构**：
  - **Local-Demo Profile**：内置开箱即用的高性能内存仓储（基于 `ConcurrentHashMap` 与原子引用设计），**无需安装任何外部数据库**即可一键极速拉起，便于开发调试与面试演示。
  - **Production Profile**：支持标准 JDBC / PostgreSQL 持久化与 Flyway 版本化数据库迁移。
- **RBAC 鉴权与安全控制**：基于 Bearer Token 的无状态身份认证，精细化拦截与隔离顾客（Customer）、客服（Agent）及商户（Merchant）角色。
- **财务级高精度计算**：全链路采用 `BigDecimal` 处理商品价格、订单合计、运费门槛与会员等级折扣，杜绝浮点数精度失真。
- **跨域安全互信**：规范化配置 WebMvc CORS 跨域策略，支持多端口动态匹配与安全凭据共享。
- **数据看板与商业指标**：提供 24 小时访问量（PV/UV）时序分析、加购转化漏斗、热销/滞销商品排行榜与多维度渠道分析。

---

## 📂 项目工程结构

```text
ai-ecommerce-platform/
├── zeshop_java/               # Java 后端核心服务 (Spring Boot 3 + Java 21)
│   ├── src/main/java/
│   │   └── com/example/shop/
│   │       ├── ShopApplication.java      # 启动入口与跨域 CORS 配置
│   │       ├── controller/               # RESTful API 控制器 (Commerce / Chat / Knowledge)
│   │       ├── commerce/                 # 核心电商领域服务 (CatalogService / SupportStore)
│   │       ├── security/                 # JWT Token 签发与 RBAC 拦截鉴权
│   │       ├── ai/                       # AI 服务抽象与 Spring AI 扩展点
│   │       └── knowledge/                # 知识库与政策文档服务
│   ├── src/main/resources/
│   │   ├── application.yml               # 默认配置
│   │   └── application-local-demo.yml    # 无数据库轻量演示配置 (端口: 8083)
│   └── pom.xml                           # Maven 依赖定义
│
├── zeshop_web/                # 响应式前端系统 (Vue 3 + Vite + TypeScript)
│   ├── src/
│   │   ├── admin/                        # 商户管理中台 (看板/商品/订单/售后/客户/文章/AI设置)
│   │   ├── pages/                        # 商城客户端 (首页/目录/商品详情/购物车/结账/工单)
│   │   ├── components/                   # 通用交互组件
│   │   ├── router.ts                     # 前端动态路由与守卫
│   │   ├── store.ts                      # 响应式状态管理
│   │   └── api.ts                        # API 请求封装 (默认请求 8083 端口)
│   ├── package.json
│   └── vite.config.ts
├── .gitignore                 # 标准构建忽略规则
└── README.md                  # 项目详细说明文档
```

---

## 🚀 快速启动指南

### 环境依赖
- **Java**：JDK 21 或更高版本
- **Maven**：3.8+
- **Node.js**：18.x 或更高版本

---

### 第一步：启动 Java 后端 (Port: 8083)

本项目提供了免数据库的 **Local-Demo** 模式，无需安装任何数据库即可运行：

```bash
cd zeshop_java
# 使用 local-demo profile 启动（内置初始商品、分类与演示数据）
mvn spring-boot:run "-Dspring-boot.run.profiles=local-demo"
```

> 服务启动成功后，健康检查端点：`http://127.0.0.1:8083/api/health`（返回 `{"status":"ok","backend":"java"}`）

---

### 第二步：启动前端界面 (Port: 5173)

打开新的终端窗口：

```bash
cd zeshop_web

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

启动完成后，在浏览器中访问：`http://localhost:5173`。

---

## 🔑 默认演示账号

| 角色 | 登录账号 (Email) | 密码 | 权限与访问入口 |
| :--- | :--- | :--- | :--- |
| **商户管理员** | `merchant@example.com` | *(留空即可)* | 管理后台全部模块（`/admin`）：商品管理、订单履约、售后审核、数据报表、文章管理 |
| **客服人员** | `agent@example.com` | *(留空即可)* | 售后工单处理（`/admin/tickets`）、客户对话查看 |
| **普通顾客** | `customer@example.com` | *(留空即可)* | 前台商城购物、购物车加购、模拟结账、个人售后工单查看 |

---

## 📋 核心 API 端点概览

### 1. 认证与商品
- `POST /api/auth/login`：商户 / 顾客登录与 Bearer Token 签发
- `GET /api/products`：在售商品分页与多维度查询
- `GET /api/products/{id}`：商品详情查询
- `POST /api/products`：新建商品（商户权限）
- `PUT /api/products/{id}`：修改商品信息与规格
- `DELETE /api/products/{id}`：下架/软删除商品

### 2. 订单与履约
- `POST /api/orders`：客户端购物车一键下单与库存扣减
- `GET /api/orders`：顾客个人历史订单查询
- `GET /api/admin/orders`：商户端全局订单池与状态筛选
- `PATCH /api/admin/orders/{id}`：更新订单支付与物流发货状态
- `DELETE /api/admin/orders/{id}`：订单关闭与废弃

### 3. 售后管理 (RMA)
- `POST /api/support/tickets`：顾客提交退货/换货工单
- `GET /api/support/tickets`：商户/客服售后待办列表
- `PATCH /api/support/tickets/{id}`：审批工单状态与录入处理决议
- `POST /api/support/tickets/{id}/messages`：工单留言与客服回复流转

### 4. 统计与分析
- `GET /api/admin/dashboard/stats`：数据看板核心指标（访客数、加购数、转化率、漏斗、热销/滞销榜）
- `GET /api/admin/reports/sales`：销售额时序走势与客户消费排行

---
