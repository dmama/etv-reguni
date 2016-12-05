package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.LengthConstants;

/**
 * Représente un immeuble au registre foncier
 */
@Entity
@Table(name = "RF_IMMEUBLE", uniqueConstraints = @UniqueConstraint(name="CS_IMMEUBLE_RF_ID", columnNames = "ID_RF"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class ImmeubleRF {

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
	 * URL d'accès à Intercapi pour l'immeuble concerné
	 */
	private String urlIntercapi;

	/**
	 * Les situations (historisées) de l'immeuble.
	 */
	private Set<SituationRF> situations;

	/**
	 * Les surfaces totales (historisées) de l'immeuble.
	 */
	private Set<SurfaceTotaleRF> surfacesTotales;

	/**
	 * Les surfaces au sol (multiples + historisées) de l'immeuble.
	 */
	private Set<SurfaceAuSolRF> surfacesAuSol;

	/**
	 * Les estimations (historisées) de l'immeuble.
	 */
	private Set<EstimationRF> estimations;

	/**
	 * Le ou les bâtiments (multiples + historisés) implanté sur cet immeuble.
	 */
	private Set<ImplantationRF> implantations;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "ID_RF", nullable = false, length = LengthConstants.RF_ID_RF)
	public String getIdRF() {
		return idRF;
	}

	public void setIdRF(String idRF) {
		this.idRF = idRF;
	}

	@Column(name = "EGRID", length = LengthConstants.RF_EGRID)
	public String getEgrid() {
		return egrid;
	}

	public void setEgrid(String egrid) {
		this.egrid = egrid;
	}

	@Column(name = "URL_INTERCAPI", length = LengthConstants.RF_URL_INTERCAPI)
	public String getUrlIntercapi() {
		return urlIntercapi;
	}

	public void setUrlIntercapi(String urlIntercapi) {
		this.urlIntercapi = urlIntercapi;
	}

	// configuration hibernate : l'immeuble possède les situations
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SIT_RF_IMMEUBLE_ID")
	public Set<SituationRF> getSituations() {
		return situations;
	}

	public void setSituations(Set<SituationRF> situations) {
		this.situations = situations;
	}

	public void addSituation(@NotNull SituationRF situation) {
		if (this.situations == null) {
			this.situations = new HashSet<>();
		}
		this.situations.add(situation);
	}

	// configuration hibernate : l'immeuble possède les surfaces totales
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SURF_TOT_RF_IMMEUBLE_ID")
	public Set<SurfaceTotaleRF> getSurfacesTotales() {
		return surfacesTotales;
	}

	public void setSurfacesTotales(Set<SurfaceTotaleRF> surfacesTotales) {
		this.surfacesTotales = surfacesTotales;
	}

	public void addSurfaceTotale(@NotNull SurfaceTotaleRF surfaceTotale) {
		if (this.surfacesTotales == null) {
			this.surfacesTotales = new HashSet<>();
		}
		this.surfacesTotales.add(surfaceTotale);
	}

	// configuration hibernate : l'immeuble ne possède pas les surfaces au sol
	@OneToMany(mappedBy = "immeuble")
	public Set<SurfaceAuSolRF> getSurfacesAuSol() {
		return surfacesAuSol;
	}

	public void setSurfacesAuSol(Set<SurfaceAuSolRF> surfacesAuSol) {
		this.surfacesAuSol = surfacesAuSol;
	}

	// configuration hibernate : l'immeuble possède les estimations fiscales
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_ESTIM_RF_IMMEUBLE_ID")
	public Set<EstimationRF> getEstimations() {
		return estimations;
	}

	public void setEstimations(Set<EstimationRF> estimations) {
		this.estimations = estimations;
	}

	public void addEstimation(@NotNull EstimationRF estimation) {
		if (this.estimations == null) {
			this.estimations = new HashSet<>();
		}
		this.estimations.add(estimation);
	}

	// configuration hibernate : l'immeuble ne possède pas les implantations
	@OneToMany(mappedBy = "immeuble")
	public Set<ImplantationRF> getImplantations() {
		return implantations;
	}

	public void setImplantations(Set<ImplantationRF> implantations) {
		this.implantations = implantations;
	}
}
