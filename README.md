# Better Food

<!-- Navigation / 导航 / ナビゲーション -->
[English](#english) | [简体中文](#简体中文) | [日本語](#日本語)

---

<a name="english"></a>
## English

This is a **Forge** mod for **Minecraft 1.20.1**. It aims to revolutionize the Minecraft food system by introducing physics-based cooking, realistic decay, and immersive interaction without GUIs.

### 🍳 Key Features

#### 1. High-Performance Decay System
*   **Lag-free Logic**: Uses a timestamp mechanism to calculate decay, completely solving the "hand shaking" animation and server lag issues found in traditional mods.
*   **Universal Support**: Supports decay in inventories, chests, and dropped items.
*   **Smart Stacking**: **Right-click** to intelligently merge items of different freshness and calculate the weighted average shelf life.
*   **Visual Feedback**: Displays freshness via the durability bar and detailed countdowns in the item tooltip.

#### 2. Immersive Physical Cooking
*   **No GUIs**: Say goodbye to slot-based cooking. Place ingredients directly into the cookware.
*   **Cooking Pan**: Supports up to 4 ingredients. Use a **Spatula** to flip food with a physics-based parabolic animation.
*   **Oil System**: Use the Spatula to transfer oil from Oil Bowls to the Pan to prevent burning and change cooking speeds.
*   **Large Pot**: Supports liquids and boiling mechanics (Currently in development).

#### 3. Realistic Food Preparation
*   **Cutting Board & Knife**: Process raw ingredients on a cutting board. Features a 3-stage cutting progress with **Squash & Stretch** animations and particle feedback for satisfying "impact."
*   **Portable Design**: Shift + Right-click to pick up a Cutting Board *with* the food and progress still on it.
*   **Visual Rendering**: Items are rendered physically on the board, stacking naturally or spreading out based on quantity.

#### 4. Dynamic Food Attributes
*   **Cookedness**: Food is no longer just "Raw" or "Cooked." It tracks a precise heat level (0% - 100%+).
*   **Dynamic Recipes**: Recipes are based on **Categories** (e.g., Meat, Vegetable) and **Doneness** rather than fixed Item IDs, allowing for flexible culinary combinations.
*   **Smart Configuration**: Use `/betterfood menu` to configure shelf life and attributes in-game.

### 📧 Feedback & Contact

If you encounter any bugs or have suggestions, please email me at:
**tanxiaocdut@gmail.com**

### 🗺️ Roadmap

*   [x] **Decay System**: Timestamps, Tooltips, Smart Stacking.
*   [x] **Basic Cooking**: Pan, Spatula, Knife, Cutting Board.
*   [ ] **Preservation**: Double-layer refrigerator (freezer/fridge) and thawing mechanics.
*   [ ] **Advanced Cooking**: Deep frying, steaming, and plating system (placing food on plates).
*   [ ] **Visual Aging**: Food textures changing dynamically with freshness.

---

<a name="简体中文"></a>
## 简体中文

这是一个适用于 **Minecraft 1.20.1** 的 **Forge** 模组。本模组旨在通过无 GUI 的物理交互、真实的腐烂机制，提供一个高性能且极具沉浸感的烹饪体验。

### 🍳 主要功能

#### 1. 高性能腐烂系统
*   **无卡顿机制**：采用时间戳机制计算腐烂，彻底解决传统模组导致的“手持物品抖动”和服务器卡顿问题。支持背包、箱子及掉落物全场景腐烂。
*   **智能堆叠**：左键保持原版交换，**右键**同类食物可智能合并并计算平均保质期。
*   **可视化 UI**：通过耐久条和悬停提示（Tooltip）实时显示食物的新鲜度与精确倒计时。

#### 2. 沉浸式物理烹饪
*   **去 GUI 化**：告别传统的界面操作，所有食材直接放入锅具。
*   **平底锅**：支持4层食材堆叠。使用**锅铲**右键可触发带有物理抛物线动画的“翻炒”动作。
*   **油脂系统**：使用锅铲从油碗中取油并加入锅中，改变烹饪效率与声音反馈。
*   **大炖锅**：支持流体渲染与炖煮逻辑（开发中）。

#### 3. 拟真食材处理
*   **菜板与厨刀**：在菜板上处理食材。拥有三段式切割进度，伴随**挤压形变（Squash & Stretch）**的打击感动画与粒子效果。
*   **便携搬运**：蹲下+空手右键，可以将菜板连同上面的食材、切割进度一起完整搬起。
*   **动态渲染**：菜板上的食物不再是贴图，而是真实的物品模型渲染，支持堆叠与自然散落效果。

#### 4. 动态属性系统
*   **熟度机制**：食物不再只有“生”和“熟”，而是拥有精确的受热数值（0% - 120%）。
*   **模糊配方**：烹饪结果基于食材的**分类**（如：肉类、蔬菜）和**熟度**判定，而非死板的物品ID，允许玩家自由发挥。
*   **高度自由**：提供游戏内菜单 (`/betterfood menu`) 快速调整任意食物的保质期与属性。

### 📧 反馈与联系

如果你在使用过程中遇到 BUG，或者有好的建议，欢迎发送邮件至：
**tanxiaocdut@gmail.com**

### 🗺️ 开发计划

*   [x] **腐烂系统**：时间戳算法、UI显示、智能堆叠。
*   [x] **基础烹饪**：平底锅、锅铲、菜板、厨刀交互。
*   [ ] **保鲜设施**：加入双层冰箱（冷藏/冷冻）及解冻机制。
*   [ ] **进阶烹饪**：油炸、蒸煮以及摆盘系统。
*   [ ] **状态反馈**：食物材质随新鲜度动态变化（发霉/变色）。

---

<a name="日本語"></a>
## 日本語

これは **Minecraft 1.20.1** 用の **Forge** Modです。GUIを使用しない物理的なインタラクションやリアルな腐敗メカニズムを通じて、高パフォーマンスかつ没入感のある料理体験を提供することを目的としています。

### 🍳 主な機能

#### 1. 高パフォーマンスな腐敗システム
*   **ラグのないメカニズム**: タイムスタンプ方式で腐敗を計算し、従来のModで見られた「手持ちアイテムの振動」やサーバーの遅延問題を完全に解決します。インベントリ、チェスト、ドロップアイテムなど、あらゆる状況での腐敗をサポートします。
*   **スマートスタッキング**: 左クリックはバニラ通りの入れ替えですが、**右クリック**で同じ食品をスマートに統合し、加重平均で賞味期限を計算します。
*   **可視化されたUI**: 耐久値バーとツールチップによるリアルタイムな鮮度表示および正確なカウントダウンを提供します。

#### 2. 没入感のある物理料理
*   **GUIの廃止**: 従来のスロットベースの操作に別れを告げ、食材を調理器具に直接投入します。
*   **フライパン**: 最大4つの食材をスタック可能。**フライ返し（Spatula）**を右クリックすることで、物理演算に基づいた放物線を描く「返し（フリップ）」アクションが発生します。
*   **油システム**: フライ返しを使って油の入ったボウルから油をすくい、フライパンに加えることで、焦げ付き防止や調理効率、調理音を変化させます。
*   **大鍋**: 液体の描画と煮込みロジックをサポート（現在開発中）。

#### 3. リアルな食材加工
*   **まな板と包丁**: まな板の上で食材を加工します。3段階のカット進行度を持ち、**スクワッシュ＆ストレッチ（押し潰しと伸縮）**のアニメーションやパーティクル効果による心地よい「打撃感」を伴います。
*   **ポータブル設計**: シフト＋素手で右クリックすると、食材やカット進行度を保持したまま、まな板ごと持ち運ぶことができます。
*   **ダイナミックレンダリング**: まな板上の食材はテクスチャではなく、実際のアイテムモデルとして物理的に描画され、数量に応じたスタックや自然な散らばりを表現します。

#### 4. 動的な属性システム
*   **加熱度メカニズム**: 食材は単なる「生」か「調理済み」ではなく、正確な受熱数値（0% - 100%+）を持ちます。
*   **動的なレシピ**: 調理結果は固定されたアイテムIDではなく、食材の**分類**（例：肉類、野菜）と**加熱度**に基づいて判定され、柔軟な料理の組み合わせを可能にします。
*   **高い自由度**: ゲーム内メニュー (`/betterfood menu`) を使用して、任意の食材の賞味期限や属性を素早く調整できます。

### 📧 フィードバックと連絡先

バグを発見した場合や、良い提案がある場合は、以下のメールアドレスまでご連絡ください：
**tanxiaocdut@gmail.com**

### 🗺️ ロードマップ

*   [x] **腐敗システム**: タイムスタンプ、ツールチップ、スマートスタッキング。
*   [x] **基本調理**: フライパン、フライ返し、包丁、まな板。
*   [ ] **保存設備**: 2層式冷蔵庫（冷蔵/冷凍）および解凍メカニズム。
*   [ ] **高度な調理**: 揚げ物、蒸し料理、および盛り付けシステム。
*   [ ] **視覚的な経年変化**: 鮮度に応じた食材テクスチャの動的な変化。
