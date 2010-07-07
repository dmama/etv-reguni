package ch.vd.uniregctb.evenement.demenagement;

import ch.vd.uniregctb.common.DonneesCivilesException;
import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.EvenementAdapterAvecAdresses;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

/**
 * Modélise un événement de déménagement.
 *
 * @author Ludovic Bertin </a>
 */
public class DemenagementAdapter extends EvenementAdapterAvecAdresses implements Demenagement {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(DemenagementAdapter.class);

	/**
	 * L'adresse de départ.
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private CommuneSimple nouvelleCommunePrincipale;
	
	/**
	 * @throws EvenementAdapterException
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivil,
	 *      ch.vd.uniregctb.interfaces.service.HostCivilService,
	 *      ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilData evenementCivilData, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		super.init(evenementCivilData, serviceCivil, infrastructureService);

		// il faut récupérer les adresses actuelles, ce seront les nouvelles
		// adresses

		// Distinction adresse principale et adresse courrier
		final AdressesCiviles adresses;
		try {
			adresses = serviceCivil.getAdresses(super.getNoIndividu(), evenementCivilData.getDateEvenement().getOneDayBefore(), false);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementAdapterException(e);
		}
		this.ancienneAdressePrincipale = adresses.principale;

		// on recupere la commune de la nouvelle adresse
		try {
			this.nouvelleCommunePrincipale = infrastructureService.getCommuneByAdresse(getNouvelleAdressePrincipale());
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException(e);
		}
	}

	/**
	 * @see ch.vd.uniregctb.evenement.demenagement.Demenagement#getNouvelleCommuneAdressePrincipale()
	 */
	public CommuneSimple getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	/**
	 * @see ch.vd.uniregctb.evenement.demenagement.Demenagement#getNouvelleAdresseCourrier()
	 */
	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	/**
	 * @see ch.vd.uniregctb.evenement.demenagement.Demenagement#getNouvelleAdressePrincipale()
	 */
	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}
}
