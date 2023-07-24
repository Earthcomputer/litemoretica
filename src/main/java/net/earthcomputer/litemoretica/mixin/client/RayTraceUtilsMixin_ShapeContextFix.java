package net.earthcomputer.litemoretica.mixin.client;

import fi.dy.masa.litematica.util.RayTraceUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(value = RayTraceUtils.class, remap = false)
public class RayTraceUtilsMixin_ShapeContextFix {
    @Unique
    @Nullable
    private static WeakReference<Entity> litemoretica_currentEntity;

    @Inject(method = "traceToSchematicWorld", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocks(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Lnet/minecraft/util/hit/BlockHitResult;", remap = true))
    private static void traceToSchematicWorld_preRayTraceBlocks(Entity entity, double range, boolean respectRenderRange, boolean targetFluids, CallbackInfoReturnable<BlockHitResult> cir) {
        litemoretica_currentEntity = new WeakReference<>(entity);
    }

    @Inject(method = "traceToSchematicWorld", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocks(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Lnet/minecraft/util/hit/BlockHitResult;", shift = At.Shift.AFTER, remap = true))
    private static void traceToSchematicWorld_postRayTraceBlocks(CallbackInfoReturnable<BlockHitResult> cir) {
        litemoretica_currentEntity = null;
    }

    @Inject(method = "getFurthestSchematicWorldBlockBeforeVanilla", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocksToList(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Ljava/util/List;", remap = true))
    private static void getFurthestSchematicWorldBlockBeforeVanilla_preRayTraceBlocksToList(World worldClient, Entity entity, double maxRange, boolean requireVanillaBlockBehind, CallbackInfoReturnable<BlockPos> cir) {
        litemoretica_currentEntity = new WeakReference<>(entity);
    }

    @Inject(method = "getFurthestSchematicWorldBlockBeforeVanilla", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocksToList(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Ljava/util/List;", shift = At.Shift.AFTER, remap = true))
    private static void getFurthestSchematicWorldBlockBeforeVanilla_postRayTraceBlocksToList(CallbackInfoReturnable<BlockPos> cir) {
        litemoretica_currentEntity = null;
    }

    @Inject(method = "getRayTraceFromEntity", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocks(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Lnet/minecraft/util/hit/BlockHitResult;", remap = true))
    private static void getRayTraceFromEntity_preRayTraceBlocks(World world, Entity entity, boolean useLiquids, double range, CallbackInfoReturnable<HitResult> cir) {
        litemoretica_currentEntity = new WeakReference<>(entity);
    }

    @Inject(method = "getRayTraceFromEntity", at = @At(value = "INVOKE", target = "Lfi/dy/masa/litematica/util/RayTraceUtils;rayTraceBlocks(Lnet/minecraft/world/World;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/world/RaycastContext$FluidHandling;ZZZI)Lnet/minecraft/util/hit/BlockHitResult;", shift = At.Shift.AFTER, remap = true))
    private static void getRayTraceFromEntity_postRayTraceBlocks(CallbackInfoReturnable<HitResult> cir) {
        litemoretica_currentEntity = null;
    }

    @Redirect(method = {"traceFirstStep", "traceLoopSteps"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/util/shape/VoxelShape;", remap = true))
    private static VoxelShape redirectGetOutlineShape(BlockState instance, BlockView blockView, BlockPos blockPos) {
        Entity currentEntity;
        if (litemoretica_currentEntity != null && (currentEntity = litemoretica_currentEntity.get()) != null) {
            return instance.getOutlineShape(blockView, blockPos, ShapeContext.of(currentEntity));
        } else {
            return instance.getOutlineShape(blockView, blockPos);
        }
    }
}
