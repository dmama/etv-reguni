package ch.vd.unireg.evenement.civil.engine.ech;

import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.type.TypeAdresseCivil;

public class AdresseResidenceSecondaireComparisonStrategy extends AdresseResidenceComparisonStrategy {

	private static final String ATTRIBUT = "adresse de résidence secondaire";

	public AdresseResidenceSecondaireComparisonStrategy(ServiceInfrastructureService infraService) {
		super(infraService);
	}

	@Override
	protected String getAttribute() {
		return ATTRIBUT;
	}

	@Override
	protected boolean isTakenIntoAccount(Adresse adresse) {
		return adresse.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE;
	}
}
