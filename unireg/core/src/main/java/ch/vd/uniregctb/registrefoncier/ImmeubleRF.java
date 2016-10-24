package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.rf.TypeImmeuble;

/**
 * Représente un immeuble au registre foncier
 */
@Entity
@Table(name = "RF_IMMEUBLE")
public class ImmeubleRF {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Identifiant technique de l'immeuble au registre foncier.
	 */
	private String idRF;

	/**
	 * Identifiant fédéral de l'immeuble.
	 */
	private String egrid;

	/**
	 * Le type d'immeuble.
	 */
	private TypeImmeuble type;

	/**
	 * URL d'accès à Intercapi pour l'immeuble concerné
	 */
	private String urlIntercapi;

	/**
	 * Vrai si l'immeuble est une construction sur fond d'autrui (CFA).
	 */
	private boolean cfa;

	/**
	 * La quote-part de l'immeuble dans le cas d'une PPE
	 */
	@Nullable
	private Fraction quotePartPPE;

	/**
	 * Les situations de l'immeuble.
	 */
	private Set<SituationRF> situations;

	/**
	 * Les surfaces de l'immeuble.
	 */
	private Set<SurfaceRF> surfaces;

	/**
	 * Les estimations de l'immeuble.
	 */
	private Set<EstimationRF> estimations;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ID_RF", nullable = false)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "EGRID", length = 14)
	public String getEgrid() {
		return egrid;
	}

	public void setEgrid(String egrid) {
		this.egrid = egrid;
	}

	@Column(name = "TYPE", nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeImmeuble getType() {
		return type;
	}

	public void setType(TypeImmeuble type) {
		this.type = type;
	}

	@Column(name = "URL_INTERCAPI", length = 2000)
	public String getUrlIntercapi() {
		return urlIntercapi;
	}

	public void setUrlIntercapi(String urlIntercapi) {
		this.urlIntercapi = urlIntercapi;
	}

	@Column(name = "CFA", nullable = false)
	public boolean isCfa() {
		return cfa;
	}

	public void setCfa(boolean cfa) {
		this.cfa = cfa;
	}

	@Nullable
	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "QUOTE_PART_NUM")),
			@AttributeOverride(name = "denominateur", column = @Column(name = "QUOTE_PART_DENOM"))
	})
	public Fraction getQuotePartPPE() {
		return quotePartPPE;
	}

	public void setQuotePartPPE(@Nullable Fraction quotePartPPE) {
		this.quotePartPPE = quotePartPPE;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SIT_RF_IMMEUBLE_ID")
	public Set<SituationRF> getSituations() {
		return situations;
	}

	public void setSituations(Set<SituationRF> situations) {
		this.situations = situations;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SURF_RF_IMMEUBLE_ID")
	public Set<SurfaceRF> getSurfaces() {
		return surfaces;
	}

	public void setSurfaces(Set<SurfaceRF> surfaces) {
		this.surfaces = surfaces;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_ESTIM_RF_IMMEUBLE_ID")
	public Set<EstimationRF> getEstimations() {
		return estimations;
	}

	public void setEstimations(Set<EstimationRF> estimations) {
		this.estimations = estimations;
	}
}
