# 错误分析与遥测

## 错误日记分析标准

- 这一节的目标不是机械判断"栈里有没有 `mindustryX.*`"，而是从崩溃日志里提炼出真正能推动定位、复现、修复和质量提升的线索。
- 先看日志是否足够形成有效问题：版本、平台、触发步骤、mod 列表、存档/坏档信息、崩溃日志本体是否齐全；缺这些关键信息时，先标记为"信息不足"，不要过早下结论。
- 先找真正抛异常的地方，不要只看栈里出现过什么类；要分清"根因""传播链"和"表层崩点"，不要把最后一帧默认当根因。
- 分析时先提炼关键信号：异常类型、首个可信抛点、直接触发条件、涉及的数据对象、是否带 mod/坏档/玩家输入/坏内容定义、是否可稳定复现。
- 先分清"和 MindustryX 真正有关"与"只是经过 MindustryX"。异常根因在 `mindustryX.*`、`mindustryX.*` 是主动功能入口、或 `mindustryX.*` 先制造非法状态随后在 `mindustry.*` 中崩溃时，应优先沿 MindustryX 方向继续分析。
- 只是经过包装层，如 `RenderExt.onGroupDraw/onBlockDraw`，或只是正常引导链，如 `loader.Main` / `DesktopImpl`，通常不是有效根因线索，不要因为栈里出现过就误判方向。
- `mindustryX.*` 只是入口转发、包装或正常经过路径，而坏数据明显来自玩家输入、第三方 mod、坏档或坏内容定义时，分析时默认视为噪声，不要优先沿着 MindustryX 方向跟修。
- 明显属于玩家输入、第三方 mod、坏档或坏内容定义时，分析时默认视为噪声，不要优先投入 MindustryX 修复精力。
- 同一根因反复报错时，按一类问题归并，不要把重复日志当多个独立 bug；已有 issue 或已修过的问题，不因为日志文本不同就重新拆成新问题。
- 能直接定位到触发条件的，顺手记下触发条件、影响范围、是否可稳定复现，以及它会阻塞哪类功能；这些信息通常比"栈里出现了哪些类名"更有用。
- 建议记录格式：异常 / 关键线索 / 可能归属 / 触发条件 / 复现性 / 影响范围 / 状态。

## BetterStack 遥测查询

MindustryX 通过 HTTP 上报崩溃日志到 BetterStack。生产和分析时使用以下数据源：

### 数据源

| 用途 | Source ID | Table | 保留期 |
|---|---|---|---|
| 日志（含崩溃栈） | 1367486 (MindustryX client, http) | `t385879.mindustryx_client` | **3 天** |
| Java 错误归组 | 2355770 (MindustryX client, java_errors) | `t385879.mindustryx_client_2` | 90 天 |

> **注意**：日志只保留 3 天，超过 3 天的数据无法查询。崩溃分析以 log source (1367486) 为主，errors application (2355770) 创建较晚（2026-04-12），历史数据少。

### 查询方式

```sql
-- 热数据（近 30 分钟）
SELECT dt, raw FROM remote(t385879_mindustryx_client_logs)
WHERE dt > now() - INTERVAL 30 MINUTE

-- 冷数据（30 分钟以上），必须加 _row_type = 1
SELECT dt, raw FROM s3Cluster(primary, t385879_mindustryx_client_s3)
WHERE _row_type = 1 AND dt BETWEEN '2026-04-16' AND '2026-04-20'
```

### raw JSON 关键字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `cause` | String | 完整异常栈（有值 = 崩溃上报） |
| `likelyCause` | String | 简短根因摘要 |
| `deviceId` | String | 设备 UUID |
| `userId` | String | 用户 ID（SHA1） |
| `version` | String | 版本字符串，如 `release build 157.3\nMindustryX 2026.04.18.B450` |
| `env.os` | String | 操作系统 |
| `env.Android` | Int64 | **Android SDK 级别**（非布尔值），如 31=Android 12，null=桌面 |
| `env.isLoader` | Bool | 是否 loader 模式 |
| `env.javaVersion` | String | Java 版本 |
| `mods` | Object | 已启用 mod 及版本，如 `{"mindustryx":"2026.04.X32","kotlin":"2.3.20"}` |
| `disabledMods` | Object | 被禁用的 mod 及原因 |
| `state.state` | String | 游戏状态：menu/playing/paused |
| `state.mapName` | String | 当前地图 |
| `settings` | Object | 游戏设置项 |

### 常用查询模式

```sql
-- 1. 筛选纯崩溃（排除 likelyCause 摘要）
WHERE JSONExtract(raw, 'cause', 'Nullable(String)') IS NOT NULL
  AND JSONExtract(raw, 'likelyCause', 'Nullable(String)') IS NULL

-- 2. 提取异常首行（用于分组统计）
extractAll(assumeNotNull(JSONExtract(raw, 'cause', 'String')), '([^\n]+)')[1] AS exception_head

-- 3. 过滤 2026.04+ 版本
AND JSONExtract(raw, 'version', 'Nullable(String)') LIKE '%2026.04%'

-- 4. 按异常类型分组统计（带设备数、版本、时间范围）
SELECT
  extractAll(...)[1] AS exception_head,
  count(*) AS cnt,
  uniq(JSONExtract(raw, 'deviceId', 'Nullable(String)')) AS devices,
  groupUniqArray(JSONExtract(raw, 'version', 'Nullable(String)')) AS versions,
  min(dt) AS first_seen,
  max(dt) AS last_seen
FROM s3Cluster(...)
WHERE ...
GROUP BY exception_head
ORDER BY cnt DESC
```

### 注意事项

- `JSONExtract` 必须用 `Nullable(String)` 等 Nullable 类型，否则字段缺失时查询报错
- `extractAll` 不能接受 Nullable 类型，需要先用 `assumeNotNull()` 包装
- `groupUniqArray` 用于聚合去重版本/mod 列表，比 `anyLast` 更全面
- 查 mod 维度时注意 `mods` 是 JSON Object，直接用 `JSONExtract(raw, 'mods', 'Nullable(String)')` 拿到字符串表示即可做 LIKE 过滤

- **栈中没有 `mindustryX.*` 不代表与 MDTX 无关。** MDTX 通过 `patches/` 中的 .patch 文件直接修改上游源码（`work/` 和 `Arc/`），patch 改过的文件编译后的栈帧显示的是上游类名。分析每个高频崩溃前，必须先 grep `patches/` 目录确认该文件是否被任何 .patch 修改过，再阅读对应 patch 的 diff 内容判断 MDTX 的改动是否引入了崩溃条件。
- 判断流程：(1) 栈中是否有 `mindustryX.*` → 有则优先排查 MDTX；(2) 栈中无 `mindustryX.*` 但崩溃文件被 patch 改过 → 读 patch diff，看改动的行是否在崩溃调用链上；(3) 都排除后才归因为上游/mod。
