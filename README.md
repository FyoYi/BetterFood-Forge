# Better Food

<!-- 顶部导航栏 -->
[English](#english) | [简体中文](#简体中文)

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
