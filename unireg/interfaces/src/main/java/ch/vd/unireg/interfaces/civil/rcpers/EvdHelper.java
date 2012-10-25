package ch.vd.unireg.interfaces.civil.rcpers;

import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;

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
}
