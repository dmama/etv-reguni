package ch.vd.uniregctb.evenement.organisation.interne.reinscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.Entreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-11.
 */
public class ReinscriptionStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReinscriptionStrategy.class);

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public ReinscriptionStrategy(EvenementOrganisationContext context, EvenementOrganisationOptions options) {
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
	public EvenementOrganisationInterne matchAndCreate(EvenementOrganisation event, final Organisation organisation, Entreprise entreprise) throws EvenementOrganisationException {

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<SiteOrganisation> sitePrincipalAvantRange = organisation.getSitePrincipal(dateAvant);

		if (sitePrincipalAvantRange == null) {
			if (isExisting(organisation, dateApres)) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
				                            String.format("Site principal introuvable sur organisation n°%s en date du %s", organisation.getNumeroOrganisation(), RegDateHelper.dateToDisplayString(dateAvant)));
			}
		} else {
			final SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

			final InscriptionRC rcAvant = sitePrincipalAvant.getDonneesRC().getInscription(dateAvant);
			final InscriptionRC rcApres = sitePrincipalApres.getDonneesRC().getInscription(dateApres);
			final RegDate dateRadiationRCApres = rcApres != null ? rcApres.getDateRadiationCH() : null;
			final StatusInscriptionRC statusInscriptionAvant = rcAvant != null ? rcAvant.getStatus() : null;
			final StatusInscriptionRC statusInscriptionApres = rcApres != null ? rcApres.getStatus() : null;

			if (statusInscriptionAvant == StatusInscriptionRC.RADIE && (statusInscriptionApres == StatusInscriptionRC.ACTIF || statusInscriptionApres == StatusInscriptionRC.EN_LIQUIDATION)) {
				LOGGER.info(String.format("Réinscription au RC de l'entreprise n°%s (civil: %d).%s",
				                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()),
				                          organisation.getNumeroOrganisation(),
				                          dateRadiationRCApres != null ? " Cependant, l'ancienne date de radiation persiste dans RCEnt: " + RegDateHelper.dateToDisplayString(dateRadiationRCApres) + "." : ""
				));
				return new Reinscription(event, organisation, entreprise, context, options);
			}
		}
		LOGGER.info("Pas de réinscription au RC de l'entreprise.");
		return null;
	}
}
