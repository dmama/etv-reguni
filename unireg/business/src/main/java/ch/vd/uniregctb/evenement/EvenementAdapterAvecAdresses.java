package ch.vd.uniregctb.evenement;

import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public abstract class EvenementAdapterAvecAdresses extends GenericEvenementAdapter implements EvenementCivilAvecAdresses {

	/**
	 * L'adresse principale de l'individu .
	 */
	private Adresse adressePrincipale;

	/**
	 * L'adresse secondaire de l'individu.
	 */
	private Adresse adresseSecondaire;

	/**
	 * L'adresse courrier de l'individu .
	 */
	private Adresse adresseCourrier;

	@Override
	protected void fillRequiredParts(Set<EnumAttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(EnumAttributeIndividu.ADRESSES);
	}

	@Override
	public void init(EvenementCivilData evenement, ServiceCivilService serviceCivil, ServiceInfrastructureService infrastructureService, DataEventService dataEventService) throws EvenementAdapterException {
		super.init(evenement, serviceCivil, infrastructureService, dataEventService);

		// Distinction adresse principale et adresse courrier
		// On recupère les adresses à la date de l'événement plus 1 jour
		try {
			final AdressesCiviles adresses = serviceCivil.getAdresses(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayAfter(), false);
			Assert.notNull(adresses, "L'individu principal n'a pas d'adresses valide");

			this.adressePrincipale = adresses.principale;
			this.adresseSecondaire = adresses.secondaire;
			this.adresseCourrier = adresses.courrier;
		}
		catch (DonneesCivilesException e) {
			throw new EvenementAdapterException(e);
		}
	}

	public Adresse getAdressePrincipale() {
		return adressePrincipale;
	}

	public Adresse getAdresseSecondaire() {
		return adresseSecondaire;
	}

	public Adresse getAdresseCourrier() {
		return adresseCourrier;
	}
}
