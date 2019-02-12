package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;

public class ExerciceCommercialView implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final boolean oneYearLong;
	private final boolean first;
	private final boolean withDI;
	private final boolean tooOldToHaveDI;
	private final boolean dateDebutExerciceRenseignee;
	private final boolean bouclementsRenseignes;


	public ExerciceCommercialView(ExerciceCommercial exercice, boolean first, boolean withDI, boolean tooOldToHaveDI, boolean dateDebutExerciceRenseignee, boolean bouclementsRenseignes) {
		this.dateDebut = exercice.getDateDebut();
		this.dateFin = exercice.getDateFin();
		this.oneYearLong = exercice.getDateDebut().addYears(1).getOneDayBefore() == exercice.getDateFin();
		this.first = first;
		this.withDI = withDI;
		this.tooOldToHaveDI = tooOldToHaveDI;
		this.dateDebutExerciceRenseignee = dateDebutExerciceRenseignee;
		this.bouclementsRenseignes = bouclementsRenseignes;
	}

	@Override
	public RegDate getDateDebut() {
		//SIFISC-30339 Si pas de date saisie on n'affiche pas de valeur calculé
		if (!dateDebutExerciceRenseignee && first) {
			return null;
		}
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		//SIFISC-30339 Si pas de bouclement existant on n'affiche pas de valeur calculé
		if (!bouclementsRenseignes) {
			return null;
		}
		return dateFin;
	}

	public boolean isOneYearLong() {
		return oneYearLong;
	}

	public boolean isFirst() {
		return first;
	}

	public boolean isWithDI() {
		return withDI;
	}

	public boolean isTooOldToHaveDI() {
		return tooOldToHaveDI;
	}

	public boolean isDateDebutExerciceRenseignee() {
		return dateDebutExerciceRenseignee;
	}

	public boolean isBouclementsRenseignes() {
		return bouclementsRenseignes;
	}
}
