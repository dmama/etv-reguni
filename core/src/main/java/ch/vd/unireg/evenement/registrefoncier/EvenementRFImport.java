package ch.vd.unireg.evenement.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;

/**
 * Un événement d'import d'une extraction hebdomadaire du registre foncier.
 */
@Entity
@Table(name = "EVENEMENT_RF_IMPORT", indexes = {
		@Index(name="IDX_EV_RF_IMP_TYPE", columnList = "TYPE"),
		@Index(name="IDX_EV_RF_IMP_ETAT", columnList = "ETAT")
})
public class EvenementRFImport extends HibernateEntity {

	private Long id;

	/**
	 * Le type d'import concerné.
	 */
	private TypeImportRF type;

	/**
	 * L'état courant de l'événement.
	 */
	private EtatEvenementRF etat;

	/**
	 * La date de valeur du fichier.
	 */
	private RegDate dateEvenement;

	/**
	 * L'URL Raft du fichier qui contient les données à traiter.
	 */
	private String fileUrl;

	/**
	 * Un message d'erreur en cas d'erreur de traitement.
	 */
	@Nullable
	private String errorMessage;

	/**
	 * La callstack complète en cas d'erreur de traitement.
	 */
	@Nullable
	private String callstack;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(generator = "defaultGenerator")
	@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "TYPE", length = LengthConstants.RF_TYPE_IMPORT, nullable = false)
	@Enumerated(EnumType.STRING)
	public TypeImportRF getType() {
		return type;
	}

	public void setType(TypeImportRF type) {
		this.type = type;
	}

	@Column(name = "ETAT", length = LengthConstants.RF_ETAT_EVENEMENT, nullable = false)
	@Enumerated(EnumType.STRING)
	public EtatEvenementRF getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementRF etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	@Column(name = "FILE_URL", length = LengthConstants.RF_FILE_URL)
	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	@Column(name = "ERROR_MESSAGE", length = LengthConstants.RF_ERROR_MESSAGE)
	@Nullable
	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(@Nullable String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Column(name = "CALLSTACK")
	@Type(type = "ch.vd.unireg.hibernate.StringAsClobUserType")
	@Nullable
	public String getCallstack() {
		return callstack;
	}

	public void setCallstack(@Nullable String callstack) {
		this.callstack = callstack;
	}
}
