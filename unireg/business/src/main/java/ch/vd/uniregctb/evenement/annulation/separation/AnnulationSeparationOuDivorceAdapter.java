package ch.vd.uniregctb.evenement.annulation.separation;

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
 * Adapter pour l'annulation de séparation.
 *
 * @author Pavel BLANCO
 *
 */
public class AnnulationSeparationOuDivorceAdapter extends GenericEvenementAdapter implements AnnulationSeparation {

	private AnnulationSeparationOuDivorceHandler handler;

	protected AnnulationSeparationOuDivorceAdapter(EvenementCivilData evenement, EvenementCivilContext context, AnnulationSeparationOuDivorceHandler handler) throws EvenementAdapterException {
		super(evenement, context);
		this.handler = handler;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
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
