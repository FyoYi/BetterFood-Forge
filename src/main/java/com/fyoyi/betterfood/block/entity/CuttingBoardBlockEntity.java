package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.better_food;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CuttingBoardBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public CuttingBoardBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.CUTTING_BOARD_BE.get(), pPos, pBlockState);
    }

    public boolean processCutting(Player player) {
        if (level == null || level.isClientSide || !(level instanceof ServerLevel) || isEmpty()) {
            return false;
        }
        ItemStack itemToCut = this.getItem(0);

        if (itemToCut.getItem() == Items.BEEF) {
            ResourceLocation lootTableId = new ResourceLocation(better_food.MOD_ID, "gameplay/cutting_board_beef");
            LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableId);
            if (lootTable == LootTable.EMPTY) { return false; }

            LootParams.Builder lootParamsBuilder = new LootParams.Builder((ServerLevel) this.level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                    .withParameter(LootContextParams.THIS_ENTITY, player); // <--- 修复点

            LootParams lootParams = lootParamsBuilder.create(LootContextParamSets.GIFT);
            List<ItemStack> generatedLoot = lootTable.getRandomItems(lootParams);

            for (ItemStack dropStack : generatedLoot) {
                Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.15, this.worldPosition.getZ() + 0.5, dropStack);
            }

            this.clearContent();
            return true;
        }

        return false;
    }

    public void saveToItem(ItemStack stack) { if (!isEmpty()) { CompoundTag beTag = new CompoundTag(); ContainerHelper.saveAllItems(beTag, this.items, true); stack.addTagElement("BlockEntityTag", beTag); } }
    public void markUpdated() { setChanged(); if (level != null && !level.isClientSide) { level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3); } }
    @Override public void load(CompoundTag pTag) { super.load(pTag); this.items.clear(); ContainerHelper.loadAllItems(pTag, this.items); }
    @Override protected void saveAdditional(CompoundTag pTag) { super.saveAdditional(pTag); ContainerHelper.saveAllItems(pTag, this.items); }
    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public int getContainerSize() { return this.items.size(); }
    @Override public boolean isEmpty() { return this.items.get(0).isEmpty(); }
    @Override public ItemStack getItem(int pSlot) { return this.items.get(pSlot); }
    public ItemStack getItem() { return getItem(0); }
    @Override public ItemStack removeItem(int pSlot, int pAmount) { ItemStack item = ContainerHelper.removeItem(this.items, pSlot, pAmount); if (!item.isEmpty()) markUpdated(); return item; }
    public ItemStack removeItem() { return removeItem(0, 64); }
    @Override public ItemStack removeItemNoUpdate(int pSlot) { return ContainerHelper.takeItem(this.items, pSlot); }
    @Override public void setItem(int pSlot, ItemStack pStack) { this.items.set(pSlot, pStack); if (pStack.getCount() > this.getMaxStackSize()) pStack.setCount(this.getMaxStackSize()); markUpdated(); }
    public void setItem(ItemStack pStack) { setItem(0, pStack); }
    @Override public boolean stillValid(Player pPlayer) { return Container.stillValidBlockEntity(this, pPlayer); }
    @Override public void clearContent() { this.items.clear(); markUpdated(); }
}