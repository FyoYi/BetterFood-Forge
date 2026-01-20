package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.better_food;
import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.util.TimeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CuttingBoardBlockEntity extends BlockEntity implements Container {
    public static final int INVENTORY_SIZE = 4;
    private final NonNullList<ItemStack> items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private final NonNullList<Long> itemRandomSeeds = NonNullList.withSize(INVENTORY_SIZE, 0L);

    // 存储原料的随机位置偏移
    private float rawIngredientOffsetX = 0f;
    private float rawIngredientOffsetZ = 0f;

    private int cutProgress = 0;
    private static final int MAX_CUTS_REQUIRED = 3;
    private int squashTimer = 0;
    private static final float ANIMATION_DURATION = 10.0f;

    public CuttingBoardBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.CUTTING_BOARD_BE.get(), pPos, pBlockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CuttingBoardBlockEntity be) {
        if (be.squashTimer > 0) {
            be.squashTimer--;
        }
    }

    public boolean interactKnife(Player player, ItemStack knifeStack) {
        if (level == null || isEmpty())
            return false;
        if (!isRawIngredientState())
            return false;

        this.cutProgress++;
        this.squashTimer = (int) ANIMATION_DURATION;
        this.markUpdated();

        if (!level.isClientSide) {
            if (cutProgress < MAX_CUTS_REQUIRED) {
                level.playSound(null, worldPosition, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0f,
                        0.9f + (cutProgress * 0.1f));
            } else {
                level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 1.0f,
                        1.2f);
            }

            if (level instanceof ServerLevel serverLevel) {
                ItemStack particleItem = items.get(0);
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleItem),
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5,
                        3, 0.1, 0.05, 0.1, 0.05);
            }
        }

        if (cutProgress >= MAX_CUTS_REQUIRED) {
            finishCutting(player, knifeStack);
        }
        return true;
    }

    private void finishCutting(Player player, ItemStack knifeStack) {
        if (!(level instanceof ServerLevel serverLevel))
            return;

        ItemStack inputStack = items.get(0);
        ResourceLocation lootTableId = getLootTableForInput(inputStack);
        if (lootTableId == null)
            return;

        LootTable lootTable = serverLevel.getServer().getLootData().getLootTable(lootTableId);
        LootParams lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.TOOL, knifeStack)
                .create(LootContextParamSets.CHEST);

        List<ItemStack> generatedLoot = lootTable.getRandomItems(lootParams);

        // 过滤只保留本mod和minecraft的物品，避免与其他mod冲突
        generatedLoot.removeIf(stack -> {
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return rl == null
                    || (!rl.getNamespace().equals("minecraft") && !rl.getNamespace().equals(better_food.MOD_ID));
        });

        // 记录菜谱（切菜）
        String inputItemName = inputStack.getHoverName().getString();
        if (!generatedLoot.isEmpty()) {
            String outputItemName = generatedLoot.get(0).getHoverName().getString();
            recordCuttingRecipe(player, inputItemName, outputItemName);
        }

        this.items.set(0, ItemStack.EMPTY);
        this.cutProgress = 0;

        // 计算输入新鲜度
        float inputFreshness = FreshnessHelper.getFreshnessPercentage(level, inputStack);
        float enhancedFreshness = inputFreshness * 1.1f;
        long currentTime = TimeManager.getEffectiveTime(level);

        // 获取可用的空槽位列表
        java.util.List<Integer> availableSlots = new java.util.ArrayList<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (items.get(i).isEmpty()) {
                availableSlots.add(i);
            }
        }

        // 随机打乱槽位顺序
        java.util.Collections.shuffle(availableSlots);

        // 根据随机槽位放置物品
        int slotIndex = 0;
        for (ItemStack loot : generatedLoot) {
            while (!loot.isEmpty() && slotIndex < availableSlots.size()) {
                int randomSlot = availableSlots.get(slotIndex);
                ItemStack single = loot.split(1);
                // 设置产物新鲜度
                setCuttingProductFreshness(single, enhancedFreshness, currentTime);
                items.set(randomSlot, single);
                // 为此物品设置随机seed，使用纳秒时间确保每次都不同
                itemRandomSeeds.set(randomSlot, System.nanoTime());
                slotIndex++;
            }
            if (!loot.isEmpty()) {
                // 设置掉落物新鲜度
                setCuttingProductFreshness(loot, enhancedFreshness, currentTime);
                serverLevel.addFreshEntity(new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, loot));
            }
        }
        markUpdated();
    }

    private void setCuttingProductFreshness(ItemStack stack, float enhancedFreshness, long currentTime) {
        if (!FoodConfig.canRot(stack))
            return;

        long lifetimeTicks = FoodConfig.getItemLifetime(stack.getItem());
        if (lifetimeTicks == FoodConfig.SHELF_LIFE_INFINITE) {
            // 无限保质期
            FreshnessHelper.setExpiryTime(stack, -1);
        } else {
            // 计算剩余ticks: 总保质期ticks * enhancedFreshness，但不超过总保质期
            long remainingTicks = (long) (lifetimeTicks * enhancedFreshness);
            remainingTicks = Math.min(remainingTicks, lifetimeTicks);
            long expiryTime = currentTime + remainingTicks;
            FreshnessHelper.setExpiryTime(stack, expiryTime);
        }
    }

    public boolean isRawIngredientState() {
        if (items.get(0).isEmpty())
            return false;
        for (int i = 1; i < INVENTORY_SIZE; i++) {
            if (!items.get(i).isEmpty())
                return false;
        }
        return true;
    }

    public float getSquashScaleY(float partialTick) {
        if (squashTimer <= 0)
            return 1.0f;
        float t = (squashTimer - partialTick) / ANIMATION_DURATION;
        float impact = (float) Math.sin(t * Math.PI);
        return 1.0f - (impact * 0.6f);
    }

    public float getSquashScaleXZ(float partialTick) {
        if (squashTimer <= 0)
            return 1.0f;
        float t = (squashTimer - partialTick) / ANIMATION_DURATION;
        float impact = (float) Math.sin(t * Math.PI);
        return 1.0f + (impact * 0.3f);
    }

    private ResourceLocation getLootTableForInput(ItemStack stack) {
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return rl == null ? null
                : new ResourceLocation(better_food.MOD_ID,
                        "gameplay/cutting/" + rl.getNamespace() + "_" + rl.getPath());
    }

    public boolean addItem(ItemStack stack) {
        if (isRawIngredientState() && !items.get(0).isEmpty())
            return false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                cutProgress = 0;
                markUpdated();
                return true;
            }
        }
        return false;
    }

    public void dropAllItems() {
        if (level == null || level.isClientSide)
            return;
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, items.get(i));
                items.set(i, ItemStack.EMPTY);
            }
        }
        cutProgress = 0;
        markUpdated();
    }

    @Override
    public void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        ContainerHelper.saveAllItems(t, items);
        t.putInt("CutProgress", cutProgress);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            t.putLong("RandomSeed_" + i, itemRandomSeeds.get(i));
        }
        t.putFloat("RawIngredientOffsetX", rawIngredientOffsetX);
        t.putFloat("RawIngredientOffsetZ", rawIngredientOffsetZ);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        items.clear();
        ContainerHelper.loadAllItems(t, items);
        cutProgress = t.getInt("CutProgress");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            itemRandomSeeds.set(i, t.getLong("RandomSeed_" + i));
        }
        rawIngredientOffsetX = t.getFloat("RawIngredientOffsetX");
        rawIngredientOffsetZ = t.getFloat("RawIngredientOffsetZ");
    }

    public void markUpdated() {
        setChanged();
        if (level != null)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items)
            if (!s.isEmpty())
                return false;
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return items.get(i);
    }

    @Override
    public ItemStack removeItem(int i, int c) {
        ItemStack s = ContainerHelper.removeItem(items, i, c);
        if (!s.isEmpty())
            markUpdated();
        return s;
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return ContainerHelper.takeItem(items, i);
    }

    @Override
    public void setItem(int i, ItemStack s) {
        items.set(i, s);
        markUpdated();
    }

    public long getRandomSeed(int i) {
        return itemRandomSeeds.get(i);
    }

    public void setRawIngredientPosition(float offsetX, float offsetZ) {
        this.rawIngredientOffsetX = offsetX;
        this.rawIngredientOffsetZ = offsetZ;
    }

    public float getRawIngredientOffsetX() {
        return rawIngredientOffsetX;
    }

    public float getRawIngredientOffsetZ() {
        return rawIngredientOffsetZ;
    }

    /**
     * 记录切菜菜谱到玩家菜谱书
     */
    private void recordCuttingRecipe(Player player, String inputItemName, String outputItemName) {
        // 触发菜谱解锁事件
        com.fyoyi.betterfood.event.RecipeUnlockEvent event = new com.fyoyi.betterfood.event.RecipeUnlockEvent(
                player,
                com.fyoyi.betterfood.event.RecipeUnlockEvent.RecipeType.CUTTING_BOARD,
                inputItemName + " → " + outputItemName,
                100.0f, // 切菜总是满分
                new java.util.ArrayList<>() // 切菜没有特殊的食材要求
        );

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public boolean stillValid(Player p) {
        return Container.stillValidBlockEntity(this, p);
    }

    @Override
    public void clearContent() {
        items.clear();
        markUpdated();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // [核心修改] 将进度也保存到物品的 BlockEntityTag 中
    public void saveToItem(ItemStack s) {
        // 只要有东西或者是切了一半的，都保存
        if (!isEmpty() || cutProgress > 0) {
            CompoundTag t = new CompoundTag();
            ContainerHelper.saveAllItems(t, items, true);
            t.putInt("CutProgress", cutProgress); // 保存进度
            s.addTagElement("BlockEntityTag", t);
        }
    }
}