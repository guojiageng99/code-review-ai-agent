03 代码审查项目数据库表设计
03 代码审查项目数据库表设计
===============
[来自：
Java突击队](https://wx.zsxq.com/group/28851182188851)
![用户头像](https://images.zsxq.com/Fmi3vQuLu0pbpK9VaWXFQBafZ2nn?e=2127196800&token=q6iZ0sQtf9U7s1qz0r4yMawNq3-u2w6lbnai6y2J:SOQ6CmXVJEmTfl0pb__MJvX2THo=)
苏三
2025年12月05日 19:17
咱们代码审查AI Agent项目，前面已经做了系统设计，在正式开发之前，先做好数据库表设计。  
   
由于咱们规划使用的数据库是PostgreSQL，因此，咱们打算使用PostgreSQL 15+的版本。  
   
这篇文章中的sql语句，都是用PostgreSQL中的语法。
---
1. 数据库概述
--------
### 1.1 数据库命名规范
* **数据库名** : `code_guardian`
* **Schema** : `public`（默认）
* **表命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **字段命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **索引命名** : `idx_表名_字段名` 或 `idx_表名_字段名1_字段名2`（复合索引）
### 1.2 设计原则
* **规范化** : 遵循第三范式（3NF），减少数据冗余
* **性能优化** : 合理使用索引，考虑查询性能
* **可扩展性** : 预留扩展字段，支持未来功能扩展
* **数据完整性** : 使用约束保证数据一致性
* **PostgreSQL特性** : 充分利用JSON、全文检索等特性
**数据库ER图**
![image.png](https://article-images.zsxq.com/Fk-tSthbEWZDXAoHwiua0pjObYOW)![]()
3. 数据表设计
--------
### 3.1 users（用户表）
#### 3.1.1 表说明
存储系统用户的基本信息，包括用户名、邮箱、密码哈希等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 用户ID，自增主键 |
| username | VARCHAR(32) | NOT NULL, UNIQUE | - | 用户名，唯一 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | - | 邮箱地址，唯一（符合RFC 5321标准） |
| password\_hash | CHAR(60) | NOT NULL | - | 密码哈希值（BCrypt固定60字符） |
| real\_name | VARCHAR(64) | - | NULL | 真实姓名 |
| phone | VARCHAR(16) | - | NULL | 手机号码（支持国际格式） |
| avatar\_url | TEXT | - | NULL | 头像URL |
| status | SMALLINT | NOT NULL | 0 | 用户状态：0=ACTIVE, 1=INACTIVE, 2=LOCKED |
| last\_login\_at | TIMESTAMPTZ | - | NULL | 最后登录时间（带时区） |
| last\_login\_ip | INET | - | NULL | 最后登录IP（PostgreSQL原生IP类型） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.1.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户名唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (username)
-- 邮箱唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (email)
-- 状态索引（用于查询特定状态的用户）
CREATE INDEX idx_users_status ON users(status);
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_users_created_at ON users(created_at DESC);
-- 用户名全文检索索引
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
```
#### 3.1.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE users ADD CONSTRAINT chk_users_status 
CHECK (status IN (0, 1, 2));
-- 邮箱格式检查约束（简单验证）
ALTER TABLE users ADD CONSTRAINT chk_users_email 
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
```
#### 3.1.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：ACTIVE(激活)/INACTIVE(未激活)/LOCKED(锁定)';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP地址';
```
### 3.2 roles（角色表）
#### 3.2.1 表说明
存储系统角色信息，如管理员、审查员、查看者等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 角色ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 角色代码，唯一标识（如：ADMIN、REVIEWER、VIEWER） |
| name | VARCHAR(64) | NOT NULL | - | 角色名称（如：管理员、审查员、查看者） |
| description | TEXT | - | NULL | 角色描述 |
| status | SMALLINT | NOT NULL | 0 | 角色状态：0=ACTIVE, 1=INACTIVE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色代码唯一索引（自动创建）
-- UNIQUE (code)
-- 状态索引
CREATE INDEX idx_roles_status ON roles(status);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE roles ADD CONSTRAINT chk_roles_status 
CHECK (status IN (0, 1));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.description IS '角色描述';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
```
### 3.3 permissions（权限表）
#### 3.3.1 表说明
存储系统权限定义，如查询、审查、配置等。
#### 3.3.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 权限ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 权限代码，唯一标识（如：QUERY、REVIEW、CONFIG、ADMIN） |
| name | VARCHAR(64) | NOT NULL | - | 权限名称（如：查询权限、审查权限、配置权限） |
| description | TEXT | - | NULL | 权限描述 |
| resource | SMALLINT | - | NULL | 资源类型：0=TASK, 1=REPORT, 2=CONFIG |
| action | SMALLINT | - | NULL | 操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.3.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 权限代码唯一索引（自动创建）
-- UNIQUE (code)
-- 资源索引（用于按资源查询权限）
CREATE INDEX idx_permissions_resource ON permissions(resource);
```
#### 3.3.4 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
COMMENT ON COLUMN permissions.description IS '权限描述';
COMMENT ON COLUMN permissions.resource IS '资源类型：0=TASK, 1=REPORT, 2=CONFIG';
COMMENT ON COLUMN permissions.action IS '操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE';
```
---
### 3.4 user\_roles（用户角色关联表）
#### 3.4.1 表说明
存储用户和角色的多对多关联关系。
#### 3.4.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| user\_id | BIGINT | NOT NULL, FK | - | 用户ID |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.4.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
-- 角色ID索引
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- 唯一约束索引（一个用户不能重复分配同一个角色）
CREATE UNIQUE INDEX idx_user_roles_unique ON user_roles(user_id, role_id);
```
#### 3.4.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
-- 唯一约束（一个用户不能重复分配同一个角色）
-- UNIQUE (user_id, role_id) -- 已在索引中定义
```
#### 3.4.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.id IS '关联ID，自增主键';
COMMENT ON COLUMN user_roles.user_id IS '用户ID';
COMMENT ON COLUMN user_roles.role_id IS '角色ID';
```
---
### 3.5 role\_permissions（角色权限关联表）
#### 3.5.1 表说明
存储角色和权限的多对多关联关系。
#### 3.5.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| permission\_id | BIGINT | NOT NULL, FK | - | 权限ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.5.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色ID索引
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
-- 权限ID索引
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- 唯一约束索引（一个角色不能重复分配同一个权限）
CREATE UNIQUE INDEX idx_role_permissions_unique ON role_permissions(role_id, permission_id);
```
#### 3.5.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission_id 
FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;
-- 唯一约束（一个角色不能重复分配同一个权限）
-- UNIQUE (role_id, permission_id) -- 已在索引中定义
```
#### 3.5.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE role_permissions IS '角色权限关联表';
COMMENT ON COLUMN role_permissions.id IS '关联ID，自增主键';
COMMENT ON COLUMN role_permissions.role_id IS '角色ID';
COMMENT ON COLUMN role_permissions.permission_id IS '权限ID';
```
### 3.6 review\_tasks（审查任务表）
#### 3.1.1 表说明
存储代码审查任务的基本信息，包括任务状态、审查类型、审查范围等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 任务ID，自增主键 |
| name | VARCHAR(128) | NOT NULL | - | 任务名称 |
| review\_type | SMALLINT | NOT NULL | - | 审查类型：0=PROJECT, 1=DIRECTORY, 2=FILE, 3=SNIPPET, 4=GIT |
| scope | TEXT | - | NULL | 审查范围（文件路径、目录路径或代码片段） |
| status | SMALLINT | NOT NULL | 0 | 任务状态：0=PENDING, 1=RUNNING, 2=COMPLETED, 3=FAILED |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| completed\_at | TIMESTAMPTZ | - | NULL | 完成时间（带时区） |
| error\_message | TEXT | - | NULL | 错误信息（任务失败时） |
| user\_id | BIGINT | NOT NULL, FK | - | 创建用户ID |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.6.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引（用于查询用户创建的任务）
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
-- 审查类型索引（用于按类型查询）
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
-- 状态索引（用于查询特定状态的任务）
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
-- 创建时间索引（用于时间范围查询和排序）
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
-- 复合索引（用于常见查询组合）
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
-- 名称模糊查询索引（使用GIN索引支持全文检索）
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
```
#### 3.6.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_tasks ADD CONSTRAINT fk_review_tasks_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
-- 状态值检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_status 
CHECK (status IN (0, 1, 2, 3));
-- 审查类型检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_type 
CHECK (review_type IN (0, 1, 2, 3, 4));
-- 完成时间检查约束（完成时间必须晚于创建时间）
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_time 
CHECK (completed_at IS NULL OR completed_at >= created_at);
```
#### 3.6.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
```
---
### 3.7 findings（审查发现表）
#### 3.2.1 表说明
存储代码审查发现的问题，包括问题严重程度、位置、描述、修复建议等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 发现ID，自增主键 |
| task\_id | BIGINT | NOT NULL, FK | - | 关联的审查任务ID |
| severity | SMALLINT | NOT NULL | - | 严重程度：0=CRITICAL, 1=HIGH, 2=MEDIUM, 3=LOW |
| title | TEXT | NOT NULL | - | 问题标题 |
| location | TEXT | NOT NULL | - | 问题位置（文件路径或代码位置描述） |
| start\_line | INTEGER | - | NULL | 起始行号 |
| end\_line | INTEGER | - | NULL | 结束行号 |
| description | TEXT | NOT NULL | - | 问题详细描述 |
| suggestion | TEXT | - | NULL | 修复建议 |
| diff | TEXT | - | NULL | 修复代码差异（Diff格式） |
| category | SMALLINT | - | NULL | 问题类别：0=SECURITY, 1=PERFORMANCE, 2=BUG, 3=CODE\_STYLE, 4=MAINTAINABILITY |
| rule\_id | BIGINT | - | NULL | 规则ID（如果来自规则引擎，预留字段） |
| confidence | DECIMAL(3,2) | - | NULL | 置信度（0.00-1.00，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID索引（用于查询特定任务的所有问题）
CREATE INDEX idx_findings_task_id ON findings(task_id);
-- 严重程度索引（用于按严重程度筛选）
CREATE INDEX idx_findings_severity ON findings(severity);
-- 类别索引（用于按类别筛选）
CREATE INDEX idx_findings_category ON findings(category);
-- 复合索引（用于常见查询组合：任务+严重程度）
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
-- 复合索引（用于常见查询组合：任务+类别）
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
-- 标题全文检索索引（使用GIN索引）
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
-- 描述全文检索索引（使用GIN索引，可选，如果描述字段查询频繁）
-- CREATE INDEX idx_findings_description_gin ON findings USING gin(description gin_trgm_ops);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE findings ADD CONSTRAINT fk_findings_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 严重程度检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_severity 
CHECK (severity IN (0, 1, 2, 3));
-- 类别检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_category 
CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4));
-- 行号检查约束（结束行号必须大于等于起始行号）
ALTER TABLE findings ADD CONSTRAINT chk_findings_line 
CHECK ((start_line IS NULL AND end_line IS NULL) OR 
       (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line));
-- 置信度检查约束（0.00-1.00）
ALTER TABLE findings ADD CONSTRAINT chk_findings_confidence 
CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：0=CRITICAL(严重), 1=HIGH(高), 2=MEDIUM(中), 3=LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
COMMENT ON COLUMN findings.rule_id IS '规则ID，如果问题来自规则引擎';
COMMENT ON COLUMN findings.confidence IS '置信度，0.00-1.00';
```
### 3.8 review\_reports（审查报告表）
#### 3.8.1 表说明
存储代码审查报告，包括HTML格式、Markdown格式和统计信息。
#### 3.8.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 报告ID，自增主键 |
| task\_id | BIGINT | NOT NULL, UNIQUE, FK | - | 关联的审查任务ID（一对一关系） |
| html\_content | TEXT | - | NULL | HTML格式报告内容 |
| markdown\_content | TEXT | - | NULL | Markdown格式报告内容 |
| statistics | JSONB | - | NULL | 统计信息（JSON格式） |
| pdf\_path | TEXT | - | NULL | PDF文件路径（如果生成PDF，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.8.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (task_id)
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
-- 统计信息GIN索引（用于JSON查询，如果需要在统计信息中搜索）
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
#### 3.8.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_reports ADD CONSTRAINT fk_review_reports_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 任务ID唯一约束（一个任务只能有一个报告）
-- UNIQUE (task_id) -- 已在字段定义中设置
```
#### 3.8.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
COMMENT ON COLUMN review_reports.pdf_path IS 'PDF文件路径（如果生成PDF文件）';
COMMENT ON COLUMN review_reports.created_at IS '报告创建时间';
COMMENT ON COLUMN review_reports.updated_at IS '报告更新时间';
```
---
4. 权限体系设计
---------
### 4.1 权限类型定义
系统定义了以下权限类型：
| 权限代码 | 权限名称 | 说明 | 资源 | 操作 |
| --- | --- | --- | --- | --- |
| QUERY | 查询权限 | 可以查看审查任务、报告、历史记录等 | 0 (TASK) | 0 (READ) |
| REVIEW | 审查权限 | 可以创建和执行代码审查任务 | 0 (TASK) | 1 (CREATE) |
| CONFIG | 配置权限 | 可以修改系统配置、AI配置等 | 2 (CONFIG) | 2 (UPDATE) |
| ADMIN | 管理员权限 | 拥有所有权限，包括用户管理、角色管理等 | NULL (ALL) | NULL (ALL) |
**注意** : resource和action字段使用SMALLINT类型，值为NULL表示所有资源/操作。
### 4.2 角色定义
系统预定义了以下角色：
| 角色代码 | 角色名称 | 说明 | 包含权限 |
| --- | --- | --- | --- |
| ADMIN | 管理员 | 系统管理员，拥有所有权限 | QUERY, REVIEW, CONFIG, ADMIN |
| REVIEWER | 审查员 | 可以创建和查看审查任务 | QUERY, REVIEW |
| VIEWER | 查看者 | 只能查看审查任务和报告 | QUERY |
### 4.3 权限检查逻辑
权限检查采用RBAC（基于角色的访问控制）模型：
- 1.
  **用户登录** : 验证用户名和密码
- 2.
  **获取角色** : 查询用户关联的所有角色
- 3.
  **获取权限** : 查询角色关联的所有权限
- 4.
  **权限验证** : 检查用户是否拥有执行操作的权限
### 4.4 权限验证示例
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 查询用户的所有权限
SELECT DISTINCT p.code, p.name, p.resource, p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? AND u.status = 0 AND r.status = 0;
-- 检查用户是否有特定权限
SELECT COUNT(*) > 0 AS has_permission
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? 
  AND u.status = 0 
  AND r.status = 0
  AND p.code = ?;
```
---
5. 完整SQL建表语句
------------
### 5.1 创建数据库
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建数据库
CREATE DATABASE code_guardian
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
-- 连接到数据库
\c code_guardian;
-- 启用扩展（用于全文检索）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
```
### 5.2 创建表
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- 1. 创建用户表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash CHAR(60) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(16),
    avatar_url TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT chk_users_status 
        CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_users_email 
        CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);
-- ============================================
-- 2. 创建角色表
-- ============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_roles_status 
        CHECK (status IN (0, 1))
);
-- ============================================
-- 3. 创建权限表
-- ============================================
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    resource SMALLINT,
    action SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ============================================
-- 4. 创建用户角色关联表
-- ============================================
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_roles_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_roles_unique UNIQUE (user_id, role_id)
);
-- ============================================
-- 5. 创建角色权限关联表
-- ============================================
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_role_permissions_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id 
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permissions_unique UNIQUE (role_id, permission_id)
);
-- ============================================
-- 6. 创建审查任务表
-- ============================================
CREATE TABLE review_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    review_type SMALLINT NOT NULL,
    scope TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT fk_review_tasks_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_review_tasks_status 
        CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_review_tasks_type 
        CHECK (review_type IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_review_tasks_time 
        CHECK (completed_at IS NULL OR completed_at >= created_at)
);
-- ============================================
-- 2. 创建审查发现表
-- ============================================
CREATE TABLE findings (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    severity SMALLINT NOT NULL,
    title TEXT NOT NULL,
    location TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    description TEXT NOT NULL,
    suggestion TEXT,
    diff TEXT,
    category SMALLINT,
    rule_id BIGINT,
    confidence DECIMAL(3,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_findings_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE,
    
    -- 检查约束
    CONSTRAINT chk_findings_severity 
        CHECK (severity IN (0, 1, 2, 3)),
    CONSTRAINT chk_findings_category 
        CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_findings_line 
        CHECK ((start_line IS NULL AND end_line IS NULL) OR 
               (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line)),
    CONSTRAINT chk_findings_confidence 
        CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00))
);
-- ============================================
-- 3. 创建审查报告表
-- ============================================
CREATE TABLE review_reports (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    html_content TEXT,
    markdown_content TEXT,
    statistics JSONB,
    pdf_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_review_reports_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE
);
```
### 5.3 创建索引
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- users 表索引
-- ============================================
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
-- ============================================
-- roles 表索引
-- ============================================
CREATE INDEX idx_roles_status ON roles(status);
-- ============================================
-- permissions 表索引
-- ============================================
CREATE INDEX idx_permissions_resource ON permissions(resource);
-- ============================================
-- user_roles 表索引
-- ============================================
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- ============================================
-- role_permissions 表索引
-- ============================================
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- ============================================
-- review_tasks 表索引
-- ============================================
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
CREATE INDEX idx_review_tasks_metadata_gin ON review_tasks USING gin(metadata);
-- ============================================
-- findings 表索引
-- ============================================
CREATE INDEX idx_findings_task_id ON findings(task_id);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_category ON findings(category);
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
CREATE INDEX idx_findings_created_at ON findings(created_at DESC);
-- ============================================
-- review_reports 表索引
-- ============================================
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
### 5.4 添加表注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- users 表注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：0=ACTIVE(激活), 1=INACTIVE(未激活), 2=LOCKED(锁定)';
-- roles 表注释
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
-- permissions 表注释
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
-- user_roles 表注释
COMMENT ON TABLE user_roles IS '用户角色关联表';
-- role_permissions 表注释
COMMENT ON TABLE role_permissions IS '角色权限关联表';
-- review_tasks 表注释
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
-- findings 表注释
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：CRITICAL(严重)/HIGH(高)/MEDIUM(中)/LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
-- review_reports 表注释
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
```
6. 初始化数据
--------
### 6.1 初始化角色数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义角色
INSERT INTO roles (code, name, description) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('REVIEWER', '审查员', '可以创建和查看审查任务'),
('VIEWER', '查看者', '只能查看审查任务和报告');
```
### 6.2 初始化权限数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义权限
-- resource: 0=TASK, 1=REPORT, 2=CONFIG
-- action: 0=READ, 1=CREATE, 2=UPDATE, 3=DELETE
INSERT INTO permissions (code, name, description, resource, action) VALUES
('QUERY', '查询权限', '可以查看审查任务、报告、历史记录等', 0, 0),
('REVIEW', '审查权限', '可以创建和执行代码审查任务', 0, 1),
('CONFIG', '配置权限', '可以修改系统配置、AI配置等', 2, 2),
('ADMIN', '管理员权限', '拥有所有权限，包括用户管理、角色管理等', NULL, NULL);
```
### 6.3 初始化角色权限关联
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 管理员角色拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN';
-- 审查员角色拥有查询和审查权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'REVIEWER' AND p.code IN ('QUERY', 'REVIEW');
-- 查看者角色只拥有查询权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code = 'QUERY';
```
### 6.4 创建默认管理员用户
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建默认管理员用户（密码：admin123，实际使用时需要修改）
-- 密码哈希值使用BCrypt加密，示例：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, email, password_hash, real_name, status) VALUES
('admin', 'admin@codeguardian.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE');
-- 为管理员用户分配管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```
---
7. 触发器设计
--------
### 5.1 自动更新时间戳触发器
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建更新时间戳的函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- 为 review_tasks 表创建触发器
CREATE TRIGGER trigger_review_tasks_updated_at
    BEFORE UPDATE ON review_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
