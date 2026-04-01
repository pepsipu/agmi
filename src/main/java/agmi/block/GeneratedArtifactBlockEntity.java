package agmi.block;

import agmi.Agmi;
import agmi.ai.ArtifactSpec;
import agmi.util.ArtifactStackData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class GeneratedArtifactBlockEntity extends BlockEntity {
	private ArtifactSpec spec = new ArtifactSpec();

	public GeneratedArtifactBlockEntity(BlockPos pos, BlockState blockState) {
		super(Agmi.GENERATED_ARTIFACT_BLOCK_ENTITY, pos, blockState);
	}

	public void apply(ArtifactSpec spec) {
		this.spec = spec;
		this.setChanged();
		if (this.level != null && !this.level.isClientSide()) {
			BlockState blockState = this.getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, blockState, blockState, Block.UPDATE_CLIENTS);
		}
	}

	public ArtifactSpec spec() {
		return this.spec;
	}

	public ItemStack toItemStack() {
		return ArtifactStackData.createStack(this.spec);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		this.spec.id = input.getStringOr("spec_id", "");
		this.spec.name = input.getStringOr("spec_name", "");
		this.spec.kind = input.getStringOr("spec_kind", "block");
		this.spec.rarity = input.getStringOr("spec_rarity", "rare");
		this.spec.summary = input.getStringOr("spec_summary", "");
		this.spec.itemTexturePrompt = input.getStringOr("spec_item_texture_prompt", input.getStringOr("spec_texture_prompt", ""));
		this.spec.blockTexturePrompt = input.getStringOr("spec_block_texture_prompt", this.spec.itemTexturePrompt);
		this.spec.generatedJavaSource = input.getStringOr("spec_java_source", input.getStringOr("spec_java_preview", ""));
		this.spec.itemTexturePngBase64 = input.getStringOr("spec_item_texture_png_base64", input.getStringOr("spec_texture_png_base64", ""));
		this.spec.blockTexturePngBase64 = input.getStringOr("spec_block_texture_png_base64", this.spec.itemTexturePngBase64);
		this.spec.texturePrompt = this.spec.itemTexturePrompt;
		this.spec.texturePngBase64 = this.spec.itemTexturePngBase64;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		output.putString("spec_id", this.spec.id);
		output.putString("spec_name", this.spec.name);
		output.putString("spec_kind", this.spec.kind);
		output.putString("spec_rarity", this.spec.rarity);
		output.putString("spec_summary", this.spec.summary);
		output.putString("spec_item_texture_prompt", this.spec.itemTexturePrompt());
		output.putString("spec_block_texture_prompt", this.spec.blockTexturePrompt());
		output.putString("spec_java_source", this.spec.generatedJavaSource);
		output.putString("spec_item_texture_png_base64", this.spec.itemTextureBase64());
		output.putString("spec_block_texture_png_base64", this.spec.blockTextureBase64());
	}
}
