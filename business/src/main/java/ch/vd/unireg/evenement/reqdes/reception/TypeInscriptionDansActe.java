package ch.vd.uniregctb.evenement.reqdes.reception;

import ch.vd.unireg.xml.event.reqdes.v1.InscriptionType;
import ch.vd.uniregctb.reqdes.TypeInscription;

public enum TypeInscriptionDansActe {

	PROPRIETE,
	SERVITUDE,
	CHARGE_FONCIERE,
	ANNOTATION;

	public static TypeInscriptionDansActe valueOf(InscriptionType mode) {
		if (mode == null) {
			return null;
		}
		switch (mode) {
		case PROPERTY:
			return PROPRIETE;
		case EASEMENT:
			return SERVITUDE;
		case PROPERTY_RESPONSIBILITY:
			return CHARGE_FONCIERE;
		case ANNOTATION:
			return ANNOTATION;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + mode);
		}
	}

	public TypeInscription toCore() {
		switch (this) {
		case PROPRIETE:
			return TypeInscription.PROPRIETE;
		case SERVITUDE:
			return TypeInscription.SERVITUDE;
		case CHARGE_FONCIERE:
			return TypeInscription.CHARGE_FONCIERE;
		case ANNOTATION:
			return TypeInscription.ANNOTATION;
		default:
			throw new IllegalArgumentException("Valeur inconnue : " + this);
		}
	}
}