-- 为 review_reports 表创建触发器
CREATE TRIGGER trigger_review_reports_updated_at
    BEFORE UPDATE ON review_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```
---
8. 视图设计
-------
### 8.1 用户权限视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建用户权限视图（用于快速查询用户的所有权限）
CREATE OR REPLACE VIEW v_user_permissions AS
SELECT 
    u.id AS user_id,
    u.username,
    u.email,
    r.id AS role_id,
    r.code AS role_code,
    r.name AS role_name,
    p.id AS permission_id,
    p.code AS permission_code,
    p.name AS permission_name,
    p.resource,
    p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.status = 0 AND r.status = 0;
COMMENT ON VIEW v_user_permissions IS '用户权限视图，包含用户的所有角色和权限信息';
```
### 8.2 任务统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建任务统计视图（包含问题统计）
CREATE OR REPLACE VIEW v_task_statistics AS
SELECT 
    t.id AS task_id,
    t.name AS task_name,
    t.review_type,
    t.status,
    t.created_at,
    t.completed_at,
    COUNT(f.id) AS total_findings,
    COUNT(CASE WHEN f.severity = 0 THEN 1 END) AS critical_count,
    COUNT(CASE WHEN f.severity = 1 THEN 1 END) AS high_count,
    COUNT(CASE WHEN f.severity = 2 THEN 1 END) AS medium_count,
    COUNT(CASE WHEN f.severity = 3 THEN 1 END) AS low_count,
    CASE 
        WHEN t.completed_at IS NOT NULL AND t.created_at IS NOT NULL 
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))
        ELSE NULL 
    END AS duration_seconds
FROM review_tasks t
LEFT JOIN findings f ON t.id = f.task_id
GROUP BY t.id, t.name, t.review_type, t.status, t.created_at, t.completed_at;
COMMENT ON VIEW v_task_statistics IS '任务统计视图，包含每个任务的问题统计信息';
```
### 8.3 问题分类统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建问题分类统计视图
CREATE OR REPLACE VIEW v_finding_category_statistics AS
SELECT 
    category,
    severity,
    COUNT(*) AS count,
    ROUND(AVG(confidence), 2) AS avg_confidence
FROM findings
WHERE category IS NOT NULL
GROUP BY category, severity
ORDER BY category, severity;
COMMENT ON VIEW v_finding_category_statistics IS '问题分类统计视图，按类别和严重程度统计';
```
***##9.字段类型优化说明###9.1优化原则本数据库设计遵循一线大厂的表设计标准，主要优化原则如下：1.** 节省存储空间*\*:根据实际需求选择合适长度的字段类型2.\*\* 提高查询性能\*\*:使用固定长度字段（CHAR）替代可变长度字段（VARCHAR）用于短字符串3.\*\* 时区处理\*\*:使用TIMESTAMPTZ确保时间戳带时区信息，避免时区问题4.\*\* 类型精确性\*\*: 使用PostgreSQL原生类型（如INET）提高数据准确性和查询效率
### 9.2 主要优化项
#### 14.2.1 时间类型优化
* **优化前** : `TIMESTAMP`（不带时区）
* **优化后** : `TIMESTAMPTZ`（带时区）
* **原因** :
  + 符合一线大厂标准，避免时区转换问题
  + 支持多时区应用场景
  + 提高时间查询的准确性
