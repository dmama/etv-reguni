package ch.vd.uniregctb.evenement.changement.nom;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.EvenementCivilErreur;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de changement de nom.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ChangementNomAdapter extends GenericEvenementAdapter{// implements ChangementNom {

	protected static Logger LOGGER = Logger.getLogger(ChangementNomAdapter.class);

	private ChangementNomHandler handler;

	protected ChangementNomAdapter(EvenementCivilData evenement, EvenementCivilContext context, ChangementNomHandler handler) throws EvenementAdapterException {
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
