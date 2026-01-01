package com.fyoyi.betterfood.block.entity;

import com.fyoyi.betterfood.recipe.CulinaryRecipe;
import com.fyoyi.betterfood.recipe.ModRecipes;
import com.fyoyi.betterfood.util.CookednessHelper;
import com.fyoyi.betterfood.util.FreshnessHelper;
import com.fyoyi.betterfood.config.FoodConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.Set;

public class PotBlockEntity extends BlockEntity implements Container {

    private final NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private int flipTimer = 0;
    public static final int FLIP_ANIMATION_DURATION = 10;
    private int flipCount = 0;

    private int oilSizzleTimer = 0;

    private boolean hasOil = false;

    // 效率: 底层100%, 二层50%, 三层20%, 顶层0%
    private static final float[] LAYER_EFFICIENCY = {1.0f, 0.5f, 0.2f, 0.0f};
    private static final float BASE_HEAT_PER_TICK = 0.1f;

    public PotBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.POT_BE.get(), pPos, pBlockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PotBlockEntity pEntity) {
        // 1. 翻炒动画逻辑
        if (pEntity.flipTimer > 0) {
            if (pEntity.flipTimer == FLIP_ANIMATION_DURATION && !pEntity.isEmpty()) {
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.BLOCKS, 1.0F, 0.8F);
            }

            if (pEntity.flipTimer == FLIP_ANIMATION_DURATION / 2) pEntity.cycleItemsOrder();
            pEntity.flipTimer--;

            if (pEntity.flipTimer == 0) {
                pEntity.flipCount++;
                pEntity.markUpdated();

                if(!pEntity.isEmpty()) {
                    if (isHeated(level, pos)) {
                        pEntity.spawnBlackSmokeParticles();
                        level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5F, 1.5F);
                    }
                    level.playSound(null, pos, SoundEvents.MUD_STEP, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        // 2. 加热逻辑 (仅服务端)
        if (!level.isClientSide) {
            if (isHeated(level, pos)) {
                pEntity.applyHeat();
            }
        }

        if (pEntity.oilSizzleTimer > 0 && isHeated(level, pos)) {
            pEntity.oilSizzleTimer--;

            // 仅在服务端运行特效逻辑
            if (!level.isClientSide) {
                pEntity.spawnSustainedOilEffects();
            }
        }

    }

    private void applyHeat() {
        boolean changed = false;
        boolean hasItems = false;

        // 遍历所有物品进行加热
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            hasItems = true;

            float efficiency = (i < LAYER_EFFICIENCY.length) ? LAYER_EFFICIENCY[i] : 0.0f;
            if (efficiency <= 0) continue;

            float current = CookednessHelper.getCurrentCookedness(stack);

            if (current < 120.0f) {
                current += BASE_HEAT_PER_TICK * efficiency;
                CookednessHelper.setCookedness(stack, current);
                changed = true;
            }
        }

        // 数据同步
        if (changed) {
            setChanged();
            if (this.level.getGameTime() % 20 == 0) {
                this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            }
        }

        // === 持续烹饪特效 ===
        if (hasItems || this.hasOil) {
            float hasOilSoundAdd = 0f;
            if(this.hasOil) hasOilSoundAdd = 0.1f;
            // 声音特效
            if (this.level.getGameTime() % 2 == 0) {
                float rng = this.level.random.nextFloat();
                if (rng < 0.3f) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.065F + hasOilSoundAdd, 2.5F + (this.level.random.nextFloat() - 0.5F) * 0.5F);
                } else if (rng < 0.6f) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.085F + hasOilSoundAdd, 2.0F + (this.level.random.nextFloat() - 0.5F) * 0.5F);
                } else if (rng < 0.9f) {
                    this.level.playSound(null, this.worldPosition, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 0.1F + hasOilSoundAdd, 2.0F);
                }
            }
            if (this.level.getGameTime() % 10 == 0 && this.level.random.nextFloat() < 0.1f && this.hasOil) {
                this.level.playSound(null, this.worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 1.5F + this.level.random.nextFloat() * 0.5F);
            }

            // 烟雾粒子
            if (this.level.getGameTime() % 5 == 0) {
                spawnAmbientParticles();
            }
            if (this.level.getGameTime() % 4 == 0 && !this.hasOil) {
                spawnBurntParticles();
            }
        }

        // 环境热浪声 (只要有火就响，不一定非要有食物)
        if (this.level.getGameTime() % 20 == 0 && this.level.random.nextFloat() < 0.7f) {
            this.level.playSound(null, this.worldPosition, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.0F + this.level.random.nextFloat() * 0.4F);
        }
    }

    // === 【新增】油的 Getter 和 Setter (解决报错的关键) ===
    public boolean hasOil() {
        return this.hasOil;
    }

    public void setHasOil(boolean hasOil) {
        this.hasOil = hasOil;
        markUpdated(); // 状态改变，通知客户端
    }

    // === 翻锅时的烟雾 ===
    private void spawnBlackSmokeParticles() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.2;
            double z = pos.getZ() + 0.5;
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 5, 0.1, 0.1, 0.1, 0.05);
        }
    }

    // === 入锅特效烟雾 ===
    public void spawnInputParticles() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.2;
            double z = pos.getZ() + 0.5;
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 4, 0.1, 0.1, 0.1, 0.08);
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 3, 0.15, 0.2, 0.15, 0.1);
        }
    }

    // === 倒油进锅的特效 ===
    public void spawnOilInputParticles() {
        // 1. 启动持续特效倒计时 (2秒)
        this.oilSizzleTimer = 30;

        if (level instanceof ServerLevel serverLevel) {

            BlockPos pos = this.getBlockPos();
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.25;
            double z = pos.getZ() + 0.5;

            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y+0.2, z, 6, 0.15, 0.1, 0.15, 0.03);
            serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 8, 0.1, 0.1, 0.1, 0.15);
            serverLevel.sendParticles(ParticleTypes.LANDING_HONEY, x, y, z, 15, 0.3, 0.3, 0.3, 0.1);
        }
    }

    // === 持续的油沸腾余韵 ===
    private void spawnSustainedOilEffects() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();

            // 随机生成坐标，范围稍微大一点，覆盖整个锅底
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.4;

            // 持续冒出小黑烟和岩浆泡 (模拟沸腾)
            if (this.oilSizzleTimer % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 4, 0.1, 0.05, 0.1, 0.02);
            }
            // 偶尔冒出一股浓烟
            if (this.oilSizzleTimer % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.1, z, 1, 0.05, 0.1, 0.05, 0.01);
            }
            // 偶尔冒个油花
            if (level.random.nextFloat() < 0.1f) {
                serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z, 1, 0.0, 0.05, 0.0, 0.0);
            }

            // 持续的噼啪声 (密集但音量逐渐变小)
            if (this.oilSizzleTimer % 3 == 0) {

                float volume = 0.4f + (this.oilSizzleTimer / 40.0f) * 0.7f;

                level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, volume, 2.6F + (level.random.nextFloat() - 0.5f) * 0.4f);
            }
        }
    }

    // === 持续烹饪烟雾 ===
    private void spawnAmbientParticles() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.03, 0.0, 0.0);
            if (level.random.nextFloat() < 0.1f) {
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
            }
        }
    }

    // === 干烧/烧焦特效 (没油的时候) ===
    private void spawnBurntParticles() {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = this.getBlockPos();
            // 随机位置
            double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.3;

            // 使用 LARGE_SMOKE (大黑烟)
            // 数量: 2, 速度: 0.05 (升起速度比普通烟雾快一点，表现热气流急)
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.1, 0.1, 0.1, 0.05);

            // 偶尔加一点点普通烟雾增加层次感
            if (level.random.nextFloat() < 0.5f) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
    }

    // === 入锅逻辑 ===
    public boolean pushItem(ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                ItemStack toAdd = stack.copy();
                toAdd.setCount(1);

                float currentVal = CookednessHelper.getCurrentCookedness(toAdd);
                if (currentVal <= 0.0f) {
                    float configInitialVal = getInitialCookednessFromConfig(toAdd);
                    if (configInitialVal > 0) {
                        CookednessHelper.setCookedness(toAdd, configInitialVal);
                    }
                }

                items.set(i, toAdd);
                markUpdated();

                if (level != null && !level.isClientSide) {
                    if(isHeated(this.level, this.worldPosition)) {
                        level.playSound(null, this.worldPosition, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.4F, 1.8F);
                        spawnInputParticles();
                    }
                }
                return true;
            }
        }
        return false;
    }

    // === 出锅匹配 ===
    public ItemStack serveDish() {
        if (level == null || isEmpty()) return ItemStack.EMPTY;

        List<CulinaryRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.CULINARY_TYPE.get());

        ItemStack result = ItemStack.EMPTY;

        for (CulinaryRecipe recipe : recipes) {
            if (recipe.matches(this, level)) {
                result = recipe.assemble(this, level.registryAccess());
                applyEvaluationToResult(result);
                break;
            }
        }

        if (result.isEmpty()) {
            result = new ItemStack(Items.MUSHROOM_STEW);
        }

        this.clearContent();
        this.markUpdated();
        return result;
    }

    // ... 评价相关私有方法 ...
    private void applyEvaluationToResult(ItemStack result) {
        if (result.isEmpty()) return;
        List<CulinaryRecipe> recipes = this.level != null ? this.level.getRecipeManager().getAllRecipesFor(ModRecipes.CULINARY_TYPE.get()) : new ArrayList<>();
        float avgFreshness = calculateAverageFreshness();
        float avgCookednessDeviation = calculateAverageCookednessDeviation(recipes);
        float score = calculateScore(avgFreshness, avgCookednessDeviation);
        CompoundTag tag = result.getOrCreateTag();
        tag.putFloat("DishScore", score);
        tag.putFloat("DishFreshness", avgFreshness);
        tag.putFloat("DishCookednessDeviation", avgCookednessDeviation);
    }

    private float calculateAverageFreshness() {
        float total = 0; int count = 0;
        for (ItemStack s : items) { if (s.isEmpty()) continue; total += (FreshnessHelper.getFreshnessPercentage(this.level, s) * 100f >= 95f ? 100f : (FreshnessHelper.getFreshnessPercentage(this.level, s) * 100f / 95f) * 95f); count++; }
        return count > 0 ? total / count : 0;
    }

    private float calculateAverageCookednessDeviation(List<CulinaryRecipe> recipes) {
        float total = 0; int count = 0;
        for (ItemStack s : items) {
            if (s.isEmpty()) continue;
            float current = CookednessHelper.getCurrentCookedness(s);
            float expected = 70.0f;
            String cls = null;
            Set<String> tags = FoodConfig.getFoodTags(s);
            for(String t : tags) if(t.startsWith("分类:")) { cls = t.substring(3).trim(); break; }
            if (cls != null) {
                for (CulinaryRecipe r : recipes) { for (CulinaryRecipe.Requirement req : r.getRequirements()) { if (cls.equals(req.classification)) { expected = req.idealCookedness; break; }}}
            }
            total += Math.abs(current - expected); count++;
        }
        return count > 0 ? total / count : 0;
    }

    private float calculateScore(float f, float d) { return Math.max(0, Math.min(100, f * 1.0f + d * -0.5f)); }
    private float getInitialCookednessFromConfig(ItemStack s) { try { for(String t : FoodConfig.getFoodTags(s)) if(t.startsWith("熟度:")) return Float.parseFloat(t.substring(3).replace("%","").trim()); } catch(Exception e){} return 0f; }

    public static boolean isHeated(Level l, BlockPos p) {
        BlockState b = l.getBlockState(p.below());
        if (b.is(Blocks.FIRE) || b.is(Blocks.SOUL_FIRE) || b.is(Blocks.LAVA) || b.is(Blocks.MAGMA_BLOCK)) return true;
        if (b.getBlock() instanceof CampfireBlock && b.getValue(CampfireBlock.LIT)) return true;
        return false;
    }

    private void cycleItemsOrder() {
        List<ItemStack> t = new ArrayList<>();
        for(ItemStack s : items) if(!s.isEmpty()) t.add(s);
        if(t.size() < 2) return;
        ItemStack b = t.remove(0);
        t.add(b);
        clearContent();
        for(int i = 0; i < t.size(); i++) items.set(i, t.get(i));
        markUpdated();
    }

    public void markUpdated() {
        setChanged();
        if(level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // === NBT 保存与加载 (包含油的状态) ===
    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        ContainerHelper.saveAllItems(t, items);
        t.putInt("FlipCount", flipCount);
        t.putBoolean("HasOil", hasOil); // 保存油
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        items.clear();
        ContainerHelper.loadAllItems(t, items);
        if(t.contains("FlipCount")) flipCount = t.getInt("FlipCount");
        if(t.contains("HasOil")) hasOil = t.getBoolean("HasOil"); // 读取油
    }

    @Override
    public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection n, ClientboundBlockEntityDataPacket p) {
        this.load(p.getTag());
    }

    public void saveToItem(ItemStack s) {
        CompoundTag t = new CompoundTag();
        ContainerHelper.saveAllItems(t, items);
        t.putInt("FlipCount", flipCount);
        t.putBoolean("HasOil", hasOil); // 保存油到物品
        
        // 保存热源状态到物品
        boolean hasHeat = isHeated(this.level, this.worldPosition);
        t.putBoolean("IsHeated", hasHeat);

        boolean hasItem = false;
        for(ItemStack stack : items) if(!stack.isEmpty()) hasItem = true;

        // 只要有物品或者有油，就保存 NBT
        if(hasItem || hasOil) {
            s.addTagElement("BlockEntityTag", t);
        }
    }

    // Container 接口实现
    public int getFlipCount() { return flipCount; }
    public void triggerFlip() { if(flipTimer == 0) flipTimer = FLIP_ANIMATION_DURATION; }
    public float getFlipProgress(float p) { if(flipTimer <= 0) return 0f; return (flipTimer - p) / FLIP_ANIMATION_DURATION; }
    public NonNullList<ItemStack> getItems() { return items; }
    public ItemStack popItem() { for(int i=items.size()-1; i>=0; i--) if(!items.get(i).isEmpty()) { ItemStack s=items.get(i).copy(); items.set(i, ItemStack.EMPTY); markUpdated(); return s; } return ItemStack.EMPTY; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { for(ItemStack s : items) if(!s.isEmpty()) return false; return true; }
    @Override public ItemStack getItem(int i) { return items.get(i); }
    @Override public ItemStack removeItem(int i, int c) { return ContainerHelper.removeItem(items, i, c); }
    @Override public ItemStack removeItemNoUpdate(int i) { return ContainerHelper.takeItem(items, i); }
    @Override public void setItem(int i, ItemStack s) { items.set(i, s); if(s.getCount() > getMaxStackSize()) s.setCount(getMaxStackSize()); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { items.clear(); }
}