#### 14.2.2 字符串类型优化
| 字段类型 | 优化前 | 优化后 | 原因 |
| --- | --- | --- | --- |
| 用户名 | VARCHAR(50) | VARCHAR(32) | 用户名通常较短，32字符足够 |
| 邮箱 | VARCHAR(100) | VARCHAR(255) | 符合RFC 5321标准，最长255字符 |
| 密码哈希 | VARCHAR(255) | CHAR(60) | BCrypt固定60字符，使用CHAR节省空间 |
| 状态码 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 审查类型 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 严重程度 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 问题类别 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 资源类型 | VARCHAR(32) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 操作类型 | VARCHAR(16) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 规则ID | VARCHAR(64) | BIGINT | 规则ID应为数字类型，支持外键关联 |
| 代码字段 | VARCHAR(50) | VARCHAR(32) | 代码通常较短，32字符足够 |
| 真实姓名 | VARCHAR(100) | VARCHAR(64) | 根据实际需求优化 |
| 手机号 | VARCHAR(20) | VARCHAR(16) | 国际格式最长15位 |
| URL/路径 | VARCHAR(500) | TEXT | 路径可能很长，TEXT更灵活 |
| 标题/位置 | VARCHAR(500/1000) | TEXT | 不确定长度，TEXT更合适 |
#### 9.2.3 特殊类型优化
* **IP地址** : `VARCHAR(50)` → `INET`
  + 使用PostgreSQL原生IP类型
  + 支持IPv4和IPv6
  + 提供IP地址验证和查询优化
* **密码哈希** : `VARCHAR(255)` → `CHAR(60)`
  + BCrypt算法固定输出60字符
  + CHAR类型固定长度，节省空间
  + 提高索引效率
* **枚举字段** : `VARCHAR/CHAR` → `SMALLINT`
  + status、review\_type、severity、category等枚举字段使用SMALLINT
  + SMALLINT仅占用2字节，比字符串类型节省大量空间
  + 数字比较比字符串比较更高效
  + 索引效率更高，查询性能更好
#### 9.2.4 空间节省估算
以users表为例（假设100万用户）：
* 密码哈希：从255字节降至60字节，节省约195字节/行
* 状态字段：从20字节降至10字节，节省约10字节/行
* IP地址：从50字节降至约16字节（INET），节省约34字节/行
* **总计每行节省约239字节，100万用户可节省约228MB存储空间**
### 9.3 性能提升
- 1.
  **索引效率** : CHAR类型和固定长度字段的索引更高效
- 2.
  **查询性能** : INET类型支持IP地址范围查询和网络操作
- 3.
  **时区处理** : TIMESTAMPTZ避免应用层时区转换，提高查询准确性
