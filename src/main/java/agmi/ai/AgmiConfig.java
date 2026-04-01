package agmi.ai;

import agmi.Agmi;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class AgmiConfig {
	private static final String OPENROUTER_API_KEY_ENV = "OPENROUTER_API_KEY";
	private static final String DEFAULT_TEXT_MODEL = "openai/gpt-5.4-nano";
	private static final String DEFAULT_IMAGE_MODEL = "google/gemini-2.5-flash-image";
	private static final String LEGACY_IMAGE_MODEL = "google/gemini-2.5-flash-image-preview";

	private final Path baseDir;
	private final String apiKey;
	private final String textModel;
	private final String imageModel;
	private final String siteUrl;
	private final String siteName;
	private final boolean overrideVanillaCrafting;
	private final boolean generateTextures;
	private final int requestTimeoutSeconds;

	private AgmiConfig(
		Path baseDir,
		String apiKey,
		String textModel,
		String imageModel,
		String siteUrl,
		String siteName,
		boolean overrideVanillaCrafting,
		boolean generateTextures,
		int requestTimeoutSeconds
	) {
		this.baseDir = baseDir;
		this.apiKey = apiKey;
		this.textModel = textModel;
		this.imageModel = imageModel;
		this.siteUrl = siteUrl;
		this.siteName = siteName;
		this.overrideVanillaCrafting = overrideVanillaCrafting;
		this.generateTextures = generateTextures;
		this.requestTimeoutSeconds = requestTimeoutSeconds;
	}

	public static AgmiConfig load() {
		Path baseDir = FabricLoader.getInstance().getConfigDir().resolve(Agmi.MOD_ID);
		Path propertiesPath = baseDir.resolve("agmi.properties");
		Properties properties = new Properties();

		try {
			Files.createDirectories(baseDir);

			if (Files.exists(propertiesPath)) {
				try (Reader reader = Files.newBufferedReader(propertiesPath)) {
					properties.load(reader);
				}
			} else {
				properties.setProperty("openrouter.api_key", "");
				properties.setProperty("openrouter.text_model", DEFAULT_TEXT_MODEL);
				properties.setProperty("openrouter.image_model", DEFAULT_IMAGE_MODEL);
				properties.setProperty("openrouter.site_url", "");
				properties.setProperty("openrouter.site_name", "AGMI");
				properties.setProperty("crafting.override_vanilla", "true");
				properties.setProperty("textures.generate", "true");
				properties.setProperty("requests.timeout_seconds", "60");

				storeProperties(propertiesPath, properties);
			}
		} catch (IOException exception) {
			Agmi.LOGGER.error("Failed to load AGMI config", exception);
		}

		String configuredTextModel = properties.getProperty("openrouter.text_model", DEFAULT_TEXT_MODEL).trim();
		String configuredImageModel = properties.getProperty("openrouter.image_model", DEFAULT_IMAGE_MODEL).trim();
		String textModel = resolveTextModel(configuredTextModel);
		String imageModel = resolveImageModel(configuredImageModel);
		boolean migrated = false;

		if (!textModel.equals(configuredTextModel)) {
			properties.setProperty("openrouter.text_model", textModel);
			migrated = true;
		}
		if (!imageModel.equals(configuredImageModel)) {
			properties.setProperty("openrouter.image_model", imageModel);
			migrated = true;
		}
		if (migrated) {
			try {
				storeProperties(propertiesPath, properties);
			} catch (IOException exception) {
				Agmi.LOGGER.warn("Failed to persist AGMI config migration", exception);
			}
		}

		String apiKey = System.getenv(OPENROUTER_API_KEY_ENV);
		if (apiKey == null || apiKey.isBlank()) {
			apiKey = properties.getProperty("openrouter.api_key", "").trim();
		}

		return new AgmiConfig(
			baseDir,
			apiKey,
			textModel,
			imageModel,
			properties.getProperty("openrouter.site_url", "").trim(),
			properties.getProperty("openrouter.site_name", "AGMI").trim(),
			Boolean.parseBoolean(properties.getProperty("crafting.override_vanilla", "true")),
			Boolean.parseBoolean(properties.getProperty("textures.generate", "true")),
			parseInt(properties.getProperty("requests.timeout_seconds"), 60)
		);
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static String resolveImageModel(String configuredValue) {
		String trimmed = configuredValue == null ? "" : configuredValue.trim();
		if (trimmed.isBlank()
			|| LEGACY_IMAGE_MODEL.equals(trimmed)
			|| "google/gemini-3.1-flash-image-preview".equals(trimmed)) {
			return DEFAULT_IMAGE_MODEL;
		}

		return trimmed;
	}

	private static String resolveTextModel(String configuredValue) {
		String trimmed = configuredValue == null ? "" : configuredValue.trim();
		if (trimmed.isBlank()) {
			return DEFAULT_TEXT_MODEL;
		}

		return trimmed;
	}

	private static void storeProperties(Path propertiesPath, Properties properties) throws IOException {
		try (Writer writer = Files.newBufferedWriter(propertiesPath)) {
			properties.store(
				writer,
				"AGMI runtime configuration. Prefer OPENROUTER_API_KEY for the secret instead of writing it here."
			);
		}
	}

	public Path baseDir() {
		return this.baseDir;
	}

	public Path cacheDir() {
		return this.baseDir.resolve("cache");
	}

	public boolean hasApiKey() {
		return !this.apiKey.isBlank();
	}

	public String apiKey() {
		return this.apiKey;
	}

	public String textModel() {
		return this.textModel;
	}

	public String imageModel() {
		return this.imageModel;
	}

	public String siteUrl() {
		return this.siteUrl;
	}

	public String siteName() {
		return this.siteName;
	}

	public boolean overrideVanillaCrafting() {
		return this.overrideVanillaCrafting;
	}

	public boolean generateTextures() {
		return this.generateTextures;
	}

	public int requestTimeoutSeconds() {
		return this.requestTimeoutSeconds;
	}
}
