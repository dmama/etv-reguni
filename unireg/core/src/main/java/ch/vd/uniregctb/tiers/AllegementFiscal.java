package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@Table(name = "ALLEGEMENT_FISCAL")
public class AllegementFiscal extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<AllegementFiscal> {

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
	private TypeCollectivite typeCollectivite;
	private Integer noOfsCommune;

	public AllegementFiscal() {
	}

	public AllegementFiscal(RegDate dateDebut, RegDate dateFin, BigDecimal pourcentageAllegement,
	                        TypeImpot typeImpot, TypeCollectivite typeCollectivite, Integer noOfsCommune) {
		super(dateDebut, dateFin);
		this.pourcentageAllegement = pourcentageAllegement;
		this.typeImpot = typeImpot;
		this.typeCollectivite = typeCollectivite;
		this.noOfsCommune = noOfsCommune;
	}

	public AllegementFiscal(AllegementFiscal src) {
		super(src);
		this.pourcentageAllegement = src.pourcentageAllegement;
		this.typeImpot = src.typeImpot;
		this.typeCollectivite = src.typeCollectivite;
		this.noOfsCommune = src.noOfsCommune;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}

	@Transient
	@Override
	public AllegementFiscal duplicate() {
		return new AllegementFiscal(this);
	}

	@Transient
	@Override
	protected String getBusinessName() {
		final String localData;
		if (typeImpot == null && typeCollectivite == null) {
			localData = " universel";
		}
		else {
			final StringBuilder b = new StringBuilder();
			if (typeImpot != null) {
				b.append(" ").append(typeImpot);
			}
			if (typeCollectivite != null) {
				b.append(" ").append(typeCollectivite);
			}
			if (noOfsCommune != null) {
				b.append(" (commune ").append(noOfsCommune).append(")");
			}
			localData = b.toString();
		}
		return String.format("%s%s", super.getBusinessName(), localData);
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
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

	@Column(name = "POURCENTAGE_ALLEGEMENT", nullable = false, precision = 5, scale = 2)
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

	@Column(name = "TYPE_COLLECTIVITE", length = LengthConstants.ALLEGEMENT_FISCAL_TYPE_COLLECTIVITE)
	@Enumerated(EnumType.STRING)
	public TypeCollectivite getTypeCollectivite() {
		return typeCollectivite;
	}

	public void setTypeCollectivite(TypeCollectivite typeCollectivite) {
		this.typeCollectivite = typeCollectivite;
	}

	@Column(name = "NO_OFS_COMMUNE")
	public Integer getNoOfsCommune() {
		return noOfsCommune;
	}

	public void setNoOfsCommune(Integer noOfsCommune) {
		this.noOfsCommune = noOfsCommune;
	}
}
