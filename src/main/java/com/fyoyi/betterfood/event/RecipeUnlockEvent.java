package com.fyoyi.betterfood.event;

import com.fyoyi.betterfood.recipe.RecipeBook;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;

/**
 * 菜谱解锁事件 - 监听玩家完成菜谱时触发
 * 
 * 用法：在PotBlockEntity完成菜谱时，调用
 * RecipeUnlockEvent.unlock(player, recipeName, finalScore, ingredients);
 */
public class RecipeUnlockEvent extends PlayerEvent {

    public enum RecipeType {
        COOKING_PAN,
        LARGE_POT,
        CUTTING_BOARD
    }

    private final RecipeType recipeType;
    private final String recipeName;
    private final float finalScore;
    private final java.util.List<RecipeBook.IngredientRequirement> ingredients;

    public RecipeUnlockEvent(Player player, RecipeType recipeType, String recipeName,
            float finalScore, java.util.List<RecipeBook.IngredientRequirement> ingredients) {
        super(player);
        this.recipeType = recipeType;
        this.recipeName = recipeName;
        this.finalScore = finalScore;
        this.ingredients = ingredients;
    }

    public RecipeType getRecipeType() {
        return recipeType;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public float getFinalScore() {
        return finalScore;
    }

    public java.util.List<RecipeBook.IngredientRequirement> getIngredients() {
        return ingredients;
    }

    @Mod.EventBusSubscriber(modid = "better_food", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RecipeUnlockHandler {

        @SubscribeEvent
        public static void onRecipeUnlock(RecipeUnlockEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide)
                return; // 仅在服务端处理

            net.minecraft.nbt.CompoundTag playerTag = player.getPersistentData();
            net.minecraft.nbt.CompoundTag betterFoodTag = playerTag.getCompound("BetterFoodData");

            String recipeName = event.getRecipeName();
            float score = event.getFinalScore();

            // 根据菜谱类型记录到NBT
            switch (event.getRecipeType()) {
                case COOKING_PAN:
                    net.minecraft.nbt.ListTag panRecipes = betterFoodTag.getList("CookingPanRecipes", 10);
                    net.minecraft.nbt.CompoundTag panRecipe = new net.minecraft.nbt.CompoundTag();
                    panRecipe.putString("name", recipeName);
                    panRecipe.putFloat("score", score);
                    panRecipes.add(panRecipe);
                    betterFoodTag.put("CookingPanRecipes", panRecipes);

                    player.displayClientMessage(
                            net.minecraft.network.chat.Component
                                    .literal("✅ 解锁平底锅菜谱: " + recipeName + " (评分: " + Math.round(score) + ")")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN),
                            false);
                    break;

                case LARGE_POT:
                    net.minecraft.nbt.ListTag potRecipes = betterFoodTag.getList("LargePotRecipes", 10);
                    net.minecraft.nbt.CompoundTag potRecipe = new net.minecraft.nbt.CompoundTag();
                    potRecipe.putString("name", recipeName);
                    potRecipe.putFloat("score", score);
                    potRecipes.add(potRecipe);
                    betterFoodTag.put("LargePotRecipes", potRecipes);

                    player.displayClientMessage(
                            net.minecraft.network.chat.Component
                                    .literal("✅ 解锁炖锅菜谱: " + recipeName + " (评分: " + Math.round(score) + ")")
                                    .withStyle(net.minecraft.ChatFormatting.AQUA),
                            false);
                    break;

                case CUTTING_BOARD:
                    net.minecraft.nbt.ListTag cuttingRecipes = betterFoodTag.getList("CuttingRecipes", 8);
                    cuttingRecipes.add(net.minecraft.nbt.StringTag.valueOf(recipeName));
                    betterFoodTag.put("CuttingRecipes", cuttingRecipes);

                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("✅ 解锁切菜菜谱: " + recipeName)
                                    .withStyle(net.minecraft.ChatFormatting.YELLOW),
                            false);
                    break;
            }

            // 保存回玩家数据
            playerTag.put("BetterFoodData", betterFoodTag);

            // 播放粒子效果和音效
            player.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1, player.getZ(),
                    0, 0.5, 0);
            player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
}
