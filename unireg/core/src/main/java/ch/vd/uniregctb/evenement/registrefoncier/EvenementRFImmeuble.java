package ch.vd.uniregctb.evenement.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Blob;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;

@Entity
@Table(name = "EVENEMENT_RF_IMMEUBLE")
public class EvenementRFImmeuble extends HibernateEntity {

	private Long id;

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
	private Blob errorMessage;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	public Long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "ETAT")
	@Enumerated(EnumType.STRING)
	public EtatEvenementRF getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementRF etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	@Column(name = "FILE_URL")
	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	@Column(name = "ERROR_MESSAGE", nullable = true)
	@Lob
	@Nullable
	public Blob getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(@Nullable Blob errorMessage) {
		this.errorMessage = errorMessage;
	}
}
