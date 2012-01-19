package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import java.util.List;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ModificationAdresseNotification extends ModificationAdresseBase {

	protected ModificationAdresseNotification(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
	}

	@Override
	protected void doHandle(PersonnePhysique pp, List<EvenementCivilExterneErreur> warnings) throws EvenementCivilException {
		fermeAdresseTiersTemporaire(pp, getDate().getOneDayBefore());
	}
}
