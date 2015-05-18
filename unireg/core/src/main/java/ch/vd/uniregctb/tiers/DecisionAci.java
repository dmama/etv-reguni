package ch.vd.uniregctb.tiers;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@Table(name = "DECISION_ACI")
public class DecisionAci extends HibernateEntity implements LinkedEntity, DateRange, Duplicable<DecisionAci>, BusinessComparable<DecisionAci> {

	private Long id;
	private Contribuable contribuable;
	private RegDate dateDebut;
	private RegDate dateFin;
	private Integer numeroOfsAutoriteFiscale;
	private TypeAutoriteFiscale typeAutoriteFiscale;
	private String remarque;

	public DecisionAci() {
	}

	public DecisionAci(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, String remarque) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
		this.typeAutoriteFiscale = typeAutoriteFiscale;
		this.remarque = remarque;
		this.contribuable = contribuable;
	}

	public DecisionAci(DecisionAci da) {
		this(da.getContribuable(), da.getDateDebut(), da.getDateFin(), da.getNumeroOfsAutoriteFiscale(), da.getTypeAutoriteFiscale(), da.getRemarque());
	}

	@Override
	@Transient
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DECISION_ACI_TIERS_ID", columnNames = "TIERS_ID")
	public Contribuable getContribuable() {
		return contribuable;
	}

	public void setContribuable(Contribuable contribuable) {
		this.contribuable = contribuable;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return  !isAnnule() && RegDateHelper.isBetween(date == null ? RegDate.get() : date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Column(name = "NUMERO_OFS", nullable = false)
	public Integer getNumeroOfsAutoriteFiscale() {
		return numeroOfsAutoriteFiscale;
	}

	public void setNumeroOfsAutoriteFiscale(Integer numeroOfsAutoriteFiscale) {
		this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
	}

	@Column(name = "TYPE_AUT_FISC", nullable = false, length = LengthConstants.FOR_AUTORITEFISCALE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAutoriteFiscaleUserType")
	public TypeAutoriteFiscale getTypeAutoriteFiscale() {
		return typeAutoriteFiscale;
	}

	public void setTypeAutoriteFiscale(TypeAutoriteFiscale typeAutoriteFiscale) {
		this.typeAutoriteFiscale = typeAutoriteFiscale;
	}

	@Column(name = "REMARQUE", length = LengthConstants.TIERS_REMARQUE)
	public String getRemarque() {
		return remarque;
	}

	public void setRemarque(String remarque) {
		this.remarque = remarque;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return contribuable == null ? null : Collections.singletonList(contribuable);
	}

	/**
	 * Retourne true si la decision contient les mêmes informations que celle passée en paramètre.
	 *
	 */
	public boolean equalsTo(DecisionAci obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		return ComparisonHelper.areEqual(dateDebut, obj.dateDebut)
				&& ComparisonHelper.areEqual(dateFin, obj.dateFin)
				&& ComparisonHelper.areEqual(numeroOfsAutoriteFiscale, obj.numeroOfsAutoriteFiscale)
				&& ComparisonHelper.areEqual(typeAutoriteFiscale, obj.typeAutoriteFiscale)
				&& ComparisonHelper.areEqual(remarque, obj.remarque)
				&& ComparisonHelper.areEqual(isAnnule(), obj.isAnnule());
	}

	@Override
	public DecisionAci duplicate() {
		return new DecisionAci(this);
	}


	@Override
	public String toString() {
		final String dateDebutStr = dateDebut != null ? RegDateHelper.dateToDisplayString(dateDebut) : "?";
		final String dateFinStr = dateFin != null ? RegDateHelper.dateToDisplayString(dateFin) : "?";
		return String.format("%s (%s - %s)", getClass().getSimpleName(), dateDebutStr, dateFinStr);
	}


}