---
10. 附录
------
### 10.1 数据类型说明
* **BIGSERIAL** : 自增长整型，范围 -9223372036854775808 到 9223372036854775807
* **VARCHAR(n)** : 可变长度字符串，最大n个字符，根据实际需求选择合适长度
* **CHAR(n)** : 固定长度字符串，用于固定长度的字段（如状态码、密码哈希）
* **TEXT** : 可变长度字符串，无长度限制，用于不确定长度的文本内容
* **TIMESTAMPTZ** : 带时区的时间戳，精度到微秒（推荐使用，符合一线大厂标准）
* **INET** : PostgreSQL原生IP地址类型，支持IPv4和IPv6
* **JSONB** : 二进制JSON格式，支持索引和查询
* **DECIMAL(p,s)** : 精确数值类型，p为精度，s为小数位数
### 10.2 PostgreSQL扩展
* **pg\_trgm** : 用于全文检索的trigram扩展
* **btree\_gin** : 用于GIN索引的B-tree操作符类
### 10.3 枚举值映射表
#### 10.3.1 用户状态（users.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
| 2 | LOCKED - 锁定 |
#### 10.3.2 角色状态（roles.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
#### 10.3.3 任务状态（review\_tasks.status）
| 值 | 说明 |
| --- | --- |
| 0 | PENDING - 待处理 |
| 1 | RUNNING - 运行中 |
| 2 | COMPLETED - 已完成 |
| 3 | FAILED - 失败 |
#### 10.3.4 审查类型（review\_tasks.review\_type）
| 值 | 说明 |
| --- | --- |
| 0 | PROJECT - 项目 |
| 1 | DIRECTORY - 目录 |
| 2 | FILE - 文件 |
| 3 | SNIPPET - 代码片段 |
| 4 | GIT - Git项目 |
#### 10.3.5 严重程度（findings.severity）
| 值 | 说明 |
| --- | --- |
| 0 | CRITICAL - 严重 |
| 1 | HIGH - 高 |
| 2 | MEDIUM - 中 |
| 3 | LOW - 低 |
#### 10.3.6 问题类别（findings.category）
| 值 | 说明 |
| --- | --- |
| 0 | SECURITY - 安全 |
| 1 | PERFORMANCE - 性能 |
| 2 | BUG - 缺陷 |
| 3 | CODE\_STYLE - 代码风格 |
| 4 | MAINTAINABILITY - 可维护性 |
#### 10.3.7 资源类型（permissions.resource）
| 值 | 说明 |
| --- | --- |
| 0 | TASK - 任务 |
| 1 | REPORT - 报告 |
| 2 | CONFIG - 配置 |
#### 10.3.8 操作类型（permissions.action）
| 值 | 说明 |
| --- | --- |
| 0 | READ - 读取 |
| 1 | CREATE - 创建 |
| 2 | UPDATE - 更新 |
| 3 | DELETE - 删除 |
### 15.4 参考文档
* [PostgreSQL官方文档](https://www.postgresql.org/docs/)
* [PostgreSQL索引类型](https://www.postgresql.org/docs/current/indexes-types.html)
* [PostgreSQL JSON操作](https://www.postgresql.org/docs/current/functions-json.html)
---
咱们代码审查AI Agent项目，前面已经做了系统设计，在正式开发之前，先做好数据库表设计。  
   
由于咱们规划使用的数据库是PostgreSQL，因此，咱们打算使用PostgreSQL 15+的版本。  
   
这篇文章中的sql语句，都是用PostgreSQL中的语法。
---
1. 数据库概述
--------
### 1.1 数据库命名规范
* **数据库名** : `code_guardian`
* **Schema** : `public`（默认）
* **表命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **字段命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **索引命名** : `idx_表名_字段名` 或 `idx_表名_字段名1_字段名2`（复合索引）
### 1.2 设计原则
* **规范化** : 遵循第三范式（3NF），减少数据冗余
* **性能优化** : 合理使用索引，考虑查询性能
* **可扩展性** : 预留扩展字段，支持未来功能扩展
* **数据完整性** : 使用约束保证数据一致性
* **PostgreSQL特性** : 充分利用JSON、全文检索等特性
**数据库ER图**
![image.png](https://article-images.zsxq.com/Fk-tSthbEWZDXAoHwiua0pjObYOW)![]()
3. 数据表设计
--------
### 3.1 users（用户表）
#### 3.1.1 表说明
存储系统用户的基本信息，包括用户名、邮箱、密码哈希等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 用户ID，自增主键 |
| username | VARCHAR(32) | NOT NULL, UNIQUE | - | 用户名，唯一 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | - | 邮箱地址，唯一（符合RFC 5321标准） |
| password\_hash | CHAR(60) | NOT NULL | - | 密码哈希值（BCrypt固定60字符） |
| real\_name | VARCHAR(64) | - | NULL | 真实姓名 |
| phone | VARCHAR(16) | - | NULL | 手机号码（支持国际格式） |
| avatar\_url | TEXT | - | NULL | 头像URL |
| status | SMALLINT | NOT NULL | 0 | 用户状态：0=ACTIVE, 1=INACTIVE, 2=LOCKED |
| last\_login\_at | TIMESTAMPTZ | - | NULL | 最后登录时间（带时区） |
| last\_login\_ip | INET | - | NULL | 最后登录IP（PostgreSQL原生IP类型） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.1.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户名唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (username)
-- 邮箱唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (email)
-- 状态索引（用于查询特定状态的用户）
CREATE INDEX idx_users_status ON users(status);
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_users_created_at ON users(created_at DESC);
-- 用户名全文检索索引
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
```
#### 3.1.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE users ADD CONSTRAINT chk_users_status 
CHECK (status IN (0, 1, 2));
-- 邮箱格式检查约束（简单验证）
ALTER TABLE users ADD CONSTRAINT chk_users_email 
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
```
#### 3.1.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：ACTIVE(激活)/INACTIVE(未激活)/LOCKED(锁定)';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP地址';
```
### 3.2 roles（角色表）
#### 3.2.1 表说明
存储系统角色信息，如管理员、审查员、查看者等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 角色ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 角色代码，唯一标识（如：ADMIN、REVIEWER、VIEWER） |
| name | VARCHAR(64) | NOT NULL | - | 角色名称（如：管理员、审查员、查看者） |
| description | TEXT | - | NULL | 角色描述 |
| status | SMALLINT | NOT NULL | 0 | 角色状态：0=ACTIVE, 1=INACTIVE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色代码唯一索引（自动创建）
-- UNIQUE (code)
-- 状态索引
CREATE INDEX idx_roles_status ON roles(status);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE roles ADD CONSTRAINT chk_roles_status 
CHECK (status IN (0, 1));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.description IS '角色描述';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
```
### 3.3 permissions（权限表）
#### 3.3.1 表说明
存储系统权限定义，如查询、审查、配置等。
#### 3.3.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 权限ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 权限代码，唯一标识（如：QUERY、REVIEW、CONFIG、ADMIN） |
| name | VARCHAR(64) | NOT NULL | - | 权限名称（如：查询权限、审查权限、配置权限） |
| description | TEXT | - | NULL | 权限描述 |
| resource | SMALLINT | - | NULL | 资源类型：0=TASK, 1=REPORT, 2=CONFIG |
| action | SMALLINT | - | NULL | 操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.3.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 权限代码唯一索引（自动创建）
-- UNIQUE (code)
-- 资源索引（用于按资源查询权限）
CREATE INDEX idx_permissions_resource ON permissions(resource);
```
#### 3.3.4 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
COMMENT ON COLUMN permissions.description IS '权限描述';
COMMENT ON COLUMN permissions.resource IS '资源类型：0=TASK, 1=REPORT, 2=CONFIG';
COMMENT ON COLUMN permissions.action IS '操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE';
```
---
### 3.4 user\_roles（用户角色关联表）
#### 3.4.1 表说明
存储用户和角色的多对多关联关系。
#### 3.4.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| user\_id | BIGINT | NOT NULL, FK | - | 用户ID |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.4.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
-- 角色ID索引
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- 唯一约束索引（一个用户不能重复分配同一个角色）
CREATE UNIQUE INDEX idx_user_roles_unique ON user_roles(user_id, role_id);
```
#### 3.4.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
-- 唯一约束（一个用户不能重复分配同一个角色）
-- UNIQUE (user_id, role_id) -- 已在索引中定义
```
#### 3.4.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.id IS '关联ID，自增主键';
COMMENT ON COLUMN user_roles.user_id IS '用户ID';
COMMENT ON COLUMN user_roles.role_id IS '角色ID';
```
---
### 3.5 role\_permissions（角色权限关联表）
#### 3.5.1 表说明
存储角色和权限的多对多关联关系。
#### 3.5.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| permission\_id | BIGINT | NOT NULL, FK | - | 权限ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.5.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色ID索引
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
-- 权限ID索引
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- 唯一约束索引（一个角色不能重复分配同一个权限）
CREATE UNIQUE INDEX idx_role_permissions_unique ON role_permissions(role_id, permission_id);
```
#### 3.5.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission_id 
FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;
-- 唯一约束（一个角色不能重复分配同一个权限）
-- UNIQUE (role_id, permission_id) -- 已在索引中定义
```
#### 3.5.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE role_permissions IS '角色权限关联表';
COMMENT ON COLUMN role_permissions.id IS '关联ID，自增主键';
COMMENT ON COLUMN role_permissions.role_id IS '角色ID';
COMMENT ON COLUMN role_permissions.permission_id IS '权限ID';
```
### 3.6 review\_tasks（审查任务表）
#### 3.1.1 表说明
存储代码审查任务的基本信息，包括任务状态、审查类型、审查范围等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 任务ID，自增主键 |
| name | VARCHAR(128) | NOT NULL | - | 任务名称 |
| review\_type | SMALLINT | NOT NULL | - | 审查类型：0=PROJECT, 1=DIRECTORY, 2=FILE, 3=SNIPPET, 4=GIT |
| scope | TEXT | - | NULL | 审查范围（文件路径、目录路径或代码片段） |
| status | SMALLINT | NOT NULL | 0 | 任务状态：0=PENDING, 1=RUNNING, 2=COMPLETED, 3=FAILED |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| completed\_at | TIMESTAMPTZ | - | NULL | 完成时间（带时区） |
| error\_message | TEXT | - | NULL | 错误信息（任务失败时） |
| user\_id | BIGINT | NOT NULL, FK | - | 创建用户ID |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.6.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引（用于查询用户创建的任务）
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
-- 审查类型索引（用于按类型查询）
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
-- 状态索引（用于查询特定状态的任务）
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
-- 创建时间索引（用于时间范围查询和排序）
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
-- 复合索引（用于常见查询组合）
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
-- 名称模糊查询索引（使用GIN索引支持全文检索）
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
```
#### 3.6.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_tasks ADD CONSTRAINT fk_review_tasks_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
-- 状态值检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_status 
CHECK (status IN (0, 1, 2, 3));
-- 审查类型检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_type 
CHECK (review_type IN (0, 1, 2, 3, 4));
-- 完成时间检查约束（完成时间必须晚于创建时间）
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_time 
CHECK (completed_at IS NULL OR completed_at >= created_at);
```
#### 3.6.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
```
---
### 3.7 findings（审查发现表）
#### 3.2.1 表说明
存储代码审查发现的问题，包括问题严重程度、位置、描述、修复建议等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 发现ID，自增主键 |
| task\_id | BIGINT | NOT NULL, FK | - | 关联的审查任务ID |
| severity | SMALLINT | NOT NULL | - | 严重程度：0=CRITICAL, 1=HIGH, 2=MEDIUM, 3=LOW |
| title | TEXT | NOT NULL | - | 问题标题 |
| location | TEXT | NOT NULL | - | 问题位置（文件路径或代码位置描述） |
| start\_line | INTEGER | - | NULL | 起始行号 |
| end\_line | INTEGER | - | NULL | 结束行号 |
| description | TEXT | NOT NULL | - | 问题详细描述 |
| suggestion | TEXT | - | NULL | 修复建议 |
| diff | TEXT | - | NULL | 修复代码差异（Diff格式） |
| category | SMALLINT | - | NULL | 问题类别：0=SECURITY, 1=PERFORMANCE, 2=BUG, 3=CODE\_STYLE, 4=MAINTAINABILITY |
| rule\_id | BIGINT | - | NULL | 规则ID（如果来自规则引擎，预留字段） |
| confidence | DECIMAL(3,2) | - | NULL | 置信度（0.00-1.00，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID索引（用于查询特定任务的所有问题）
CREATE INDEX idx_findings_task_id ON findings(task_id);
-- 严重程度索引（用于按严重程度筛选）
CREATE INDEX idx_findings_severity ON findings(severity);
-- 类别索引（用于按类别筛选）
CREATE INDEX idx_findings_category ON findings(category);
-- 复合索引（用于常见查询组合：任务+严重程度）
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
-- 复合索引（用于常见查询组合：任务+类别）
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
-- 标题全文检索索引（使用GIN索引）
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
-- 描述全文检索索引（使用GIN索引，可选，如果描述字段查询频繁）
-- CREATE INDEX idx_findings_description_gin ON findings USING gin(description gin_trgm_ops);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE findings ADD CONSTRAINT fk_findings_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 严重程度检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_severity 
CHECK (severity IN (0, 1, 2, 3));
-- 类别检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_category 
CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4));
-- 行号检查约束（结束行号必须大于等于起始行号）
ALTER TABLE findings ADD CONSTRAINT chk_findings_line 
CHECK ((start_line IS NULL AND end_line IS NULL) OR 
       (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line));
-- 置信度检查约束（0.00-1.00）
ALTER TABLE findings ADD CONSTRAINT chk_findings_confidence 
CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：0=CRITICAL(严重), 1=HIGH(高), 2=MEDIUM(中), 3=LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
COMMENT ON COLUMN findings.rule_id IS '规则ID，如果问题来自规则引擎';
COMMENT ON COLUMN findings.confidence IS '置信度，0.00-1.00';
```
### 3.8 review\_reports（审查报告表）
#### 3.8.1 表说明
存储代码审查报告，包括HTML格式、Markdown格式和统计信息。
#### 3.8.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 报告ID，自增主键 |
| task\_id | BIGINT | NOT NULL, UNIQUE, FK | - | 关联的审查任务ID（一对一关系） |
| html\_content | TEXT | - | NULL | HTML格式报告内容 |
| markdown\_content | TEXT | - | NULL | Markdown格式报告内容 |
| statistics | JSONB | - | NULL | 统计信息（JSON格式） |
| pdf\_path | TEXT | - | NULL | PDF文件路径（如果生成PDF，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.8.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (task_id)
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
-- 统计信息GIN索引（用于JSON查询，如果需要在统计信息中搜索）
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
#### 3.8.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_reports ADD CONSTRAINT fk_review_reports_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 任务ID唯一约束（一个任务只能有一个报告）
-- UNIQUE (task_id) -- 已在字段定义中设置
```
#### 3.8.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
COMMENT ON COLUMN review_reports.pdf_path IS 'PDF文件路径（如果生成PDF文件）';
COMMENT ON COLUMN review_reports.created_at IS '报告创建时间';
COMMENT ON COLUMN review_reports.updated_at IS '报告更新时间';
```
---
4. 权限体系设计
---------
### 4.1 权限类型定义
系统定义了以下权限类型：
| 权限代码 | 权限名称 | 说明 | 资源 | 操作 |
| --- | --- | --- | --- | --- |
| QUERY | 查询权限 | 可以查看审查任务、报告、历史记录等 | 0 (TASK) | 0 (READ) |
| REVIEW | 审查权限 | 可以创建和执行代码审查任务 | 0 (TASK) | 1 (CREATE) |
| CONFIG | 配置权限 | 可以修改系统配置、AI配置等 | 2 (CONFIG) | 2 (UPDATE) |
| ADMIN | 管理员权限 | 拥有所有权限，包括用户管理、角色管理等 | NULL (ALL) | NULL (ALL) |
**注意** : resource和action字段使用SMALLINT类型，值为NULL表示所有资源/操作。
### 4.2 角色定义
系统预定义了以下角色：
| 角色代码 | 角色名称 | 说明 | 包含权限 |
| --- | --- | --- | --- |
| ADMIN | 管理员 | 系统管理员，拥有所有权限 | QUERY, REVIEW, CONFIG, ADMIN |
| REVIEWER | 审查员 | 可以创建和查看审查任务 | QUERY, REVIEW |
| VIEWER | 查看者 | 只能查看审查任务和报告 | QUERY |
### 4.3 权限检查逻辑
权限检查采用RBAC（基于角色的访问控制）模型：
- 1.
  **用户登录** : 验证用户名和密码
- 2.
  **获取角色** : 查询用户关联的所有角色
- 3.
  **获取权限** : 查询角色关联的所有权限
- 4.
  **权限验证** : 检查用户是否拥有执行操作的权限
### 4.4 权限验证示例
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 查询用户的所有权限
SELECT DISTINCT p.code, p.name, p.resource, p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? AND u.status = 0 AND r.status = 0;
-- 检查用户是否有特定权限
SELECT COUNT(*) > 0 AS has_permission
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? 
  AND u.status = 0 
  AND r.status = 0
  AND p.code = ?;
```
---
5. 完整SQL建表语句
------------
### 5.1 创建数据库
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建数据库
CREATE DATABASE code_guardian
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
-- 连接到数据库
\c code_guardian;
-- 启用扩展（用于全文检索）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
```
### 5.2 创建表
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- 1. 创建用户表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash CHAR(60) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(16),
    avatar_url TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT chk_users_status 
        CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_users_email 
        CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);
-- ============================================
-- 2. 创建角色表
-- ============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_roles_status 
        CHECK (status IN (0, 1))
);
-- ============================================
-- 3. 创建权限表
-- ============================================
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    resource SMALLINT,
    action SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ============================================
-- 4. 创建用户角色关联表
-- ============================================
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_roles_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_roles_unique UNIQUE (user_id, role_id)
);
-- ============================================
-- 5. 创建角色权限关联表
-- ============================================
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_role_permissions_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id 
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permissions_unique UNIQUE (role_id, permission_id)
);
-- ============================================
-- 6. 创建审查任务表
-- ============================================
CREATE TABLE review_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    review_type SMALLINT NOT NULL,
    scope TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT fk_review_tasks_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_review_tasks_status 
        CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_review_tasks_type 
        CHECK (review_type IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_review_tasks_time 
        CHECK (completed_at IS NULL OR completed_at >= created_at)
);
-- ============================================
-- 2. 创建审查发现表
-- ============================================
CREATE TABLE findings (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    severity SMALLINT NOT NULL,
    title TEXT NOT NULL,
    location TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    description TEXT NOT NULL,
    suggestion TEXT,
    diff TEXT,
    category SMALLINT,
    rule_id BIGINT,
    confidence DECIMAL(3,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_findings_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE,
    
    -- 检查约束
    CONSTRAINT chk_findings_severity 
        CHECK (severity IN (0, 1, 2, 3)),
    CONSTRAINT chk_findings_category 
        CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_findings_line 
        CHECK ((start_line IS NULL AND end_line IS NULL) OR 
               (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line)),
    CONSTRAINT chk_findings_confidence 
        CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00))
);
-- ============================================
-- 3. 创建审查报告表
-- ============================================
CREATE TABLE review_reports (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    html_content TEXT,
    markdown_content TEXT,
    statistics JSONB,
    pdf_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_review_reports_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE
);
```
### 5.3 创建索引
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- users 表索引
-- ============================================
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
-- ============================================
-- roles 表索引
-- ============================================
CREATE INDEX idx_roles_status ON roles(status);
-- ============================================
-- permissions 表索引
-- ============================================
CREATE INDEX idx_permissions_resource ON permissions(resource);
-- ============================================
-- user_roles 表索引
-- ============================================
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- ============================================
-- role_permissions 表索引
-- ============================================
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- ============================================
-- review_tasks 表索引
-- ============================================
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
CREATE INDEX idx_review_tasks_metadata_gin ON review_tasks USING gin(metadata);
-- ============================================
-- findings 表索引
-- ============================================
CREATE INDEX idx_findings_task_id ON findings(task_id);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_category ON findings(category);
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
CREATE INDEX idx_findings_created_at ON findings(created_at DESC);
-- ============================================
-- review_reports 表索引
-- ============================================
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
### 5.4 添加表注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- users 表注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：0=ACTIVE(激活), 1=INACTIVE(未激活), 2=LOCKED(锁定)';
-- roles 表注释
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
-- permissions 表注释
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
-- user_roles 表注释
COMMENT ON TABLE user_roles IS '用户角色关联表';
-- role_permissions 表注释
COMMENT ON TABLE role_permissions IS '角色权限关联表';
-- review_tasks 表注释
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
-- findings 表注释
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：CRITICAL(严重)/HIGH(高)/MEDIUM(中)/LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
-- review_reports 表注释
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
```
6. 初始化数据
--------
### 6.1 初始化角色数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义角色
INSERT INTO roles (code, name, description) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('REVIEWER', '审查员', '可以创建和查看审查任务'),
('VIEWER', '查看者', '只能查看审查任务和报告');
```
### 6.2 初始化权限数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义权限
-- resource: 0=TASK, 1=REPORT, 2=CONFIG
-- action: 0=READ, 1=CREATE, 2=UPDATE, 3=DELETE
INSERT INTO permissions (code, name, description, resource, action) VALUES
('QUERY', '查询权限', '可以查看审查任务、报告、历史记录等', 0, 0),
('REVIEW', '审查权限', '可以创建和执行代码审查任务', 0, 1),
('CONFIG', '配置权限', '可以修改系统配置、AI配置等', 2, 2),
('ADMIN', '管理员权限', '拥有所有权限，包括用户管理、角色管理等', NULL, NULL);
```
### 6.3 初始化角色权限关联
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 管理员角色拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN';
-- 审查员角色拥有查询和审查权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'REVIEWER' AND p.code IN ('QUERY', 'REVIEW');
-- 查看者角色只拥有查询权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code = 'QUERY';
```
### 6.4 创建默认管理员用户
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建默认管理员用户（密码：admin123，实际使用时需要修改）
-- 密码哈希值使用BCrypt加密，示例：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, email, password_hash, real_name, status) VALUES
('admin', 'admin@codeguardian.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE');
-- 为管理员用户分配管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```
---
7. 触发器设计
--------
### 5.1 自动更新时间戳触发器
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建更新时间戳的函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- 为 review_tasks 表创建触发器
CREATE TRIGGER trigger_review_tasks_updated_at
    BEFORE UPDATE ON review_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
