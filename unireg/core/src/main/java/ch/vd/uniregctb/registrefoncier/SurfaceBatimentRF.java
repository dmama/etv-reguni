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

import ch.vd.uniregctb.common.HibernateDateRangeEntity;

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
	 * La surface en mètre carrés (m2).
	 */
	private int surface;

	/**
	 * Le bâtiment concerné par la surface.
	 */
	private BatimentRF batiment;

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

	@Column(name = "SURFACE", nullable = false)
	public int getSurface() {
		return surface;
	}

	public void setSurface(int surface) {
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