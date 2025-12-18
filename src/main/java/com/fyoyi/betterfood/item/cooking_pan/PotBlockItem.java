package com.fyoyi.betterfood.item.cooking_pan;

import com.fyoyi.betterfood.client.renderer.cooking_pan.PotItemRenderer;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class PotBlockItem extends BlockItem implements Equipable {

    protected static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    protected static final UUID BASE_ATTACK_SPEED_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    protected static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("632897F2-39C1-47A6-808B-740700392EF5");

    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public PotBlockItem(Block block, Properties properties) {
        super(block, properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 3.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    // === 【修复点1】必须实现这个无参方法，否则报错 ===
    // 这是 Equipable 接口要求的默认槽位
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    // === 【修复点2】Forge 的扩展方法，用于动态判断 ===
    // 游戏实际逻辑会优先调用这个方法
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        // 1. 检查是否有数据
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);

            // 2. 如果有任何物品，强制只能拿在手上 (MAINHAND)
            // 这样右键时就无法装备到头上
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    return EquipmentSlot.MAINHAND;
                }
            }
        }

        // 3. 如果是空的，允许戴在头上
        return EquipmentSlot.HEAD;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.defaultModifiers;
        }

        if (slot == EquipmentSlot.HEAD) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ARMOR, new AttributeModifier(ARMOR_MODIFIER_UUID, "Armor modifier", 3.0D, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }

        return super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) {
            NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items);

            int count = 0;
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    count++;
                    tooltip.add(Component.literal("- ").append(item.getHoverName()).withStyle(ChatFormatting.GRAY));
                }
            }

            if (count > 0) {
                tooltip.add(Component.literal("包含 " + count + " 个食材").withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.literal("(装满食物时不可装备)").withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new PotItemRenderer();
            }
        });
    }
}