package ch.vd.unireg.evenement.organisation.interne.inscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementOrganisation;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationContext;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationException;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.unireg.tiers.Entreprise;

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
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public InscriptionStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event   un événement organisation reçu de RCEnt
	 * @param organisation
	 * @return
	 * @throws EvenementOrganisationException
	 */
	@Override
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event,
	                                                   final Organisation organisation,
	                                                   Entreprise entreprise) throws EvenementOrganisationException {

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);
		if (sitePrincipalAvantRange != null) {
			final SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final InscriptionRC inscriptionAvant = sitePrincipalAvant.getDonneesRC().getInscription(dateAvant);
			if (inscriptionAvant == null || !inscriptionAvant.isInscrit()) {
				final RegDate dateInscriptionRCAvant = inscriptionAvant != null ? inscriptionAvant.getDateInscriptionCH() : null;
				final RegDate dateRadiationRCAvant = inscriptionAvant != null ? inscriptionAvant.getDateRadiationCH() : null;

				final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();
				final InscriptionRC inscriptionApres = sitePrincipalApres.getDonneesRC().getInscription(dateApres);
				final RegDate dateInscriptionRCApres = inscriptionApres != null ? inscriptionApres.getDateInscriptionCH() : null;

				if (dateInscriptionRCAvant == null && dateRadiationRCAvant == null && dateInscriptionRCApres != null) {
					LOGGER.info(String.format("Inscription au RC de l'entreprise n°%s (civil: %d).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), organisation.getNumeroOrganisation()));
					return new Inscription(event, organisation, entreprise, context, options);
				}
			}
		}
		LOGGER.info("Pas d'inscription au RC de l'entreprise.");
		return null;
	}
}
