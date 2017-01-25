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

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * La surface totale d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_SURFACE_TOTALE")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class SurfaceTotaleRF extends HibernateDateRangeEntity implements LinkedEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La surface en mètre carrés (m2).
	 */
	private int surface;

	/**
	 * L'immeuble concerné par la situation.
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

	@Column(name = "SURFACE", nullable = false)
	public int getSurface() {
		return surface;
	}

	public void setSurface(int surface) {
		this.surface = surface;
	}

	// configuration hibernate : l'immeuble possède les surfaces totales
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "IMMEUBLE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_SURF_TOT_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
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
	 *     <li>la surface</li>
	 * </ul>
	 * @param right une autre surface.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	public int compareTo(@NotNull SurfaceTotaleRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		return Integer.compare(surface, right.surface);
	}

	@Override
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return Collections.singletonList(immeuble);
	}
}
