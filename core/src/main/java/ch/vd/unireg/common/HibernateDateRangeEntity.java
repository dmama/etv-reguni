package ch.vd.unireg.common;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

@MappedSuperclass
public abstract class HibernateDateRangeEntity extends HibernateEntity implements DateRange {

	private RegDate dateDebut;
	private RegDate dateFin;

	public HibernateDateRangeEntity() {
	}

	public HibernateDateRangeEntity(RegDate dateDebut, RegDate dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	protected HibernateDateRangeEntity(HibernateDateRangeEntity src) {
		this(src.dateDebut, src.dateFin);
	}

	@Override
	@Column(name = "DATE_DEBUT", nullable = false)
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

	@Transient
	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@Override
	public String toString() {
		final String dateDebutStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateDebut), "?");
		final String dateFinStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?");
		return String.format("%s (%s - %s)", getBusinessName(), dateDebutStr, dateFinStr);
	}

	@Transient
	protected String getBusinessName() {
		return getClass().getSimpleName();
	}
}
