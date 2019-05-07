package ch.vd.unireg.mandataire;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.HibernateEntity;

/**
 * Données d'une demande faite par un mandataire (actuellement contient les demandes de délais groupés faites de l'application e-Délai, mais c'est extensible à d'autres types de demandes/applications dans le futur).
 */
@Entity
@Table(name = "DEMANDE_MANDATAIRE")
public class DemandeDelaisMandataire extends HibernateEntity {

	/**
	 * Id technique propre à Unireg.
	 */
	private Long id;

	/**
	 * Le numéro de contribuable tel que saisi par le mandataire (sans assurance de validité).
	 */
	@Nullable
	private Long numeroCtbMandataire;

	/**
	 * Le numéro IDE du mandataire (sans assurance de validité).
	 */
	private String numeroIDE;

	/**
	 * La raison sociale du mandataire (sans assurance de validité).
	 */
	private String raisonSociale;

	/**
	 * Le business ID de la demande.
	 */
	private String businessId;

	/**
	 * Un ID de référence supplémentaire.
	 */
	@Nullable
	private String referenceId;

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

	@Column(name = "NUMERO_CTB_MANDATAIRE")
	@Nullable
	public Long getNumeroCtbMandataire() {
		return numeroCtbMandataire;
	}

	public void setNumeroCtbMandataire(@Nullable Long numeroCtbMandataire) {
		this.numeroCtbMandataire = numeroCtbMandataire;
	}

	@Column(name = "NUMERO_IDE_MANDATAIRE", nullable = false)
	public String getNumeroIDE() {
		return numeroIDE;
	}

	public void setNumeroIDE(String numeroIDE) {
		this.numeroIDE = numeroIDE;
	}

	@Column(name = "RAISON_SOCIALE_MANDATAIRE", nullable = false)
	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	@Column(name = "BUSINESS_ID", nullable = false)
	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	@Nullable
	@Column(name = "REFERENCE_ID")
	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(@Nullable String referenceId) {
		this.referenceId = referenceId;
	}
}
