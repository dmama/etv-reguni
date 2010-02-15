package ch.vd.uniregctb.evenement.mariage;

import org.apache.log4j.Logger;

import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Modélise un événement conjugal (mariage, pacs)
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class MariageAdapter extends GenericEvenementAdapter implements Mariage {

	protected static Logger LOGGER = Logger.getLogger(MariageAdapter.class);

	/**
	 * Le nouveau conjoint de l'individu concerné par le mariage.
	 */
	private Individu nouveauConjoint;

	/**
	 * Récupère la commune à partir de l'adresse principale
	 * @throws EvenementAdapterException
	 */
	@Override
	public void init(EvenementCivilRegroupe evenementCivil, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivil, serviceCivil, infrastructureService);

		/*
		 * Calcul de l'année où a eu lieu l'événement
		 */
		int anneeEvenement = getDate().year();

		/*
		 * Récupération des informations sur le conjoint de l'individu depuis le host.
		 * getIndividu().getConjoint() peut être null si mariage le 01.01
		 */
		EnumAttributeIndividu[] attributs = { EnumAttributeIndividu.CONJOINT };
		final long noIndividu = getIndividu().getNoTechnique();
		Individu individuPrincipal = serviceCivil.getIndividu(noIndividu, anneeEvenement, attributs);
		this.nouveauConjoint = individuPrincipal.getConjoint();
	}

	/*
	 * (non-Javadoc)
	 * @see ch.vd.uniregctb.evenement.mariage.Mariage#getNouveauConjoint()
	 */
	public Individu getNouveauConjoint() {
		return nouveauConjoint;
	}

}
