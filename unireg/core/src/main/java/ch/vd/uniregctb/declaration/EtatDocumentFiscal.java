package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
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
import java.util.Comparator;
import java.util.List;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 */
@Entity
@Table(name = "ETAT_DOCUMENT_FISCAL")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class EtatDocumentFiscal<T, E extends EtatDocumentFiscal> extends HibernateEntity implements DateRange, Comparable<EtatDocumentFiscal>, LinkedEntity {


	/**
	 * The ID
	 */
	private Long id;


	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Waz4wOqfEdySTq6PFlf9jQ"
	 */
	private RegDate dateObtention;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ"
	 */
	private DocumentFiscal documentFiscal;

	public EtatDocumentFiscal() {
	}

	public EtatDocumentFiscal(RegDate dateObtention) {
		this.dateObtention = dateObtention;

	}

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
	 * @return the etat
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_TNdzAOqfEdySTq6PFlf9jQ?GETTER"
	 */
	@Transient
	public abstract T getEtat();

	@Transient
	public abstract Comparator<E> getComparator();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateObtention
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Waz4wOqfEdySTq6PFlf9jQ?GETTER"
	 */
	@Column(name = "DATE_OBTENTION")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateObtention() {
		// begin-user-code
		return dateObtention;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateObtention the dateObtention to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Waz4wOqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDateObtention(RegDate theDateObtention) {
		// begin-user-code
		dateObtention = theDateObtention;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the document
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_ET_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public DocumentFiscal getDocumentFiscal() {
		// begin-user-code
		return documentFiscal;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDocumentFiscal the declaration to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0jfjkuqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setDocumentFiscal(DocumentFiscal theDocumentFiscal) {
		// begin-user-code
		documentFiscal = theDocumentFiscal;
		// end-user-code
	}

	/**
	 * Compare d'apres la date de EtatDeclaration
	 *
	 * @see Comparable#compareTo(Object)
	 */
	@Override
	public int compareTo(@NotNull EtatDocumentFiscal other) {
		// Descending =>   * -1
		return - DateRangeComparator.compareRanges(this, other);
	}

	/**
	 * return date obtention
	 */
	@Override
	@Transient
	public RegDate getDateDebut() {
		return dateObtention;
	}

	/**
	 * return null
	 */
	@Override
	@Transient
	public RegDate getDateFin() {
		// On n'a pas de date de fin, on renvoie null
		return null;
	}

	@Override
	@Transient
	public boolean isValidAt(RegDate date) {
		return true;
	}

	@Override
	public String toString() {
		final String dateObtentionStr = dateObtention != null ? RegDateHelper.dateToDisplayString(dateObtention) : "?";
		return String.format("%s le %s", getClass().getSimpleName(), dateObtentionStr);
	}

	@Override
	@Transient
	public List<?> getLinkedEntities(@NotNull Context context, boolean includeAnnuled) {
		return documentFiscal == null ? null : Collections.singletonList(documentFiscal);
	}
}
