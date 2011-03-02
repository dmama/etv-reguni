package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneBase;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class AnnulationDecesAdapter extends EvenementCivilInterneBase implements AnnulationDeces {

	protected static Logger LOGGER = Logger.getLogger(AnnulationDecesAdapter.class);

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;

	private AnnulationDecesHandler handler;

	protected AnnulationDecesAdapter(EvenementCivilExterne evenement, EvenementCivilContext context, AnnulationDecesHandler handler) throws EvenementCivilInterneException {
		super(evenement, context);
		this.handler = handler;
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
	}

	public Individu getConjointSurvivant() {

		final TypeEtatCivil typeEtatCivil = getIndividu().getEtatCivilCourant().getTypeEtatCivil();

		// [UNIREG-1190] on n'expose pas le conjoint dans l'état-civil séparé (pas de différence avec le divorce au niveau fiscal)
		if (typeEtatCivil == TypeEtatCivil.CELIBATAIRE || typeEtatCivil == TypeEtatCivil.DIVORCE || typeEtatCivil == TypeEtatCivil.PACS_ANNULE ||
				typeEtatCivil == TypeEtatCivil.PACS_INTERROMPU || typeEtatCivil == TypeEtatCivil.VEUF || typeEtatCivil == TypeEtatCivil.SEPARE) {
			return null;
		}

		return conjointSurvivant;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
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
