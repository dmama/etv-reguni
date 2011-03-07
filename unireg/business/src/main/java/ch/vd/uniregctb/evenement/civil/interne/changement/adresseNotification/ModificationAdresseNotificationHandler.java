package ch.vd.uniregctb.evenement.civil.interne.changement.adresseNotification;

import java.util.HashSet;
import java.util.Set;

import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.evenement.civil.interne.changement.AbstractChangementHandler;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class ModificationAdresseNotificationHandler extends AbstractChangementHandler {

	@Override
	public EvenementCivilInterneBase createAdapter(EvenementCivilExterne event, EvenementCivilContext context) throws EvenementCivilInterneException {
		return new ModificationAdresseNotificationAdapter(event, context);
	}

	@Override
	protected Set<TypeEvenementCivil> getHandledType() {
		final Set<TypeEvenementCivil> types = new HashSet<TypeEvenementCivil>();
		types.add(TypeEvenementCivil.MODIF_ADRESSE_NOTIFICATION);
		types.add(TypeEvenementCivil.CORREC_ADRESSE);
		return types;
	}
}
