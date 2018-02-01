package ch.vd.unireg.evenement.civil.engine.ech;

import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseResidenceSecondaireComparisonStrategyTest extends AbstractAdresseResidenceComparisonStrategyTest {

	@Override
	protected TypeAdresseCivil getTypeAdresseResidence() {
		return TypeAdresseCivil.SECONDAIRE;
	}

	@Override
	protected AdresseResidenceComparisonStrategy buildStrategy() {
		return new AdresseResidenceSecondaireComparisonStrategy(serviceInfra);
	}

	@Override
	protected String getNomAttribut() {
		return "adresse de résidence secondaire";
	}
}
