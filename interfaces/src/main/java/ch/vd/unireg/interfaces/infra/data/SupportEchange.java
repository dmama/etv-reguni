package ch.vd.unireg.interfaces.infra.data;

import org.jetbrains.annotations.Nullable;

public enum SupportEchange {
	ACI,
	ALL,
	CSV,
	OCC;

	@Nullable
	protected static SupportEchange getSupportEchange(@Nullable ch.vd.fidor.xml.colladm.v1.SupportEchange echange) {
		if (echange == null) {
			return null;
		}
		switch (echange) {
		case ACI:
			return SupportEchange.ACI;
		case CSV:
			return SupportEchange.CSV;
		case OCC:
			return SupportEchange.OCC;
		case ALL:
			return SupportEchange.ALL;
		default:
			throw new IllegalArgumentException("Type d'echange inconnu = [" + echange + "]");
		}
	}

}
