package dev.riftborn.awakening.client;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.riftborn.Riftborn;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
public final class AbyssSkyRenderer {
    private static final Identifier SKY = Riftborn.id("textures/environment/abyss_sky.png"),
                                    PLANET = Riftborn.id("textures/environment/abyss_planet.png"),
                                    STORM = Riftborn.id("textures/environment/abyss_storm.png");
    private static void begin() {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BackgroundRenderer.clearFog();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
    }
    private static void end() {
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }
    private static void cube(WorldRenderContext context, Identifier texture, int alpha) {
        RenderSystem.setShaderTexture(0, texture);
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(context.positionMatrix());
        for (int face = 0; face < 6; face++) {
            matrices.push();
            switch (face) {
                case 1 -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                case 2 -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
                case 3 -> matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                case 4 -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
                case 5 -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90));
                default -> {
                }
            }
            var m = matrices.peek().getPositionMatrix();
            var b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            b.vertex(m, -100, -100, -100).texture(0, 0).color(255, 255, 255, alpha);
            b.vertex(m, -100, -100, 100).texture(0, 1).color(255, 255, 255, alpha);
            b.vertex(m, 100, -100, 100).texture(1, 1).color(255, 255, 255, alpha);
            b.vertex(m, 100, -100, -100).texture(1, 0).color(255, 255, 255, alpha);
            BufferRenderer.drawWithGlobalProgram(b.end());
            matrices.pop();
        }
    }
    public static void render(WorldRenderContext context) {
        begin();
        cube(context, SKY, 255);
        RenderSystem.setShaderTexture(0, PLANET);
        var matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(context.positionMatrix());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25));
        Matrix4f m = matrices.peek().getPositionMatrix();
        var b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        b.vertex(m, -65, 60, -99).texture(0, 0).color(255, 255, 255, 255);
        b.vertex(m, -65, -12, -99).texture(0, 1).color(255, 255, 255, 255);
        b.vertex(m, 30, -12, -99).texture(1, 1).color(255, 255, 255, 255);
        b.vertex(m, 30, 60, -99).texture(1, 0).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(b.end());
        if (AwakeningClient.eventKind != 0)
            cube(context, STORM, AwakeningClient.eventKind == 2 ? 185 : 110);
        end();
    }
    public static void stormOverlay(WorldRenderContext context) {
        if (AwakeningClient.eventKind == 0)
            return;
        begin();
        cube(context, STORM, AwakeningClient.eventKind == 2 ? 170 : 95);
        end();
    }
    private AbyssSkyRenderer() {}
}
