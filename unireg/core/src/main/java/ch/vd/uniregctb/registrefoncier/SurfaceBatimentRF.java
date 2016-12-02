package ch.vd.uniregctb.registrefoncier;

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

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * La surface d'un bâtiment valide pendant une période donnée.
 */
@Entity
@Table(name = "RF_SURFACE_BATIMENT")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class SurfaceBatimentRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le type de surface.
	 */
	@Nullable
	private String type;

	/**
	 * La surface en mètre carrés (m2).
	 */
	@Nullable
	private Integer surface;

	/**
	 * Le bâtiment concerné par la surface.
	 */
	private BatimentRF batiment;

	public SurfaceBatimentRF() {
	}

	public SurfaceBatimentRF(@Nullable String type, @Nullable Integer surface) {
		this(type, surface, null, null);
	}

	public SurfaceBatimentRF(@Nullable String type, @Nullable Integer surface, RegDate dateDebut, RegDate dateFin) {
		super(dateDebut, dateFin);
		if (type == null && surface == null) {
			throw new IllegalArgumentException("Un des deux valeurs type ou surface doit être renseignée.");
		}
		this.type = type;
		this.surface = surface;
	}

	@Transient
	@Override
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

	@Nullable
	@Column(name = "TYPE", length = LengthConstants.RF_TYPE_BATIMENT)
	public String getType() {
		return type;
	}

	public void setType(@Nullable String type) {
		this.type = type;
	}

	@Nullable
	@Column(name = "SURFACE", nullable = true)
	public Integer getSurface() {
		return surface;
	}

	public void setSurface(@Nullable Integer surface) {
		this.surface = surface;
	}

	// configuration hibernate : le bâtiment possède les surfaces du bâtiment
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "BATIMENT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_SURF_BAT_RF_BATIMENT_ID", columnNames = "BATIMENT_ID")
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}
}