package ch.vd.uniregctb.evenement.organisation.interne.radiation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateRangeHelper;
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
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tiers.Entreprise;

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

		final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();

		final StatusRC statusRCApres = sitePrincipalApres.getDonneesRC().getStatus(dateApres);
		final StatusInscriptionRC statusInscriptionRCApres = sitePrincipalApres.getDonneesRC().getStatusInscription(dateApres);
		final StatusRegistreIDE statusRegistreIDEApres = sitePrincipalApres.getDonneesRegistreIDE().getStatus(dateApres);

		try {
			if (statusRCApres != null && statusRCApres == StatusRC.INSCRIT &&
					statusRegistreIDEApres != null && (statusRegistreIDEApres == StatusRegistreIDE.RADIE || statusRegistreIDEApres == StatusRegistreIDE.DEFINITIVEMENT_RADIE)) {

				if (statusInscriptionRCApres != null && statusInscriptionRCApres != StatusInscriptionRC.RADIE) {
					throw new EvenementOrganisationException(String.format("L'entreprise %s est radiée à l'IDE mais pas au RC!", entreprise));
				}

				if (isAssujetti(entreprise, dateApres, context)) {
					return new TraitementManuel(event, organisation, entreprise, context, options,
					                            String.format("Entreprise %s radiée. Assujettissement en cours!", entreprise.getNumero()));
				}

				LOGGER.info(String.format("Entreprise %s radiée. Pas d'assujettissement en cours.", entreprise.getNumero()));
				return new RadiationRC(event, organisation, entreprise, context, options);
			}
			else if (statusRCApres != null && statusRCApres == StatusRC.INSCRIT &&
					statusRegistreIDEApres == null && statusInscriptionRCApres != null && statusInscriptionRCApres == StatusInscriptionRC.RADIE) {
				String message = String.format("Le status de l'entreprise %s est indéterminé à l'IDE, mais radiée au RC. %s",
				                               entreprise.getNumero(), isAssujetti(entreprise, dateApres, context) ? "De plus, l'entreprise est toujours assujettie." : "");
				return new TraitementManuel(event, organisation, entreprise, context, options, message);
			}
		}
		catch (AssujettissementException e) {
			throw new EvenementOrganisationException(String.format("Impossible de déterminer si l'entreprise %s est assujettie: %s", entreprise.getNumero(), e.getMessage()), e);
		}

		LOGGER.info("Pas de radiation de l'entreprise.");
		return null;
	}

	private boolean isAssujetti(Entreprise entreprise, RegDate date, EvenementOrganisationContext context) throws AssujettissementException {
		List<Assujettissement> assujettissements = context.getAssujettissementService().determine(entreprise);
		Assujettissement assujettissement = null;
		if (assujettissements != null && !assujettissements.isEmpty()) {
			assujettissement = DateRangeHelper.rangeAt(assujettissements, date);
		}
		return assujettissement != null;
	}
}
