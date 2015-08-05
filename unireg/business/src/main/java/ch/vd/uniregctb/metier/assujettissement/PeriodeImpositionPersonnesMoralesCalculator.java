package ch.vd.uniregctb.metier.assujettissement;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Calculateur des périodes d'imposition des entreprises
 */
public class PeriodeImpositionPersonnesMoralesCalculator implements PeriodeImpositionCalculator<Entreprise> {

	private final ParametreAppService parametreService;
	private final BouclementService bouclementService;

	public PeriodeImpositionPersonnesMoralesCalculator(ParametreAppService parametreService, BouclementService bouclementService) {
		this.parametreService = parametreService;
		this.bouclementService = bouclementService;
	}

	/**
	 * Point d'entrée du calcul des périodes d'imposition pour les contribuables assimilés "personne morale"
	 * @param entreprise contribuable cible
	 * @param assujettissements les assujettissements du contribuable, calculés par ailleurs
	 * @return la liste des périodes d'imposition du contribuable
	 */
	@NotNull
	@Override
	public List<PeriodeImposition> determine(Entreprise entreprise, List<Assujettissement> assujettissements) throws AssujettissementException {

		// si pas d'assujettissement, pas de période d'imposition
		if (assujettissements == null || assujettissements.isEmpty()) {
			return Collections.emptyList();
		}

		// il y a des assujettissements, donc il y a des fors principaux, pas la peine de re-vérfier, ou bien ?
		final List<ForFiscalPrincipalPM> forsPrincipaux = entreprise.getForsFiscauxPrincipauxActifsSorted();

		// liste à retourner
		final List<PeriodeImposition> resultat = new LinkedList<>();

		// il faut au moins découper l'assujettissement en périodes correspondants aux exercices commerciaux
		final RegDate dateDebutRange = getDebutPremierePeriodeImposition(forsPrincipaux.get(0).getDateDebut(), entreprise.getBouclements());
		final List<ExerciceCommercial> exercices = bouclementService.getExercicesCommerciaux(entreprise.getBouclements(), new DateRangeHelper.Range(dateDebutRange, RegDate.get()));
		for (ExerciceCommercial exercice : exercices) {
			for (Assujettissement assujettissement : assujettissements) {
				final DateRange intersection = DateRangeHelper.intersection(exercice, assujettissement);
				if (intersection != null) {

					// détermination de la cause de fermeture de l'assujettissement
					final PeriodeImposition.CauseFermeture causeFermeture;
					if (intersection.getDateFin() == assujettissement.getDateFin()) {
						if ((assujettissement.getMotifFractFin() == MotifFor.VENTE_IMMOBILIER || assujettissement.getMotifFractFin() == MotifFor.FIN_EXPLOITATION) && assujettissement instanceof HorsSuisse) {
							causeFermeture = PeriodeImposition.CauseFermeture.FIN_ASSUJETTISSEMENT_HS;
						}
						else {
							causeFermeture = PeriodeImposition.CauseFermeture.AUTRE;
						}
					}
					else {
						causeFermeture = null;
					}

					// détermination du type de contribuable
					final TypeContribuable typeContribuable;
					switch (assujettissement.getType()) {
					case HORS_CANTON:
						typeContribuable = TypeContribuable.HORS_CANTON;
						break;
					case HORS_SUISSE:
						typeContribuable = TypeContribuable.HORS_SUISSE;
						break;
					case VAUDOIS_ORDINAIRE:
						typeContribuable = TypeContribuable.VAUDOIS_ORDINAIRE;
						break;
					default:
						throw new IllegalArgumentException("Type d'assujettissement PM non-supporté dans le calculateur de périodes d'assujettissement : " + assujettissement.getType());
					}

					// création de la structure pour la période d'imposition
					resultat.add(new PeriodeImpositionPersonnesMorales(intersection.getDateDebut(),
					                                                   intersection.getDateFin(),
					                                                   entreprise,
					                                                   false,
					                                                   false,
					                                                   causeFermeture,
					                                                   null,
					                                                   exercices,
					                                                   typeContribuable,
					                                                   null));      // TODO un type de document PM sera nécessaire
				}
			}
		}

		return DateRangeHelper.collate(resultat);
	}

	/**
	 * Récupère la date de début de la première période d'imposition à calculer :
	 * <ul>
	 *     <li>on calcule d'abord la dernière date de bouclement de l'entreprise avant le 01.01.2009 (première année pour les périodes d'imposition PM)</li>
	 *     <li>si la date de premier for principal est postérieure à cette date, c'est la date de début du for qui est renvoyée;</li>
	 *     <li>sinon, c'est le lendemain de la date de dernier bouclement trouvé qui est renvoyée.</li>
	 * </ul>
	 */
	private RegDate getDebutPremierePeriodeImposition(@NotNull RegDate debutPremierForPrincipal, Collection<Bouclement> bouclements) {
		final int premiereAnnee = parametreService.getPremierePeriodeFiscalePersonnesMorales();
		final RegDate debutPremiereAnnee = RegDate.get(premiereAnnee, 1, 1);
		final RegDate dernierBouclementOublie = bouclementService.getDateDernierBouclement(bouclements, debutPremiereAnnee, false);
		return RegDateHelper.isAfter(debutPremierForPrincipal, dernierBouclementOublie, NullDateBehavior.EARLIEST)
				? debutPremierForPrincipal
				: dernierBouclementOublie.getOneDayAfter();
	}
}
