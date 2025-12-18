# 文件结构说明

为了更好地组织代码，我们对文件结构进行了重新整理，将不同的厨具分离到各自的包中。

## 新的文件结构

```
src/main/java/com/fyoyi/betterfood/
├── block/
│   ├── cooking_pan/
│   │   └── SimpleFoodBlock.java
│   ├── large_pot/
│   │   └── LargePotBlock.java
│   └── ModBlocks.java
├── item/
│   ├── cooking_pan/
│   │   └── PotBlockItem.java
│   ├── large_pot/
│   │   └── LargePotBlockItem.java
│   └── ModItems.java
├── client/
│   ├── renderer/
│   │   ├── cooking_pan/
│   │   │   └── PotItemRenderer.java
│   │   ├── large_pot/
│   │   │   └── LargePotItemRenderer.java
│   │   └── PotBlockRenderer.java
│   └── ClientModEvents.java
├── block/entity/
│   ├── ModBlockEntities.java
│   └── PotBlockEntity.java
└── 其他文件...
```

## 说明

1. **烹饪锅 (Cooking Pan)**: 所有与平底锅相关的文件都放在 `cooking_pan` 包中
2. **大炖锅 (Large Pot)**: 所有与大炖锅相关的文件都放在 `large_pot` 包中
3. **通用渲染器**: `PotBlockRenderer` 是通用的方块实体渲染器，用于渲染锅内物品，所以保留在 `renderer` 包中
4. **注册类**: `ModBlocks` 和 `ModItems` 作为注册中心，保留在原来的包中

这样的结构使得代码更易于维护和扩展，当需要添加新的厨具时，只需创建新的包并添加相应文件即可。