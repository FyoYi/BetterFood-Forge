package com.fyoyi.betterfood.client.gui;

import com.fyoyi.betterfood.recipe.RecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 原版书籍风格的菜谱书屏幕
 * 通过翻页切换不同餐具的菜谱
 */
public class RecipeBookScreen extends Screen {

    // 原版书籍UI纹理
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft",
            "textures/gui/book.png");
    private static final int TEXTURE_WIDTH = 192;
    private static final int TEXTURE_HEIGHT = 192;

    private final RecipeBook recipeBook;
    private int currentPage = 0; // 全局页码
    private int totalPages = 1;

    public RecipeBookScreen(RecipeBook recipeBook) {
        super(Component.literal("菜谱书"));
        this.recipeBook = recipeBook;
        this.totalPages = calculateTotalPages();
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);

        int screenX = (this.width - TEXTURE_WIDTH) / 2;
        int screenY = (this.height - TEXTURE_HEIGHT) / 2;

        // 绘制书籍背景（左右两页）
        guiGraphics.blit(BOOK_TEXTURE, screenX, screenY, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH,
                TEXTURE_HEIGHT);

        // 绘制标题
        guiGraphics.drawCenteredString(this.font, "§l菜谱书§r", screenX + TEXTURE_WIDTH / 2, screenY + 16, 0xFF000000);

        // 绘制左页（48-118）和右页（118-188）的内容
        int leftPageX = screenX + 12;
        int rightPageX = screenX + 100;
        int pageStartY = screenY + 32;

        // 左页
        drawPageContent(guiGraphics, leftPageX, pageStartY, currentPage);

        // 右页
        drawPageContent(guiGraphics, rightPageX, pageStartY, currentPage + 1);

        // 绘制页码
        guiGraphics.drawCenteredString(this.font, String.valueOf(currentPage + 1), screenX + TEXTURE_WIDTH / 2 - 20,
                screenY + TEXTURE_HEIGHT - 16, 0xFF8B8B8B);
        guiGraphics.drawCenteredString(this.font, String.valueOf(currentPage + 2), screenX + TEXTURE_WIDTH / 2 + 20,
                screenY + TEXTURE_HEIGHT - 16, 0xFF8B8B8B);

        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    /**
     * 绘制单页内容
     */
    private void drawPageContent(GuiGraphics guiGraphics, int x, int y, int pageNum) {
        if (pageNum >= totalPages) {
            return;
        }

        int lineHeight = 10;
        int linesPerPage = 13;

        if (pageNum == 0) {
            // 目录页
            drawTableOfContents(guiGraphics, x, y);
        } else if (pageNum <= 2) {
            // 平底锅菜谱页（第1-2页）
            drawCookingPanRecipes(guiGraphics, x, y, lineHeight, linesPerPage, (pageNum - 1) * linesPerPage);
        } else if (pageNum <= 4) {
            // 炖锅菜谱页（第3-4页）
            drawLargePotRecipes(guiGraphics, x, y, lineHeight, linesPerPage, (pageNum - 3) * linesPerPage);
        } else {
            // 切菜板菜谱页（第5-6页）
            drawCuttingBoardRecipes(guiGraphics, x, y, lineHeight, linesPerPage, (pageNum - 5) * linesPerPage);
        }
    }

    /**
     * 绘制目录页
     */
    private void drawTableOfContents(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font, "§l目录§r", x, y, 0xFF000000);

        y += 16;
        guiGraphics.drawString(this.font, "§n平底锅菜谱§r", x, y, 0xFF000000);
        y += 10;
        guiGraphics.drawString(this.font, "共 " + recipeBook.getAllCookingPanRecipes().size() + " 道", x + 8, y,
                0xFF666666);

        y += 16;
        guiGraphics.drawString(this.font, "§n炖锅菜谱§r", x, y, 0xFF000000);
        y += 10;
        guiGraphics.drawString(this.font, "共 " + recipeBook.getAllLargePotRecipes().size() + " 道", x + 8, y,
                0xFF666666);

        y += 16;
        guiGraphics.drawString(this.font, "§n切菜板菜谱§r", x, y, 0xFF000000);
        y += 10;
        guiGraphics.drawString(this.font, "共 " + recipeBook.getAllCuttingRecipes().size() + " 种", x + 8, y, 0xFF666666);
    }

    /**
     * 绘制平底锅菜谱
     */
    private void drawCookingPanRecipes(GuiGraphics guiGraphics, int x, int y, int lineHeight, int linesPerPage,
            int startLine) {
        var recipes = recipeBook.getAllCookingPanRecipes();
        if (recipes.isEmpty()) {
            guiGraphics.drawString(this.font, "还没有解锁菜谱", x, y, 0xFF999999);
            return;
        }

        int currentLine = 0;
        int drawnLines = 0;

        // 标题
        if (startLine == 0) {
            guiGraphics.drawString(this.font, "§l平底锅菜谱§r", x, y, 0xFF000000);
            drawnLines++;
        }
        currentLine++;

        for (RecipeBook.CookRecipe recipe : recipes) {
            if (currentLine >= startLine && drawnLines < linesPerPage) {
                // 菜名
                guiGraphics.drawString(this.font, "• " + recipe.dishName, x, y + drawnLines * lineHeight, 0xFF000000);
                drawnLines++;

                // 评分
                if (drawnLines < linesPerPage) {
                    int score = (int) recipe.finalScore;
                    String scoreColor = score >= 90 ? "§a" : (score >= 70 ? "§e" : "§c");
                    guiGraphics.drawString(this.font, "  评分: " + scoreColor + score + "§r", x,
                            y + drawnLines * lineHeight, 0xFF666666);
                    drawnLines++;
                }
            }
            currentLine += 2;
        }
    }

    /**
     * 绘制炖锅菜谱
     */
    private void drawLargePotRecipes(GuiGraphics guiGraphics, int x, int y, int lineHeight, int linesPerPage,
            int startLine) {
        var recipes = recipeBook.getAllLargePotRecipes();
        if (recipes.isEmpty()) {
            guiGraphics.drawString(this.font, "还没有解锁菜谱", x, y, 0xFF999999);
            return;
        }

        int currentLine = 0;
        int drawnLines = 0;

        // 标题
        if (startLine == 0) {
            guiGraphics.drawString(this.font, "§l炖锅菜谱§r", x, y, 0xFF000000);
            drawnLines++;
        }
        currentLine++;

        for (RecipeBook.CookRecipe recipe : recipes) {
            if (currentLine >= startLine && drawnLines < linesPerPage) {
                // 菜名
                guiGraphics.drawString(this.font, "• " + recipe.dishName, x, y + drawnLines * lineHeight, 0xFF000000);
                drawnLines++;

                // 评分
                if (drawnLines < linesPerPage) {
                    int score = (int) recipe.finalScore;
                    String scoreColor = score >= 90 ? "§a" : (score >= 70 ? "§e" : "§c");
                    guiGraphics.drawString(this.font, "  评分: " + scoreColor + score + "§r", x,
                            y + drawnLines * lineHeight, 0xFF666666);
                    drawnLines++;
                }
            }
            currentLine += 2;
        }
    }

    /**
     * 绘制切菜板菜谱
     */
    private void drawCuttingBoardRecipes(GuiGraphics guiGraphics, int x, int y, int lineHeight, int linesPerPage,
            int startLine) {
        var recipes = recipeBook.getAllCuttingRecipes();
        if (recipes.isEmpty()) {
            guiGraphics.drawString(this.font, "还没有解锁菜谱", x, y, 0xFF999999);
            return;
        }

        int currentLine = 0;
        int drawnLines = 0;

        // 标题
        if (startLine == 0) {
            guiGraphics.drawString(this.font, "§l切菜板菜谱§r", x, y, 0xFF000000);
            drawnLines++;
        }
        currentLine++;

        for (String recipe : recipes) {
            if (currentLine >= startLine && drawnLines < linesPerPage) {
                guiGraphics.drawString(this.font, "• " + recipe, x, y + drawnLines * lineHeight, 0xFF333333);
                drawnLines++;
            }
            currentLine++;
        }
    }

    /**
     * 计算总页数
     */
    private int calculateTotalPages() {
        int count = 1; // 目录页

        int cookingCount = recipeBook.getAllCookingPanRecipes().size() * 2;
        int largeCount = recipeBook.getAllLargePotRecipes().size() * 2;
        int cuttingCount = recipeBook.getAllCuttingRecipes().size();

        // 每13行一页
        count += (cookingCount + 13 - 1) / 13;
        count += (largeCount + 13 - 1) / 13;
        count += (cuttingCount + 13 - 1) / 13;

        return count;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        int screenX = (this.width - TEXTURE_WIDTH) / 2;
        int screenY = (this.height - TEXTURE_HEIGHT) / 2;

        // 上一页（左下角）
        if (pMouseX >= screenX + 20 && pMouseX <= screenX + 60 &&
                pMouseY >= screenY + TEXTURE_HEIGHT - 22 && pMouseY <= screenY + TEXTURE_HEIGHT - 10) {
            if (currentPage > 0) {
                currentPage--;
                return true;
            }
        }

        // 下一页（右下角）
        if (pMouseX >= screenX + TEXTURE_WIDTH - 60 && pMouseX <= screenX + TEXTURE_WIDTH - 20 &&
                pMouseY >= screenY + TEXTURE_HEIGHT - 22 && pMouseY <= screenY + TEXTURE_HEIGHT - 10) {
            if (currentPage + 2 < totalPages) {
                currentPage += 2;
                return true;
            }
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
