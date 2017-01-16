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
import java.util.Objects;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;

/**
 * Estimation fiscale d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_ESTIMATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = false)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN"))
})
public class EstimationRF extends HibernateDateRangeEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le montant en francs de l'estimation fiscale.
	 */
	@Nullable
	private Long montant;

	/**
	 * Numéro de référence.
	 */
	@Nullable
	private String reference;

	/**
	 * La date de calcul de l'estimation fiscale.
	 */
	private RegDate dateEstimation;

	/**
	 * Vrai si l'estimation fiscale est en cours de révision.
	 */
	private boolean enRevision;

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

	@Nullable
	@Column(name = "MONTANT")
	public Long getMontant() {
		return montant;
	}

	public void setMontant(@Nullable Long montant) {
		this.montant = montant;
	}

	@Nullable
	@Column(name = "REFERENCE", length = LengthConstants.RF_REFERENCE_ESTIMATION)
	public String getReference() {
		return reference;
	}

	public void setReference(@Nullable String reference) {
		this.reference = reference;
	}

	@Column(name = "DATE_ESTIMATION")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEstimation() {
		return dateEstimation;
	}

	public void setDateEstimation(RegDate dateEstimation) {
		this.dateEstimation = dateEstimation;
	}

	@Column(name = "EN_REVISION", nullable = false)
	public boolean isEnRevision() {
		return enRevision;
	}

	public void setEnRevision(boolean enRevision) {
		this.enRevision = enRevision;
	}

	// configuration hibernate : l'immeuble possède les estimations fiscales
	@ManyToOne(cascade = {
			CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH
	})
	@JoinColumn(name = "IMMEUBLE_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ESTIM_RF_IMMEUBLE_ID", columnNames = "IMMEUBLE_ID")
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}

	/**
	 * Compare l'estimation courante avec une autre estimation. Les propriétés utilisées pour la comparaison sont :
	 * <ul>
	 *     <li>les dates de début et de fin</li>
	 *     <li>la date d'estimation</li>
	 *     <li>le numéro de référence</li>
	 *     <li>le montant</li>
	 * </ul>
	 * @param right une autre estimation.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	public int compareTo(@NotNull EstimationRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		c = Objects.compare(dateEstimation, right.dateEstimation, RegDate::compareTo);
		if (c != 0) {
			return c;
		}
		c = Objects.compare(reference, right.reference, String::compareTo);
		if (c != 0) {
			return c;
		}
		return Objects.compare(montant, right.montant, Long::compareTo);
	}
}
