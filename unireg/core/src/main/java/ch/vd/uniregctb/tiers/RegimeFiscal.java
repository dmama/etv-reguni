package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

@Entity
@Table(name = "REGIME_FISCAL")
public class RegimeFiscal extends HibernateDateRangeEntity {

	public enum Portee {
		VD,
		CH
	}

	private Long id;
	private Entreprise entreprise;
	private Portee portee;
	private TypeRegimeFiscal type;

	public RegimeFiscal() {
	}

	public RegimeFiscal(RegDate dateDebut, RegDate dateFin, Portee portee, TypeRegimeFiscal type) {
		super(dateDebut, dateFin);
		this.portee = portee;
		this.type = type;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false, updatable = false)
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Column(name = "PORTEE", length = LengthConstants.REGIME_FISCAL_PORTEE, nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	public Portee getPortee() {
		return portee;
	}

	public void setPortee(Portee portee) {
		this.portee = portee;
	}

	@Column(name = "TYPE", length = LengthConstants.REGIME_FISCAL_TYPE, nullable = false, updatable = false)
	@Enumerated(EnumType.STRING)
	public TypeRegimeFiscal getType() {
		return type;
	}

	public void setType(TypeRegimeFiscal type) {
		this.type = type;
	}

	@Transient
	@Override
	protected String getBusinessName() {
		return String.format("%s %s", super.getBusinessName(), portee);
	}
}
