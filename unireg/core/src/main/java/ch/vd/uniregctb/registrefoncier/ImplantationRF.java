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
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.HibernateDateRangeEntity;

/**
 * L'implantation d'un bâtiment sur une parcelle (immeuble)
 */
@Entity
@Table(name = "RF_IMPLANTATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class ImplantationRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La surface concernée en mètre carrés (m2).
	 */
	@Nullable
	private Integer surface;

	/**
	 * L'immeuble concerné par l'implantation.
	 */
	private ImmeubleRF immeuble;

	/**
	 * Le bâtiment concerné par l'implantation.
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

	@Column(name = "SURFACE")
	@Nullable
	public Integer getSurface() {
		return surface;
	}

	public void setSurface(@Nullable Integer surface) {
		this.surface = surface;
	}

	// configuration hibernate : l'immeuble ne possède pas les implantations
	@ManyToOne
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false)
	@ForeignKey(name = "FK_IMPLANTATION_RF_IMMEUBLE_ID")
	@Index(name = "IDX_IMPLANTATION_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	// configuration hibernate : le bâtiment possède les implantations
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "BATIMENT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_IMPLANTATION_RF_BATIMENT_ID", columnNames = "BATIMENT_ID")
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}
}
