package agmi.ai;

import agmi.Agmi;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OpenRouterClient {
	private static final Gson GSON = new Gson();
	private static final String DEFAULT_API_BASE_URL = "https://openrouter.ai/api/v1";

	private final AgmiConfig config;
	private final HttpClient httpClient;
	private final String chatCompletionsUrl;

	public OpenRouterClient(AgmiConfig config) {
		this.config = config;
		this.httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(Math.max(5, config.requestTimeoutSeconds())))
			.build();
		this.chatCompletionsUrl = normalizeApiBaseUrl(DEFAULT_API_BASE_URL) + "/chat/completions";
	}

	public ArtifactSpec generateArtifact(String signatureHash, String craftingDescription) {
		ArtifactSpec spec = this.requestSpec(signatureHash, craftingDescription);
		if (this.config.generateTextures()) {
			try {
				this.generateMissingTextures(spec);
			} catch (RuntimeException exception) {
				Agmi.LOGGER.warn("Artifact texture generation failed for {}", signatureHash, exception);
				spec.itemTexturePngBase64 = "";
				spec.blockTexturePngBase64 = "";
			}
		}
		return ArtifactSpecSanitizer.sanitize(spec, signatureHash);
	}

	public void generateMissingTextures(ArtifactSpec spec) {
		if (spec == null || this.config.imageModel().isBlank()) {
			return;
		}

		if (spec.itemTextureBase64().isBlank()) {
			spec.itemTexturePngBase64 = this.requestTexture(spec, spec.itemTexturePrompt(), true);
		}

		if (spec.artifactKind() == ArtifactKind.BLOCK && spec.blockTextureBase64().isBlank()) {
			spec.blockTexturePngBase64 = this.requestTexture(spec, spec.blockTexturePrompt(), false);
		}
	}

	private ArtifactSpec requestSpec(String signatureHash, String craftingDescription) {
		JsonObject body = new JsonObject();
		body.addProperty("model", this.config.textModel());
		body.addProperty("temperature", 0.75D);
		body.addProperty("max_tokens", 1800);
		body.add("plugins", responseHealingPlugin());
		body.add("response_format", responseFormatSchema());

		JsonArray messages = new JsonArray();
		messages.add(message("system", systemPrompt()));
		messages.add(message("user", userPrompt(signatureHash, craftingDescription)));
		body.add("messages", messages);

		JsonObject response = this.postJson(this.chatCompletionsUrl, body);
		String content = firstMessageContent(response);
		return GSON.fromJson(content, ArtifactSpec.class);
	}

	private String requestTexture(ArtifactSpec spec, String texturePrompt, boolean transparentBackground) {
		if (this.config.imageModel().isBlank()) {
			return "";
		}

		JsonObject body = new JsonObject();
		body.addProperty("model", this.config.imageModel());
		body.add("modalities", modalities("text", "image"));
		body.add("messages", singleUserMessage(imagePrompt(spec, texturePrompt, transparentBackground)));

		JsonObject imageConfig = new JsonObject();
		imageConfig.addProperty("aspect_ratio", "1:1");
		imageConfig.addProperty("image_size", "1K");
		body.add("image_config", imageConfig);

		JsonObject response = this.postJson(this.chatCompletionsUrl, body);
		return firstMessageImageBase64(response);
	}

	private JsonObject postJson(String url, JsonObject body) {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofSeconds(Math.max(5, this.config.requestTimeoutSeconds())))
			.header("Authorization", "Bearer " + this.config.apiKey())
			.header("Content-Type", "application/json");

		if (!this.config.siteUrl().isBlank()) {
			requestBuilder.header("HTTP-Referer", this.config.siteUrl());
		}

		if (!this.config.siteName().isBlank()) {
			requestBuilder.header("X-OpenRouter-Title", this.config.siteName());
		}

		HttpRequest request = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();

		try {
			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			String responseBody = response.body() == null ? "" : response.body();
			if (response.statusCode() >= 400) {
				throw new IllegalStateException(describeErrorResponse(response.statusCode(), responseBody));
			}

			JsonObject parsed = GSON.fromJson(responseBody, JsonObject.class);
			if (parsed == null) {
				throw new IllegalStateException("OpenRouter returned an empty response body");
			}
			return parsed;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("OpenRouter request failed", exception);
		} catch (IOException exception) {
			throw new IllegalStateException("OpenRouter request failed", exception);
		}
	}

	private static JsonArray responseHealingPlugin() {
		JsonArray plugins = new JsonArray();
		JsonObject plugin = new JsonObject();
		plugin.addProperty("id", "response-healing");
		plugins.add(plugin);
		return plugins;
	}

	private static JsonObject responseFormatSchema() {
		JsonObject responseFormat = new JsonObject();
		responseFormat.addProperty("type", "json_schema");

		JsonObject jsonSchema = new JsonObject();
		jsonSchema.addProperty("name", "agmi_artifact_spec");
		jsonSchema.addProperty("strict", true);
		jsonSchema.add("schema", schemaObject());
		responseFormat.add("json_schema", jsonSchema);
		return responseFormat;
	}

	private static JsonObject schemaObject() {
		JsonObject schema = new JsonObject();
		schema.addProperty("type", "object");

		JsonObject properties = new JsonObject();
		properties.add("name", stringSchema());
		properties.add("kind", enumSchema("item", "block"));
		properties.add("rarity", enumSchema("common", "uncommon", "rare", "epic"));
		properties.add("summary", stringSchema());
		properties.add("itemTexturePrompt", stringSchema());
		properties.add("blockTexturePrompt", stringSchema());
		properties.add("generatedJavaSource", stringSchema());
		schema.add("properties", properties);
		schema.add("required", required("name", "kind", "rarity", "summary", "itemTexturePrompt", "blockTexturePrompt", "generatedJavaSource"));
		schema.addProperty("additionalProperties", false);
		return schema;
	}

	private static JsonObject stringSchema() {
		JsonObject schema = new JsonObject();
		schema.addProperty("type", "string");
		return schema;
	}

	private static JsonObject enumSchema(String... values) {
		JsonObject schema = new JsonObject();
		schema.addProperty("type", "string");
		JsonArray enumArray = new JsonArray();
		for (String value : values) {
			enumArray.add(value);
		}
		schema.add("enum", enumArray);
		return schema;
	}

	private static JsonArray required(String... values) {
		JsonArray required = new JsonArray();
		for (String value : values) {
			required.add(value);
		}
		return required;
	}

	private static JsonObject message(String role, String content) {
		JsonObject message = new JsonObject();
		message.addProperty("role", role);
		message.addProperty("content", content);
		return message;
	}

	private static JsonArray singleUserMessage(String content) {
		JsonArray messages = new JsonArray();
		messages.add(message("user", content));
		return messages;
	}

	private static JsonArray modalities(String... values) {
		JsonArray modalities = new JsonArray();
		for (String value : values) {
			modalities.add(value);
		}
		return modalities;
	}

	private static String systemPrompt() {
		return """
			You invent original Minecraft artifacts for a mod.
			Return JSON only.
			The generatedJavaSource field is compiled and executed verbatim on the Minecraft server.
			You may use arbitrary server-side Java and Minecraft/Fabric APIs available on the classpath.
			Design surprising and expressive results.
			""";
	}

	private static String userPrompt(String signatureHash, String craftingDescription) {
		String className = ArtifactSpec.generatedClassNameForHash(signatureHash);
		return """
			Crafting table contents:
			%s

			Constraints:
			- invent exactly one artifact
			- support kinds: item or block
			- summary should be 1-2 sentences
			- itemTexturePrompt must describe a single square Minecraft inventory sprite with a transparent background, no text, and no watermark
			- blockTexturePrompt must describe a tileable square Minecraft block texture for all six cube faces; use an empty string only when kind is item
			- generatedJavaSource must be raw Java source with no markdown fences
			- generatedJavaSource must be one complete compilable Java source file, not pseudocode
			- generatedJavaSource must begin with package agmi.generated;
			- generatedJavaSource must declare exactly this public class: %s
			- generatedJavaSource must implement agmi.codegen.GeneratedArtifactProgram
			- generatedJavaSource must include a public no-arg constructor
			- generatedJavaSource must end with the final closing brace of the class
			- close every brace, parenthesis, and string literal
			- prefer fully qualified class names or explicit imports; do not rely on omitted imports
			- valid hook methods are:
			  initialize(agmi.codegen.ArtifactRuntimeContext context)
			  onCraft(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack)
			  onUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack)
			  onHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LivingEntity attacker, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.item.ItemStack stack)
			  onBlockUse(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.block.entity.BlockEntity blockEntity)
			- initialize(context) is called once when the generated program is first compiled and loaded
			- use initialize(context) to register your own Fabric or Minecraft hooks when you want behavior beyond the built-in direct item/block hooks
			- only override the hooks you need
			- the code runs on the logical server and may use arbitrary Minecraft/Fabric Java APIs
			- useful server-side API patterns include:
			  player.serverLevel()
			  player.level()
			  player.getServer()
			  player.blockPosition()
			  player.position()
			  player.teleportTo(...)
			  player.displayClientMessage(...)
			  player.sendSystemMessage(...)
			  player.addEffect(...)
			  player.startUsingItem(...)
			  player.getInventory()
			  level.explode(...)
			  net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level)
			  level.addFreshEntity(...)
			  level.setBlock(...)
			  level.getBlockEntity(...)
			  player.getServer().getCommands().performPrefixedCommand(...)
			  net.minecraft.world.item.ItemStack stack and its components / custom data
			  standard Java collections, math, random, strings, control flow, helper methods, and private fields
			  context.spec()
			  context.id()
			  context.name()
			  context.matches(stack)
			  context.matches(blockEntity)
			  net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> { ... })
			  net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> { ... })
			  net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> { ... })
			  net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> { ... })
			  net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> { ... })
			- you may import any Minecraft, Fabric, or Java classes available on the classpath
			- prefer direct API usage over pseudocode
			- use this exact structural template as the baseline shape of the file:
			  package agmi.generated;
			  
			  public final class %s implements agmi.codegen.GeneratedArtifactProgram {
			  	public %s() {
			  	}
			  
			  	@Override
			  	public void initialize(agmi.codegen.ArtifactRuntimeContext context) throws Exception {
			  	}
			  
			  	@Override
			  	public void onCraft(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.ItemStack stack) throws Exception {
			  	}
			  }
			- you may add imports, fields, helper methods, more overridden hooks, and extra logic, but keep the file syntactically valid Java
			- every object field must be present in the JSON output
			- use empty strings for unused text fields
			- signature hash: %s
			""".formatted(craftingDescription, className, className, className, signatureHash);
	}

	private static String imagePrompt(ArtifactSpec spec, String texturePrompt, boolean transparentBackground) {
		return """
			Create a single square Minecraft texture for this generated artifact.
			Name: %s
			Kind: %s
			Summary: %s
			Style: crisp stylized game art, no text, no watermark.
			Background: %s
			Texture guidance: %s
			""".formatted(
			spec.name,
			spec.kind,
			spec.summary,
			transparentBackground ? "fully transparent outside the item silhouette" : "fully filled tileable texture suitable for a placed cube block",
			texturePrompt
		);
	}

	private static String firstMessageContent(JsonObject response) {
		JsonObject message = firstMessage(response);
		JsonElement content = message.get("content");
		if (content == null) {
			throw new IllegalStateException("OpenRouter response did not contain message content");
		}

		if (content.isJsonPrimitive()) {
			return content.getAsString();
		}

		if (content.isJsonArray()) {
			StringBuilder builder = new StringBuilder();
			for (JsonElement partElement : content.getAsJsonArray()) {
				JsonObject part = partElement.getAsJsonObject();
				if (part.has("text")) {
					builder.append(part.get("text").getAsString());
				}
			}
			return builder.toString();
		}

		throw new IllegalStateException("Unsupported OpenRouter message content format");
	}

	private static String firstMessageImageBase64(JsonObject response) {
		JsonObject message = firstMessage(response);
		JsonArray images = message.getAsJsonArray("images");
		if (images != null && !images.isEmpty()) {
			String imageUrl = extractImageUrl(images.get(0).getAsJsonObject());
			if (!imageUrl.isBlank()) {
				return stripDataUrl(imageUrl);
			}
		}

		JsonElement content = message.get("content");
		if (content != null && content.isJsonArray()) {
			for (JsonElement partElement : content.getAsJsonArray()) {
				JsonObject part = partElement.getAsJsonObject();
				String imageUrl = extractImageUrl(part);
				if (!imageUrl.isBlank()) {
					return stripDataUrl(imageUrl);
				}
			}
		}

		return "";
	}

	private static JsonObject firstMessage(JsonObject response) {
		JsonArray choices = response.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			throw new IllegalStateException("OpenRouter response did not contain choices");
		}

		JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
		if (message == null) {
			throw new IllegalStateException("OpenRouter response did not contain a message");
		}

		return message;
	}

	private static String extractImageUrl(JsonObject imageObject) {
		if (imageObject == null) {
			return "";
		}

		if (imageObject.has("image_url") && imageObject.get("image_url").isJsonObject()) {
			JsonObject imageUrl = imageObject.getAsJsonObject("image_url");
			if (imageUrl.has("url")) {
				return imageUrl.get("url").getAsString();
			}
		}

		if (imageObject.has("imageUrl") && imageObject.get("imageUrl").isJsonObject()) {
			JsonObject imageUrl = imageObject.getAsJsonObject("imageUrl");
			if (imageUrl.has("url")) {
				return imageUrl.get("url").getAsString();
			}
		}

		return "";
	}

	private static String stripDataUrl(String value) {
		int commaIndex = value.indexOf(',');
		return value.startsWith("data:") && commaIndex >= 0
			? value.substring(commaIndex + 1)
			: value;
	}

	private static String normalizeApiBaseUrl(String apiBaseUrl) {
		String trimmed = apiBaseUrl == null ? "" : apiBaseUrl.trim();
		if (trimmed.isBlank()) {
			return DEFAULT_API_BASE_URL;
		}

		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	private static String describeErrorResponse(int statusCode, String body) {
		String compactBody = body == null ? "" : body.replaceAll("\\s+", " ").trim();
		if (compactBody.length() > 280) {
			compactBody = compactBody.substring(0, 280) + "...";
		}

		return "OpenRouter request failed: HTTP " + statusCode + (compactBody.isBlank() ? "" : " " + compactBody);
	}
}
