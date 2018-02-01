package ch.vd.unireg.evenement.civil.interne.obtentionpermis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ObtentionNationaliteSuisse extends ObtentionNationalite {

	protected ObtentionNationaliteSuisse(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	protected ObtentionNationaliteSuisse(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	/**
	 * Pour les tests uniquement
	 */
	@SuppressWarnings({"JavaDoc"})
	protected ObtentionNationaliteSuisse(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, Integer numeroOfsCommunePrincipale, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, numeroOfsCommunePrincipale, true, context);
	}

	@Override
	protected boolean doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// quelle que soit la nationalité, si l'individu correspond à un non-habitant (= ancien habitant)
		// il faut mettre à jour la nationalité chez nous
		if (pp != null && !pp.isHabitantVD()) {
			pp.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);
		}
		return true;
	}
}
