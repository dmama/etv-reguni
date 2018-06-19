package ch.vd.unireg.evenement.organisation.interne.etablissement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.organisation.EvenementEntreprise;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseContext;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseOptions;
import ch.vd.unireg.evenement.organisation.interne.AbstractEntrepriseStrategy;
import ch.vd.unireg.evenement.organisation.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.evenement.organisation.interne.MessageWarningPreExectution;
import ch.vd.unireg.evenement.organisation.interne.TraitementManuel;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

/**
 * Modification des établissements secondaires
 *
 * @author Raphaël Marmier, 2016-02-26.
 */
public class EtablissementsSecondairesStrategy extends AbstractEntrepriseStrategy {

	/**
	 * @param context le context d'exécution de l'événement
	 * @param options des options de traitement
	 */
	public EtablissementsSecondairesStrategy(EvenementEntrepriseContext context, EvenementEntrepriseOptions options) {
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


		final List<Etablissement> etablissementsAFermer = new ArrayList<>();
		final List<EtablissementCivil> etablissementsACreer = new ArrayList<>();

		final DateRanged<EtablissementCivil> etablissementPrincipalAvantRange = entrepriseCivile.getEtablissementPrincipal(dateAvant);
		if (etablissementPrincipalAvantRange == null) {
			Audit.info("EntrepriseCivile nouvelle au civil mais déjà connue d'Unireg. Des établissements secondaires ont peut-être changé.");
			// FIXME: Cela pose la question de savoir si on ne devrait pas utiliser Unireg comme "avant" dans ces cas là?
			return new MessageWarningPreExectution(event, entrepriseCivile, null, context, options,
			                                       String.format("L'entreprise civile n°%d est déjà connue d'Unireg, mais nouvelle au civil. Veuillez vérifier la transition entre les données du registre " +
					                                                     "fiscal et du registre civil, notamment les établissements secondaires.", entrepriseCivile.getNumeroEntreprise()));
		}
		else {

			final List<EtablissementCivil> etablissementsVDAvant = uniquementEtablissementsActifs(entrepriseCivile.getEtablissementsSecondaires(dateAvant), dateAvant);
			final List<EtablissementCivil> etablissementsVDApres = uniquementEtablissementsActifs(entrepriseCivile.getEtablissementsSecondaires(dateApres), dateApres);

			determineChangementsEtablissements(etablissementsVDAvant, etablissementsVDApres, etablissementsAFermer, etablissementsACreer, context);

			try {
				List<EtablissementsSecondaires.Demenagement> demenagements = determineChangementsDomiciles(etablissementsVDAvant, etablissementsVDApres, dateApres, context);

				if (!etablissementsAFermer.isEmpty() || !etablissementsACreer.isEmpty() || !demenagements.isEmpty()) {
					final String message = String.format("Modification des établissements secondaires de l'entreprise %s (civil: %d).",
					                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entrepriseCivile.getNumeroEntreprise());
					Audit.info(event.getId(), message);
					return new EtablissementsSecondaires(event, entrepriseCivile, entreprise, context, options, etablissementsAFermer, etablissementsACreer, demenagements);
				}
			}
			catch (EvenementEntrepriseException e) {
				final String message = String.format("Erreur lors de la determination des changements de domicile des établissements secondaires de l'entreprise n°%s (civil: %d): %s",
				                                     FormatNumeroHelper.numeroCTBToDisplay(entreprise.getNumero()), entrepriseCivile.getNumeroEntreprise(), e.getMessage());
				Audit.error(event.getId(), message);
				return new TraitementManuel(event, entrepriseCivile, entreprise, context, options, message);
			}
		}

		return null;
	}

	private List<EtablissementCivil> uniquementEtablissementsActifs(List<EtablissementCivil> etablissementsSecondaires, RegDate date) {
		List<EtablissementCivil> filtre = new ArrayList<>(etablissementsSecondaires.size());
		for (EtablissementCivil etablissement : etablissementsSecondaires) {
			Domicile domicile = etablissement.getDomicile(date);
			if (domicile != null
					&& etablissement.isActif(date)) {
				filtre.add(etablissement);
			}
		}
		return filtre;
	}

	protected void determineChangementsEtablissements(List<EtablissementCivil> etablissementsAvant, List<EtablissementCivil> etablissementsApres, List<Etablissement> etablissementsAFermer, List<EtablissementCivil> etablissementACreer, EvenementEntrepriseContext context) {

		// Determiner les établissements civils qui ont disparus
		for (EtablissementCivil ancienEtablissement : etablissementsAvant) {
			boolean disparu = true;
			for (EtablissementCivil presentEtablissement : etablissementsApres) {
				if (ancienEtablissement.getNumeroEtablissement() == presentEtablissement.getNumeroEtablissement()) {
					disparu = false;
					break;
				}
			}
			if (disparu) {
				Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroEtablissementCivil(ancienEtablissement.getNumeroEtablissement());
				if (etablissement != null) {
					etablissementsAFermer.add(etablissement);
				}
			}
		}

		for (EtablissementCivil presentEtablissement : etablissementsApres) {
			boolean nouveau = true;
			for (EtablissementCivil ancienEtablissement : etablissementsAvant) {
				if (presentEtablissement.getNumeroEtablissement() == ancienEtablissement.getNumeroEtablissement()) {
					nouveau = false;
					break;
				}
			}
			if (nouveau) {
				etablissementACreer.add(presentEtablissement);
			}
		}
	}

	protected List<EtablissementsSecondaires.Demenagement> determineChangementsDomiciles(List<EtablissementCivil> etablissementsAvant, List<EtablissementCivil> etablissementsApres, RegDate date, EvenementEntrepriseContext context) {

		final List<EtablissementsSecondaires.Demenagement> demenagements = new ArrayList<>();

		// Determiner les établissements civils qui ont déménagé.
		for (EtablissementCivil ancienEtablissmeent : etablissementsAvant) {
			for (EtablissementCivil presentEtablissement : etablissementsApres) {
				if (ancienEtablissmeent.getNumeroEtablissement() == presentEtablissement.getNumeroEtablissement()) { // On ne retient que les cas ou l'établissement n'a pas changé.

					// On compare les domiciles tels que les voit RCEnt
					final Domicile domicileAvant = ancienEtablissmeent.getDomicile(date.getOneDayBefore());
					final Domicile domicileApres = presentEtablissement.getDomicile(date);

					// On considère qu'on est en présence d'un déménagement que si on connait déjà l'établissement dans Unireg.
					Etablissement etablissement = context.getTiersDAO().getEtablissementByNumeroEtablissementCivil(ancienEtablissmeent.getNumeroEtablissement());
					if (etablissement != null && !Objects.equals(domicileAvant.getNumeroOfsAutoriteFiscale(), domicileApres.getNumeroOfsAutoriteFiscale())) {
						demenagements.add(new EtablissementsSecondaires.Demenagement(etablissement, presentEtablissement, domicileAvant, domicileApres, date));
					}
				}
			}
		}
		return demenagements;
	}
}
