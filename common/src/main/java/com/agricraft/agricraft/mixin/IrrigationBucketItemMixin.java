package com.agricraft.agricraft.mixin;

import com.agricraft.agricraft.common.block.IrrigationBlock;
import com.agricraft.agricraft.common.block.IrrigationTankBlock;
import com.agricraft.agricraft.common.block.entity.IrrigationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class IrrigationBucketItemMixin extends Item {
    @Shadow @Final private Fluid content;

    protected IrrigationBucketItemMixin(Properties properties) { super(properties); }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void agricraft$useTank(Level level, Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (content != Fluids.WATER && content != Fluids.EMPTY) return;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IrrigationTankBlock tank)) return;
        ItemStack held = player.getItemInHand(hand);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held)) {
            cir.setReturnValue(new InteractionResultHolder<>(InteractionResult.CONSUME, held));
            return;
        }
        // Sneaking bypasses Block.use; route it through the same atomic reservoir transfer.
        InteractionResult result = tank.use(state, level, pos, player, hand, hit);
        cir.setReturnValue(new InteractionResultHolder<>(result == InteractionResult.PASS
                ? InteractionResult.CONSUME : result, player.getItemInHand(hand)));
    }

    @Inject(method = "emptyContents", at = @At("HEAD"), cancellable = true)
    private void agricraft$protectIrrigation(Player player, Level level, BlockPos pos, BlockHitResult hit,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!(level.getBlockState(pos).getBlock() instanceof IrrigationBlock)) return;
        // Also handle direct callers such as dispensers; never fall back to placing flowing
        // world water beside a full tank or destroying another irrigation component.
        boolean stored = content == Fluids.WATER
                && level.getBlockEntity(pos) instanceof IrrigationBlockEntity irrigation
                && level.getBlockState(pos).getBlock() instanceof IrrigationTankBlock
                && (level.isClientSide || irrigation.transferBucket(true));
        cir.setReturnValue(stored);
    }
}
