package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * L'implantation d'un bâtiment sur une parcelle (immeuble)
 */
@Entity
@Table(name = "RF_IMPLANTATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class ImplantationRF extends HibernateDateRangeEntity implements LinkedEntity, Comparable<ImplantationRF> {

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

	public ImplantationRF() {
	}

	public ImplantationRF(@Nullable Integer surface, ImmeubleRF immeuble) {
		this.surface = surface;
		this.immeuble = immeuble;
	}

	public ImplantationRF(@Nullable Integer surface, ImmeubleRF immeuble, RegDate dateDebut, RegDate dateFin) {
		super(dateDebut, dateFin);
		this.surface = surface;
		this.immeuble = immeuble;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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
	@JoinColumn(name = "IMMEUBLE_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_IMPLANTATION_RF_IMMEUBLE_ID"))
	@Index(name = "IDX_IMPLANT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
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
	@Index(name = "IDX_IMPLANT_RF_BATIMENT_ID", columnNames = "BATIMENT_ID")
	public BatimentRF getBatiment() {
		return batiment;
	}

	public void setBatiment(BatimentRF batiment) {
		this.batiment = batiment;
	}

	/**
	 * Compare l'implantation courante avec une autre implantation. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 * <li>les dates de début et de fin</li>
	 * <li>la surface</li>
	 * </ul>
	 *
	 * @param right une autre implantation.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull ImplantationRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		return ObjectUtils.compare(surface, right.surface, false);
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Arrays.asList(immeuble, batiment);
	}
}
