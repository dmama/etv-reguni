package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.ChangementAdapterBase;
import ch.vd.uniregctb.tiers.PersonnePhysique;

public class ModificationAdresseNotificationAdapter extends ChangementAdapterBase {

	private ModificationAdresseNotificationHandler handler;

	protected ModificationAdresseNotificationAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, ModificationAdresseNotificationHandler handler) throws
			EvenementCivilInterneException {
		super(evenement, context, handler);
		this.handler = handler;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validateSpecific(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}

	@Override
	protected boolean autoriseIndividuInconnuFiscalement() {
		return false;
	}
}
