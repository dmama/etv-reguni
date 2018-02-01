package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeAutoriteFiscale;

@MappedSuperclass
public abstract class LocalisationDatee extends HibernateDateRangeEntity implements LocalizedDateRange {

	private TypeAutoriteFiscale typeAutoriteFiscale;
	private Integer numeroOfsAutoriteFiscale;

	public LocalisationDatee() {
	}

	public LocalisationDatee(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfsAutoriteFiscale) {
		super(dateDebut, dateFin);
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.numeroOfsAutoriteFiscale = noOfsAutoriteFiscale;
	}

	public LocalisationDatee(LocalisationDatee source) {
		super(source);
		this.typeAutoriteFiscale = source.typeAutoriteFiscale;
		this.numeroOfsAutoriteFiscale = source.numeroOfsAutoriteFiscale;
	}

	@Column(name = "TYPE_AUT_FISC", length = LengthConstants.FOR_AUTORITEFISCALE)
	@Type(type = "ch.vd.unireg.hibernate.TypeAutoriteFiscaleUserType")
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
}
