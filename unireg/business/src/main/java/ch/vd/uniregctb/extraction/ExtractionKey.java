package ch.vd.uniregctb.extraction;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clé d'identification d'une extraction
 */
public final class ExtractionKey implements Serializable {

	private final String visa;
	private final UUID uuid;

	public ExtractionKey(String visa) {
		this.visa = visa;
		this.uuid = UUID.randomUUID();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final ExtractionKey that = (ExtractionKey) o;

		if (!uuid.equals(that.uuid)) return false;
		if (visa != null ? !visa.equals(that.visa) : that.visa != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = visa != null ? visa.hashCode() : 0;
		result = 31 * result + uuid.hashCode();
		return result;
	}

	public String toString() {
		return String.format("{visa=%s, uuid=%s}", visa, uuid);
	}

	public String getVisa() {
		return visa;
	}

	public UUID getUuid() {
		return uuid;
	}
}
