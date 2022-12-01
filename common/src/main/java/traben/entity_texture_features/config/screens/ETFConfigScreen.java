package traben.entity_texture_features.config.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static traben.entity_texture_features.ETFClientCommon.MOD_ID;

//inspired by puzzles custom gui code
public abstract class ETFConfigScreen extends Screen {
    static final RotatingCubeMapRenderer backgroundCube = new RotatingCubeMapRenderer(new CubeMapRenderer(new Identifier(MOD_ID + ":textures/gui/background/panorama")));
    final Screen parent;



    public ETFConfigScreen(Text text, Screen parent) {
        super(text);
        this.parent = parent;
    }
    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        assert this.client != null;
        this.client.setScreen(parent);
    }
    public static void renderGUITexture(Identifier texture, double x1, double y1, double x2, double y2) {

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(x1, y2, 0.0).texture(0, 1/*(float)x1, (float)y2*heightYValue*/).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x2, y2, 0.0).texture(1, 1/*(float)x2*widthXValue, (float)y2*heightYValue*/).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x2, y1, 0.0).texture(1, 0/*(float)x2*widthXValue, (float)y1*/).color(255, 255, 255, 255).next();
        bufferBuilder.vertex(x1, y1, 0.0).texture(0, 0/*(float)x1, (float)y1*/).color(255, 255, 255, 255).next();
        tessellator.draw();
    }

    public static void renderBackgroundTexture(int vOffset, Identifier texture, int height, int width) {
        renderBackgroundTexture(vOffset, texture, height, width, 0);
    }

    public static void renderBackgroundTexture(int vOffset, Identifier texture, int height, int width, int minHeight) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        bufferBuilder.vertex(0.0, height, 0.0).texture(0.0F, (float) height / 32.0F + (float) vOffset).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(width, height, 0.0).texture((float) width / 32.0F, (float) height / 32.0F + (float) vOffset).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(width, minHeight, 0.0).texture((float) width / 32.0F, (float) minHeight / 32.0F + (float) vOffset).color(64, 64, 64, 255).next();
        bufferBuilder.vertex(0.0, minHeight, 0.0).texture(0.0F, (float) minHeight / 32.0F + (float) vOffset).color(64, 64, 64, 255).next();
        tessellator.draw();
    }


    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // ETFUtils2.renderBackgroundTexture(0,new Identifier("textures/block/deepslate_tiles.png"),this.height,this.width);
        backgroundCube.render((float) 0.5, 1);

        renderBackgroundTexture(0, new Identifier("textures/gui/options_background.png"), (int) (height * 0.15), width);
        renderBackgroundTexture(0, new Identifier("textures/gui/options_background.png"), height, width, (int) (height * 0.85));
        this.fillGradient(matrices, 0, (int) (height * 0.15), width, (int) (height * 0.85), -1072689136, -804253680);

        drawCenteredText(matrices, textRenderer, title, width / 2, 15, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    ButtonWidget getETFButton(int x, int y, int width, @SuppressWarnings("SameParameterValue") int height, Text buttonText, ButtonWidget.PressAction onPress) {
        return getETFButton(x, y, width, height, buttonText, onPress, Text.of(""));
    }

    ButtonWidget getETFButton(int x, int y, int width, int height, Text buttonText, ButtonWidget.PressAction onPress, Text toolTipText) {
        int nudgeLeftEdge;
        if (width > 384) {
            nudgeLeftEdge = (width-384)/2;
            width = 384;
        }else{
            nudgeLeftEdge = 0;
        }
//        if (width > 800)
//            height=80;
//        if (width > 1600)
//            height=16;
        boolean tooltipIsEmpty = toolTipText.getString().isBlank();
        String[] strings = toolTipText.getString().split("\n");
        List<Text> lines = new ArrayList<>();
        for (String str :
                strings) {
            lines.add(Text.of(str.strip()));
        }

        return new ButtonWidget(x+nudgeLeftEdge, y, width, height,
                buttonText,
                onPress,
                (buttonWidget, matrices, mouseX, mouseY) -> {
                    if (buttonWidget.isHovered() && !tooltipIsEmpty) {
                        this.renderTooltip(matrices, lines, mouseX, mouseY);
                    }
                });
    }
}
