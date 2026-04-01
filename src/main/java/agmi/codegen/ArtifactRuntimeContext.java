package agmi.codegen;

import agmi.ai.ArtifactSpec;
import agmi.block.GeneratedArtifactBlockEntity;
import agmi.util.ArtifactStackData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public record ArtifactRuntimeContext(ArtifactSpec spec) {
	public String id() {
		return this.spec.id;
	}

	public String name() {
		return this.spec.name;
	}

	public boolean matches(ItemStack stack) {
		ArtifactSpec stackSpec = ArtifactStackData.read(stack);
		return stackSpec != null && this.id().equals(stackSpec.id);
	}

	public boolean matches(BlockEntity blockEntity) {
		return blockEntity instanceof GeneratedArtifactBlockEntity generatedBlockEntity && this.id().equals(generatedBlockEntity.spec().id);
	}
}
