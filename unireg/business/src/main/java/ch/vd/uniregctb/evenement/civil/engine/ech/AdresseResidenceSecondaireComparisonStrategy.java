package ch.vd.uniregctb.evenement.civil.engine.ech;

import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public class AdresseResidenceSecondaireComparisonStrategy extends AdresseResidenceComparisonStrategy {

	private static final String ATTRIBUT = "adresse de résidence secondaire";

	@Override
	protected String getAttribute() {
		return ATTRIBUT;
	}

	@Override
	protected boolean isTakenIntoAccount(Adresse adresse) {
		return adresse.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE;
	}
}
