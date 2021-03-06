package ch.vd.unireg.evenement.civil.interne.annulation.divorce;

import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.annulation.separation.AnnulationSeparationOuDivorce;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;

/**
 * Adapter pour l'annulation de divorce.
 * 
 * @author Pavel BLANCO
 */
public class AnnulationDivorce extends AnnulationSeparationOuDivorce {

	protected AnnulationDivorce(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public AnnulationDivorce(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}
}
