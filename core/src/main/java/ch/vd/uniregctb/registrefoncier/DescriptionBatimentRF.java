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
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;

/**
 * La description d'un bâtiment (valide pendant une période donnée).
 */
@Entity
@Table(name = "RF_DESCRIPTION_BATIMENT")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT")),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class DescriptionBatimentRF extends HibernateDateRangeEntity implements LinkedEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le type de batiment.
	 */
	@Nullable
	private String type;

	/**
	 * La surface en mètre carrés (m2).
	 */
	@Nullable
	private Integer surface;

	/**
	 * Le bâtiment concerné par la description.
	 */
	private BatimentRF batiment;

	public DescriptionBatimentRF() {
	}

	public DescriptionBatimentRF(@Nullable String type, @Nullable Integer surface) {
		this(type, surface, null, null);
	}

	public DescriptionBatimentRF(@Nullable String type, @Nullable Integer surface, RegDate dateDebut, RegDate dateFin) {
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
	@Column(name = "SURFACE")
	public Integer getSurface() {
		return surface;
	}

	public void setSurface(@Nullable Integer surface) {
		this.surface = surface;
	}

	// configuration hibernate : le bâtiment possède les descriptions du bâtiment
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "BATIMENT_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DESCR_BAT_RF_BATIMENT_ID", columnNames = "BATIMENT_ID")
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Collections.singletonList(batiment);
	}
}