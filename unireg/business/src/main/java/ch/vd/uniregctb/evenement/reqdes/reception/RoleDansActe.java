package ch.vd.uniregctb.evenement.reqdes.reception;

import ch.vd.unireg.xml.event.reqdes.v1.StakeholderRole;
import ch.vd.uniregctb.reqdes.TypeRole;

/**
 * Rôles d'une partie prenante dans un acte notarié envoyé de ReqDes
 */
public enum RoleDansActe {

	ACQUEREUR,
	ALIENATEUR,
	AUTRE;

	public static RoleDansActe valueOf(StakeholderRole role) {
		if (role == null) {
			return null;
		}
		switch (role) {
		case BUYER:
			return ACQUEREUR;
		case DISPOSER:
			return ALIENATEUR;
		case OTHER:
			return AUTRE;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + role);
		}
	}

	public TypeRole toCore() {
		switch (this) {
		case ALIENATEUR:
			return TypeRole.ALIENATEUR;
		case ACQUEREUR:
			return TypeRole.ACQUEREUR;
		case AUTRE:
			return TypeRole.AUTRE;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + this);
		}
	}
}
