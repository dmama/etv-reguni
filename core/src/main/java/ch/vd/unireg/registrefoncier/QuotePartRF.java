package ch.vd.unireg.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Index;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateDateRangeEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;

/**
 * La quote-part d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_QUOTE_PART")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class QuotePartRF extends HibernateDateRangeEntity implements LinkedEntity, Comparable<QuotePartRF> {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * La quote-part de l'immeuble
	 */
	private Fraction quotePart;

	/**
	 * L'immeuble concerné par la situation.
	 */
	private ImmeubleAvecQuotePartRF immeuble;

	public QuotePartRF() {
	}

	public QuotePartRF(RegDate dateDebut, RegDate dateFin, Fraction quotePart) {
		super(dateDebut, dateFin);
		this.quotePart = quotePart;
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

	@Embedded
	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "QUOTE_PART_NUM", nullable = false)),
			@AttributeOverride(name = "denominateur", column = @Column(name = "QUOTE_PART_DENOM", nullable = false))
	})
	public Fraction getQuotePart() {
		return quotePart;
	}

	public void setQuotePart(@Nullable Fraction quotePart) {
		this.quotePart = quotePart;
	}

	// configuration hibernate : l'immeuble possède les quotes-parts
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "IMMEUBLE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_QUOTE_PART_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleAvecQuotePartRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleAvecQuotePartRF immeuble) {
		this.immeuble = immeuble;
	}

	/**
	 * Compare la quote-part courante avec une autre quote-part. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 *     <li>les dates de début et de fin</li>
	 *     <li>la quote-part</li>
	 * </ul>
	 * @param right une autre surface.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull QuotePartRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		return Objects.compare(quotePart, right.quotePart, Comparator.naturalOrder());
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Collections.singletonList(immeuble);
	}
}
