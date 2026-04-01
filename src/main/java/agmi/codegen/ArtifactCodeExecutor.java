package agmi.codegen;

import agmi.Agmi;
import agmi.ai.ArtifactSpec;
import agmi.util.ArtifactStackData;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ArtifactCodeExecutor {
	private static final Map<String, LoadedProgram> PROGRAMS = new ConcurrentHashMap<>();

	private ArtifactCodeExecutor() {
	}

	public static void triggerCraft(ServerPlayer player, ItemStack stack) {
		ArtifactSpec spec = ArtifactStackData.read(stack);
		if (spec == null) {
			return;
		}

		run(spec, player, program -> program.onCraft(player, stack.copy()));
	}

	public static void triggerUse(ServerPlayer player, ItemStack stack) {
		ArtifactSpec spec = ArtifactStackData.read(stack);
		if (spec == null) {
			return;
		}

		run(spec, player, program -> program.onUse(player, stack.copy()));
	}

	public static void triggerHit(LivingEntity attacker, LivingEntity target, ItemStack stack) {
		if (!(attacker.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		ArtifactSpec spec = ArtifactStackData.read(stack);
		if (spec == null) {
			return;
		}

		ServerPlayer feedbackPlayer = attacker instanceof ServerPlayer serverPlayer ? serverPlayer : null;
		run(spec, feedbackPlayer, program -> program.onHit(serverLevel, attacker, target, stack.copy()));
	}

	public static void triggerBlockUse(ServerPlayer player, ArtifactSpec spec, BlockEntity blockEntity) {
		if (spec == null) {
			return;
		}

		run(spec, player, program -> program.onBlockUse(player, blockEntity));
	}

	private static void run(ArtifactSpec spec, ServerPlayer feedbackPlayer, ProgramAction action) {
		if (!spec.hasExecutableSource()) {
			return;
		}

		try {
			action.run(load(spec));
		} catch (Exception exception) {
			Agmi.LOGGER.error("Generated artifact code failed for {}", spec.id, exception);
			if (feedbackPlayer != null) {
				feedbackPlayer.sendSystemMessage(Component.literal("AGMI generated code failed for " + spec.name));
			}
		}
	}

	private static GeneratedArtifactProgram load(ArtifactSpec spec) throws Exception {
		String fingerprint = fingerprint(spec);
		return PROGRAMS.computeIfAbsent(fingerprint, ignored -> compileAndLoad(spec, fingerprint)).program();
	}

	private static LoadedProgram compileAndLoad(ArtifactSpec spec, String fingerprint) {
		try {
			Path root = Agmi.CONFIG.cacheDir().resolve("generated-code").resolve(fingerprint);
			Path sourceRoot = root.resolve("src");
			Path classesRoot = root.resolve("classes");
			Path sourcePath = sourceRoot.resolve(spec.generatedClassName().replace('.', '/') + ".java");
			Files.createDirectories(sourcePath.getParent());
			Files.createDirectories(classesRoot);
			Files.writeString(sourcePath, spec.generatedJavaSource, StandardCharsets.UTF_8);

			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new IllegalStateException("No Java compiler is available in this runtime.");
			}

			DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
			try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
				fileManager.setLocation(StandardLocation.CLASS_OUTPUT, java.util.List.of(classesRoot.toFile()));
				Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(sourcePath.toFile());
				String classpath = System.getProperty("java.class.path", "");
				java.util.List<String> options = java.util.List.of("-classpath", classpath, "-d", classesRoot.toString());
				boolean success = Boolean.TRUE.equals(compiler.getTask(null, fileManager, diagnostics, options, null, units).call());
				if (!success) {
					StringBuilder builder = new StringBuilder("Compilation failed for ").append(spec.generatedClassName());
					for (var diagnostic : diagnostics.getDiagnostics()) {
						builder.append(System.lineSeparator()).append(diagnostic);
					}
					throw new IllegalStateException(builder.toString());
				}
			}

			URLClassLoader loader = new URLClassLoader(new URL[]{classesRoot.toUri().toURL()}, ArtifactCodeExecutor.class.getClassLoader());
			Class<?> loadedClass = Class.forName(spec.generatedClassName(), true, loader);
			if (!GeneratedArtifactProgram.class.isAssignableFrom(loadedClass)) {
				throw new IllegalStateException(spec.generatedClassName() + " does not implement GeneratedArtifactProgram");
			}

			GeneratedArtifactProgram program = (GeneratedArtifactProgram) loadedClass.getDeclaredConstructor().newInstance();
			program.initialize(new ArtifactRuntimeContext(spec));
			return new LoadedProgram(program, loader);
		} catch (Exception exception) {
			throw new IllegalStateException("Failed to compile generated code for " + spec.id, exception);
		}
	}

	private static String fingerprint(ArtifactSpec spec) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		digest.update(spec.generatedClassName().getBytes(StandardCharsets.UTF_8));
		digest.update((byte) '\n');
		digest.update(spec.generatedJavaSource.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(digest.digest());
	}

	private record LoadedProgram(GeneratedArtifactProgram program, URLClassLoader classLoader) {
	}

	@FunctionalInterface
	private interface ProgramAction {
		void run(GeneratedArtifactProgram program) throws Exception;
	}
}