-- 为 review_reports 表创建触发器
CREATE TRIGGER trigger_review_reports_updated_at
    BEFORE UPDATE ON review_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```
---
8. 视图设计
-------
### 8.1 用户权限视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建用户权限视图（用于快速查询用户的所有权限）
CREATE OR REPLACE VIEW v_user_permissions AS
SELECT 
    u.id AS user_id,
    u.username,
    u.email,
    r.id AS role_id,
    r.code AS role_code,
    r.name AS role_name,
    p.id AS permission_id,
    p.code AS permission_code,
    p.name AS permission_name,
    p.resource,
    p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.status = 0 AND r.status = 0;
COMMENT ON VIEW v_user_permissions IS '用户权限视图，包含用户的所有角色和权限信息';
```
### 8.2 任务统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建任务统计视图（包含问题统计）
CREATE OR REPLACE VIEW v_task_statistics AS
SELECT 
    t.id AS task_id,
    t.name AS task_name,
    t.review_type,
    t.status,
    t.created_at,
    t.completed_at,
    COUNT(f.id) AS total_findings,
    COUNT(CASE WHEN f.severity = 0 THEN 1 END) AS critical_count,
    COUNT(CASE WHEN f.severity = 1 THEN 1 END) AS high_count,
    COUNT(CASE WHEN f.severity = 2 THEN 1 END) AS medium_count,
    COUNT(CASE WHEN f.severity = 3 THEN 1 END) AS low_count,
    CASE 
        WHEN t.completed_at IS NOT NULL AND t.created_at IS NOT NULL 
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))
        ELSE NULL 
    END AS duration_seconds
FROM review_tasks t
LEFT JOIN findings f ON t.id = f.task_id
GROUP BY t.id, t.name, t.review_type, t.status, t.created_at, t.completed_at;
COMMENT ON VIEW v_task_statistics IS '任务统计视图，包含每个任务的问题统计信息';
```
### 8.3 问题分类统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建问题分类统计视图
CREATE OR REPLACE VIEW v_finding_category_statistics AS
SELECT 
    category,
    severity,
    COUNT(*) AS count,
    ROUND(AVG(confidence), 2) AS avg_confidence
FROM findings
WHERE category IS NOT NULL
GROUP BY category, severity
ORDER BY category, severity;
COMMENT ON VIEW v_finding_category_statistics IS '问题分类统计视图，按类别和严重程度统计';
```
***##9.字段类型优化说明###9.1优化原则本数据库设计遵循一线大厂的表设计标准，主要优化原则如下：1.** 节省存储空间*\*:根据实际需求选择合适长度的字段类型2.\*\* 提高查询性能\*\*:使用固定长度字段（CHAR）替代可变长度字段（VARCHAR）用于短字符串3.\*\* 时区处理\*\*:使用TIMESTAMPTZ确保时间戳带时区信息，避免时区问题4.\*\* 类型精确性\*\*: 使用PostgreSQL原生类型（如INET）提高数据准确性和查询效率
### 9.2 主要优化项
#### 14.2.1 时间类型优化
* **优化前** : `TIMESTAMP`（不带时区）
* **优化后** : `TIMESTAMPTZ`（带时区）
* **原因** :
  + 符合一线大厂标准，避免时区转换问题
  + 支持多时区应用场景
  + 提高时间查询的准确性
#### 14.2.2 字符串类型优化
| 字段类型 | 优化前 | 优化后 | 原因 |
| --- | --- | --- | --- |
| 用户名 | VARCHAR(50) | VARCHAR(32) | 用户名通常较短，32字符足够 |
| 邮箱 | VARCHAR(100) | VARCHAR(255) | 符合RFC 5321标准，最长255字符 |
| 密码哈希 | VARCHAR(255) | CHAR(60) | BCrypt固定60字符，使用CHAR节省空间 |
| 状态码 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 审查类型 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 严重程度 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 问题类别 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 资源类型 | VARCHAR(32) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 操作类型 | VARCHAR(16) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 规则ID | VARCHAR(64) | BIGINT | 规则ID应为数字类型，支持外键关联 |
| 代码字段 | VARCHAR(50) | VARCHAR(32) | 代码通常较短，32字符足够 |
| 真实姓名 | VARCHAR(100) | VARCHAR(64) | 根据实际需求优化 |
| 手机号 | VARCHAR(20) | VARCHAR(16) | 国际格式最长15位 |
| URL/路径 | VARCHAR(500) | TEXT | 路径可能很长，TEXT更灵活 |
| 标题/位置 | VARCHAR(500/1000) | TEXT | 不确定长度，TEXT更合适 |
#### 9.2.3 特殊类型优化
* **IP地址** : `VARCHAR(50)` → `INET`
  + 使用PostgreSQL原生IP类型
  + 支持IPv4和IPv6
  + 提供IP地址验证和查询优化
* **密码哈希** : `VARCHAR(255)` → `CHAR(60)`
  + BCrypt算法固定输出60字符
  + CHAR类型固定长度，节省空间
  + 提高索引效率
* **枚举字段** : `VARCHAR/CHAR` → `SMALLINT`
  + status、review\_type、severity、category等枚举字段使用SMALLINT
  + SMALLINT仅占用2字节，比字符串类型节省大量空间
  + 数字比较比字符串比较更高效
  + 索引效率更高，查询性能更好
#### 9.2.4 空间节省估算
以users表为例（假设100万用户）：
* 密码哈希：从255字节降至60字节，节省约195字节/行
* 状态字段：从20字节降至10字节，节省约10字节/行
* IP地址：从50字节降至约16字节（INET），节省约34字节/行
* **总计每行节省约239字节，100万用户可节省约228MB存储空间**
### 9.3 性能提升
- 1.
  **索引效率** : CHAR类型和固定长度字段的索引更高效
- 2.
  **查询性能** : INET类型支持IP地址范围查询和网络操作
- 3.
  **时区处理** : TIMESTAMPTZ避免应用层时区转换，提高查询准确性
