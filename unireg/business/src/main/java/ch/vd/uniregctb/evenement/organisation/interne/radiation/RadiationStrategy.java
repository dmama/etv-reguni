package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.InscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.tiers.CategorieEntrepriseHelper;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.CategorieEntreprise;

/**
 * Radiation du RC
 *
 * @author Raphaël Marmier, 2015-11-10.
 */
public class RadiationStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadiationStrategy.class);

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

		// On ne s'occupe que d'entités déjà connues
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();
		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();

		final SiteOrganisation sitePrincipalAvant = getSitePrincipal(organisation, dateAvant);
		if (sitePrincipalAvant != null) {

			final SiteOrganisation sitePrincipalApres = getSitePrincipal(organisation, dateApres);

			final boolean enCoursDeRadiationRC = sitePrincipalAvant.isConnuInscritAuRC(dateAvant) && !sitePrincipalAvant.isRadieDuRC(dateAvant) && sitePrincipalApres.isRadieDuRC(dateApres);

			final CategorieEntreprise categorieEntreprise = CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateApres);
			if (categorieEntreprise == null) {
				return new TraitementManuel(event, organisation, entreprise, context, options,
				                            String.format("Traitement manuel requis: Impossible de déterminer la catégorie de l'entreprise n°%s!",
				                                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()))
				);
			}

			if (enCoursDeRadiationRC) {
				final InscriptionRC inscriptionRC = sitePrincipalApres.getDonneesRC().getInscription(dateApres);
				final RegDate dateRadiation = inscriptionRC != null ? inscriptionRC.getDateRadiationCH() : null;
				if (dateRadiation == null) {
					return new TraitementManuel(event, organisation, entreprise, context, options,
					                            String.format("Traitement manuel requis: l'entreprise n°%s est radiée du RC mais la date de radiation est introuvable!",
					                                          FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()))
					);
				}
				else {
					if (categorieEntreprise == CategorieEntreprise.APM) {
						LOGGER.info(String.format("Radiation de l'association n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), CategorieEntreprise.APM.getLibelle()));
						return new RadiationAPM(event, organisation, entreprise, context, options, dateRadiation);
					}
					else {
						LOGGER.info(String.format("Radiation de l'entreprise n°%s (%s).", FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), categorieEntreprise.getLibelle()));
						return new Radiation(event, organisation, entreprise, context, options, dateRadiation);
					}
				}
			}
		}

		LOGGER.info("Pas de radiation de l'entreprise.");
		return null;
	}

	protected SiteOrganisation getSitePrincipal(Organisation organisation, RegDate dateAvant) {
		final DateRanged<SiteOrganisation> sitePrincipal = organisation.getSitePrincipal(dateAvant);
		return sitePrincipal == null ? null : sitePrincipal.getPayload();
	}
}
