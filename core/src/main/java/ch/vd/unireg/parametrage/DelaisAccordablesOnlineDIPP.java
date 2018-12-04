package ch.vd.unireg.parametrage;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.DayMonth;

/**
 * Délais accordables pour les demandes de délais online (e-Délai) sur des DIs PP et
 *  * valables pendant une plage temporelle déterminée (e.g. du 1er janvier au 15 mars,
 *  * les délais accordables sont 30.06 et 15.09).
 */
@Entity
@DiscriminatorValue("DI_PP")
public class DelaisAccordablesOnlineDIPP extends DelaisAccordablesOnline implements DateRange {

	private RegDate dateDebut;
	private RegDate dateFin;
	private List<DayMonth> delaisDemandeUnitaire;
	private List<DayMonth> delaisDemandeGroupee;

	// nécessaire pour Hibernate
	public DelaisAccordablesOnlineDIPP() {
	}

	public DelaisAccordablesOnlineDIPP(RegDate dateDebut, RegDate dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.delaisDemandeUnitaire = Collections.emptyList();
		this.delaisDemandeGroupee = Collections.emptyList();
	}

	public DelaisAccordablesOnlineDIPP(@NotNull RegDate dateDebut, @NotNull RegDate dateFin, @Nullable DayMonth delaiDemandeUnitaire, @Nullable DayMonth delaiDemandeGroupee) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.delaisDemandeUnitaire = (delaiDemandeUnitaire == null ? Collections.emptyList() : Collections.singletonList(delaiDemandeUnitaire));
		this.delaisDemandeGroupee = (delaiDemandeGroupee == null ? Collections.emptyList() : Collections.singletonList(delaiDemandeGroupee));
	}

	public DelaisAccordablesOnlineDIPP(@NotNull RegDate dateDebut, @NotNull RegDate dateFin, List<DayMonth> delaisDemandeUnitaire, List<DayMonth> delaisDemandeGroupee) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.delaisDemandeUnitaire = delaisDemandeUnitaire;
		this.delaisDemandeGroupee = delaisDemandeGroupee;
	}

	public DelaisAccordablesOnlineDIPP(@NotNull DelaisAccordablesOnlineDIPP right, int periodeFiscale) {
		this.dateDebut = RegDate.get(periodeFiscale + 1, right.getDateDebut().month(), right.getDateDebut().day());
		this.dateFin = RegDate.get(periodeFiscale + 1, right.getDateFin().month(), right.getDateFin().day());
		this.delaisDemandeUnitaire = new ArrayList<>(right.getDelaisDemandeUnitaire()); // les objets DayMonth sont immutables, inutile d'en faire des copies
		this.delaisDemandeGroupee = new ArrayList<>(right.getDelaisDemandeGroupee());
	}

	@Override
	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "UNITAIRE_PP")
	@Type(type = "ch.vd.unireg.hibernate.DayMonthListUserType")
	public List<DayMonth> getDelaisDemandeUnitaire() {
		return delaisDemandeUnitaire;
	}

	public void setDelaisDemandeUnitaire(List<DayMonth> delaisDemandeUnitaire) {
		this.delaisDemandeUnitaire = delaisDemandeUnitaire;
	}

	public void addDelaisDemandeUnitaire(@NotNull DayMonth delais) {
		if (this.delaisDemandeUnitaire == null) {
			this.delaisDemandeUnitaire = new ArrayList<>();
		}
		this.delaisDemandeUnitaire.add(delais);
	}

	@Column(name = "GROUPEE_PP")
	@Type(type = "ch.vd.unireg.hibernate.DayMonthListUserType")
	public List<DayMonth> getDelaisDemandeGroupee() {
		return delaisDemandeGroupee;
	}

	public void setDelaisDemandeGroupee(List<DayMonth> delaisDemandeGroupee) {
		this.delaisDemandeGroupee = delaisDemandeGroupee;
	}

	public void addDelaisDemandeGroupee(@NotNull DayMonth delais) {
		if (this.delaisDemandeGroupee == null) {
			this.delaisDemandeGroupee = new ArrayList<>();
		}
		this.delaisDemandeGroupee.add(delais);
	}

	@Transient
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@NotNull
	@Override
	public DelaisAccordablesOnlineDIPP duplicateFor(int periodeFiscale) {
		return new DelaisAccordablesOnlineDIPP(this, periodeFiscale);
	}

	public void copyTo(@NotNull DelaisAccordablesOnlineDIPP right) {
		right.setDateDebut(dateDebut);
		right.setDateFin(dateFin);
		right.setDelaisDemandeUnitaire(new ArrayList<>(this.delaisDemandeUnitaire));
		right.setDelaisDemandeGroupee(new ArrayList<>(this.delaisDemandeGroupee));
	}
}
