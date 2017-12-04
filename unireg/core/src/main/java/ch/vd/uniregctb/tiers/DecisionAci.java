package ch.vd.uniregctb.tiers;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessComparable;
import ch.vd.uniregctb.common.ComparisonHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@Entity
@Table(name = "DECISION_ACI")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN")),
		@AttributeOverride(name = "numeroOfsAutoriteFiscale", column = @Column(name = "NUMERO_OFS", nullable = false)),
		@AttributeOverride(name = "typeAutoriteFiscale", column = @Column(name = "TYPE_AUT_FISC", nullable = false, length = LengthConstants.FOR_AUTORITEFISCALE))
})
public class DecisionAci extends LocalisationDatee implements LinkedEntity, Duplicable<DecisionAci>, BusinessComparable<DecisionAci> {

	private Long id;
	private Contribuable contribuable;
	private String remarque;

	public DecisionAci() {
	}

	public DecisionAci(Contribuable contribuable, RegDate dateDebut, RegDate dateFin, Integer numeroOfsAutoriteFiscale, TypeAutoriteFiscale typeAutoriteFiscale, String remarque) {
		super(dateDebut, dateFin, typeAutoriteFiscale, numeroOfsAutoriteFiscale);
		this.remarque = remarque;
		this.contribuable = contribuable;
	}

	public DecisionAci(DecisionAci da) {
		super(da);
		this.remarque = da.remarque;
		this.contribuable = da.contribuable;
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
		return super.isValidAt(date == null ? RegDate.get() : date);
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
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return contribuable == null ? null : Collections.singletonList(contribuable);
	}

	/**
	 * Retourne true si la decision contient les mêmes informations que celle passée en paramètre.
	 */
	public boolean equalsTo(DecisionAci obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		return ComparisonHelper.areEqual(getDateDebut(), obj.getDateDebut())
				&& ComparisonHelper.areEqual(getDateFin(), obj.getDateFin())
				&& ComparisonHelper.areEqual(remarque, obj.remarque)
				&& ComparisonHelper.areEqual(getNumeroOfsAutoriteFiscale(), obj.getNumeroOfsAutoriteFiscale())
				&& ComparisonHelper.areEqual(getTypeAutoriteFiscale(), obj.getTypeAutoriteFiscale())
				&& ComparisonHelper.areEqual(isAnnule(), obj.isAnnule());
	}

	@Override
	public DecisionAci duplicate() {
		return new DecisionAci(this);
	}
}
