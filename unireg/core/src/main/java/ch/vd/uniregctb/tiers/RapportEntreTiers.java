package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Duplicable;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1qOJkJNUEdygKK6Oe0tVlw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_1qOJkJNUEdygKK6Oe0tVlw"
 */
@Entity
@Table(name = "RAPPORT_ENTRE_TIERS")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "RAPPORT_ENTRE_TIERS_TYPE", discriminatorType = DiscriminatorType.STRING)
public abstract class RapportEntreTiers extends HibernateEntity implements DateRange, Duplicable<RapportEntreTiers> {

	private static final long serialVersionUID = 956676848057330463L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uEJMMJNYEdygKK6Oe0tVlw"
	 */
	private RegDate dateDebut;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xEySsJNYEdygKK6Oe0tVlw"
	 */
	private RegDate dateFin;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcZNZEdygKK6Oe0tVlw"
	 */
	private Long sujetId;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw"
	 */
	private Long objetId;


	public RapportEntreTiers() {
		// nothing to do here
	}

	public RapportEntreTiers(RegDate dateDebut, RegDate dateFin, Tiers sujet, Tiers objet) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.sujetId = (sujet == null ? null : sujet.getId());
		this.objetId = (objet == null ? null : objet.getId());
	}

	protected RapportEntreTiers(RegDate dateDebut, RegDate dateFin, Long sujetId, Long objetId) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.sujetId = sujetId;
		this.objetId = objetId;
	}

	public RapportEntreTiers(RapportEntreTiers rapport) {
		this(rapport.getDateDebut(), rapport.getDateFin(), rapport.getSujetId(), rapport.getObjetId());
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
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the dateDebut
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uEJMMJNYEdygKK6Oe0tVlw?GETTER"
	 * TODO (GDY) ajouter nullable = false
	 */
	@Column(name = "DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		// begin-user-code
		return dateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDateDebut the dateDebut to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uEJMMJNYEdygKK6Oe0tVlw?SETTER"
	 */
	public void setDateDebut(RegDate theDateDebut) {
		// begin-user-code
		dateDebut = theDateDebut;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the dateFin
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xEySsJNYEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		// begin-user-code
		return dateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDateFin the dateFin to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xEySsJNYEdygKK6Oe0tVlw?SETTER"
	 */
	public void setDateFin(RegDate theDateFin) {
		// begin-user-code
		dateFin = theDateFin;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the sujet
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcZNZEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "TIERS_SUJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_SUJ_ID", columnNames = "TIERS_SUJET_ID")
	@ForeignKey(name = "FK_RET_TRS_SUJ_ID")
	public Long getSujetId() {
		// begin-user-code
		return sujetId;
		// end-user-code
	}

	public void setSujetId(Long sujetId) {
		this.sujetId = sujetId;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param sujet the sujet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcZNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setSujet(Tiers sujet) {
		// begin-user-code
		this.sujetId = (sujet == null ? null : sujet.getId());
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the objet
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw?GETTER"
	 */
	@Column(name = "TIERS_OBJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_OBJ_ID", columnNames = "TIERS_OBJET_ID")
	@ForeignKey(name = "FK_RET_TRS_OBJ_ID")
	public Long getObjetId() {
		// begin-user-code
		return objetId;
		// end-user-code
	}

	public void setObjetId(Long objetId) {
		this.objetId = objetId;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param objet the objet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setObjet(Tiers objet) {
		// begin-user-code
		this.objetId = (objet == null ? null : objet.getId());
		// end-user-code
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public boolean equalsTo(RapportEntreTiers rapportEntreTiers) {
		EqualsBuilder equalsBuilder = new EqualsBuilder();
		equalsBuilder.append(this.sujetId, rapportEntreTiers.sujetId);
		equalsBuilder.append(this.getDateDebut(), rapportEntreTiers.getDateDebut());
		equalsBuilder.append(this.getDateFin(), rapportEntreTiers.getDateFin());
		equalsBuilder.append(this.objetId, rapportEntreTiers.objetId);
		equalsBuilder.append(this.getType(), rapportEntreTiers.getType());
		equalsBuilder.append(this.isAnnule(), rapportEntreTiers.isAnnule());
		return equalsBuilder.isEquals();
	}

	@Transient
	public abstract TypeRapportEntreTiers getType();
}
