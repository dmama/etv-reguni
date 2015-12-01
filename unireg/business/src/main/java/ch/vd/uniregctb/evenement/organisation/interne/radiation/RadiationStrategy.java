package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationContext;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationOptions;
import ch.vd.uniregctb.evenement.organisation.interne.AbstractOrganisationStrategy;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.evenement.organisation.interne.TraitementManuel;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
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
		// TODO: Retrouver aussi les entreprises n'ayant pas d'id cantonal.
		if (entreprise == null) {
			return null;
		}

		final RegDate dateApres = event.getDateEvenement();
		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();

		final SiteOrganisation sitePrincipalAvant = organisation.getSitePrincipal(dateAvant).getPayload();
		final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		final StatusRC statusRCAvant = sitePrincipalAvant.getDonneesRC().getStatus(dateAvant);
		final StatusRC statusRCApres = sitePrincipalApres.getDonneesRC().getStatus(dateApres);

		final StatusInscriptionRC statusInscriptionRCAvant = sitePrincipalAvant.getDonneesRC().getStatusInscription(dateAvant);
		final StatusInscriptionRC statusInscriptionRCApres = sitePrincipalApres.getDonneesRC().getStatusInscription(dateApres);

		final StatusRegistreIDE statusRegistreIDEAvant = sitePrincipalAvant.getDonneesRegistreIDE().getStatus(dateAvant);
		final StatusRegistreIDE statusRegistreIDEApres = sitePrincipalApres.getDonneesRegistreIDE().getStatus(dateApres);

		final boolean enCoursDeRadiationRC = isInscritRC(statusRCAvant) && ! isRadieRC(statusInscriptionRCAvant) && isRadieRC(statusInscriptionRCApres);
		final boolean enCoursDeRadiationIDE = ! isRadieIDE(statusRegistreIDEAvant) && isRadieIDE(statusRegistreIDEApres);

		try {
			if (enCoursDeRadiationIDE) {
				if (isInscritRC(statusRCApres) && !isRadieRC(statusInscriptionRCApres)) {
					throw new EvenementOrganisationException(String.format("L'entreprise %s est radiée de l'IDE mais pas du RC!", entreprise));
				}

				if (isAssujetti(entreprise, dateApres, context)) {
					LOGGER.info(String.format("Entreprise %s %s %sradiée de l'IDE, mais encore assujettie.",
					                          entreprise.getNumero(), enCoursDeRadiationRC ? "radiée du RC, " : "", isInscritRC(statusRCAvant) ? "" : "non inscrite au RC "));
					return new TraitementManuel(event, organisation, entreprise, context, options,
					                            "Traitement manuel requis pour le contrôle de la radiation d’une entreprise encore assujettie.");
				}
				LOGGER.info(String.format("Entreprise %s radiée %sde l'IDE.", entreprise.getNumero(), enCoursDeRadiationRC ? "du RC et " : ""));
				return new Radiation(event, organisation, entreprise, context, options);

			} else if (statusRegistreIDEApres == null && enCoursDeRadiationRC) {
				String message = String.format("Le status de l'entreprise %s est radiée du RC, mais indéterminé à l'IDE.%s",
				                               entreprise.getNumero(), isAssujetti(entreprise, dateApres, context) ? " De plus, l'entreprise est toujours assujettie." : "");
				LOGGER.info(message);
				return new TraitementManuel(event, organisation, entreprise, context, options, message);

			} else if (enCoursDeRadiationRC) {
				if (CategorieEntrepriseHelper.getCategorieEntreprise(organisation, dateApres) != CategorieEntreprise.APM) {
					final String message = String.format("Entreprise %s non APM radiée du RC mais pourtant toujours présente à l'IDE.", entreprise.getNumero());
					LOGGER.info(message);
					return new TraitementManuel(event, organisation, entreprise, context, options, message);
				}
				LOGGER.info(String.format("Entreprise %s de type APM radiée du RC mais qui reste à l'IDE.", entreprise.getNumero()));
				return new Radiation(event, organisation, entreprise, context, options);

			}
		}
		catch (AssujettissementException e) {
			throw new EvenementOrganisationException(String.format("Impossible de déterminer si l'entreprise %s est assujettie: %s", entreprise.getNumero(), e.getMessage()), e);
		}

		LOGGER.info("Pas de radiation de l'entreprise.");
		return null;
	}
}
