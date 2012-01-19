package ch.vd.uniregctb.evenement.civil.interne;

import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.DonneesCivilesException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;

public abstract class EvenementCivilInterneAvecAdresses extends EvenementCivilInterne {

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

	protected EvenementCivilInterneAvecAdresses(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);

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
			throw new EvenementCivilException(e);
		}
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilInterneAvecAdresses(Individu individu, Individu conjoint, RegDate dateEvenement,
	                                            Integer numeroOfsCommuneAnnonce, Adresse adressePrincipale, Adresse adresseSecondaire, Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, conjoint, dateEvenement, numeroOfsCommuneAnnonce, context);
		this.adressePrincipale = adressePrincipale;
		this.adresseSecondaire = adresseSecondaire;
		this.adresseCourrier = adresseCourrier;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected EvenementCivilInterneAvecAdresses(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate dateEvenement,
	                                            Integer numeroOfsCommuneAnnonce, Adresse adressePrincipale, Adresse adresseSecondaire, Adresse adresseCourrier, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, dateEvenement, numeroOfsCommuneAnnonce, context);
		this.adressePrincipale = adressePrincipale;
		this.adresseSecondaire = adresseSecondaire;
		this.adresseCourrier = adresseCourrier;
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
