package agmi.codegen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface GeneratedArtifactProgram {
	default void initialize(ArtifactRuntimeContext context) throws Exception {
	}

	default void onCraft(ServerPlayer player, ItemStack stack) throws Exception {
	}

	default void onUse(ServerPlayer player, ItemStack stack) throws Exception {
	}

	default void onHit(ServerLevel level, LivingEntity attacker, LivingEntity target, ItemStack stack) throws Exception {
	}

	default void onBlockUse(ServerPlayer player, BlockEntity blockEntity) throws Exception {
	}
}
