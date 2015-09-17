package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeContribuable;

/**
 * Calculateur des périodes d'imposition des entreprises
 */
public class PeriodeImpositionPersonnesMoralesCalculator implements PeriodeImpositionCalculator<Entreprise> {

	private final ParametreAppService parametreService;
	private final TiersService tiersService;

	public PeriodeImpositionPersonnesMoralesCalculator(ParametreAppService parametreService, TiersService tiersService) {
		this.parametreService = parametreService;
		this.tiersService = tiersService;
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
		// (mais on ne calcule pas les périodes d'assujettissement depuis la nuit des temps, seulement depuis une année extraite des paramètres)
		final int premiereAnneePeriodesImposition = parametreService.getPremierePeriodeFiscalePersonnesMorales();
		final List<ExerciceCommercial> exercicesBruts = tiersService.getExercicesCommerciaux(entreprise);
		final List<ExerciceCommercial> exercices = extraireDepuisPeriode(exercicesBruts, premiereAnneePeriodesImposition);
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
	 * @param bruts une liste d'exercices commerciaux
	 * @param premierePeriode une année
	 * @return la sous-liste des exercices commerciaux fournis dont la date de fin est au plus tôt dans l'année fournie
	 */
	@NotNull
	private static List<ExerciceCommercial> extraireDepuisPeriode(List<ExerciceCommercial> bruts, int premierePeriode) {
		if (bruts == null || bruts.isEmpty()) {
			return Collections.emptyList();
		}
		final List<ExerciceCommercial> nets = new ArrayList<>(bruts.size());
		for (ExerciceCommercial ex : bruts) {
			if (ex.getDateFin().year() >= premierePeriode) {
				nets.add(ex);
			}
		}
		return nets;
	}
}
