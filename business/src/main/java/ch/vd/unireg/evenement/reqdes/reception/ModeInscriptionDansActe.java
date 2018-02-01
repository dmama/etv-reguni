package ch.vd.unireg.evenement.reqdes.reception;

import ch.vd.unireg.xml.event.reqdes.v1.InscriptionMode;
import ch.vd.unireg.reqdes.ModeInscription;

public enum ModeInscriptionDansActe {

	INSCRIPTION,
	MODIFICATION,
	RADIATION;

	public static ModeInscriptionDansActe valueOf(InscriptionMode mode) {
		if (mode == null) {
			return null;
		}
		switch (mode) {
		case INSCRIPTION:
			return INSCRIPTION;
		case MODIFICATION:
			return MODIFICATION;
		case DEREGISTRATION:
			return RADIATION;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + mode);
		}
	}

	public ModeInscription toCore() {
		switch (this) {
		case INSCRIPTION:
			return ModeInscription.INSCRIPTION;
		case MODIFICATION:
			return ModeInscription.MODIFICATION;
		case RADIATION:
			return ModeInscription.RADIATION;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + this);
		}
	}
}
