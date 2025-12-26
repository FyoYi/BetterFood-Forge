package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PotBlockEntity extends BlockEntity implements Container {

    // 物品列表 (4层)
    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);

    // === 翻炒动画变量 ===
    private int flipTimer = 0;
    public static final int FLIP_ANIMATION_DURATION = 10;
    private int flipCount = 0; // 用于渲染器的随机种子

    // === 烹饪配置 ===
    // 效率: 底层(0) 100%, 二层(1) 50%, 三层(2) 20%, 顶层(3) 0%
    private static final float[] LAYER_EFFICIENCY = {1.0f, 0.5f, 0.2f, 0.0f};
    // 基础火力: 每tick 0.1% (即每秒 2%)
    private static final float BASE_HEAT_PER_TICK = 0.1f;

    // NBT 标签常量
    public static final String NBT_COOKED_PROGRESS = "BetterFood_CookedProgress";

    public PotBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.POT_BE.get(), pPos, pBlockState);
    }

    // === 核心 Tick 方法 ===
    public static void tick(Level level, BlockPos pos, BlockState state, PotBlockEntity pEntity) {
        // 1. 翻炒动画逻辑
        if (pEntity.flipTimer > 0) {
            // 动画进行到一半时，交换物品顺序
            if (pEntity.flipTimer == FLIP_ANIMATION_DURATION / 2) {
                pEntity.cycleItemsOrder();
            }
            pEntity.flipTimer--;

            // 动画结束时，更新计数器 (改变渲染随机位置)
            if (pEntity.flipTimer == 0) {
                pEntity.flipCount++;
                pEntity.markUpdated();
            }
        }

        // 2. 加热逻辑 (仅服务端运行)
        if (!level.isClientSide) {
            if (isHeated(level, pos)) {
                pEntity.applyHeat();
            }
        }
    }

    // === 加热核心逻辑 ===
    private void applyHeat() {
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            // 获取该层的效率
            float efficiency = (i < LAYER_EFFICIENCY.length) ? LAYER_EFFICIENCY[i] : 0.0f;
            if (efficiency <= 0) continue; // 顶层不加热

            CompoundTag nbt = stack.getOrCreateTag();
            float current = nbt.getFloat(NBT_COOKED_PROGRESS); // 默认为0

            // 上限 120%
            if (current < 120.0f) {
                current += BASE_HEAT_PER_TICK * efficiency;

                // 逻辑优化：如果熟度极低(<=0)，移除标签以便堆叠
                if (current <= 0.0f) {
                    if (nbt.contains(NBT_COOKED_PROGRESS)) {
                        nbt.remove(NBT_COOKED_PROGRESS);
                        if (nbt.isEmpty()) stack.setTag(null);
                    }
                } else {
                    // 写入精确数值
                    nbt.putFloat(NBT_COOKED_PROGRESS, current);
                }
                changed = true;
            }
        }

        if (changed) {
            setChanged();
            // 每秒同步一次给客户端 (用于更新 HUD 和 物品提示)
            if (this.level.getGameTime() % 20 == 0) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

     //出锅逻辑：根据配方生成菜肴
    public ItemStack serveDish() {
        if (level == null || items.isEmpty()) return ItemStack.EMPTY;

        // 1. 检查锅是否为空
        boolean hasItem = false;
        for(ItemStack s : items) {
            if(!s.isEmpty()) { hasItem = true; break; }
        }
        if (!hasItem) return ItemStack.EMPTY;

        // 2. 获取所有烹饪配方
        List<com.fyoyi.betterfood.recipe.CulinaryRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(com.fyoyi.betterfood.recipe.ModRecipes.CULINARY_TYPE.get());

        ItemStack result = ItemStack.EMPTY;

        // 3. 遍历匹配
        for (com.fyoyi.betterfood.recipe.CulinaryRecipe recipe : recipes) {
            if (recipe.matches(this, level)) {
                // 匹配成功！生成菜肴
                result = recipe.assemble(this, level.registryAccess());
                break;
            }
        }

        // 4. 如果没有匹配到 -> 变成蘑菇煲 (黑暗料理)
        if (result.isEmpty()) {
            result = new ItemStack(net.minecraft.world.item.Items.MUSHROOM_STEW);
            result.setHoverName(net.minecraft.network.chat.Component.literal("奇怪的杂烩"));
        }

        // 5. 清空锅内物品
        items.clear();
        for(int i=0; i<items.size(); i++) items.set(i, ItemStack.EMPTY);
        markUpdated();

        return result;
    }

    // === 交互逻辑：放入物品 ===
    public boolean pushItem(ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                ItemStack toAdd = stack.copy();
                toAdd.setCount(1);

                // === 数据互通逻辑 ===
                // 如果放入的物品没有动态熟度NBT，尝试从配置读取默认熟度
                CompoundTag nbt = toAdd.getOrCreateTag();
                if (!nbt.contains(NBT_COOKED_PROGRESS)) {
                    float initialVal = getInitialCookedness(toAdd);
                    if (initialVal > 0) {
                        nbt.putFloat(NBT_COOKED_PROGRESS, initialVal);
                    }
                }

                items.set(i, toAdd);
                markUpdated();
                return true;
            }
        }
        return false;
    }

    public ItemStack popItem() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack stack = items.get(i).copy();
                items.set(i, ItemStack.EMPTY);
                markUpdated();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // === 辅助方法：从配置读取初始熟度 ===
    private float getInitialCookedness(ItemStack stack) {
        try {
            Set<String> tags = FoodConfig.getFoodTags(stack);
            for (String tag : tags) {
                if (tag.startsWith("熟度:")) {
                    String val = tag.substring(3).replace("%", "").trim();
                    return Float.parseFloat(val);
                }
            }
        } catch (Exception e) {
            return 0.0f;
        }
        return 0.0f;
    }

    // === 热源检测 ===
    private static boolean isHeated(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)) return true;
        // 必须点燃的营火
        if (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT)) return true;
        return false;
    }

    // === 动画逻辑：循环移位 (底->顶) ===
    private void cycleItemsOrder() {
        List<ItemStack> temp = new ArrayList<>();
        for (ItemStack stack : items) if (!stack.isEmpty()) temp.add(stack);
        if (temp.size() < 2) return;

        ItemStack bottomItem = temp.remove(0);
        temp.add(bottomItem);

        this.clearContent();
        for (int i = 0; i < temp.size(); i++) items.set(i, temp.get(i));
        markUpdated();
    }

    // === 数据同步与保存 ===
    public void markUpdated() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        ContainerHelper.saveAllItems(pTag, items);
        pTag.putInt("FlipCount", flipCount);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        items.clear();
        ContainerHelper.loadAllItems(pTag, items);
        if (pTag.contains("FlipCount")) flipCount = pTag.getInt("FlipCount");
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, items);
        nbt.putInt("FlipCount", flipCount);
        // 只要有物品就存NBT，保证熟度数据不丢失
        boolean hasItem = false;
        for(ItemStack s : items) if(!s.isEmpty()) hasItem = true;
        if (hasItem) stack.addTagElement("BlockEntityTag", nbt);
    }

    // === Getter/Setter & Container 实现 (为了匹配 Recipe 接口) ===
    public int getFlipCount() { return flipCount; }
    public void triggerFlip() { if (this.flipTimer == 0) this.flipTimer = FLIP_ANIMATION_DURATION; }
    public float getFlipProgress(float partialTick) { if (flipTimer <= 0) return 0.0f; return (flipTimer - partialTick) / FLIP_ANIMATION_DURATION; }
    public NonNullList<ItemStack> getItems() { return items; }

    // Container 接口最简实现 (Recipe 匹配需要)
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { for (ItemStack item : items) if (!item.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int index) { return items.get(index); }
    @Override public ItemStack removeItem(int index, int count) { return ContainerHelper.removeItem(items, index, count); }
    @Override public ItemStack removeItemNoUpdate(int index) { return ContainerHelper.takeItem(items, index); }
    @Override public void setItem(int index, ItemStack stack) { items.set(index, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }
}