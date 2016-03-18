package ch.vd.uniregctb.migration.pm.engine.data;

public final class FlagFormesJuridiquesIncompatiblesData {

	private final boolean incompatible;

	public FlagFormesJuridiquesIncompatiblesData(boolean incompatible) {
		this.incompatible = incompatible;
	}

	public boolean isIncompatible() {
		return incompatible;
	}

	public boolean isCompatible() {
		return !incompatible;
	}
}
