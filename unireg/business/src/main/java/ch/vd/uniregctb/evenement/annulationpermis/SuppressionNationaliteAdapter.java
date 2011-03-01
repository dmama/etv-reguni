package ch.vd.uniregctb.evenement.annulationpermis;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour la suppresion de l'obtention d'une nationalité.
 * 
 * @author Pavel BLANCO
 *
 */
public class SuppressionNationaliteAdapter extends GenericEvenementAdapter implements SuppressionNationalite {

	private SuppressionNationaliteHandler handler;

	protected SuppressionNationaliteAdapter(EvenementCivilData evenement, EvenementCivilContext context, SuppressionNationaliteHandler handler) throws EvenementAdapterException {
		super(evenement, context);
		this.handler = handler;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilErreur> erreurs, List<EvenementCivilErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
