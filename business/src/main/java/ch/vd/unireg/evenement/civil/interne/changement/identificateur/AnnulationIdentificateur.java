package ch.vd.unireg.evenement.civil.interne.changement.identificateur;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;

public class AnnulationIdentificateur extends ChangementIdentificateur {

	public AnnulationIdentificateur(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	protected boolean shouldOverwriteAvs(String navs13) {
		return true;
	}
}
