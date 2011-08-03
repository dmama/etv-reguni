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
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

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
@Table(name = "ETAT_DECLARATION")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class EtatDeclaration extends HibernateEntity implements DateRange, Comparable<EtatDeclaration>, LinkedEntity {

	/**
	 * Permet de trier les états d'une déclaration du plus ancien au plus récent. En cas de plusieurs états tombant le même jour, des règles
	 * de métier permettent de les départager.
	 *
	 * @author Manuel Siggen <manuel.siggen@vd.ch>
	 */
	public static class Comparator implements java.util.Comparator<EtatDeclaration> {

		@Override
		public int compare(EtatDeclaration o1, EtatDeclaration o2) {

			final RegDate dateObtention1 = o1.getDateObtention();
			final RegDate dateObtention2 = o2.getDateObtention();
			Assert.notNull(dateObtention1);
			Assert.notNull(dateObtention2);

			if (dateObtention1 != dateObtention2) {
				// cas normal
				return dateObtention1.compareTo(dateObtention2);
			}

			// cas exceptionnel : deux états obtenu le même jour.
			final TypeEtatDeclaration etat1 = o1.getEtat();
			final TypeEtatDeclaration etat2 = o2.getEtat();
			Assert.notNull(etat1);
			Assert.notNull(etat2);

			// l'ordre est simplement l'ordre logique de l'enum (EMISE -> SOMMEE -> ECHUE -> RETOURNEE)
			if (etat1 != etat2) {
				return etat1.compareTo(etat2);
			}

			// s'il y a des états identiques annulés aux mêmes dates, on mets l'état annulé avant
			final boolean e1annule = o1.isAnnule();
			final boolean e2annule = o2.isAnnule();
			return e1annule == e2annule ? 0 : (e1annule ? -1 : 1);
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -7856018681261138158L;

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
	private Declaration declaration;

	public EtatDeclaration() {
	}

	public EtatDeclaration(RegDate dateObtention) {
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
//	@Column(name = "TYPE", length = LengthConstants.DI_ETAT)
//	@Type(type = "ch.vd.uniregctb.hibernate.TypeEtatDeclarationUserType")
	@Transient
	public abstract  TypeEtatDeclaration getEtat();



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
	@JoinColumn(name = "DECLARATION_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ET_DI_DI_ID", columnNames = "DECLARATION_ID")
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

	/**
	 * Compare d'apres la date de EtatDeclaration
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(EtatDeclaration other) {
		// Descending =>   * -1
		int value = (-1) * DateRangeComparator.compareRanges(this, other);
		return value;
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
	@Transient
	public List<?> getLinkedEntities(boolean includeAnnuled) {
		return declaration == null ? null : Arrays.asList(declaration);
	}

	@Override
	public String toString() {
		final String dateObtentionStr = dateObtention != null ? RegDateHelper.dateToDisplayString(dateObtention) : "?";
		return String.format("%s le %s", getClass().getSimpleName(), dateObtentionStr);
	}
}
