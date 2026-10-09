package com.agricraft.agricraft.client.ber;

import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** Standard baked-model rendering; only the metal head moves, the wooden mount stays put. */
public class SprinklerBlockEntityRenderer implements BlockEntityRenderer<IrrigationBlockEntity> {
    public static final ResourceLocation HEAD = new ResourceLocation("agricraft", "block/irrigation/sprinkler_head");

    public SprinklerBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(IrrigationBlockEntity entity, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        if (!(entity.getBlockState().getBlock() instanceof IrrigationBlock block)
                || block.kind != IrrigationBlock.Kind.SPRINKLER) return;
        var minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().bakedRegistry.get(HEAD);
        if (model == null) return;
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(entity.getSprinklerAngle(partialTick)));
        pose.translate(-.5, 0, -.5);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffer.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)), Blocks.IRON_BLOCK.defaultBlockState(),
                model, 1, 1, 1, light, overlay);
        pose.popPose();
    }

    public static void spawnDrops(IrrigationBlockEntity entity) {
        var level = entity.getLevel();
        if (level == null || !entity.getBlockState().getValue(IrrigationBlock.ACTIVE)) return;
        var minecraft = Minecraft.getInstance();
        var pos = entity.getBlockPos();
        if (minecraft.player == null || minecraft.player.distanceToSqr(pos.getX() + .5,
                pos.getY() + .35, pos.getZ() + .5) > 32 * 32) return;
        ParticleStatus setting = minecraft.options.particles().get();
        int interval = setting == ParticleStatus.MINIMAL ? 8 : setting == ParticleStatus.DECREASED ? 4 : 2;
        if (level.getGameTime() % interval != 0) return;
        double angle = -Math.toRadians(entity.getSprinklerAngle(1));
        boolean vapour = level.dimensionType().ultraWarm();
        for (int arm = 0; arm < 4; arm++) {
            double dx = Math.cos(angle + arm * Math.PI / 2), dz = Math.sin(angle + arm * Math.PI / 2);
            for (int drop = 0; drop < (vapour ? 1 : 3); drop++) {
                var particle = minecraft.particleEngine.createParticle(vapour ? ParticleTypes.CLOUD : ParticleTypes.FALLING_WATER,
                        pos.getX() + .5 + dx * .28, pos.getY() + .35, pos.getZ() + .5 + dz * .28,
                        0, 0, 0);
                // Drip particles initialize their own speed; explicitly supply the jet's
                // velocity so drops leave the head, fall under gravity and collide below.
                if (particle != null) {
                    double speed = .12 + drop * .10;
                    particle.setParticleSpeed(dx * speed, vapour ? .10 : -.025 - drop * .055, dz * speed);
                }
            }
        }
    }
}
