package com.agricraft.agricraft.mixin.create;

import com.agricraft.agricraft.compat.create.CreateHarvestCompat;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No Create classes are linked when the optional mod is absent. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour", remap = false)
public abstract class HarvesterMovementBehaviourMixin {
    @Inject(method = "visitNewPosition", at = @At("HEAD"), cancellable = true, remap = false)
    private void agricraft$harvest(@Coerce Object context, BlockPos pos, CallbackInfo callback) {
        if (CreateHarvestCompat.visit(this, context, pos)) callback.cancel();
    }
}
