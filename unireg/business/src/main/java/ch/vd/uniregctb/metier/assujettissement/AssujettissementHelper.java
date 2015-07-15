package ch.vd.uniregctb.metier.assujettissement;

import java.util.ArrayList;
import java.util.List;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
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
}
