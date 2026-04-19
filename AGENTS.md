# MindustryX 开发指南

## 项目与工作流

- 本仓库通过 `patches/` 追踪对上游 `Arc/` 和 `work/` 的修改。
- 这是一个 patch-first 仓库；真正参与构建和运行验证的是 `work/` 与 `Arc/`，不是根目录的 patch 文本本身。
- `src/` 是 MindustryX 自有源码层/补丁层源码，但不是这个仓库唯一的构建事实来源；不要把它当成“唯一真源”。
- 根目录 `src/` 会被 `work/core` 构建直接作为额外源码目录编译，不是手工同步到 `work/` 的镜像目录；判断改动归属时要按“源码所有权”而不是“文件是否位于 `work/` 内”来区分。
- 不要把 `patches/*.patch` 当主要编辑对象；除非任务明确要求，否则优先改展开后的源码再回生成 patch。
- **不要只改 `work/` 或 `Arc/` 后直接提交根仓库**，否则变更不会被正确追踪。
- `scripts/genPatches.sh` 只会从 `work/` 和 `Arc/` 的提交重新生成 `patches/`；如果本次只修改了根目录 `src/`、`assets/`、`buildPlugins/`、脚本或文档，不需要为了形式单独回生 patch。只要改动触达 `work/` 或 `Arc/`，交付前仍必须重新生成并检查 `patches/`。
- patch 改动应最小化；能在 `src/` 通过 hook、旁路或覆盖实现且不依赖 patch 时，优先放在 `src/`。
- `src/mindustryX/` 更偏向 MindustryX 自有功能、loader、hook、独立 UI/工具逻辑；`src/arc/` 更偏向 Arc 侧覆盖实现。涉及上游游戏逻辑、现有类行为改动、构建接线或必须贴近上游源码的修改时，可以直接落在 `work/` / `Arc/` 的展开源码侧；不要为了“强行留在 src”把实现做复杂，优先选择更容易实现且可维护的方案，但 patch 仍应保持最小化。
- 同一个功能的 patch 应尽量合并，不要为同一功能反复新建 patch。
- 开始改动前先确认本次任务落在哪一层：`work/`、`Arc/`、`src/`、脚本或 CI；不要混层修改。
- 根目录仓库和 `work/`/`Arc/` 是不同 Git 上下文；常规贡献流是在 `work/` / `Arc/` 中完成源码修改与对应 commit，再回根目录执行 `genPatches.sh` 并提交生成出的 patch。执行 `git`、`gh`、看 diff、提交前先确认当前目录，不要把根目录提交和 `work/`/`Arc/` 内提交混为同一个 Git 操作。
- `work/` / `Arc/` 中会进入 patch 链的新 commit，其 commit message 需要尽量遵循 `patches/README.md` 中的 V3 命名规则，因为回生后的 patch 标题与文件名会直接继承这些提交信息；旧 patch 保持现状，不要为了统一命名顺手重命名历史 patch。
- 如果遇到 `safe.directory`、子模块 revision 缺失、Gradle 依赖下载失败、GitHub API/CLI 瞬时 `EOF`，优先视为环境或工具链问题，不要直接归因于代码逻辑。

### 初始化

```bash
git clone --recursive https://github.com/TinyLake/MindustryX.git
cd MindustryX
git submodule update --init --depth=10
bash ./scripts/applyPatches.sh
```

### 构建

- 要求：**JDK 17**、**Gradle 8.12**
- 项目多数 Java 模块仍以 Java 8 目标发布，新增代码不要随意引入高版本 Java API。
- 验证命令默认在 `work/` 下执行；只有生成补丁、应用补丁等操作才回到根目录。
- 如果验证被网络、依赖下载或本地环境阻塞，要区分“代码失败”和“环境失败”。
- 修改 `work/` 或 `Arc/` 后，交付前要检查生成出来的 `patches/` 结果。
- 临时产物只能用于快速验证，不能代替 patch 再生成与正式构建链路。
- 日常测试：

```bash
cd work
gradle desktop:build
gradle android:build
```

- 完整构建：

```bash
cd work
gradle --parallel desktop:dist server:dist core:genLoaderModAll android:assembleRelease
```

## 关键目录

