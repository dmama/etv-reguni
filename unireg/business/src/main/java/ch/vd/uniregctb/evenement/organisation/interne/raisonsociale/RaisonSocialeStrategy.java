package ch.vd.uniregctb.evenement.organisation.interne.raisonsociale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
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
import ch.vd.uniregctb.tiers.Etablissement;

/**
 * Détection du changement de la raison sociale
 *
 * @author Raphaël Marmier, 2016-05-18.
 */
public class RaisonSocialeStrategy extends AbstractOrganisationStrategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(RaisonSocialeStrategy.class);

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
		if (sitePrincipalAvantRange != null) {

			SiteOrganisation sitePrincipalAvant = sitePrincipalAvantRange.getPayload();
			final String raisonSocialeAvant = sitePrincipalAvant.getNom(dateAvant);

			final SiteOrganisation sitePrincipalApres = organisation.getSitePrincipal(dateApres).getPayload();
			final String raisonSocialeApres = sitePrincipalApres.getNom(dateApres);

			final Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison = new ListOrderedMap<>();

			final Etablissement etablissementPrincipal = context.getTiersDAO().getEtablissementByNumeroSite(sitePrincipalApres.getNumeroSite());
			final List<Pair<String, Boolean>> pairesPrincipales =
					traiteEtablissement(sitePrincipalApres.getNumeroSite(), raisonSocialeAvant, raisonSocialeApres, etablissementPrincipal, true);
			if (!pairesPrincipales.isEmpty()) {
				changementsRaison.put(etablissementPrincipal, pairesPrincipales);
			}

			final List<DateRanged<Etablissement>> etablissementsSecondairesRanges = context.getTiersService().getEtablissementsSecondairesEntreprise(entreprise);
			for (DateRanged<Etablissement> range : etablissementsSecondairesRanges) {
				if (range.isValidAt(dateApres) && range.getPayload().isConnuAuCivil()) {
					final Etablissement etab = range.getPayload();
					final SiteOrganisation site = context.getTiersService().getSiteOrganisationPourEtablissement(etab);
					final List<Pair<String, Boolean>> paires = traiteEtablissement(etab.getNumeroEtablissement(), site.getNom(dateAvant), site.getNom(dateApres), etab, false);
					if (!paires.isEmpty()) {
						changementsRaison.put(etab, paires);
					}
				}

			}
			if (!changementsRaison.isEmpty()) {
				return new RaisonSociale(event, organisation, entreprise, context, options, changementsRaison);
			}
		}
		LOGGER.info("Pas de changement de raison sociale de l'entreprise ou de ses établissements.");
		return null;
	}

	private List<Pair<String, Boolean>> traiteEtablissement(Long numeroSite, String raisonSocialeSiteAvant, String raisonSocialeSiteApres,
	                                                               Etablissement etablissement, boolean principal
	                                 ) throws EvenementOrganisationException {

		// On ne s'occupe que des établissements connus d'Unireg et qui n'apparaissent ou disparaissent pas.
		if (etablissement != null && StringUtils.isNotBlank(raisonSocialeSiteAvant) && raisonSocialeSiteApres != null && !raisonSocialeSiteAvant.equals(raisonSocialeSiteApres)) {
			List<Pair<String, Boolean>> paires = new ArrayList<>();

			String message = String.format("Changement de raison sociale de l'établissement %s n°%s (civil: %d). %s devient %s.",
			                               principal ? "principal" : "secondaire",
			                               FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
			                               numeroSite,
			                               raisonSocialeSiteAvant,
			                               raisonSocialeSiteApres);
			paires.add(new ImmutablePair<>(message, false));

			final String enseigne = etablissement.getEnseigne();
			if (StringUtils.isNotBlank(enseigne) && !enseigne.equals(raisonSocialeSiteApres)) {
				String messageWarning = String.format("Avertissement: l'enseigne %s de l'établissement %s n°%s ne correspond pas à la nouvelle raison sociale %s. Veuillez corriger à la main si nécessaire.",
				                                      enseigne,
				                                      principal ? "principal" : "secondaire",
				                                      FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                      raisonSocialeSiteApres);
				paires.add(new ImmutablePair<>(messageWarning, true));
			}
			return paires;
		}
		return Collections.emptyList();
	}
}