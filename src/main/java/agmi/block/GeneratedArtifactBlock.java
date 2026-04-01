package agmi.block;

import agmi.ai.ArtifactSpec;
import agmi.codegen.ArtifactCodeExecutor;
import agmi.util.ArtifactStackData;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class GeneratedArtifactBlock extends BaseEntityBlock {
	public static final MapCodec<GeneratedArtifactBlock> CODEC = simpleCodec(GeneratedArtifactBlock::new);

	public GeneratedArtifactBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new GeneratedArtifactBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity) {
			ArtifactSpec spec = ArtifactStackData.read(stack);
			if (spec != null) {
				generatedBlockEntity.apply(spec);
			}
		}
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity) {
				ArtifactCodeExecutor.triggerBlockUse(serverPlayer, generatedBlockEntity.spec(), generatedBlockEntity);
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useItemOn(
		ItemStack stack,
		BlockState state,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity) {
				ArtifactCodeExecutor.triggerBlockUse(serverPlayer, generatedBlockEntity.spec(), generatedBlockEntity);
			}
		}

		return InteractionResult.SUCCESS;
	}

	@Override
	public void playerDestroy(
		Level level,
		Player player,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity,
		ItemStack tool
	) {
		super.playerDestroy(level, player, pos, state, blockEntity, tool);

		if (!level.isClientSide() && !player.isCreative() && blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity) {
			Block.popResource(level, pos, generatedBlockEntity.toItemStack());
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity) {
			return generatedBlockEntity.toItemStack();
		}

		return super.getCloneItemStack(level, pos, state, includeData);
	}
}
