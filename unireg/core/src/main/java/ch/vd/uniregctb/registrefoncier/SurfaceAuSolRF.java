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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * Surface d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_SURFACE_AU_SOL")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class SurfaceAuSolRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le type de surface.
	 */
	private String type;

	/**
	 * La surface en mètre carrés (m2).
	 */
	private int surface;

	/**
	 * L'immeuble concerné par la surface.
	 */
	private ImmeubleRF immeuble;

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

	@Index(name = "IDX_SURF_SOL_RF_TYPE")
	@Column(name = "TYPE", nullable = false, length = LengthConstants.RF_TYPE_SURFACE_AU_SOL)
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Index(name = "IDX_SURF_SOL_RF_SURFACE")
	@Column(name = "SURFACE", nullable = false)
	public int getSurface() {
		return surface;
	}

	public void setSurface(int surface) {
		this.surface = surface;
	}

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_SURF_SOL_RF_IMMEUBLE_ID")
	@Index(name = "IDX_SURF_SOL_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}
}
