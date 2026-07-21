## Bug 修复 & UI 微调（冲煮手法管理）

- 移除冲煮手法列表顶部的「+ 新建手法」引导卡片（与 TopAppBar 右上角 + 按钮重复），空列表提示改为「点击右上角 + 新建」
- 修复编辑冲煮手法后排序被置顶的问题：保存时遗漏 sortOrder 导致被重置为 0、且 updatedAt 刷新为当前时间，在 `sortOrder ASC, updatedAt DESC` 排序下跳到顶部；现编辑时保留原始 sortOrder 与 updatedAt，位置保持不变（新建仍走原逻辑）

## 版本信息

- versionCode: 75 → 76
- versionName: 2.6.0 → 2.6.1
