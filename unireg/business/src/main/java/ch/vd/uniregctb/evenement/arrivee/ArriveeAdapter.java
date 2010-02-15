package ch.vd.uniregctb.evenement.arrivee;

import ch.vd.uniregctb.common.DonneesCivilesException;
import org.apache.log4j.Logger;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Modélise un événement d'arrivée.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ArriveeAdapter extends GenericEvenementAdapter implements Arrivee {

	protected static Logger LOGGER = Logger.getLogger(ArriveeAdapter.class);

	private Adresse ancienneAdressePrincipale;

	private Adresse ancienneAdresseSecondaire;

	private Commune ancienneCommunePrincipale;

	private Commune ancienneCommuneSecondaire;

	private Commune nouvelleCommunePrincipale;

	private Commune nouvelleCommuneSecondaire;

	public final Adresse getAncienneAdressePrincipale() {
		return ancienneAdressePrincipale;
	}

	public final Adresse getAncienneAdresseSecondaire() {
		return ancienneAdresseSecondaire;
	}

	public Commune getAncienneCommunePrincipale() {
		return ancienneCommunePrincipale;
	}

	public Commune getAncienneCommuneSecondaire() {
		return ancienneCommuneSecondaire;
	}

	public final Adresse getNouvelleAdressePrincipale() {
		return getAdressePrincipale(); // par définition
	}

	public final Adresse getNouvelleAdresseSecondaire() {
		return getAdresseSecondaire(); // par définition
	}

	public final Commune getNouvelleCommunePrincipale() {
		return nouvelleCommunePrincipale;
	}

	public final Commune getNouvelleCommuneSecondaire() {
		return nouvelleCommuneSecondaire;
	}

	@Override
	public void init(EvenementCivilRegroupe evenementCivilRegroupe, ServiceCivilService serviceCivil,
			ServiceInfrastructureService infrastructureService) throws EvenementAdapterException {
		Assert.isTrue(isEvenementArrivee(evenementCivilRegroupe.getType()));

		super.init(evenementCivilRegroupe, serviceCivil, infrastructureService);

		// on récupère les nouvelles adresses (= à la date d'événement)
		final RegDate veilleArrivee = evenementCivilRegroupe.getDateEvenement().getOneDayBefore();
		final AdressesCiviles anciennesAdresses;
		try {
			anciennesAdresses = serviceCivil.getAdresses(super.getIndividu().getNoTechnique(), veilleArrivee, false);
		}
		catch (DonneesCivilesException e) {
			throw new EvenementAdapterException(e);
		}

		this.ancienneAdressePrincipale = anciennesAdresses.principale;
		this.ancienneAdresseSecondaire = anciennesAdresses.secondaire;

		try {
			// on récupère les nouvelles communes
			this.nouvelleCommunePrincipale = infrastructureService.getCommuneByAdresse(getNouvelleAdressePrincipale());
			this.nouvelleCommuneSecondaire = infrastructureService.getCommuneByAdresse(getNouvelleAdresseSecondaire());

			// on récupère les anciennes communes
			this.ancienneCommunePrincipale = infrastructureService.getCommuneByAdresse(ancienneAdressePrincipale);
			this.ancienneCommuneSecondaire = infrastructureService.getCommuneByAdresse(ancienneAdresseSecondaire);
		}
		catch (InfrastructureException e) {
			throw new EvenementAdapterException(e);
		}

	}

	@Override
	public boolean isContribuablePresentBefore() {
		/*
		 * par définition, dans le case d'une arrivée le contribuable peut ne pas encore exister dans la base de données fiscale
		 */
		return false;
	}

	private boolean isEvenementArrivee(TypeEvenementCivil type) {
		boolean isPresent = false;
		if (type.equals(TypeEvenementCivil.ARRIVEE_DANS_COMMUNE) || type.equals(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HC)
				|| type.equals(TypeEvenementCivil.ARRIVEE_PRINCIPALE_HS)
				|| type.equals(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE) || type.equals(TypeEvenementCivil.ARRIVEE_SECONDAIRE)) {
			isPresent = true;

		}
		return isPresent;
	}

}
