package ch.vd.unireg.param.view;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.parametrage.DelaisAccordablesOnlineDIPP;
import ch.vd.unireg.type.DayMonth;

/**
 * View des délais accordables sur les PPs pour une période fiscale déterminée.
 */
public class DelaisAccordablesOnlinePPView {

	private RegDate dateDebut;
	private RegDate dateFin;
	@Nullable
	private DayMonth delai1DemandeUnitaire;
	@Nullable
	private DayMonth delai2DemandeUnitaire;
	@Nullable
	private DayMonth delai1DemandeGroupee;
	@Nullable
	private DayMonth delai2DemandeGroupee;

	public DelaisAccordablesOnlinePPView() {
	}

	public DelaisAccordablesOnlinePPView(@NotNull DelaisAccordablesOnlineDIPP right) {
		this.dateDebut = right.getDateDebut();
		this.dateFin = right.getDateFin();

		// pour l'instant, l'IHM ne permet que de voir/définir 2 délais par type de demande (même si le modèle en permet plus)
		final List<DayMonth> delaisUnitaires = right.getDelaisDemandeUnitaire();
		final int sizeUnitaires = delaisUnitaires.size();
		if (sizeUnitaires < 1) {
			this.delai1DemandeUnitaire = null;
			this.delai2DemandeUnitaire = null;
		}
		else if (sizeUnitaires > 1) {
			this.delai1DemandeUnitaire = delaisUnitaires.get(0);
			this.delai2DemandeUnitaire = delaisUnitaires.get(1);
		}
		else {
			this.delai1DemandeUnitaire = delaisUnitaires.get(0);
			this.delai2DemandeUnitaire = null;
		}

		final List<DayMonth> delaisGroupees = right.getDelaisDemandeGroupee();
		final int sizeGroupees = delaisGroupees.size();
		if (sizeGroupees < 1) {
			this.delai1DemandeGroupee = null;
			this.delai2DemandeGroupee = null;
		}
		else if (sizeGroupees > 1) {
			this.delai1DemandeGroupee = delaisGroupees.get(0);
			this.delai2DemandeGroupee = delaisGroupees.get(1);
		}
		else {
			this.delai1DemandeGroupee = delaisGroupees.get(0);
			this.delai2DemandeGroupee = null;
		}
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Nullable
	public DayMonth getDelai1DemandeUnitaire() {
		return delai1DemandeUnitaire;
	}

	public void setDelai1DemandeUnitaire(@Nullable DayMonth delai1DemandeUnitaire) {
		this.delai1DemandeUnitaire = delai1DemandeUnitaire;
	}

	@Nullable
	public DayMonth getDelai2DemandeUnitaire() {
		return delai2DemandeUnitaire;
	}

	public void setDelai2DemandeUnitaire(@Nullable DayMonth delai2DemandeUnitaire) {
		this.delai2DemandeUnitaire = delai2DemandeUnitaire;
	}

	@Nullable
	public DayMonth getDelai1DemandeGroupee() {
		return delai1DemandeGroupee;
	}

	public void setDelai1DemandeGroupee(@Nullable DayMonth delai1DemandeGroupee) {
		this.delai1DemandeGroupee = delai1DemandeGroupee;
	}

	@Nullable
	public DayMonth getDelai2DemandeGroupee() {
		return delai2DemandeGroupee;
	}

	public void setDelai2DemandeGroupee(@Nullable DayMonth delai2DemandeGroupee) {
		this.delai2DemandeGroupee = delai2DemandeGroupee;
	}
}
