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
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

@Entity
@Table(name = "REGIME_FISCAL")
public class RegimeFiscal extends HibernateDateRangeEntity implements LinkedEntity, Duplicable<RegimeFiscal> {

	public enum Portee {
		VD,
		CH
	}

	private Long id;
	private Entreprise entreprise;
	private Portee portee;
	private String code;

	public RegimeFiscal() {
	}

	public RegimeFiscal(RegDate dateDebut, RegDate dateFin, Portee portee, String code) {
		super(dateDebut, dateFin);
		this.portee = portee;
		this.code = code;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
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
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false, updatable = false)
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Column(name = "PORTEE", length = LengthConstants.REGIME_FISCAL_PORTEE, nullable = false)
	@Enumerated(EnumType.STRING)
	public Portee getPortee() {
		return portee;
	}

	public void setPortee(Portee portee) {
		this.portee = portee;
	}

	@Column(name = "CODE", length = LengthConstants.REGIME_FISCAL_TYPE, nullable = false)
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Transient
	@Override
	protected String getBusinessName() {
		return String.format("%s %s", super.getBusinessName(), portee);
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}

	@Transient
	@Override
	public RegimeFiscal duplicate() {
		return new RegimeFiscal(getDateDebut(), getDateFin(), portee, code);
	}
}
