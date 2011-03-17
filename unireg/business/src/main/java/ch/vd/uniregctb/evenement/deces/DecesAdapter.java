package ch.vd.uniregctb.evenement.deces;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class DecesAdapter extends GenericEvenementAdapter implements Deces {

	protected static Logger LOGGER = Logger.getLogger(DecesAdapter.class);

/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;


	@Override
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService, DataEventService dataEventService) throws EvenementAdapterException {
		super.init(evenement, serviceCivil, infrastructureService, dataEventService);
		 conjointSurvivant = serviceCivil.getConjoint(evenement.getNumeroIndividuPrincipal(),evenement.getDateEvenement().getOneDayBefore());

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
}
