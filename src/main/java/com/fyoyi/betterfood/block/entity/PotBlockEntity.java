package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.config.FoodConfig;
import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import com.fyoyi.betterfood.util.CookednessHelper;
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
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PotBlockEntity extends BlockEntity implements Container {

    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int flipTimer = 0;
    public static final int FLIP_ANIMATION_DURATION = 10;
    private int flipCount = 0;

    private static final float[] LAYER_EFFICIENCY = {1.0f, 0.5f, 0.2f, 0.0f};
    private static final float BASE_HEAT_PER_TICK = 0.1f;

    public static final String NBT_COOKED_PROGRESS = "BetterFood_CookedProgress";

    public PotBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.POT_BE.get(), pPos, pBlockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PotBlockEntity pEntity) {
        if (pEntity.flipTimer > 0) {
            if (pEntity.flipTimer == FLIP_ANIMATION_DURATION / 2) pEntity.cycleItemsOrder();
            pEntity.flipTimer--;
            if (pEntity.flipTimer == 0) {
                pEntity.flipCount++;
                pEntity.markUpdated();
            }
        }

        if (!level.isClientSide) {
            if (isHeated(level, pos)) {
                pEntity.applyHeat();
            }
        }
    }

    private void applyHeat() {
        boolean changed = false;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            float efficiency = (i < LAYER_EFFICIENCY.length) ? LAYER_EFFICIENCY[i] : 0.0f;
            if (efficiency <= 0) continue;

            float current = CookednessHelper.getCurrentCookedness(stack);

            if (current < 120.0f) {
                current += BASE_HEAT_PER_TICK * efficiency;
                CookednessHelper.setCookedness(stack, current);
                changed = true;
            }
        }

        if (changed) {
            setChanged();
            if (this.level.getGameTime() % 20 == 0) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }
    }

    // === 核心：出锅匹配 ===
    public ItemStack serveDish() {
        if (level == null || isEmpty()) return ItemStack.EMPTY;

        List<CulinaryRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CULINARY_TYPE.get());

        ItemStack result = ItemStack.EMPTY;

        for (CulinaryRecipe recipe : recipes) {
            if (recipe.matches(this, level)) {
                result = recipe.assemble(this, level.registryAccess());
                break;
            }
        }

        if (result.isEmpty()) {
            result = new ItemStack(Items.MUSHROOM_STEW);
            // result.setHoverName(net.minecraft.network.chat.Component.literal("奇怪的杂烩"));
        }

        this.clearContent();
        this.markUpdated();
        return result;
    }

    // === 核心：入锅初始化 ===
    public boolean pushItem(ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                ItemStack toAdd = stack.copy();
                toAdd.setCount(1);

                // 使用CookednessHelper设置初始熟度
                float initialVal = CookednessHelper.getCurrentCookedness(toAdd);
                if (initialVal > 0) {
                    CookednessHelper.setCookedness(toAdd, initialVal);
                }

                items.set(i, toAdd);
                markUpdated();
                return true;
            }
        }
        return false;
    }

    private float getInitialCookedness(ItemStack stack) {
        return FoodConfig.getInitialCookedness(stack);
    }

    private static boolean isHeated(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(Blocks.FIRE) || below.is(Blocks.SOUL_FIRE) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK)) return true;
        if (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT)) return true;
        return false;
    }

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

    public void markUpdated() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // NBT & Sync
    @Override protected void saveAdditional(CompoundTag pTag) { super.saveAdditional(pTag); ContainerHelper.saveAllItems(pTag, items); pTag.putInt("FlipCount", flipCount); }
    @Override public void load(CompoundTag pTag) { super.load(pTag); items.clear(); ContainerHelper.loadAllItems(pTag, items); if (pTag.contains("FlipCount")) flipCount = pTag.getInt("FlipCount"); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) { this.load(pkt.getTag()); }

    public void saveToItem(ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        ContainerHelper.saveAllItems(nbt, items);
        nbt.putInt("FlipCount", flipCount);
        boolean hasItem = false;
        for(ItemStack s : items) if(!s.isEmpty()) hasItem = true;
        if (hasItem) stack.addTagElement("BlockEntityTag", nbt);
    }

    // Getters & Container Implementation
    public int getFlipCount() { return flipCount; }
    public void triggerFlip() { if (this.flipTimer == 0) this.flipTimer = FLIP_ANIMATION_DURATION; }
    public float getFlipProgress(float partialTick) { if (flipTimer <= 0) return 0.0f; return (flipTimer - partialTick) / FLIP_ANIMATION_DURATION; }
    public NonNullList<ItemStack> getItems() { return items; }
    public ItemStack popItem() { for(int i=items.size()-1;i>=0;i--) if(!items.get(i).isEmpty()) { ItemStack s=items.get(i).copy(); items.set(i,ItemStack.EMPTY); markUpdated(); return s; } return ItemStack.EMPTY; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { for (ItemStack item : items) if (!item.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int index) { return items.get(index); }
    @Override public ItemStack removeItem(int index, int count) { return ContainerHelper.removeItem(items, index, count); }
    @Override public ItemStack removeItemNoUpdate(int index) { return ContainerHelper.takeItem(items, index); }
    @Override public void setItem(int index, ItemStack stack) { items.set(index, stack); if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize()); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); }
}