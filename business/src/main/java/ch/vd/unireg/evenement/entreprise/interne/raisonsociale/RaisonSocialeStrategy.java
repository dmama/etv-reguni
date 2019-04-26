package ch.vd.unireg.evenement.entreprise.interne.raisonsociale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.entreprise.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Détection du changement de la raison sociale
 *
 * @author Raphaël Marmier, 2016-05-18.
 */
public class RaisonSocialeStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public RaisonSocialeStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
		super(context, options);
	}

	/**
	 * Détecte les mutations pour lesquelles la création d'un événement interne est nécessaire.
	 *
	 * @param event un événement entreprise civile reçu de RCEnt
	 */
	@Override
	public EvenementEntrepriseInterne matchAndCreate(EvenementEntreprise event, final EntrepriseCivile entrepriseCivile, Entreprise entreprise) throws EvenementEntrepriseException {

		if (entreprise == null) {
			return null;
		}

		final RegDate dateAvant = event.getDateEvenement().getOneDayBefore();
		final RegDate dateApres = event.getDateEvenement();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange != null) {

			EtablissementCivil etablissementPrincipalAvant = etablissementPrincipalAvantRange.getPayload();
			final String raisonSocialeAvant = etablissementPrincipalAvant.getNom(dateAvant);

			final EtablissementCivil etablissementPrincipalApres = entrepriseCivile.getEtablissementPrincipal(dateApres).getPayload();
			final String raisonSocialeApres = etablissementPrincipalApres.getNom(dateApres);

			final Map<Etablissement, List<Pair<String, Boolean>>> changementsRaison = new ListOrderedMap<>();

			final Etablissement etablissementPrincipal = context.getTiersDAO().getEtablissementByNumeroEtablissementCivil(etablissementPrincipalApres.getNumeroEtablissement());
			final List<Pair<String, Boolean>> pairesPrincipales =
					traiteEtablissement(etablissementPrincipalApres.getNumeroEtablissement(), raisonSocialeAvant, raisonSocialeApres, etablissementPrincipal, true);
			if (!pairesPrincipales.isEmpty()) {
				changementsRaison.put(etablissementPrincipal, pairesPrincipales);
			}

			final List<DateRanged<Etablissement>> etablissementsSecondairesRanges = context.getTiersService().getEtablissementsSecondairesEntreprise(entreprise);
			for (DateRanged<Etablissement> range : etablissementsSecondairesRanges) {
				if (range.isValidAt(dateApres) && range.getPayload().isConnuAuCivil()) {
					final Etablissement etab = range.getPayload();
					final EtablissementCivil etablissement = context.getTiersService().getEtablissementCivil(etab);
					final List<Pair<String, Boolean>> paires = traiteEtablissement(etab.getNumeroEtablissement(), etablissement.getNom(dateAvant), etablissement.getNom(dateApres), etab, false);
					if (!paires.isEmpty()) {
						changementsRaison.put(etab, paires);
					}
				}

			}
			if (!changementsRaison.isEmpty()) {
				context.audit.info(event.getId(), "Changement de raison sociale détecté.");
				return new RaisonSociale(event, entrepriseCivile, entreprise, context, options, changementsRaison);
			}
		}
		return null;
	}

	private List<Pair<String, Boolean>> traiteEtablissement(Long numeroEtablissementCivil, String raisonSocialeEtablissementAvant, String raisonSocialeEtablissementApres,
	                                                               Etablissement etablissement, boolean principal) {

		// On ne s'occupe que des établissements connus d'Unireg et qui n'apparaissent ou disparaissent pas.
		if (etablissement != null && StringUtils.isNotBlank(raisonSocialeEtablissementAvant) && raisonSocialeEtablissementApres != null && !raisonSocialeEtablissementAvant.equals(raisonSocialeEtablissementApres)) {
			List<Pair<String, Boolean>> paires = new ArrayList<>();

			String message = String.format("Changement de raison sociale de l'établissement %s n°%s (civil: %d). %s devient %s.",
			                               principal ? "principal" : "secondaire",
			                               FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
			                               numeroEtablissementCivil,
			                               raisonSocialeEtablissementAvant,
			                               raisonSocialeEtablissementApres);
			paires.add(new ImmutablePair<>(message, false));

			final String enseigne = etablissement.getEnseigne();
			if (StringUtils.isNotBlank(enseigne) && !enseigne.equals(raisonSocialeEtablissementApres)) {
				String messageWarning = String.format("Avertissement: l'enseigne %s de l'établissement %s n°%s ne correspond pas à la nouvelle raison sociale %s. Veuillez corriger à la main si nécessaire.",
				                                      enseigne,
				                                      principal ? "principal" : "secondaire",
				                                      FormatNumeroHelper.numeroCTBToDisplay(etablissement.getNumero()),
				                                      raisonSocialeEtablissementApres);
				paires.add(new ImmutablePair<>(messageWarning, true));
			}
			return paires;
		}
		return Collections.emptyList();
	}
}
