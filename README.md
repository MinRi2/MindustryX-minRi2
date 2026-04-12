<img src=assets/icon.png height="64"> <img src=assets/sprites-override/ui/logo.png height="64">


![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/TinyLake/MindustryX/build.yml?label=Building)  ![GitHub Release](https://img.shields.io/github/v/release/TinyLake/MindustryX?label=Latest%20Version&labelColor=blue&color=green&link=https%3A%2F%2Fgithub.com%2FTinyLake%2FMindustryX%2Freleases)  ![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/TinyLake/MindustryX/total?label=Downloads)

## MindustryX

- 新一代Mindustry分支，目标是打造一个 **高质量、更加开放** 的第三方生态。
- 目前MDTX生态包括：客户端功能与优化，服务端优化，API拓展。

### 版本号规则
正式版：`{month}.{code}`, `code` 是每个分支的编译序列码。例如  `2025.08.X15`
预览版：`{date}.{code}-{branch}`，前三位是发布日期，第四位是构件号。后面是编译分支。例如`2024.05.25.238-client-wz`

### 安装方式
正式版在 [Releases](https://github.com/TinyLake/MindustryX/releases) 中下载对应平台的MDTX
其中：
* apk为安卓版
* `desktop.jar`为桌面版，
* `loader.jar`为Mod版 **[推荐]**  
(可用原版启动，已支持pc,steam,android全平台)
### 发布类型
* apk为安卓版
* jar为桌面版
* loader.jar为Mod版 **[推荐]**  
(可用原版启动，已支持pc,steam,android全平台)

### 客户端功能
为了减少迁移不适，客户端涵盖了 **绝大部分学术端功能** ，并进行大量整理和优化。 除此之外已有大量MDTX原创功能与性能优化。

详见 [MDTX wiki](https://github.com/TinyLake/MindustryX/wiki) 或者查阅 **[Patches](./patches)**


**Loader 需要作为mod导入游戏**

### 贡献代码
1. 初始化项目:
    * 克隆使用 recursive 选项: `git clone --recursive https://github.com/TinyLake/MindustryX.git`
    * 或者在项目目录执行: `git submodule update --init --depth=10`
2. 应用 Patch 文件: 在 MDTX 根目录运行 `bash ./scripts/applyPatches.sh`
3. 选择正确修改层:
    * `work/`：Mindustry 上游展开源码与主要开发位置
    * `Arc/`：Arc 上游展开源码
    * 根目录 `src/`、`assets/`、`buildPlugins/`：MindustryX 自有源码与资源/构建逻辑
4. 修改并提交:
    * 改动 `work/` / `Arc/` 时，在对应目录内提交源码修改
    * 改动根目录文件时，在根目录提交
5. 生成 Patch 文件:
    * 如果改动触达 `work/` 或 `Arc/`，回到根目录运行 `bash ./scripts/genPatches.sh`
    * 如果只改根目录 `src/`、`assets/`、`buildPlugins/`、脚本或文档，则不需要单独回生 patch
6. 在 MDTX 根目录提交生成出的 patch 或根目录改动

```shell
  git clone --recursive https://github.com/TinyLake/MindustryX.git && cd MindustryX
  git submodule update --init --depth=10
  bash ./scripts/applyPatches.sh
  # Modify and commit inside work/Arc, or commit root-owned files in root
  bash ./scripts/genPatches.sh
  # Commit generated patches or other root changes in repository root, then push and open PR.
```

有开发能力的可私聊WZ加入开发群

### MindustryX Client
* More Api for `mods`
* Better performance
* Better Quality-of-Life(QoL)
* Compatible with official client
* More aggressive bug fixing and experience new feature.(Release more frequently than the upstream)

### Version rule
Like `2024.05.25.238-client-wz` means `{date}.{code}-{branch}`, `code` increment each ci build.

### Features
See `./patches/`.

## Contribution
1. Run `bash ./scripts/applyPatches.sh` in repository root after submodules are ready.
2. Make and commit source changes in `work/` / `Arc/`, or commit root-owned files in repository root.
3. If you changed `work/` or `Arc/`, run `bash ./scripts/genPatches.sh` in repository root, then commit the regenerated patches in root.

## Star History

<a href="https://www.star-history.com/#TinyLake/MindustryX&Date">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=TinyLake/MindustryX&type=Date&theme=dark" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=TinyLake/MindustryX&type=Date" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=TinyLake/MindustryX&type=Date" />
 </picture>
</a>
