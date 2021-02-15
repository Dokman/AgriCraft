package com.infinityraider.agricraft.render.plant;

import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.api.v1.genetics.IAgriGene;
import com.infinityraider.agricraft.api.v1.genetics.IAgriGenePair;
import com.infinityraider.infinitylib.render.IRenderUtilities;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.OptionalDouble;

@OnlyIn(Dist.CLIENT)
public class AgriGenomeRenderer implements IRenderUtilities {
    private static final AgriGenomeRenderer INSTANCE = new AgriGenomeRenderer();

    public static AgriGenomeRenderer getInstance() {
        return INSTANCE;
    }

    /** Pi, but as a float */
    private static final float PI = (float) Math.PI;

    /** Helix rotation per spoke rendering setting */
    public static final float RADIANS_PER_GENE = 30*PI/180;

    /** Helix points per distance rendering setting */
    public static final int POINTS_PER_GENE = 10;

    /** Color of inactive genes */
    public static final Vector3f COLOR_INACTIVE = new Vector3f(0.15F, 0.15F, 0.15F);

    private AgriGenomeRenderer() {}

    /**
     * Renders an AgriCraft genome.
     *
     * Renders a right-hand, double helix with the dominant alleles on the left, and the recessive ones on the right,
     * The helix will be rotated so that the selected gene (denoted by index) is along the X-axis,
     * with a smooth transition towards the next gene.
     *
     * Notes:
     *  - The full double helix will be rendered, therefore it might appear squished or stretched based on the height
     *  - The selected gene will be colored with its color, the inactive ones will be greyed out
     *
     * @param genePairs the genome for which to draw an overlay
     * @param transforms matrix stack for the transformation
     * @param buffer the vertex buffer to draw with
     * @param index the index denoting the selected gene
     * @param transition a double denoting the transition progress to the next/previous gene (bounded by 1 and -1)
     * @param radius the radius of the double helix
     * @param height the height of the double helix
     * @param alpha the transparency of the helix
     */
    public void renderDoubleHelix(List<IAgriGenePair<?>> genePairs, MatrixStack transforms, IRenderTypeBuffer buffer,
                                  int index, float transition, float radius, float height, float alpha) {

        // Define helix properties
        int count = genePairs.size();
        if(count == 0 || radius == 0 || height == 0) {
            // Should never happen
            return;
        }
        int points = POINTS_PER_GENE * count;
        float heightStep = (height + 0.0F)/points;
        float angleStep = -(RADIANS_PER_GENE + 0.0F)/POINTS_PER_GENE;
        float angleOffset = RADIANS_PER_GENE/2;
        float rotation = (index + transition)*RADIANS_PER_GENE;

        // Push transformation matrix
        transforms.push();

        // Rotate according to the index
        transforms.rotate(new Quaternion(Vector3f.YP, rotation, false));

        // Fetch vertex buffer, builder and transformation matrix
        IVertexBuilder builder = this.getVertexBuilder(buffer, this.getRenderType());
        Matrix4f matrix = transforms.getLast().getMatrix();

        // First helix
        this.drawHelix(genePairs, index, radius, angleOffset, heightStep, angleStep, points, builder, matrix, true, alpha);
        // Second helix
        this.drawHelix(genePairs, index, radius, PI + angleOffset, heightStep, angleStep, points, builder, matrix, false, alpha);
        // Spokes
        this.drawSpokes(genePairs, index, radius, angleOffset, PI + angleOffset, heightStep, angleStep, builder, matrix,alpha);

        // Pop transformation matrix from the stack
        transforms.pop();
    }

    protected void drawHelix(List<IAgriGenePair<?>> genePairs, int active, float radius, float phase, float dHeight, float dAngle,
                             int points, IVertexBuilder builder, Matrix4f matrix, boolean dominant, float alpha) {
        float x_1 = radius * MathHelper.cos(-phase);
        float y_1 = dHeight*points;
        float z_1 = radius * MathHelper.sin(-phase);
        for(int i = 0; i < points; i++) {
            // Determine color
            int index = i/POINTS_PER_GENE;
            int partial = i % POINTS_PER_GENE;
            IAgriGene<?> gene = genePairs.get(index).getGene();
            Vector3f color = this.getColor(gene, index == active, dominant);
            float r = color.getX();
            float g = color.getY();
            float b = color.getZ();
            if(partial < POINTS_PER_GENE/2) {
                int prevIndex = (index - 1) < 0 ? index : index - 1;
                IAgriGene<?> prevGene = genePairs.get(prevIndex).getGene();
                Vector3f prevColor = this.getColor(prevGene, prevIndex == active, dominant);
                float f = (partial + ((POINTS_PER_GENE + 0.0F)/2)) / POINTS_PER_GENE;
                r = MathHelper.lerp(f, prevColor.getX(), r);
                g = MathHelper.lerp(f, prevColor.getY(), g);
                b = MathHelper.lerp(f, prevColor.getZ(), b);
            } else if(partial > POINTS_PER_GENE/2) {
                int nextIndex = (index + 1) >= genePairs.size() ? index : index + 1;
                IAgriGene<?> nextGene = genePairs.get(nextIndex).getGene();
                Vector3f nextColor = this.getColor(nextGene, nextIndex == active, dominant);
                float f = (partial - ((POINTS_PER_GENE + 0.0F)/2)) / POINTS_PER_GENE;
                r = MathHelper.lerp(f, r, nextColor.getX());
                g = MathHelper.lerp(f, g, nextColor.getY());
                b = MathHelper.lerp(f, b, nextColor.getZ());
            }
            // Determine coordinates
            float x_2 = radius * MathHelper.cos(-((1 + i)*dAngle + phase));
            float y_2 = dHeight*(points - i);
            float z_2 = radius * MathHelper.sin(-((1 + i)*dAngle + phase));
            // Add vertices for line segment
            this.addVertex(builder, matrix, x_1, y_1, z_1, r, g, b, alpha);
            this.addVertex(builder, matrix, x_2, y_2, z_2, r, g, b, alpha);
            // Update previous coordinates
            x_1 = x_2;
            y_1 = y_2;
            z_1 = z_2;
        }
    }