---
10. 附录
------
### 10.1 数据类型说明
* **BIGSERIAL** : 自增长整型，范围 -9223372036854775808 到 9223372036854775807
* **VARCHAR(n)** : 可变长度字符串，最大n个字符，根据实际需求选择合适长度
* **CHAR(n)** : 固定长度字符串，用于固定长度的字段（如状态码、密码哈希）
* **TEXT** : 可变长度字符串，无长度限制，用于不确定长度的文本内容
* **TIMESTAMPTZ** : 带时区的时间戳，精度到微秒（推荐使用，符合一线大厂标准）
* **INET** : PostgreSQL原生IP地址类型，支持IPv4和IPv6
* **JSONB** : 二进制JSON格式，支持索引和查询
* **DECIMAL(p,s)** : 精确数值类型，p为精度，s为小数位数
### 10.2 PostgreSQL扩展
* **pg\_trgm** : 用于全文检索的trigram扩展
* **btree\_gin** : 用于GIN索引的B-tree操作符类
### 10.3 枚举值映射表
#### 10.3.1 用户状态（users.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
| 2 | LOCKED - 锁定 |
#### 10.3.2 角色状态（roles.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
#### 10.3.3 任务状态（review\_tasks.status）
| 值 | 说明 |
| --- | --- |
| 0 | PENDING - 待处理 |
| 1 | RUNNING - 运行中 |
| 2 | COMPLETED - 已完成 |
| 3 | FAILED - 失败 |
#### 10.3.4 审查类型（review\_tasks.review\_type）
| 值 | 说明 |
| --- | --- |
| 0 | PROJECT - 项目 |
| 1 | DIRECTORY - 目录 |
| 2 | FILE - 文件 |
| 3 | SNIPPET - 代码片段 |
| 4 | GIT - Git项目 |
#### 10.3.5 严重程度（findings.severity）
| 值 | 说明 |
| --- | --- |
| 0 | CRITICAL - 严重 |
| 1 | HIGH - 高 |
| 2 | MEDIUM - 中 |
| 3 | LOW - 低 |
#### 10.3.6 问题类别（findings.category）
| 值 | 说明 |
| --- | --- |
| 0 | SECURITY - 安全 |
| 1 | PERFORMANCE - 性能 |
| 2 | BUG - 缺陷 |
| 3 | CODE\_STYLE - 代码风格 |
| 4 | MAINTAINABILITY - 可维护性 |
#### 10.3.7 资源类型（permissions.resource）
| 值 | 说明 |
| --- | --- |
| 0 | TASK - 任务 |
| 1 | REPORT - 报告 |
| 2 | CONFIG - 配置 |
#### 10.3.8 操作类型（permissions.action）
| 值 | 说明 |
| --- | --- |
| 0 | READ - 读取 |
| 1 | CREATE - 创建 |
| 2 | UPDATE - 更新 |
| 3 | DELETE - 删除 |
### 15.4 参考文档
* [PostgreSQL官方文档](https://www.postgresql.org/docs/)
* [PostgreSQL索引类型](https://www.postgresql.org/docs/current/indexes-types.html)
* [PostgreSQL JSON操作](https://www.postgresql.org/docs/current/functions-json.html)
---
咱们代码审查AI Agent项目，前面已经做了系统设计，在正式开发之前，先做好数据库表设计。  
   
由于咱们规划使用的数据库是PostgreSQL，因此，咱们打算使用PostgreSQL 15+的版本。  
   
这篇文章中的sql语句，都是用PostgreSQL中的语法。
---
1. 数据库概述
--------
### 1.1 数据库命名规范
* **数据库名** : `code_guardian`
* **Schema** : `public`（默认）
* **表命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **字段命名** : 小写字母，多个单词用下划线分隔（snake\_case）
* **索引命名** : `idx_表名_字段名` 或 `idx_表名_字段名1_字段名2`（复合索引）
### 1.2 设计原则
* **规范化** : 遵循第三范式（3NF），减少数据冗余
* **性能优化** : 合理使用索引，考虑查询性能
* **可扩展性** : 预留扩展字段，支持未来功能扩展
* **数据完整性** : 使用约束保证数据一致性
* **PostgreSQL特性** : 充分利用JSON、全文检索等特性
**数据库ER图**
![image.png](https://article-images.zsxq.com/Fk-tSthbEWZDXAoHwiua0pjObYOW)![]()
3. 数据表设计
--------
### 3.1 users（用户表）
#### 3.1.1 表说明
存储系统用户的基本信息，包括用户名、邮箱、密码哈希等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 用户ID，自增主键 |
| username | VARCHAR(32) | NOT NULL, UNIQUE | - | 用户名，唯一 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | - | 邮箱地址，唯一（符合RFC 5321标准） |
| password\_hash | CHAR(60) | NOT NULL | - | 密码哈希值（BCrypt固定60字符） |
| real\_name | VARCHAR(64) | - | NULL | 真实姓名 |
| phone | VARCHAR(16) | - | NULL | 手机号码（支持国际格式） |
| avatar\_url | TEXT | - | NULL | 头像URL |
| status | SMALLINT | NOT NULL | 0 | 用户状态：0=ACTIVE, 1=INACTIVE, 2=LOCKED |
| last\_login\_at | TIMESTAMPTZ | - | NULL | 最后登录时间（带时区） |
| last\_login\_ip | INET | - | NULL | 最后登录IP（PostgreSQL原生IP类型） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.1.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户名唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (username)
-- 邮箱唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (email)
-- 状态索引（用于查询特定状态的用户）
CREATE INDEX idx_users_status ON users(status);
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_users_created_at ON users(created_at DESC);
-- 用户名全文检索索引
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
```
#### 3.1.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE users ADD CONSTRAINT chk_users_status 
CHECK (status IN (0, 1, 2));
-- 邮箱格式检查约束（简单验证）
ALTER TABLE users ADD CONSTRAINT chk_users_email 
CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
```
#### 3.1.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：ACTIVE(激活)/INACTIVE(未激活)/LOCKED(锁定)';
COMMENT ON COLUMN users.last_login_at IS '最后登录时间';
COMMENT ON COLUMN users.last_login_ip IS '最后登录IP地址';
```
### 3.2 roles（角色表）
#### 3.2.1 表说明
存储系统角色信息，如管理员、审查员、查看者等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 角色ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 角色代码，唯一标识（如：ADMIN、REVIEWER、VIEWER） |
| name | VARCHAR(64) | NOT NULL | - | 角色名称（如：管理员、审查员、查看者） |
| description | TEXT | - | NULL | 角色描述 |
| status | SMALLINT | NOT NULL | 0 | 角色状态：0=ACTIVE, 1=INACTIVE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色代码唯一索引（自动创建）
-- UNIQUE (code)
-- 状态索引
CREATE INDEX idx_roles_status ON roles(status);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 状态值检查约束
ALTER TABLE roles ADD CONSTRAINT chk_roles_status 
CHECK (status IN (0, 1));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.description IS '角色描述';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
```
### 3.3 permissions（权限表）
#### 3.3.1 表说明
存储系统权限定义，如查询、审查、配置等。
#### 3.3.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 权限ID，自增主键 |
| code | VARCHAR(32) | NOT NULL, UNIQUE | - | 权限代码，唯一标识（如：QUERY、REVIEW、CONFIG、ADMIN） |
| name | VARCHAR(64) | NOT NULL | - | 权限名称（如：查询权限、审查权限、配置权限） |
| description | TEXT | - | NULL | 权限描述 |
| resource | SMALLINT | - | NULL | 资源类型：0=TASK, 1=REPORT, 2=CONFIG |
| action | SMALLINT | - | NULL | 操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.3.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 权限代码唯一索引（自动创建）
-- UNIQUE (code)
-- 资源索引（用于按资源查询权限）
CREATE INDEX idx_permissions_resource ON permissions(resource);
```
#### 3.3.4 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
COMMENT ON COLUMN permissions.description IS '权限描述';
COMMENT ON COLUMN permissions.resource IS '资源类型：0=TASK, 1=REPORT, 2=CONFIG';
COMMENT ON COLUMN permissions.action IS '操作类型：0=READ, 1=CREATE, 2=UPDATE, 3=DELETE';
```
---
### 3.4 user\_roles（用户角色关联表）
#### 3.4.1 表说明
存储用户和角色的多对多关联关系。
#### 3.4.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| user\_id | BIGINT | NOT NULL, FK | - | 用户ID |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.4.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
-- 角色ID索引
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- 唯一约束索引（一个用户不能重复分配同一个角色）
CREATE UNIQUE INDEX idx_user_roles_unique ON user_roles(user_id, role_id);
```
#### 3.4.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_roles ADD CONSTRAINT fk_user_roles_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
-- 唯一约束（一个用户不能重复分配同一个角色）
-- UNIQUE (user_id, role_id) -- 已在索引中定义
```
#### 3.4.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE user_roles IS '用户角色关联表';
COMMENT ON COLUMN user_roles.id IS '关联ID，自增主键';
COMMENT ON COLUMN user_roles.user_id IS '用户ID';
COMMENT ON COLUMN user_roles.role_id IS '角色ID';
```
---
### 3.5 role\_permissions（角色权限关联表）
#### 3.5.1 表说明
存储角色和权限的多对多关联关系。
#### 3.5.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 关联ID，自增主键 |
| role\_id | BIGINT | NOT NULL, FK | - | 角色ID |
| permission\_id | BIGINT | NOT NULL, FK | - | 权限ID |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.5.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 角色ID索引
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
-- 权限ID索引
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- 唯一约束索引（一个角色不能重复分配同一个权限）
CREATE UNIQUE INDEX idx_role_permissions_unique ON role_permissions(role_id, permission_id);
```
#### 3.5.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role_id 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_permission_id 
FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE;
-- 唯一约束（一个角色不能重复分配同一个权限）
-- UNIQUE (role_id, permission_id) -- 已在索引中定义
```
#### 3.5.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE role_permissions IS '角色权限关联表';
COMMENT ON COLUMN role_permissions.id IS '关联ID，自增主键';
COMMENT ON COLUMN role_permissions.role_id IS '角色ID';
COMMENT ON COLUMN role_permissions.permission_id IS '权限ID';
```
### 3.6 review\_tasks（审查任务表）
#### 3.1.1 表说明
存储代码审查任务的基本信息，包括任务状态、审查类型、审查范围等。
#### 3.1.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 任务ID，自增主键 |
| name | VARCHAR(128) | NOT NULL | - | 任务名称 |
| review\_type | SMALLINT | NOT NULL | - | 审查类型：0=PROJECT, 1=DIRECTORY, 2=FILE, 3=SNIPPET, 4=GIT |
| scope | TEXT | - | NULL | 审查范围（文件路径、目录路径或代码片段） |
| status | SMALLINT | NOT NULL | 0 | 任务状态：0=PENDING, 1=RUNNING, 2=COMPLETED, 3=FAILED |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| completed\_at | TIMESTAMPTZ | - | NULL | 完成时间（带时区） |
| error\_message | TEXT | - | NULL | 错误信息（任务失败时） |
| user\_id | BIGINT | NOT NULL, FK | - | 创建用户ID |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
| metadata | JSONB | - | NULL | 元数据（JSON格式，预留扩展字段） |
#### 3.6.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 用户ID索引（用于查询用户创建的任务）
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
-- 审查类型索引（用于按类型查询）
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
-- 状态索引（用于查询特定状态的任务）
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
-- 创建时间索引（用于时间范围查询和排序）
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
-- 复合索引（用于常见查询组合）
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
-- 名称模糊查询索引（使用GIN索引支持全文检索）
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
```
#### 3.6.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_tasks ADD CONSTRAINT fk_review_tasks_user_id 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
-- 状态值检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_status 
CHECK (status IN (0, 1, 2, 3));
-- 审查类型检查约束
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_type 
CHECK (review_type IN (0, 1, 2, 3, 4));
-- 完成时间检查约束（完成时间必须晚于创建时间）
ALTER TABLE review_tasks ADD CONSTRAINT chk_review_tasks_time 
CHECK (completed_at IS NULL OR completed_at >= created_at);
```
#### 3.6.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
```
---
### 3.7 findings（审查发现表）
#### 3.2.1 表说明
存储代码审查发现的问题，包括问题严重程度、位置、描述、修复建议等。
#### 3.2.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 发现ID，自增主键 |
| task\_id | BIGINT | NOT NULL, FK | - | 关联的审查任务ID |
| severity | SMALLINT | NOT NULL | - | 严重程度：0=CRITICAL, 1=HIGH, 2=MEDIUM, 3=LOW |
| title | TEXT | NOT NULL | - | 问题标题 |
| location | TEXT | NOT NULL | - | 问题位置（文件路径或代码位置描述） |
| start\_line | INTEGER | - | NULL | 起始行号 |
| end\_line | INTEGER | - | NULL | 结束行号 |
| description | TEXT | NOT NULL | - | 问题详细描述 |
| suggestion | TEXT | - | NULL | 修复建议 |
| diff | TEXT | - | NULL | 修复代码差异（Diff格式） |
| category | SMALLINT | - | NULL | 问题类别：0=SECURITY, 1=PERFORMANCE, 2=BUG, 3=CODE\_STYLE, 4=MAINTAINABILITY |
| rule\_id | BIGINT | - | NULL | 规则ID（如果来自规则引擎，预留字段） |
| confidence | DECIMAL(3,2) | - | NULL | 置信度（0.00-1.00，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
#### 3.2.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID索引（用于查询特定任务的所有问题）
CREATE INDEX idx_findings_task_id ON findings(task_id);
-- 严重程度索引（用于按严重程度筛选）
CREATE INDEX idx_findings_severity ON findings(severity);
-- 类别索引（用于按类别筛选）
CREATE INDEX idx_findings_category ON findings(category);
-- 复合索引（用于常见查询组合：任务+严重程度）
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
-- 复合索引（用于常见查询组合：任务+类别）
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
-- 标题全文检索索引（使用GIN索引）
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
-- 描述全文检索索引（使用GIN索引，可选，如果描述字段查询频繁）
-- CREATE INDEX idx_findings_description_gin ON findings USING gin(description gin_trgm_ops);
```
#### 3.2.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE findings ADD CONSTRAINT fk_findings_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 严重程度检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_severity 
CHECK (severity IN (0, 1, 2, 3));
-- 类别检查约束
ALTER TABLE findings ADD CONSTRAINT chk_findings_category 
CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4));
-- 行号检查约束（结束行号必须大于等于起始行号）
ALTER TABLE findings ADD CONSTRAINT chk_findings_line 
CHECK ((start_line IS NULL AND end_line IS NULL) OR 
       (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line));
-- 置信度检查约束（0.00-1.00）
ALTER TABLE findings ADD CONSTRAINT chk_findings_confidence 
CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00));
```
#### 3.2.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：0=CRITICAL(严重), 1=HIGH(高), 2=MEDIUM(中), 3=LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
COMMENT ON COLUMN findings.rule_id IS '规则ID，如果问题来自规则引擎';
COMMENT ON COLUMN findings.confidence IS '置信度，0.00-1.00';
```
### 3.8 review\_reports（审查报告表）
#### 3.8.1 表说明
存储代码审查报告，包括HTML格式、Markdown格式和统计信息。
#### 3.8.2 字段定义
| 字段名 | 数据类型 | 约束 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| id | BIGSERIAL | PRIMARY KEY | - | 报告ID，自增主键 |
| task\_id | BIGINT | NOT NULL, UNIQUE, FK | - | 关联的审查任务ID（一对一关系） |
| html\_content | TEXT | - | NULL | HTML格式报告内容 |
| markdown\_content | TEXT | - | NULL | Markdown格式报告内容 |
| statistics | JSONB | - | NULL | 统计信息（JSON格式） |
| pdf\_path | TEXT | - | NULL | PDF文件路径（如果生成PDF，预留字段） |
| created\_at | TIMESTAMPTZ | NOT NULL | CURRENT\_TIMESTAMP | 创建时间（带时区） |
| updated\_at | TIMESTAMPTZ | - | CURRENT\_TIMESTAMP | 更新时间（带时区） |
#### 3.8.3 索引设计
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 主键索引（自动创建）
-- PRIMARY KEY (id)
-- 任务ID唯一索引（自动创建，因为UNIQUE约束）
-- UNIQUE (task_id)
-- 创建时间索引（用于时间范围查询）
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
-- 统计信息GIN索引（用于JSON查询，如果需要在统计信息中搜索）
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
#### 3.8.4 约束
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 外键约束
ALTER TABLE review_reports ADD CONSTRAINT fk_review_reports_task_id 
FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE;
-- 任务ID唯一约束（一个任务只能有一个报告）
-- UNIQUE (task_id) -- 已在字段定义中设置
```
#### 3.8.5 注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
COMMENT ON COLUMN review_reports.pdf_path IS 'PDF文件路径（如果生成PDF文件）';
COMMENT ON COLUMN review_reports.created_at IS '报告创建时间';
COMMENT ON COLUMN review_reports.updated_at IS '报告更新时间';
```
---
4. 权限体系设计
---------
### 4.1 权限类型定义
系统定义了以下权限类型：
| 权限代码 | 权限名称 | 说明 | 资源 | 操作 |
| --- | --- | --- | --- | --- |
| QUERY | 查询权限 | 可以查看审查任务、报告、历史记录等 | 0 (TASK) | 0 (READ) |
| REVIEW | 审查权限 | 可以创建和执行代码审查任务 | 0 (TASK) | 1 (CREATE) |
| CONFIG | 配置权限 | 可以修改系统配置、AI配置等 | 2 (CONFIG) | 2 (UPDATE) |
| ADMIN | 管理员权限 | 拥有所有权限，包括用户管理、角色管理等 | NULL (ALL) | NULL (ALL) |
**注意** : resource和action字段使用SMALLINT类型，值为NULL表示所有资源/操作。
### 4.2 角色定义
系统预定义了以下角色：
| 角色代码 | 角色名称 | 说明 | 包含权限 |
| --- | --- | --- | --- |
| ADMIN | 管理员 | 系统管理员，拥有所有权限 | QUERY, REVIEW, CONFIG, ADMIN |
| REVIEWER | 审查员 | 可以创建和查看审查任务 | QUERY, REVIEW |
| VIEWER | 查看者 | 只能查看审查任务和报告 | QUERY |
### 4.3 权限检查逻辑
权限检查采用RBAC（基于角色的访问控制）模型：
- 1.
  **用户登录** : 验证用户名和密码
