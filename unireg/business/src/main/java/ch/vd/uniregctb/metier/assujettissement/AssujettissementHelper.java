package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Quelques méthodes pratiques pour la manipulation de listes d'assujettissements
 */
public abstract class AssujettissementHelper {

	private static final DateRangeHelper.AdapterCallback<Assujettissement> ADAPTER = new DateRangeHelper.AdapterCallback<Assujettissement>() {
		@Override
		public Assujettissement adapt(Assujettissement assujettissement, RegDate debut, RegDate fin) {
			final RegDate debutEffectif;
			final MotifFor motifFracDebut;
			if (debut != null && assujettissement.getDateDebut() != debut) {
				debutEffectif = debut;
				motifFracDebut = null;
			}
			else {
				debutEffectif = assujettissement.getDateDebut();
				motifFracDebut = assujettissement.getMotifFractDebut();
			}

			final RegDate finEffective;
			final MotifFor motifFracFin;
			if (fin != null && assujettissement.getDateFin() != fin) {
				finEffective = fin;
				motifFracFin = null;
			}
			else {
				finEffective = assujettissement.getDateFin();
				motifFracFin = assujettissement.getMotifFractFin();
			}

			return assujettissement.duplicate(debutEffectif, finEffective, motifFracDebut, motifFracFin);
		}
	};

	/**
	 * Découpe les assujettissements spécifié année par année pour les années spécifiées
	 *
	 * @param list      les assujettissements à découper
	 * @param startYear l'année de départ (comprise)
	 * @param endYear   l'année de fin (comprise)
	 * @return une liste d'assujettissements découpés année par année
	 */
	public static List<Assujettissement> split(List<Assujettissement> list, int startYear, int endYear) {

		List<Assujettissement> splitted = new ArrayList<>();

		// split des assujettissement par années
		for (int year = startYear; year <= endYear; ++year) {
			final List<Assujettissement> extracted = extractYear(list, year);
			splitted.addAll(extracted);
		}

		return splitted;
	}

	/**
	 * Extrait la partie de l'assujettissement liée à l'année donnée
	 * @param list l'assujettissement "complet"
	 * @param annee l'année à considérer
	 * @return la liste des assujettissements de l'année considérée
	 */
	public static List<Assujettissement> extractYear(List<Assujettissement> list, int annee) {
		return extract(list, RegDate.get(annee, 1, 1), RegDate.get(annee, 12, 31));
	}

	/**
	 * Extrait la partie de l'assujettissement liée à la période entre les dates de début et de fin
	 * @param list l'assujettissement "complet"
	 * @param dateDebut la date de début de la période à considérer
	 * @param dateFin la date de fin de la période à considérer
	 * @return la liste des assujettissements de la période
	 */
	public static List<Assujettissement> extract(List<Assujettissement> list, RegDate dateDebut, RegDate dateFin) {
		if (list == null) {
			return null;
		}
		return DateRangeHelper.extract(list, dateDebut, dateFin, ADAPTER);
	}

	/**
	 * @param source calculateur d'assujettissement source
	 * @param annee année civile de limitation de l'assujettissement
	 * @param <T> type de contribuable supporté par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source à l'année civile indiquée
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> yearLimiting(final AssujettissementCalculator<T> source, final int annee) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}
				final List<Assujettissement> yearly = extractYear(all, annee);
				return yearly.isEmpty() ? null : yearly;
			}
		};
	}

	/**
	 * @param source calculateur d'assujettissement source
	 * @param range range de limitation de l'assujettissement (obligatoire si <i>collate</i> est faux)
	 * @param collate indique s'il faut fusionner les assujettissements adjacents dans le résultat fourni
	 * @param <T> type de contribuable traité par le calculateur
	 * @return calculateur qui limite l'assujettissement rendu par le calculateur source à la période indiquée
	 */
	public static <T extends Contribuable> AssujettissementCalculator<T> rangeLimiting(final AssujettissementCalculator<T> source, @Nullable final DateRange range, final boolean collate) {
		return new AssujettissementCalculator<T>() {
			@Override
			public List<Assujettissement> determine(T ctb, ForsParType fpt, @Nullable Set<Integer> noOfsCommunesVaudoises) throws AssujettissementException {
				final List<Assujettissement> all = source.determine(ctb, fpt, noOfsCommunesVaudoises);
				if (all == null) {
					return null;
				}

				final List<Assujettissement> splittedCollated;
				if (!collate) {
					if (range == null) {
						throw new IllegalArgumentException("Le range doit être spécifié si collate=false");
					}
					splittedCollated = split(all, range.getDateDebut().year(), range.getDateFin().year());
				}
				else {
					splittedCollated = DateRangeHelper.collate(all);
				}

				final List<Assujettissement> ranged;
				if (range != null) {
					// Limitation des assujettissements au range demandé
					ranged = extract(splittedCollated, range.getDateDebut(), range.getDateFin());
				}
				else {
					ranged = splittedCollated;
				}

				return ranged.isEmpty() ? null : ranged;
			}
		};
	}
}
