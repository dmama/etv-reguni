package ch.vd.uniregctb.documentfiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;

@Entity
@Table(name = "AUTRE_DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DOC_TYPE", discriminatorType = DiscriminatorType.STRING, length = LengthConstants.AUTRE_DOCUMENT_FISCAL_TYPE)
public abstract class AutreDocumentFiscal extends HibernateEntity implements LinkedEntity {

	private Long id;
	private RegDate dateEnvoi;
	private String cleArchivage;
	private String cleDocument;
	private Entreprise entreprise;

	@Transient
	@Override
	public Long getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "DATE_ENVOI", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEnvoi() {
		return dateEnvoi;
	}

	public void setDateEnvoi(RegDate dateEnvoi) {
		this.dateEnvoi = dateEnvoi;
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

	@ManyToOne
	@JoinColumn(name = "ENTREPRISE_ID", nullable = false)
	public Entreprise getEntreprise() {
		return entreprise;
	}

	public void setEntreprise(Entreprise entreprise) {
		this.entreprise = entreprise;
	}

	@Transient
	@Override
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return entreprise == null ? null : Collections.singletonList(entreprise);
	}

	@Transient
	public TypeEtatAutreDocumentFiscal getEtat() {
		return TypeEtatAutreDocumentFiscal.EMIS;
	}

	@Transient
	public Integer getPeriodeFiscale() {
		return Optional.ofNullable(dateEnvoi)
				.map(RegDate::year)
				.orElse(null);
	}
}
