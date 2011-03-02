package ch.vd.uniregctb.evenement.civil.interne.changement.nom;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de changement de nom.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ChangementNomAdapter extends EvenementCivilInterneBase {// implements ChangementNom {

	protected static Logger LOGGER = Logger.getLogger(ChangementNomAdapter.class);

	private ChangementNomHandler handler;

	protected ChangementNomAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, ChangementNomHandler handler) throws EvenementCivilInterneException {
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
