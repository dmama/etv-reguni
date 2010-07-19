package ch.vd.uniregctb.adresse;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
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

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.SubValidateable;
import ch.vd.registre.base.validation.Validateable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAdresseTiers;

@Entity
@Table(name = "ADRESSE_TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "ADR_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class AdresseTiers extends HibernateEntity implements Comparable<AdresseTiers>, DateRange, Serializable, Cloneable, SubValidateable {

	//private final Logger LOGGER = Logger.getLogger(AdresseTiers.class);

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 8636957205659696843L;

	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date de début de la validité de l'adresse postale du tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi96lx9Edygsbnw9h5bVw"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * Date de fin de la validité de l'adresse postale du tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi97Fx9Edygsbnw9h5bVw"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tsWlAJNdEdygKK6Oe0tVlw"
	 */
	private TypeAdresseTiers usage;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_20XgQKfTEdy6qP7Nc3dO8g"
	 */
	private Tiers tiers;

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

	public void setId(Long theId) {
		this.id = theId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi96lx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_DEBUT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi96lx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi97Fx9Edygsbnw9h5bVw?GETTER"
	 */
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_nLi97Fx9Edygsbnw9h5bVw?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the usage
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tsWlAJNdEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "USAGE_TYPE", length = LengthConstants.ADRESSE_TYPETIERS, nullable = false)
	// note : USAGE est un mot réservé sous MySql
	@Type(type = "ch.vd.uniregctb.hibernate.TypeAdresseTiersUserType")
	public TypeAdresseTiers getUsage() {
		// begin-user-code
		return usage;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theUsage the usage to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_tsWlAJNdEdygKK6Oe0tVlw?SETTER"
	 */
	public void setUsage(TypeAdresseTiers theUsage) {
		// begin-user-code
		usage = theUsage;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the tiers
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_20XgQKfTEdy6qP7Nc3dO8g?GETTER"
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "TIERS_ID", insertable = false, updatable = false, nullable = false)
	@Index(name = "IDX_ADR_TRS_ID", columnNames = "TIERS_ID")
	public Tiers getTiers() {
		// begin-user-code
		return tiers;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theTiers the tiers to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_20XgQKfTEdy6qP7Nc3dO8g?SETTER"
	 */
	public void setTiers(Tiers theTiers) {
		// begin-user-code
		tiers = theTiers;
		// end-user-code
	}

	/**
	 * Compare 2 adresses. La plus grande est celle avec la date de début la plus tard
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(AdresseTiers other) {
		return DateRangeComparator.compareRanges(this, other);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	/**
	 * {@inheritDoc}
	 */
	@Transient
	public Validateable getMaster() {
		return tiers;
	}
}
