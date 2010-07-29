package ch.vd.uniregctb.evenement.deces;

import org.apache.log4j.Logger;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
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

		final EnumTypeEtatCivil typeEtatCivil = getIndividu().getEtatCivilCourant().getTypeEtatCivil();

		// [UNIREG-1190] on n'expose pas le conjoint dans l'état-civil séparé (pas de différence avec le divorce au niveau fiscal)
		if (typeEtatCivil.equals(EnumTypeEtatCivil.CELIBATAIRE) || typeEtatCivil.equals(EnumTypeEtatCivil.DIVORCE)
				|| typeEtatCivil.equals(EnumTypeEtatCivil.PACS_ANNULE) || typeEtatCivil.equals(EnumTypeEtatCivil.PACS_INTERROMPU)
				|| typeEtatCivil.equals(EnumTypeEtatCivil.VEUF) || typeEtatCivil.equals(EnumTypeEtatCivil.SEPARE)) {
			return null;
		}

		return conjointSurvivant;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}
}
