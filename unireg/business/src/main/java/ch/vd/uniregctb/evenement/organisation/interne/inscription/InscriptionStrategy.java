package ch.vd.uniregctb.evenement.organisation.interne.inscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Inscription au RC
 *
 * Cette stratégie doit être évaluée AVANT la stratégie évaluant la radiation, afin que si l'entreprise
 * est inscrite et radiée le même jour, elle termine radiée du RC.
 *
 * @author Raphaël Marmier, 2016-02-23.
 */
public class InscriptionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(InscriptionStrategy.class);

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

		RegDate dateInscriptionRCAvant = null;
		RegDate dateRadiationRCAvant = null;

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {

			SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			dateInscriptionRCAvant = sitePrincipalAvant.getDonneesRC().getDateInscription(dateAvant);
			dateRadiationRCAvant = sitePrincipalAvant.getDonneesRC().getDateRadiation(dateAvant);
			final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

			final RegDate dateInscriptionRCApres = sitePrincipalApres.getDonneesRC().getDateInscription(dateApres);


			if (dateInscriptionRCAvant == null && dateRadiationRCAvant == null && dateInscriptionRCApres != null) {
				LOGGER.info(String.format("Inscription au RC de l'entreprise n°%s (civil: %d).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation()));
				return new Inscription(event, organisation, entreprise, context, options);
			}
		}
		LOGGER.info("Pas d'inscription au RC de l'entreprise.");
		return null;
	}
}
