package agmi.ai;

import agmi.Agmi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class ArtifactCache {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path cacheDir;

	public ArtifactCache(Path cacheDir) {
		this.cacheDir = cacheDir;
	}

	public Optional<ArtifactSpec> load(String signatureHash) {
		Path path = this.pathFor(signatureHash);
		if (!Files.exists(path)) {
			return Optional.empty();
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			return Optional.ofNullable(GSON.fromJson(reader, ArtifactSpec.class));
		} catch (Exception exception) {
			Agmi.LOGGER.warn("Failed to load AGMI cache entry {}", signatureHash, exception);
			return Optional.empty();
		}
	}

	public void save(String signatureHash, ArtifactSpec spec) {
		Path path = this.pathFor(signatureHash);

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(spec, writer);
			}
		} catch (IOException exception) {
			Agmi.LOGGER.warn("Failed to save AGMI cache entry {}", signatureHash, exception);
		}
	}

	private Path pathFor(String signatureHash) {
		return this.cacheDir.resolve("recipes").resolve(signatureHash + ".json");
	}
}
