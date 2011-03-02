package ch.vd.uniregctb.evenement.civil.interne;

import java.util.Set;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;

public abstract class EvenementCivilInterneAvecAdressesBase extends EvenementCivilInterneBase implements EvenementCivilInterneAvecAdresses {

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

	protected EvenementCivilInterneAvecAdressesBase(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);

		// Distinction adresse principale et adresse courrier
		// On recupère les adresses à la date de l'événement plus 1 jour
		try {
			final AdressesCiviles adresses =  new AdressesCiviles(context.getServiceCivil().getAdresses(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayAfter(), false));
			Assert.notNull(adresses, "L'individu principal n'a pas d'adresses valide");

			this.adressePrincipale = adresses.principale;
			this.adresseSecondaire = adresses.secondaire;
			this.adresseCourrier = adresses.courrier;
		}
		catch (DonneesCivilesException e) {
			throw new EvenementCivilInterneException(e);
		}
	}

	@Override
	protected void fillRequiredParts(Set<AttributeIndividu> parts) {
		super.fillRequiredParts(parts);
		parts.add(AttributeIndividu.ADRESSES);
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
