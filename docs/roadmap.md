# mrail 路线图（铁路模组）

状态：未完成（草案，等待讨论确认）。

> 本文档目前是中文讨论稿，尚未完成最终定稿。

更新日期：2026-08-24

## 1. 当前状态

- 项目定稿为铁路模组项目（mod id `mrail`，显示名 Mistbeyond's Rail）。
- 目标环境：Minecraft `26.1.2`，NeoForge `26.1.2.94`，Java 25。
- 已配置 `registry-lib`、JEI、Jade 依赖和 datagen 源集。
- 铁路系统代码位于包根 `com.mistbeyond.rail`（轨道图、调度、列车与信号已实现），mod 元数据（`mrail`）已就绪。
- `docs/architecture.md` 已补充通用工程基线，交通方式细节由 `docs/roadmap/` 中的独立文档承载。

## 2. 使用方式

- 本文件定义代码实现顺序，不是玩法阶段顺序。
- 每个节点固定包含：状态、概括、玩法与实现细则、最小实现范围、文档链接。
- 对应节点细则在需要时按需读取，不要求每个实现任务都读取全部 `docs/roadmap/*`。
- 新包、公共 API 或依赖方向变化仍先按 `docs/architecture.md` 处理。

## 3. 实现顺序

### Node 0：基础工程

- 状态：`next`
- 概括：先建立可运行、可构建、包边界清晰的 mod 骨架，并搭起 KubeJS 插件基础。
- 玩法与实现细则：
  - 创建主 `@Mod` 入口类，完成 mod 初始化。
  - 整理基础包结构与依赖方向，将通用工程基线写入 `docs/architecture.md`。
  - 在 `integration` 下建立 KubeJS 插件骨架，为后续脚本事件、绑定和类型包装预留接入点。
  - 补齐基础 datagen 管线、构建验证和测试基线；模型、语言、标签、配方等默认由 datagen 生成，避免手写模型文件。
- 最小实现范围：client 和 server 可启动，项目可构建，KubeJS 插件可被发现。
- 文档：
  - [docs/architecture.md](docs/architecture.md)

### Node 1：铁路

- 状态：`planned`
- 概括：实现第一个完整交通方式，以铁路货运为主，并允许玩家操控载具。
- 玩法与实现细则：
  - 实现轨道、站点、车辆和装卸货的基础玩法闭环。
  - 建立可测试的铁路领域模型与路线运行逻辑。
  - 提供玩家进入、控制和观察列车状态的基础交互。
  - 同步定义铁路系统的 KubeJS 事件、绑定和类型包装接入点。
- 最小实现范围：玩家可以布置基础设施、生成列车并完成一次货运或手动驾驶闭环。
- 文档：[docs/roadmap/rail.md](docs/roadmap/rail.md)

## 4. 关联文档

- [docs/architecture.md](docs/architecture.md)：通用工程基线与架构决策。
- [docs/design-principles.md](docs/design-principles.md)：项目设计原则。
- [docs/roadmap/rail.md](docs/roadmap/rail.md)：铁路细则。