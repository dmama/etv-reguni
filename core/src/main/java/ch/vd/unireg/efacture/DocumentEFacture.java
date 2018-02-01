package ch.vd.unireg.efacture;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.tiers.Tiers;

/**
 * Entité qui sert à persister en base la correspondance entre une clé d'archivage FOLDERS et
 * une clé de visualisation externe (= dans RepElec) d'un document e-Facture envoyé par Unireg
 * dans le contexte d'un contribuable
 */
@Entity
@Table(name = "DOCUMENT_EFACTURE")
public class DocumentEFacture extends HibernateEntity {

	private Long id;
	private Tiers tiers;
	private String cleArchivage;
	private String cleDocument;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false, updatable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn(name = "TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_DOC_EFACTURE_TIERS_ID")
	public Tiers getTiers() {
		return tiers;
	}

	public void setTiers(Tiers tiers) {
		this.tiers = tiers;
	}

	@Column(name = "CLE_ARCHIVAGE", length = LengthConstants.CLE_ARCHIVAGE_FOLDERS)
	public String getCleArchivage() {
		return cleArchivage;
	}

	public void setCleArchivage(String cleArchivage) {
		this.cleArchivage = cleArchivage;
	}

	@Column(name = "CLE_DOCUMENT", length = LengthConstants.CLE_DOCUMENT_DPERM)
	public String getCleDocument() {
		return cleDocument;
	}

	public void setCleDocument(String cleDocument) {
		this.cleDocument = cleDocument;
	}
}
