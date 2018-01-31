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

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.linkedentity.LinkedEntity;
import ch.vd.uniregctb.common.linkedentity.LinkedEntityContext;

/**
 * Estimation fiscale d'un immeuble inscrit au registre foncier.
 */
@Entity
@Table(name = "RF_ESTIMATION")
@AttributeOverrides({
		@AttributeOverride(name = "dateDebut", column = @Column(name = "DATE_DEBUT", nullable = true)),
		@AttributeOverride(name = "dateFin", column = @Column(name = "DATE_FIN", nullable = true))
})
public class EstimationRF extends HibernateDateRangeEntity implements LinkedEntity, Comparable<EstimationRF> {

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
	 * L'année de référence déduite du numéro de référence.
	 * <p/>
	 * <b>Note:</b> cette date ne fait pas partie des données reçues du registre foncier, elle est calculée par Unireg.
	 * Elle peut être nulle s'il n'a pas été possible de déduire une valeur.
	 */
	@Nullable
	private Integer anneeReference;

	/**
	 * La date d'inscription dans le registre foncier de l'estimation fiscale.
	 */
	private RegDate dateInscription;

	/**
	 * Le date de début de validité <i>métier</i>. Cette date est déduite du numéro de référence ou de la date d'inscription, en fonction des données renseignées.
	 * <p/>
	 * <b>Note:</b> cette date ne fait pas partie des données reçues du registre foncier, elle est calculée par Unireg.
	 * Elle peut être nulle s'il n'a pas été possible de déduire une valeur.
	 */
	@Nullable
	private RegDate dateDebutMetier;

	/**
	 * Le date de fin de validité <i>métier</i>. Cette date est déduite de la date de début de validité de l'estimation suivante.
	 * <p/>
	 * <b>Note:</b> cette date ne fait pas partie des données reçues du registre foncier, elle est calculée par Unireg.
	 * Elle peut être nulle s'il n'a pas été possible de déduire une valeur.
	 */
	@Nullable
	private RegDate dateFinMetier;

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

	@Nullable
	@Column(name = "ANNEE_REFERENCE")
	public Integer getAnneeReference() {
		return anneeReference;
	}

	public void setAnneeReference(@Nullable Integer anneeReference) {
		this.anneeReference = anneeReference;
	}

	@Column(name = "DATE_INSCRIPTION")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateInscription() {
		return dateInscription;
	}

	public void setDateInscription(RegDate dateInscription) {
		this.dateInscription = dateInscription;
	}

	@Nullable
	@Column(name = "DATE_DEBUT_METIER")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebutMetier() {
		return dateDebutMetier;
	}

	public void setDateDebutMetier(@Nullable RegDate dateDebutMetier) {
		this.dateDebutMetier = dateDebutMetier;
	}

	@Nullable
	@Column(name = "DATE_FIN_METIER")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFinMetier() {
		return dateFinMetier;
	}

	public void setDateFinMetier(@Nullable RegDate dateFinMetier) {
		this.dateFinMetier = dateFinMetier;
	}

	@Transient
	public DateRange getRangeMetier() {
		return new DateRangeHelper.Range(dateDebutMetier, dateFinMetier);
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
	 *     <li>la date d'inscription</li>
	 *     <li>le numéro de référence</li>
	 *     <li>le montant</li>
	 * </ul>
	 *
	 * @param right une autre estimation.
	 * @return le résultat de la comparaison selon {@link Comparable#compareTo(Object)}.
	 */
	@Override
	public int compareTo(@NotNull EstimationRF right) {
		int c = DateRangeComparator.compareRanges(this, right);
		if (c != 0) {
			return c;
		}
		c = ObjectUtils.compare(dateInscription, right.dateInscription, false);
		if (c != 0) {
			return c;
		}
		c = ObjectUtils.compare(reference, right.reference, false);
		if (c != 0) {
			return c;
		}
		return ObjectUtils.compare(montant, right.montant, false);
	}

	@Override
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return Collections.singletonList(immeuble);
	}
}
