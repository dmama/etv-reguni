package ch.vd.unireg.evenement.civil.interne.changement.adresseNotification;

import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.tiers.PersonnePhysique;

public class ModificationAdresseNotification extends ModificationAdresseBase {

	protected ModificationAdresseNotification(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	public ModificationAdresseNotification(EvenementCivilEchFacade event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(event, context, options);
	}

	@Override
	protected void doHandle(PersonnePhysique pp, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		fermeAdresseTiersTemporaire(pp, getDate().getOneDayBefore());
	}
}
