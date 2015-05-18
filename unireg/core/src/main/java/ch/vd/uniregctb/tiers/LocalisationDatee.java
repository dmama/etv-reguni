package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@MappedSuperclass
public abstract class LocalisationDatee extends HibernateEntity implements DateRange {

	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer numeroOfsAutoriteFiscale;

	public LocalisationDatee() {
	}

	public LocalisationDatee(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfsAutoriteFiscale) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.numeroOfsAutoriteFiscale = noOfsAutoriteFiscale;
	}

	public LocalisationDatee(LocalisationDatee source) {
		this.dateDebut = source.dateDebut;
		this.dateFin = source.dateFin;
		this.typeAutoriteFiscale = source.typeAutoriteFiscale;
		this.numeroOfsAutoriteFiscale = source.numeroOfsAutoriteFiscale;
	}

	@Override
	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "TYPE_AUT_FISC", length = LengthConstants.FOR_AUTORITEFISCALE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAutoriteFiscaleUserType")
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	@Column(name = "NUMERO_OFS_AUT_FISC")
	public Integer getNumeroOfsAutoriteFiscale() {
		return numeroOfsAutoriteFiscale;
	}

	public void setNumeroOfsAutoriteFiscale(Integer numeroOfsAutoriteFiscale) {
		this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public String toString() {
		final String dateDebutStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateDebut), "?");
		final String dateFinStr = StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(dateFin), "?");
		return String.format("%s (%s - %s)", getClass().getSimpleName(), dateDebutStr, dateFinStr);
	}
}
