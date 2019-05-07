package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Duplicable;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

@Entity
@Table(name = "ALLEGEMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE_COLLECTIVITE", length = LengthConstants.ALLEGEMENT_FISCAL_TYPE_COLLECTIVITE)
public abstract class AllegementFiscal extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<AllegementFiscal> {

	public enum TypeImpot {
		BENEFICE,
		CAPITAL
	}

	public enum TypeCollectivite {
		COMMUNE,
		CANTON,
		CONFEDERATION
	}

	private Long id;
	private Entreprise entreprise;
	private BigDecimal pourcentageAllegement;
	private TypeImpot typeImpot;

	public AllegementFiscal() {
	}

	public AllegementFiscal(RegDate dateDebut, @Nullable RegDate dateFin, @Nullable BigDecimal pourcentageAllegement, TypeImpot typeImpot) {
		super(dateDebut, dateFin);
		this.pourcentageAllegement = pourcentageAllegement;
		this.typeImpot = typeImpot;
	}

	public AllegementFiscal(AllegementFiscal src) {
		super(src);
		this.pourcentageAllegement = src.pourcentageAllegement;
		this.typeImpot = src.typeImpot;
	}

	@Transient
	public abstract TypeCollectivite getTypeCollectivite();

	@Transient
	public boolean isAllegementMontant() {
		return pourcentageAllegement == null;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}

	@Transient
	@Override
	protected String getBusinessName() {
		if (typeImpot == null) {
			return super.getBusinessName();
		}
		return String.format("%s %s", super.getBusinessName(), typeImpot);
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID")
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Column(name = "POURCENTAGE_ALLEGEMENT", precision = 5, scale = 2)
	public BigDecimal getPourcentageAllegement() {
		return pourcentageAllegement;
	}

	public void setPourcentageAllegement(BigDecimal pourcentageAllegement) {
		this.pourcentageAllegement = pourcentageAllegement;
	}

	@Column(name = "TYPE_IMPOT", length = LengthConstants.ALLEGEMENT_FISCAL_TYPE_IMPOT)
	@Enumerated(EnumType.STRING)
	public TypeImpot getTypeImpot() {
		return typeImpot;
	}

	public void setTypeImpot(TypeImpot typeImpot) {
		this.typeImpot = typeImpot;
	}

}
