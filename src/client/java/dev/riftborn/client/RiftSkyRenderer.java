package dev.riftborn.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.riftborn.Riftborn;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/** Six textured faces, no external shaders or mixins. Uses the render context's camera rotation. */
public final class RiftSkyRenderer {
    private static final Identifier SKY = Riftborn.id("textures/environment/rift_sky.png");
    private RiftSkyRenderer() { }

    public static void render(WorldRenderContext context) {
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BackgroundRenderer.clearFog();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, SKY);
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
                default -> { }
            }
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -100, -100, -100).texture(0, 0).color(255, 255, 255, 255);
            buffer.vertex(matrix, -100, -100, 100).texture(0, 1).color(255, 255, 255, 255);
            buffer.vertex(matrix, 100, -100, 100).texture(1, 1).color(255, 255, 255, 255);
            buffer.vertex(matrix, 100, -100, -100).texture(1, 0).color(255, 255, 255, 255);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            matrices.pop();
        }
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }
}
