package ch.vd.uniregctb.evenement.depart;

import ch.vd.uniregctb.common.DonneesCivilesException;
import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Modélise un événement de depart.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class DepartAdapter extends GenericEvenementAdapter implements Depart {

	/** LOGGER log4J */
	protected static Logger LOGGER = Logger.getLogger(DepartAdapter.class);


	/**
	 * l'adresse principale avant départ
	 */
	private Adresse ancienneAdressePrincipale;

	/**
	 * La commune de la nouvelle adresse principale.
	 */
	private Commune nouvelleCommunePrincipale;

	/**
	 * La commune de l'adresse principale avant départ
	 */
	private Commune ancienneCommunePrincipale;

	/**
	 * l'adresse secondaire avant départ
	 */
	private Adresse ancienneAdresseSecondaire;
	/**
	 * La commune de l'adresse secondaire avant départ
	 */
	private Commune ancienneCommuneSecondaire;

	/**
	 * adresse courrier avant départ
	 */
	private Adresse ancienneAdresseCourrier;

	private Pays paysInconnue;




	/**
	 * @throws EvenementAdapterException
	 * @see ch.vd.uniregctb.evenement.GenericEvenementAdapter#init(ch.vd.uniregctb.evenement.EvenementCivil,
	 *      ch.vd.uniregctb.interfaces.service.HostCivilService, ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService)
	 */
	@Override
	public void init(EvenementCivilRegroupe evenementCivilRegroupe, ServiceCivilService serviceCivil,
			ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {

		super.init(evenementCivilRegroupe, serviceCivil, infrastructureService);

		// on récupère les anciennes adresses (= à la date d'événement)
		final AdressesCiviles adresses;
		try {
			adresses = serviceCivil.getAdresses(super.getIndividu().getNoTechnique(), evenementCivilRegroupe.getDateEvenement(), false);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementAdapterException(e);
		}

		this.ancienneAdressePrincipale = adresses.principale;
		this.ancienneAdresseCourrier = adresses.courrier;
		this.ancienneAdresseSecondaire=adresses.secondaire;

		try {
			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommunePrincipale = infrastructureService.getCommuneByAdresse(ancienneAdressePrincipale);

			// on récupère la commune de l'adresse principale avant le départ
			this.ancienneCommuneSecondaire = infrastructureService.getCommuneByAdresse(ancienneAdresseSecondaire);

			// on récupère la commune de la nouvelle adresse principal
			this.nouvelleCommunePrincipale = infrastructureService.getCommuneByAdresse(getNouvelleAdressePrincipale());

			this.paysInconnue= infrastructureService.getPaysInconnu();
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException(e);
		}


	}

	/**
	 * @see ch.vd.uniregctb.evenement.depart.Depart#getNouvelleAdresseCourrier()
	 */
	public Adresse getNouvelleAdresseCourrier() {
		return getAdresseCourrier();
	}

	/**
	 * @see ch.vd.uniregctb.evenement.depart.Depart#getNouvelleAdressePrincipale()
	 */
	public Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale();
	}

	public Commune getNouvelleCommunePrincipale() {
		return this.nouvelleCommunePrincipale;
	}

	public Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public Adresse getAncienneAdresseCourrier() {
		return ancienneAdresseCourrier;
	}

	public Adresse getAncienneAdresseSecondaire() {

		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommuneSecondaire() {

		return ancienneCommuneSecondaire;
	}

	public Pays getPaysInconnu() {
		return paysInconnue;
	}

}
