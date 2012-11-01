package ch.vd.unireg.interfaces;

import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
import ch.vd.unireg.interfaces.efacture.data.TypeDemande;

public abstract class EvdHelper {

	public static TypeRelationVersIndividu typeRelationFromEvd1(String code) {
		final TypeRelationVersIndividu type;
		if (code == null) {
			type = null;
		}
		else if ("1".equals(code)) {
			type = TypeRelationVersIndividu.CONJOINT;
		}
		else if ("2".equals(code)) {
			type = TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE;
		}
		else if ("3".equals(code)) {
			type = TypeRelationVersIndividu.MERE;
		}
		else if ("4".equals(code)) {
			type = TypeRelationVersIndividu.PERE;
		}
		else if ("101".equals(code)) {
			type = TypeRelationVersIndividu.FILLE;
		}
		else if ("102".equals(code)) {
			type = TypeRelationVersIndividu.FILS;
		}
		else {
			throw new IllegalArgumentException("Code de type de relation inconnu : " + code);
		}
		return type;
	}

	public static TypeDemande getTypeDemandeFromEvd25(RegistrationMode mode) {
		final TypeDemande type;
		if (mode == null) {
			type = null;
		}
		else {
			switch (mode) {
				case DIRECT:
				case STANDARD:
					type = TypeDemande.INSCRIPTION;
					break;
				case UNREGISTER:
					type = TypeDemande.DESINSCRIPTION;
					break;
				default:
				    throw new IllegalArgumentException("Invalid mode : " + mode);
			}
		}
		return type;
	}
}
