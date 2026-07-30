# Local Dictionary 设计

## 目标

实现完全离线的 IntelliJ Platform 本地查词插件。用户在编辑器中悬停英文标识符时，插件将其拆分为单词并显示内置 ECDICT SQLite 词典的释义。

## 架构

- 使用 `org.xerial:sqlite-jdbc` 访问 SQLite。
- 精简 ECDICT 数据库以 `ecdict.db` 作为插件资源随产物打包。
- 首次使用时将资源复制到 IDE 系统目录下的插件缓存；后续从该文件建立只读 JDBC 连接。SQLite 不直接从 JAR 内打开。
- `DictionaryDatabase` 负责数据库文件准备、连接管理和按小写单词查询。
- `IdentifierWords` 负责拆分 camelCase、PascalCase 与 snake_case 标识符，并忽略大小写。
- 编辑器悬停处理器取得光标下标识符，按拆分顺序查询词典，并在有命中时显示简短释义。

## 插件注册与清理

- 移除模板 Tool Window、项目服务和启动活动及其注册。
- 更新插件名称、描述和消息资源，注册编辑器悬停扩展。

## 失败处理

- 词典资源复制、数据库初始化或查询失败时记录诊断日志，不显示悬停提示。
- 不发起任何网络请求。

## 测试

- 单元测试覆盖标识符拆分规则。
- 集成测试使用测试 SQLite 数据库验证不区分大小写的查询及释义读取。
- 插件测试确保扩展可加载。
