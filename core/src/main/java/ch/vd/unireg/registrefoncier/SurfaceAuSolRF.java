package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * Surface d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_SURFACE_AU_SOL")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class SurfaceAuSolRF extends HibernateDateRangeEntity implements LinkedEntity, Comparable<SurfaceAuSolRF> {

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
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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

	// configuration hibernate : l'immeuble ne possède pas les surfaces au sol
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

	/**
	 * Compare la surface courante avec une autre surface. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 *     <li>les dates de début et de fin</li>
	 *     <li>le type de surface</li>
	 *     <li>la surface</li>
	 * </ul>
	 * @param right une autre surface.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull SurfaceAuSolRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		c = type.compareTo(right.type);
		if (c != 0) {
			return c;
		}
		return Integer.compare(surface, right.surface);
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Collections.singletonList(immeuble);
	}
}
