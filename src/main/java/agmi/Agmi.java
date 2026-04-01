package agmi;

import agmi.ai.AgmiConfig;
import agmi.ai.ArtifactGenerationService;
import agmi.block.GeneratedArtifactBlock;
import agmi.block.GeneratedArtifactBlockEntity;
import agmi.item.GeneratedArtifactBlockItem;
import agmi.item.GeneratedArtifactItem;
import agmi.net.AgmiNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Agmi implements ModInitializer {
	public static final String MOD_ID = "agmi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static GeneratedArtifactItem GENERATED_ARTIFACT_ITEM;
	public static GeneratedArtifactBlock GENERATED_ARTIFACT_BLOCK;
	public static GeneratedArtifactBlockItem GENERATED_ARTIFACT_BLOCK_ITEM;
	public static BlockEntityType<GeneratedArtifactBlockEntity> GENERATED_ARTIFACT_BLOCK_ENTITY;

	public static AgmiConfig CONFIG;
	public static ArtifactGenerationService GENERATION_SERVICE;

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		CONFIG = AgmiConfig.load();
		ResourceKey<Item> generatedArtifactItemKey = ResourceKey.create(Registries.ITEM, id("generated_artifact"));
		ResourceKey<net.minecraft.world.level.block.Block> generatedArtifactBlockKey = ResourceKey.create(Registries.BLOCK, id("generated_artifact_block"));
		ResourceKey<Item> generatedArtifactBlockItemKey = ResourceKey.create(Registries.ITEM, id("generated_artifact_block"));

		GENERATED_ARTIFACT_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			id("generated_artifact"),
			new GeneratedArtifactItem(
				new Item.Properties()
					.setId(generatedArtifactItemKey)
					.stacksTo(1)
					.rarity(Rarity.RARE)
			)
		);

		GENERATED_ARTIFACT_BLOCK = Registry.register(
			BuiltInRegistries.BLOCK,
			id("generated_artifact_block"),
			new GeneratedArtifactBlock(
				BlockBehaviour.Properties.of()
					.setId(generatedArtifactBlockKey)
					.strength(2.0F, 3.0F)
					.sound(SoundType.AMETHYST)
					.noLootTable()
			)
		);

		GENERATED_ARTIFACT_BLOCK_ITEM = Registry.register(
			BuiltInRegistries.ITEM,
			id("generated_artifact_block"),
			new GeneratedArtifactBlockItem(
				GENERATED_ARTIFACT_BLOCK,
				new Item.Properties()
					.setId(generatedArtifactBlockItemKey)
					.stacksTo(1)
					.rarity(Rarity.RARE)
			)
		);

		GENERATED_ARTIFACT_BLOCK_ENTITY = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			id("generated_artifact_block_entity"),
			FabricBlockEntityTypeBuilder.create(GeneratedArtifactBlockEntity::new, GENERATED_ARTIFACT_BLOCK).build()
		);

		GENERATION_SERVICE = new ArtifactGenerationService(CONFIG);
		AgmiNetworking.init();
		LOGGER.info("AGMI initialized. OpenRouter configured: {}", CONFIG.hasApiKey());
	}
}
