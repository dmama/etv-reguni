package ch.vd.uniregctb.evenement.organisation.interne.adresse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.AdresseEffectiveRCEnt;
import ch.vd.unireg.interfaces.organisation.data.AdresseLegaleRCEnt;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Changements d'adresses
 *
 * @author Raphaël Marmier, 2016-04-11.
 */
public class AdresseStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdresseStrategy.class);

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise,
	                                                   EvenementOrganisationContext context,
	                                                   EvenementOrganisationOptions options) throws EvenementOrganisationException {

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange == null) {
			return new TraitementManuel(event, organisation, entreprise, context, options, "Organisation nouvelle au civil mais déjà connue d'Unireg. Impossible de déterminer automatiquement ce qu'il faut faire.");
		} else {
			AdresseEffectiveRCEnt nouvelleAdresseEffective = null;
			AdresseLegaleRCEnt nouvelleAdresseLegale = null;

			SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final AdresseEffectiveRCEnt adresseEffectiveAvant = sitePrincipalAvant.getDonneesRegistreIDE().getAdresseEffective(dateAvant);
			final AdresseLegaleRCEnt adresseLegaleAvant = sitePrincipalAvant.getDonneesRC().getAdresseLegale(dateAvant);

			final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();
			final AdresseEffectiveRCEnt adresseEffectiveApres = sitePrincipalApres.getDonneesRegistreIDE().getAdresseEffective(dateApres);
			final AdresseLegaleRCEnt adresseLegaleApres = sitePrincipalApres.getDonneesRC().getAdresseLegale(dateApres);

			if (adresseEffectiveAvant != adresseEffectiveApres) {
				nouvelleAdresseEffective = adresseEffectiveApres;
			}
			if (adresseLegaleAvant != adresseLegaleApres) {
				nouvelleAdresseLegale = adresseLegaleApres;
			}

			if (nouvelleAdresseEffective != null || nouvelleAdresseLegale != null) {
				LOGGER.info(String.format("Changement d'adresse %s%s%s.",
				                          nouvelleAdresseEffective != null ? "effective" : "",
				                          nouvelleAdresseEffective != null && nouvelleAdresseLegale != null ? " et d'adresse " : "",
				                          nouvelleAdresseLegale != null ? "légale" : ""
				                          )
				);
				return new Adresse(event, organisation, entreprise, context, options, nouvelleAdresseEffective, nouvelleAdresseLegale);
			}
		}
		LOGGER.info("Pas de changement d'adresse à prendre en compte.");
		return null;
	}
}