- 2.
  **获取角色** : 查询用户关联的所有角色
- 3.
  **获取权限** : 查询角色关联的所有权限
- 4.
  **权限验证** : 检查用户是否拥有执行操作的权限
### 4.4 权限验证示例
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 查询用户的所有权限
SELECT DISTINCT p.code, p.name, p.resource, p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? AND u.status = 0 AND r.status = 0;
-- 检查用户是否有特定权限
SELECT COUNT(*) > 0 AS has_permission
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.id = ? 
  AND u.status = 0 
  AND r.status = 0
  AND p.code = ?;
```
---
5. 完整SQL建表语句
------------
### 5.1 创建数据库
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建数据库
CREATE DATABASE code_guardian
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
-- 连接到数据库
\c code_guardian;
-- 启用扩展（用于全文检索）
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
```
### 5.2 创建表
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- 1. 创建用户表
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash CHAR(60) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(16),
    avatar_url TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT chk_users_status 
        CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_users_email 
        CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);
-- ============================================
-- 2. 创建角色表
-- ============================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_roles_status 
        CHECK (status IN (0, 1))
);
-- ============================================
-- 3. 创建权限表
-- ============================================
CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    resource SMALLINT,
    action SMALLINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ============================================
-- 4. 创建用户角色关联表
-- ============================================
CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_user_roles_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_roles_unique UNIQUE (user_id, role_id)
);
-- ============================================
-- 5. 创建角色权限关联表
-- ============================================
CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_role_permissions_role_id 
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission_id 
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_role_permissions_unique UNIQUE (role_id, permission_id)
);
-- ============================================
-- 6. 创建审查任务表
-- ============================================
CREATE TABLE review_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    review_type SMALLINT NOT NULL,
    scope TEXT,
    status SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    
    CONSTRAINT fk_review_tasks_user_id 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_review_tasks_status 
        CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_review_tasks_type 
        CHECK (review_type IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_review_tasks_time 
        CHECK (completed_at IS NULL OR completed_at >= created_at)
);
-- ============================================
-- 2. 创建审查发现表
-- ============================================
CREATE TABLE findings (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    severity SMALLINT NOT NULL,
    title TEXT NOT NULL,
    location TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    description TEXT NOT NULL,
    suggestion TEXT,
    diff TEXT,
    category SMALLINT,
    rule_id BIGINT,
    confidence DECIMAL(3,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_findings_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE,
    
    -- 检查约束
    CONSTRAINT chk_findings_severity 
        CHECK (severity IN (0, 1, 2, 3)),
    CONSTRAINT chk_findings_category 
        CHECK (category IS NULL OR category IN (0, 1, 2, 3, 4)),
    CONSTRAINT chk_findings_line 
        CHECK ((start_line IS NULL AND end_line IS NULL) OR 
               (start_line IS NOT NULL AND end_line IS NOT NULL AND end_line >= start_line)),
    CONSTRAINT chk_findings_confidence 
        CHECK (confidence IS NULL OR (confidence >= 0.00 AND confidence <= 1.00))
);
-- ============================================
-- 3. 创建审查报告表
-- ============================================
CREATE TABLE review_reports (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    html_content TEXT,
    markdown_content TEXT,
    statistics JSONB,
    pdf_path TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- 外键约束
    CONSTRAINT fk_review_reports_task_id 
        FOREIGN KEY (task_id) REFERENCES review_tasks(id) ON DELETE CASCADE
);
```
### 5.3 创建索引
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- ============================================
-- users 表索引
-- ============================================
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_created_at ON users(created_at DESC);
CREATE INDEX idx_users_username_gin ON users USING gin(username gin_trgm_ops);
-- ============================================
-- roles 表索引
-- ============================================
CREATE INDEX idx_roles_status ON roles(status);
-- ============================================
-- permissions 表索引
-- ============================================
CREATE INDEX idx_permissions_resource ON permissions(resource);
-- ============================================
-- user_roles 表索引
-- ============================================
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
-- ============================================
-- role_permissions 表索引
-- ============================================
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);
-- ============================================
-- review_tasks 表索引
-- ============================================
CREATE INDEX idx_review_tasks_user_id ON review_tasks(user_id);
CREATE INDEX idx_review_tasks_type ON review_tasks(review_type);
CREATE INDEX idx_review_tasks_status ON review_tasks(status);
CREATE INDEX idx_review_tasks_created_at ON review_tasks(created_at DESC);
CREATE INDEX idx_review_tasks_user_status ON review_tasks(user_id, status);
CREATE INDEX idx_review_tasks_status_created_at ON review_tasks(status, created_at DESC);
CREATE INDEX idx_review_tasks_name_gin ON review_tasks USING gin(name gin_trgm_ops);
CREATE INDEX idx_review_tasks_metadata_gin ON review_tasks USING gin(metadata);
-- ============================================
-- findings 表索引
-- ============================================
CREATE INDEX idx_findings_task_id ON findings(task_id);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_category ON findings(category);
CREATE INDEX idx_findings_task_severity ON findings(task_id, severity);
CREATE INDEX idx_findings_task_category ON findings(task_id, category);
CREATE INDEX idx_findings_title_gin ON findings USING gin(title gin_trgm_ops);
CREATE INDEX idx_findings_created_at ON findings(created_at DESC);
-- ============================================
-- review_reports 表索引
-- ============================================
CREATE INDEX idx_review_reports_created_at ON review_reports(created_at DESC);
CREATE INDEX idx_review_reports_statistics_gin ON review_reports USING gin(statistics);
```
### 5.4 添加表注释
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- users 表注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户ID，自增主键';
COMMENT ON COLUMN users.username IS '用户名，唯一标识';
COMMENT ON COLUMN users.email IS '邮箱地址，用于登录和通知';
COMMENT ON COLUMN users.password_hash IS '密码哈希值，使用BCrypt加密';
COMMENT ON COLUMN users.status IS '用户状态：0=ACTIVE(激活), 1=INACTIVE(未激活), 2=LOCKED(锁定)';
-- roles 表注释
COMMENT ON TABLE roles IS '角色表';
COMMENT ON COLUMN roles.id IS '角色ID，自增主键';
COMMENT ON COLUMN roles.code IS '角色代码，唯一标识，如：ADMIN、REVIEWER、VIEWER';
COMMENT ON COLUMN roles.name IS '角色名称，如：管理员、审查员、查看者';
COMMENT ON COLUMN roles.status IS '角色状态：0=ACTIVE(激活), 1=INACTIVE(未激活)';
-- permissions 表注释
COMMENT ON TABLE permissions IS '权限表';
COMMENT ON COLUMN permissions.id IS '权限ID，自增主键';
COMMENT ON COLUMN permissions.code IS '权限代码，唯一标识，如：QUERY、REVIEW、CONFIG、ADMIN';
COMMENT ON COLUMN permissions.name IS '权限名称，如：查询权限、审查权限、配置权限';
-- user_roles 表注释
COMMENT ON TABLE user_roles IS '用户角色关联表';
-- role_permissions 表注释
COMMENT ON TABLE role_permissions IS '角色权限关联表';
-- review_tasks 表注释
COMMENT ON TABLE review_tasks IS '代码审查任务表';
COMMENT ON COLUMN review_tasks.user_id IS '创建用户ID，关联users表';
COMMENT ON COLUMN review_tasks.id IS '任务ID，自增主键';
COMMENT ON COLUMN review_tasks.name IS '任务名称';
COMMENT ON COLUMN review_tasks.review_type IS '审查类型：PROJECT(项目)/DIRECTORY(目录)/FILE(文件)/SNIPPET(代码片段)/GIT(Git项目)';
COMMENT ON COLUMN review_tasks.scope IS '审查范围，可以是文件路径、目录路径或代码片段';
COMMENT ON COLUMN review_tasks.status IS '任务状态：PENDING(待处理)/RUNNING(运行中)/COMPLETED(已完成)/FAILED(失败)';
COMMENT ON COLUMN review_tasks.created_at IS '任务创建时间';
COMMENT ON COLUMN review_tasks.completed_at IS '任务完成时间';
COMMENT ON COLUMN review_tasks.error_message IS '错误信息，任务失败时记录';
COMMENT ON COLUMN review_tasks.metadata IS '元数据，JSON格式，用于存储扩展信息';
-- findings 表注释
COMMENT ON TABLE findings IS '代码审查发现的问题表';
COMMENT ON COLUMN findings.id IS '发现ID，自增主键';
COMMENT ON COLUMN findings.task_id IS '关联的审查任务ID';
COMMENT ON COLUMN findings.severity IS '严重程度：CRITICAL(严重)/HIGH(高)/MEDIUM(中)/LOW(低)';
COMMENT ON COLUMN findings.title IS '问题标题';
COMMENT ON COLUMN findings.location IS '问题位置，可以是文件路径或代码位置描述';
COMMENT ON COLUMN findings.start_line IS '起始行号';
COMMENT ON COLUMN findings.end_line IS '结束行号';
COMMENT ON COLUMN findings.description IS '问题详细描述';
COMMENT ON COLUMN findings.suggestion IS '修复建议';
COMMENT ON COLUMN findings.diff IS '修复代码差异，Diff格式';
COMMENT ON COLUMN findings.category IS '问题类别：0=SECURITY(安全), 1=PERFORMANCE(性能), 2=BUG(缺陷), 3=CODE_STYLE(代码风格), 4=MAINTAINABILITY(可维护性)';
-- review_reports 表注释
COMMENT ON TABLE review_reports IS '代码审查报告表';
COMMENT ON COLUMN review_reports.id IS '报告ID，自增主键';
COMMENT ON COLUMN review_reports.task_id IS '关联的审查任务ID，一对一关系';
COMMENT ON COLUMN review_reports.html_content IS 'HTML格式报告内容';
COMMENT ON COLUMN review_reports.markdown_content IS 'Markdown格式报告内容';
COMMENT ON COLUMN review_reports.statistics IS '统计信息，JSON格式，包含问题数量、严重程度分布等';
```
6. 初始化数据
--------
### 6.1 初始化角色数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义角色
INSERT INTO roles (code, name, description) VALUES
('ADMIN', '管理员', '系统管理员，拥有所有权限'),
('REVIEWER', '审查员', '可以创建和查看审查任务'),
('VIEWER', '查看者', '只能查看审查任务和报告');
```
### 6.2 初始化权限数据
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 插入预定义权限
-- resource: 0=TASK, 1=REPORT, 2=CONFIG
-- action: 0=READ, 1=CREATE, 2=UPDATE, 3=DELETE
INSERT INTO permissions (code, name, description, resource, action) VALUES
('QUERY', '查询权限', '可以查看审查任务、报告、历史记录等', 0, 0),
('REVIEW', '审查权限', '可以创建和执行代码审查任务', 0, 1),
('CONFIG', '配置权限', '可以修改系统配置、AI配置等', 2, 2),
('ADMIN', '管理员权限', '拥有所有权限，包括用户管理、角色管理等', NULL, NULL);
```
### 6.3 初始化角色权限关联
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 管理员角色拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN';
-- 审查员角色拥有查询和审查权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'REVIEWER' AND p.code IN ('QUERY', 'REVIEW');
-- 查看者角色只拥有查询权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VIEWER' AND p.code = 'QUERY';
```
### 6.4 创建默认管理员用户
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建默认管理员用户（密码：admin123，实际使用时需要修改）
-- 密码哈希值使用BCrypt加密，示例：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, email, password_hash, real_name, status) VALUES
('admin', 'admin@codeguardian.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'ACTIVE');
-- 为管理员用户分配管理员角色
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
```
---
7. 触发器设计
--------
### 5.1 自动更新时间戳触发器
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建更新时间戳的函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
-- 为 review_tasks 表创建触发器
CREATE TRIGGER trigger_review_tasks_updated_at
    BEFORE UPDATE ON review_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
-- 为 review_reports 表创建触发器
CREATE TRIGGER trigger_review_reports_updated_at
    BEFORE UPDATE ON review_reports
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```
---
8. 视图设计
-------
### 8.1 用户权限视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建用户权限视图（用于快速查询用户的所有权限）
CREATE OR REPLACE VIEW v_user_permissions AS
SELECT 
    u.id AS user_id,
    u.username,
    u.email,
    r.id AS role_id,
    r.code AS role_code,
    r.name AS role_name,
    p.id AS permission_id,
    p.code AS permission_code,
    p.name AS permission_name,
    p.resource,
    p.action
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
JOIN role_permissions rp ON r.id = rp.role_id
JOIN permissions p ON rp.permission_id = p.id
WHERE u.status = 0 AND r.status = 0;
COMMENT ON VIEW v_user_permissions IS '用户权限视图，包含用户的所有角色和权限信息';
```
### 8.2 任务统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建任务统计视图（包含问题统计）
CREATE OR REPLACE VIEW v_task_statistics AS
SELECT 
    t.id AS task_id,
    t.name AS task_name,
    t.review_type,
    t.status,
    t.created_at,
    t.completed_at,
    COUNT(f.id) AS total_findings,
    COUNT(CASE WHEN f.severity = 0 THEN 1 END) AS critical_count,
    COUNT(CASE WHEN f.severity = 1 THEN 1 END) AS high_count,
    COUNT(CASE WHEN f.severity = 2 THEN 1 END) AS medium_count,
    COUNT(CASE WHEN f.severity = 3 THEN 1 END) AS low_count,
    CASE 
        WHEN t.completed_at IS NOT NULL AND t.created_at IS NOT NULL 
        THEN EXTRACT(EPOCH FROM (t.completed_at - t.created_at))
        ELSE NULL 
    END AS duration_seconds
FROM review_tasks t
LEFT JOIN findings f ON t.id = f.task_id
GROUP BY t.id, t.name, t.review_type, t.status, t.created_at, t.completed_at;
COMMENT ON VIEW v_task_statistics IS '任务统计视图，包含每个任务的问题统计信息';
```
### 8.3 问题分类统计视图
textjavascripttypescriptcsshtmlbashjsonmarkdownpythonjavaccpprubygorustphpsqlyaml Copy
```
-- 创建问题分类统计视图
CREATE OR REPLACE VIEW v_finding_category_statistics AS
SELECT 
    category,
    severity,
    COUNT(*) AS count,
    ROUND(AVG(confidence), 2) AS avg_confidence
FROM findings
WHERE category IS NOT NULL
GROUP BY category, severity
ORDER BY category, severity;
COMMENT ON VIEW v_finding_category_statistics IS '问题分类统计视图，按类别和严重程度统计';
```
***##9.字段类型优化说明###9.1优化原则本数据库设计遵循一线大厂的表设计标准，主要优化原则如下：1.** 节省存储空间*\*:根据实际需求选择合适长度的字段类型2.\*\* 提高查询性能\*\*:使用固定长度字段（CHAR）替代可变长度字段（VARCHAR）用于短字符串3.\*\* 时区处理\*\*:使用TIMESTAMPTZ确保时间戳带时区信息，避免时区问题4.\*\* 类型精确性\*\*: 使用PostgreSQL原生类型（如INET）提高数据准确性和查询效率
### 9.2 主要优化项
#### 14.2.1 时间类型优化
* **优化前** : `TIMESTAMP`（不带时区）
* **优化后** : `TIMESTAMPTZ`（带时区）
* **原因** :
  + 符合一线大厂标准，避免时区转换问题
  + 支持多时区应用场景
  + 提高时间查询的准确性
#### 14.2.2 字符串类型优化
| 字段类型 | 优化前 | 优化后 | 原因 |
| --- | --- | --- | --- |
| 用户名 | VARCHAR(50) | VARCHAR(32) | 用户名通常较短，32字符足够 |
| 邮箱 | VARCHAR(100) | VARCHAR(255) | 符合RFC 5321标准，最长255字符 |
| 密码哈希 | VARCHAR(255) | CHAR(60) | BCrypt固定60字符，使用CHAR节省空间 |
| 状态码 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 审查类型 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 严重程度 | VARCHAR(20) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 问题类别 | VARCHAR(50) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 资源类型 | VARCHAR(32) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 操作类型 | VARCHAR(16) | SMALLINT | 使用数字枚举，节省空间且提高查询性能 |
| 规则ID | VARCHAR(64) | BIGINT | 规则ID应为数字类型，支持外键关联 |
| 代码字段 | VARCHAR(50) | VARCHAR(32) | 代码通常较短，32字符足够 |
| 真实姓名 | VARCHAR(100) | VARCHAR(64) | 根据实际需求优化 |
| 手机号 | VARCHAR(20) | VARCHAR(16) | 国际格式最长15位 |
| URL/路径 | VARCHAR(500) | TEXT | 路径可能很长，TEXT更灵活 |
| 标题/位置 | VARCHAR(500/1000) | TEXT | 不确定长度，TEXT更合适 |
#### 9.2.3 特殊类型优化
* **IP地址** : `VARCHAR(50)` → `INET`
  + 使用PostgreSQL原生IP类型
  + 支持IPv4和IPv6
  + 提供IP地址验证和查询优化
* **密码哈希** : `VARCHAR(255)` → `CHAR(60)`
  + BCrypt算法固定输出60字符
  + CHAR类型固定长度，节省空间
  + 提高索引效率
* **枚举字段** : `VARCHAR/CHAR` → `SMALLINT`
  + status、review\_type、severity、category等枚举字段使用SMALLINT
  + SMALLINT仅占用2字节，比字符串类型节省大量空间
  + 数字比较比字符串比较更高效
  + 索引效率更高，查询性能更好
#### 9.2.4 空间节省估算
以users表为例（假设100万用户）：
* 密码哈希：从255字节降至60字节，节省约195字节/行
* 状态字段：从20字节降至10字节，节省约10字节/行
* IP地址：从50字节降至约16字节（INET），节省约34字节/行
* **总计每行节省约239字节，100万用户可节省约228MB存储空间**
### 9.3 性能提升
- 1.
  **索引效率** : CHAR类型和固定长度字段的索引更高效
- 2.
  **查询性能** : INET类型支持IP地址范围查询和网络操作
- 3.
  **时区处理** : TIMESTAMPTZ避免应用层时区转换，提高查询准确性
---
10. 附录
------
### 10.1 数据类型说明
* **BIGSERIAL** : 自增长整型，范围 -9223372036854775808 到 9223372036854775807
* **VARCHAR(n)** : 可变长度字符串，最大n个字符，根据实际需求选择合适长度
* **CHAR(n)** : 固定长度字符串，用于固定长度的字段（如状态码、密码哈希）
* **TEXT** : 可变长度字符串，无长度限制，用于不确定长度的文本内容
* **TIMESTAMPTZ** : 带时区的时间戳，精度到微秒（推荐使用，符合一线大厂标准）
* **INET** : PostgreSQL原生IP地址类型，支持IPv4和IPv6
* **JSONB** : 二进制JSON格式，支持索引和查询
* **DECIMAL(p,s)** : 精确数值类型，p为精度，s为小数位数
### 10.2 PostgreSQL扩展
* **pg\_trgm** : 用于全文检索的trigram扩展
* **btree\_gin** : 用于GIN索引的B-tree操作符类
### 10.3 枚举值映射表
#### 10.3.1 用户状态（users.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
| 2 | LOCKED - 锁定 |
#### 10.3.2 角色状态（roles.status）
| 值 | 说明 |
| --- | --- |
| 0 | ACTIVE - 激活 |
| 1 | INACTIVE - 未激活 |
#### 10.3.3 任务状态（review\_tasks.status）
| 值 | 说明 |
| --- | --- |
| 0 | PENDING - 待处理 |
| 1 | RUNNING - 运行中 |
| 2 | COMPLETED - 已完成 |
| 3 | FAILED - 失败 |
#### 10.3.4 审查类型（review\_tasks.review\_type）
| 值 | 说明 |
| --- | --- |
| 0 | PROJECT - 项目 |
| 1 | DIRECTORY - 目录 |
| 2 | FILE - 文件 |
| 3 | SNIPPET - 代码片段 |
| 4 | GIT - Git项目 |
#### 10.3.5 严重程度（findings.severity）
| 值 | 说明 |
| --- | --- |
| 0 | CRITICAL - 严重 |
| 1 | HIGH - 高 |
| 2 | MEDIUM - 中 |
| 3 | LOW - 低 |
#### 10.3.6 问题类别（findings.category）
| 值 | 说明 |
| --- | --- |
| 0 | SECURITY - 安全 |
| 1 | PERFORMANCE - 性能 |
| 2 | BUG - 缺陷 |
| 3 | CODE\_STYLE - 代码风格 |
| 4 | MAINTAINABILITY - 可维护性 |
#### 10.3.7 资源类型（permissions.resource）
| 值 | 说明 |
| --- | --- |
| 0 | TASK - 任务 |
| 1 | REPORT - 报告 |
| 2 | CONFIG - 配置 |
#### 10.3.8 操作类型（permissions.action）
| 值 | 说明 |
| --- | --- |
| 0 | READ - 读取 |
| 1 | CREATE - 创建 |
| 2 | UPDATE - 更新 |
| 3 | DELETE - 删除 |
### 15.4 参考文档
* [PostgreSQL官方文档](https://www.postgresql.org/docs/)
* [PostgreSQL索引类型](https://www.postgresql.org/docs/current/indexes-types.html)
* [PostgreSQL JSON操作](https://www.postgresql.org/docs/current/functions-json.html)
---
![](https://articles.zsxq.com/assets_dweb/logo@1x.png)
知识星球
扫码加入星球
查看更多优质内容
https://wx.zsxq.com/mweb/views/joingroup/join\_group.html?group\_id=28851182188851
![]()
