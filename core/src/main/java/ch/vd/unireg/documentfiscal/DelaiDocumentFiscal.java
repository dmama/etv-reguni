package ch.vd.unireg.documentfiscal;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.linkedentity.LinkedEntity;
import ch.vd.unireg.common.linkedentity.LinkedEntityContext;
import ch.vd.unireg.mandataire.DemandeDelaisMandataire;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;

@Entity
@Table(name = "DELAI_DOCUMENT_FISCAL", indexes = {
		@Index(name = "IDX_DEL_DOCFISC_DOCFISC_ID", columnList = "DOCUMENT_FISCAL_ID"),
		@Index(name = "IDX_DELAI_DEM_MAND_ID", columnList = "DEMANDE_MANDATAIRE_ID")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "DISCRIMINATOR", discriminatorType = DiscriminatorType.STRING)
public abstract class DelaiDocumentFiscal extends HibernateEntity implements Comparable<DelaiDocumentFiscal>, LinkedEntity {

	private Long id;
	private RegDate dateDemande;
	private RegDate dateTraitement;
	private RegDate delaiAccordeAu;
	private EtatDelaiDocumentFiscal etat;
	private String cleArchivageCourrier;
	private String cleDocument;
	private DocumentFiscal documentFiscal;
	private boolean sursis;
	@Nullable
	private DemandeDelaisMandataire demandeMandataire;

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

	public void setId(Long theId) {
		this.id = theId;
	}

	@Column(name = "DATE_DEMANDE")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(RegDate theDateDemande) {
		dateDemande = theDateDemande;
	}

	@Column(name = "DATE_TRAITEMENT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(RegDate theDateTraitement) {
		dateTraitement = theDateTraitement;
	}

	@Column(name = "DELAI_ACCORDE_AU")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDelaiAccordeAu() {
		return delaiAccordeAu;
	}

	public void setDelaiAccordeAu(RegDate theDelaiAccordeAu) {
		delaiAccordeAu = theDelaiAccordeAu;
	}

	@Column(name = "ETAT", length = LengthConstants.DELAI_DECL_ETAT, nullable = false)
	@Enumerated(value = EnumType.STRING)
	public EtatDelaiDocumentFiscal getEtat() {
		return etat;
	}

	public void setEtat(EtatDelaiDocumentFiscal etat) {
		this.etat = etat;
	}

	@Column(name = "CLE_ARCHIVAGE_COURRIER", length = LengthConstants.CLE_ARCHIVAGE_FOLDERS)
	public String getCleArchivageCourrier() {
		return cleArchivageCourrier;
	}

	public void setCleArchivageCourrier(String cleArchivageCourrier) {
		this.cleArchivageCourrier = cleArchivageCourrier;
	}

	@Column(name = "CLE_DOCUMENT", length = LengthConstants.CLE_DOCUMENT_DPERM)
	public String getCleDocument() {
		return cleDocument;
	}

	public void setCleDocument(String cleDocument) {
		this.cleDocument = cleDocument;
	}

	@Column(name = "SURSIS", nullable = false)
	public boolean isSursis() {
		return sursis;
	}

	public void setSursis(boolean sursis) {
		this.sursis = sursis;
	}

	/**
	 * Compare d'apres la date de DelaiDocumentFiscal
	 *
	 * @see Comparable#compareTo(Object)
	 */
	public int compareTo(@NotNull DelaiDocumentFiscal delaiDocumentFiscal) {
		final RegDate autreDelaiAccordeAu = delaiDocumentFiscal.getDelaiAccordeAu();
		return -delaiAccordeAu.compareTo(autreDelaiAccordeAu);
	}

	// configuration hibernate : le délai est possédé par le document fiscal
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	public DocumentFiscal getDocumentFiscal() {
		return documentFiscal;
	}

	public void setDocumentFiscal(DocumentFiscal theDocumentFiscal) {
		documentFiscal = theDocumentFiscal;
	}

	// configuration hibernate : le délai ne possède pas les demandes mandataires
	@Nullable
	@ManyToOne(fetch = FetchType.LAZY)  // fetch lazy pour ne pas pénaliser les performances de chargement des déclarations (WS, entre autres)
	@JoinColumn(name = "DEMANDE_MANDATAIRE_ID", foreignKey = @ForeignKey(name = "FK_DELAI_DEM_MAND_ID"))
	public DemandeDelaisMandataire getDemandeMandataire() {
		return demandeMandataire;
	}

	public void setDemandeMandataire(@Nullable DemandeDelaisMandataire demandeMandataire) {
		this.demandeMandataire = demandeMandataire;
	}

	@Transient
	public List<?> getLinkedEntities(@NotNull LinkedEntityContext context, boolean includeAnnuled) {
		return getDocumentFiscal() == null ? null : Collections.singletonList(getDocumentFiscal());
	}

	@Transient
	public boolean isDelaiAccorde() {
		return etat == EtatDelaiDocumentFiscal.ACCORDE;
	}

	@Transient
	public boolean isRufusDelai() {
		return etat == EtatDelaiDocumentFiscal.REFUSE;
	}

	/**
	 * Comparateur qui trie les délais de déclaration du plus ancien au plus récent.
	 */
	public static class Comparator implements java.util.Comparator<DelaiDocumentFiscal> {
		@Override
		public int compare(DelaiDocumentFiscal o1, DelaiDocumentFiscal o2) {

			final RegDate date1;
			final RegDate date2;
			if (o1.getDelaiAccordeAu() == null || o2.getDelaiAccordeAu() == null) {
				// si l'un des deux éléments est sans délai accordé (= délai en cours ou même refusé),
				// on compare sur les dates de demande
				date1 = o1.getDateDemande();
				date2 = o2.getDateDemande();
			}
			else {
				date1 = o1.getDelaiAccordeAu();
				date2 = o2.getDelaiAccordeAu();
			}

			if (date1 == date2) {
				// s'il y a des délais identiques annulés aux mêmes dates, on mets l'état annulé avant
				return o1.isAnnule() == o2.isAnnule() ? 0 : (o1.isAnnule() ? -1 : 1);
			}

			// de la plus petite à la plus grande
			return NullDateBehavior.EARLIEST.compare(date1, date2);
		}
	}
}
