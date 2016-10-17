package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;

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
	private long montant;

	/**
	 * Numéro de référence.
	 */
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
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "MONTANT", nullable = false)
	public long getMontant() {
		return montant;
	}

	public void setMontant(long montant) {
		this.montant = montant;
	}

	@Column(name = "REFERENCE", nullable = false)
	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	@Column(name = "DATE_ESTIMATION", nullable = false)
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
}
