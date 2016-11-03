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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

/**
 * Représente un immeuble au registre foncier
 */
@Entity
@Table(name = "RF_IMMEUBLE")
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

	/**
	 * Le ou les bâtiments correspondants à l'immeuble.
	 */
	private Set<BatimentRF> batiments;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Index(name = "IDX_IMMEUBLE_ID_RF")
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

	@Column(name = "URL_INTERCAPI", length = 2000)
	public String getUrlIntercapi() {
		return urlIntercapi;
	}

	public void setUrlIntercapi(String urlIntercapi) {
		this.urlIntercapi = urlIntercapi;
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

	public void addSituation(@NotNull SituationRF situation) {
		if (this.situations == null) {
			this.situations = new HashSet<>();
		}
		this.situations.add(situation);
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

	public void addEstimation(@NotNull EstimationRF estimation) {
		if (this.estimations == null) {
			this.estimations = new HashSet<>();
		}
		this.estimations.add(estimation);
	}

	@ManyToMany(targetEntity = BatimentRF.class, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(name = "RF_IMMEUBLE_BATIMENT", joinColumns = @JoinColumn(name = "IMMEUBLE_ID"), inverseJoinColumns = @JoinColumn(name = "BATIMENT_ID"))
	public Set<BatimentRF> getBatiments() {
		return batiments;
	}

	public void setBatiments(Set<BatimentRF> batiments) {
		this.batiments = batiments;
	}
}
