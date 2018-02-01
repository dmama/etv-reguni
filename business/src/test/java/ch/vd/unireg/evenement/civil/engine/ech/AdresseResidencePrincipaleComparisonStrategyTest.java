package ch.vd.unireg.evenement.civil.engine.ech;

import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseResidencePrincipaleComparisonStrategyTest extends AbstractAdresseResidenceComparisonStrategyTest {

	@Override
	protected TypeAdresseCivil getTypeAdresseResidence() {
		return TypeAdresseCivil.PRINCIPALE;
	}

	@Override
	protected AdresseResidenceComparisonStrategy buildStrategy() {
		return new AdresseResidencePrincipaleComparisonStrategy(serviceInfra);
	}

	@Override
	protected String getNomAttribut() {
		return "adresse de résidence principale";
	}
}
