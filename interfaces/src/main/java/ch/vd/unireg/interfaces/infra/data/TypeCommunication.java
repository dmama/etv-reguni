package ch.vd.unireg.interfaces.infra.data;

import org.jetbrains.annotations.Nullable;

public enum TypeCommunication {
	ACI,
	CAVS,
	COMM,
	TIERS;

	@Nullable
	protected static TypeCommunication getTypeCommunication(ch.vd.fidor.xml.colladm.v1.TypeCommunication typeCommunication) {
		if (typeCommunication == null) {
			return null;
		}
		switch (typeCommunication) {
		case ACI:
			return TypeCommunication.ACI;
		case CAVS:
			return TypeCommunication.CAVS;
		case COMM:
			return TypeCommunication.COMM;
		case TIERS:
			return TypeCommunication.TIERS;
		default:
			throw new IllegalArgumentException("Type de communication inconnu = [" + typeCommunication + "]");
		}
	}

}
