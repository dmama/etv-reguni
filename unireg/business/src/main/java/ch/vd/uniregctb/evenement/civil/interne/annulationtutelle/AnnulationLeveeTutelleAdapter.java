package ch.vd.uniregctb.evenement.civil.interne.annulationtutelle;

import java.util.List;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Adapter pour l'annulation de levée de tutelle.
 * 
 * @author Pavel BLANCO
 *
 */
public class AnnulationLeveeTutelleAdapter extends EvenementCivilInterneBase implements AnnulationLeveeTutelle {

	private AnnulationLeveeTutelleHandler handler;

	protected AnnulationLeveeTutelleAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, AnnulationLeveeTutelleHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.checkCompleteness(this, erreurs, warnings);
	}

	@Override
	public void validate(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
		handler.validate(this, erreurs, warnings);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {
		return handler.handle(this, warnings);
	}
}