- `work/`：上游 Mindustry 源码与主要开发位置
- `Arc/`：上游 Arc 源码
- `patches/picked/`：前置挑选补丁，应用顺序早于 `patches/client/`
- `patches/client/`：MindustryX 客户端补丁
- `patches/arc/`：Arc 补丁
- `src/mindustryX/`：MindustryX 自有源码
- `src/arc/`：补丁层中的 Arc 侧源码/覆盖实现
- `assets/`：资源、图标、bundle、协议文本与 mod 元数据
- `buildPlugins/`：Gradle 构建逻辑与仓库自定义任务
- `scripts/applyPatches.sh`：应用补丁
- `scripts/genPatches.sh`：生成补丁

## 修改原则

- 优先做**最小改动 / 最少行数**修复。
- 不要把顺手重构混进 bugfix。
- 能在调用点拦截，就不要扩大影响面。
- 优先复用现有实现和入口，不要为了局部问题额外引入 helper、包装层或重复逻辑。
- 初始化副作用要显式，不要在隐蔽路径里偷偷注册 hook 或改全局状态。
- 数据归一化、默认值修正、兼容处理应尽量放在数据拥有者附近，不要依赖额外调用兜底。
- 涉及消息格式、协议、兼容逻辑时，要显式划清版本边界，默认行为优先保持稳定。
- 功能入口要克制；已有明确入口时，不要额外增加平行 UI 路径。
- UI 适配优先复用现有 `Table` 和现有布局能力，不要靠硬编码宽高阈值凑布局。
- 信息展示优先用结构化组件，不要把多段信息、状态、颜色和换行需求都堆进字符串拼接；UI 展示优先对组件本身设置颜色、样式和布局，绘制逻辑使用 `Draw.color(...)` 等绘制 API，不要为了 UI 文本表现去拼接富文本或颜色标记字符串，例如 `"[accent]" + text + "[]"`。
- 用户可见文案应走 bundle/资源文件，不要随意硬编码。
- `i(...)` 是项目里的内联国际化 helper，当前定位是“仅用于 UI 构建中的简单文本替换”；简单文本不应包含变量。不要滥用它，正式、长期存在、可复用的文案优先写入 bundle。
- `i(...)` 返回值不允许进行字符串拼接；含变量文案优先整句写成 bundle 项并使用占位符格式化，不要先翻译半句再拼接文件名、标点或状态值。现有代码中，动态文案更常见的形态是 `VarsX.bundle.xxx(...)`、`Core.bundle.format(...)` 或其他 bundle 格式化入口；新增变量文案优先延续这种模式，而不是继续扩张 `i(...)` 的职责。
- 同一组件相关的 i18n 文案应尽量集中维护，便于 review、补翻译和后续重命名。

## Review 与收尾

- Review 修改以“行为正确 + 改动边界合理”为目标，不要只追求“现在能跑”。
- GitHub review comment 可能锚定旧 diff 位置；处理前先对照当前实现，不要机械按旧行号修改。
- `gh` 或 GitHub API 的瞬时失败应先重试，再判断是否真的是权限或逻辑问题。
- 收尾操作尽量串行；不要并发执行 build、产物更新、commit、push、review 回复等步骤。
- 交付同时涉及展开源码与补丁回生时，要同时检查 `work/`/`Arc/` 中的实际修改点，以及根目录 `patches/` 的生成结果。

## 命令约定

- 在 Windows 运行仓库脚本`./scripts/`时需要使用 git-bash。
- 本仓库文档默认本地验证命令在 `work/` 下使用 `gradle ...`；CI 中同时存在安装版 Gradle 与 wrapper 的用法，除非任务明确要求，不要擅自把仓库所有命令统一改写成 `./gradlew` 或 `gradlew.bat`。

## 常见边界问题

- `scripts/updateUpstream.sh` 会 fetch 并 reset `work/`、`Arc/` 到指定上游 ref，然后在根目录提交子模块指针变化，并重新执行 `applyPatches.sh` / `genPatches.sh`；使用前先确认目标 ref、当前工作区状态，以及你是否真的要做一次上游同步。

## 错误分析与遥测

详见 [docs/error-analysis.md](docs/error-analysis.md)