    protected void drawSpokes(List<IAgriGenePair<?>> genePairs, int active, float radius, float phase1, float phase2,
                              float dHeight, float dAngle, IVertexBuilder builder, Matrix4f matrix, float alpha) {
        for(int spoke = 0; spoke < genePairs.size(); spoke++) {
            // Find equivalent point index
            int i = spoke*POINTS_PER_GENE + POINTS_PER_GENE/2;
            float angle = i*dAngle;
            // Find positions
            float x1 = radius*MathHelper.cos(-(angle + phase1));
            float x2 = radius*MathHelper.cos(-(angle + phase2));
            float y = dHeight * (POINTS_PER_GENE * genePairs.size() - i);
            float z1 = radius*MathHelper.sin(-(angle + phase1));
            float z2 = radius*MathHelper.sin(-(angle + phase2));
            // find colors
            IAgriGene<?> gene = genePairs.get(spoke).getGene();
            Vector3f dom = this.getColor(gene, active == spoke, true);
            Vector3f rec = this.getColor(gene, active == spoke, false);
            // First vertex of the first segment
            this.addVertex(builder, matrix, x1, y, z1, dom.getX(), dom.getY(), dom.getZ(), alpha);
            for(int j = 1; j < POINTS_PER_GENE; j++) {
                float x = MathHelper.lerp((j + 0.0F)/POINTS_PER_GENE, x1, x2);
                float z = MathHelper.lerp((j + 0.0F)/POINTS_PER_GENE, z1, z2);
                float r = MathHelper.lerp((j + 0.0F)/POINTS_PER_GENE, dom.getX(), rec.getX());
                float g = MathHelper.lerp((j + 0.0F)/POINTS_PER_GENE, dom.getY(), rec.getY());
                float b = MathHelper.lerp((j + 0.0F)/POINTS_PER_GENE, dom.getZ(), rec.getZ());
                // Second vertex of the previous segment
                this.addVertex(builder, matrix, x, y, z, r, g, b, alpha);
                // First vertex of the next segment
                this.addVertex(builder, matrix, x, y, z, r, g, b, alpha);
            }
            // Second vertex of the last segment
            this.addVertex(builder, matrix, x2, y, z2, rec.getX(), rec.getY(), rec.getZ(), alpha);
        }
    }

    protected void addVertex(IVertexBuilder builder, Matrix4f matrix, float x, float y, float z, float r, float g, float b, float a) {
        builder.pos(matrix, x, y, z)
                .color(r, g, b, a)
                .endVertex();
    }

    protected Vector3f getColor(IAgriGene<?> gene, boolean active, boolean dominant) {
        if(active) {
            return dominant ? gene.getDominantColor() : gene.getRecessiveColor();
        } else {
            return COLOR_INACTIVE;
        }
    }

    protected RenderType getRenderType() {
        return LineRenderType.INSTANCE;
    }

    public static class LineRenderType extends RenderType {
        // We need to put the static instance inside a class, as to initialize it we need to access a Builder,
        // which has protected access
        // Therefore we need a dummy constructor which will never be called ¯\_(ツ)_/¯
        private LineRenderType(String nameIn, VertexFormat formatIn, int drawModeIn, int bufferSizeIn, boolean useDelegateIn, boolean needsSortingIn, Runnable setupTaskIn, Runnable clearTaskIn) {
            super(nameIn, formatIn, drawModeIn, bufferSizeIn, useDelegateIn, needsSortingIn, setupTaskIn, clearTaskIn);
        }

        public static final String RENDER_TYPE_KEY = AgriCraft.instance.getModId() + ":genome_lines";
        public static final Double LINE_THICKNESS = 2.5;

        public static final RenderType INSTANCE = makeType(RENDER_TYPE_KEY,
                DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256,
                LineRenderType.State.getBuilder()
                        .line(new RenderState.LineState(OptionalDouble.of(LINE_THICKNESS)))
                        .layer(LineRenderType.field_239235_M_)
                        .transparency(TRANSLUCENT_TRANSPARENCY)
                        .texture(NO_TEXTURE)
                        .depthTest(DEPTH_ALWAYS)
                        .cull(CULL_DISABLED)
                        .lightmap(LIGHTMAP_DISABLED)
                        .writeMask(COLOR_WRITE)
                        .build(true));
    }
}
