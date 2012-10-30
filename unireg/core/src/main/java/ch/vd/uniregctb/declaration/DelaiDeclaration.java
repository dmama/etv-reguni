package ch.vd.uniregctb.declaration;

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
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_x3cpgOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_x3cpgOqeEdySTq6PFlf9jQ"
 */
@Entity
@Table(name = "DELAI_DECLARATION")
public class DelaiDeclaration extends HibernateEntity implements Comparable<DelaiDeclaration>, LinkedEntity {

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mKbzUOqfEdySTq6PFlf9jQ"
	 */
	private RegDate dateDemande;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kmUacOw6EdyIfo4XneeLjQ"
	 */
	private RegDate dateTraitement;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vyAxkOqfEdySTq6PFlf9jQ"
	 */
	private RegDate delaiAccordeAu;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jMDF4Oq7EdyF2-X-02dnFQ"
	 */
	private Boolean confirmationEcrite;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(Long theId) {
		this.id = theId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDemande
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mKbzUOqfEdySTq6PFlf9jQ?GETTER"
	 */
	@Column(name = "DATE_DEMANDE")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDemande() {
		// begin-user-code
		return dateDemande;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDemande the dateDemande to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_mKbzUOqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateDemande(RegDate theDateDemande) {
		// begin-user-code
		dateDemande = theDateDemande;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateTraitement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kmUacOw6EdyIfo4XneeLjQ?GETTER"
	 */
	@Column(name = "DATE_TRAITEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateTraitement() {
		// begin-user-code
		return dateTraitement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateTraitement the dateTraitement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_kmUacOw6EdyIfo4XneeLjQ?SETTER"
	 */
	public void setDateTraitement(RegDate theDateTraitement) {
		// begin-user-code
		dateTraitement = theDateTraitement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the delaiAccordeAu
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vyAxkOqfEdySTq6PFlf9jQ?GETTER"
	 */
	@Column(name = "DELAI_ACCORDE_AU")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiAccordeAu() {
		// begin-user-code
		return delaiAccordeAu;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDelaiAccordeAu the delaiAccordeAu to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_vyAxkOqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDelaiAccordeAu(RegDate theDelaiAccordeAu) {
		// begin-user-code
		delaiAccordeAu = theDelaiAccordeAu;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the confirmationEcrite
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jMDF4Oq7EdyF2-X-02dnFQ?GETTER"
	 */
	@Column(name = "CONFIRMATION_ECRITE")
	public Boolean getConfirmationEcrite() {
		// begin-user-code
		return confirmationEcrite;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theConfirmationEcrite the confirmationEcrite to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_jMDF4Oq7EdyF2-X-02dnFQ?SETTER"
	 */
	public void setConfirmationEcrite(Boolean theConfirmationEcrite) {
		// begin-user-code
		confirmationEcrite = theConfirmationEcrite;
		// end-user-code
	}

	/**
	 * Compare d'apres la date de DelaiDeclaration
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DelaiDeclaration delaiDeclaration) {
		RegDate autreDelaiAccordeAu = delaiDeclaration.getDelaiAccordeAu();
		int value = (-1) * delaiAccordeAu.compareTo(autreDelaiAccordeAu);
		return value;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ"
	 */
	private Declaration declaration;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the document
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DECLARATION_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_DE_DI_DI_ID", columnNames = "DECLARATION_ID")
	public Declaration getDeclaration() {
		// begin-user-code
		return declaration;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDocument the document to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDeclaration(Declaration theDeclaration) {
		// begin-user-code
		declaration = theDeclaration;
		// end-user-code
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return declaration == null ? null : Arrays.asList(declaration);
	}

	/**
	 * Comparateur qui trie les délais de déclaration du plus ancien au plus récent.
	 */
	public static class Comparator implements java.util.Comparator<DelaiDeclaration> {
		@Override
		public int compare(DelaiDeclaration o1, DelaiDeclaration o2) {

			if (o1.getDelaiAccordeAu() == o2.getDelaiAccordeAu()) {
				// s'il y a des délais identiques annulés aux mêmes dates, on mets l'état annulé avant
				return o1.isAnnule() == o2.isAnnule() ? 0 : (o1.isAnnule() ? -1 : 1);
			}

			// de la plus petite à la plus grande
			return o1.getDelaiAccordeAu().compareTo(o2.getDelaiAccordeAu());
		}
	}
}
