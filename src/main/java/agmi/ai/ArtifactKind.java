package agmi.ai;

public enum ArtifactKind {
	ITEM("item"),
	BLOCK("block");

	private final String serializedName;

	ArtifactKind(String serializedName) {
		this.serializedName = serializedName;
	}

	public String serializedName() {
		return this.serializedName;
	}

	public static ArtifactKind fromString(String value) {
		if (value == null) {
			return ITEM;
		}

		for (ArtifactKind kind : values()) {
			if (kind.serializedName.equalsIgnoreCase(value)) {
				return kind;
			}
		}

		return ITEM;
	}
}
