package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
	private Tiers sujet;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw"
	 */
	private Tiers objet;


	public RapportEntreTiers() {
		// nothing to do here
	}

	public RapportEntreTiers(RegDate dateDebut, RegDate dateFin, Tiers sujet, Tiers objet) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.sujet = sujet;
		this.objet = objet;
	}

	public RapportEntreTiers(RapportEntreTiers rapport) {
		this(rapport.getDateDebut(), rapport.getDateFin(), rapport.getSujet(), rapport.getObjet());
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
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "TIERS_SUJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_SUJ_ID", columnNames = "TIERS_SUJET_ID")
	@ForeignKey(name = "FK_RET_TRS_SUJ_ID")
	public Tiers getSujet() {
		// begin-user-code
		return sujet;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theSujet the sujet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_BeZlcZNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setSujet(Tiers theSujet) {
		// begin-user-code
		sujet = theSujet;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the objet
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw?GETTER"
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "TIERS_OBJET_ID", nullable = false)
	@Index(name = "IDX_RET_TRS_OBJ_ID", columnNames = "TIERS_OBJET_ID")
	@ForeignKey(name = "FK_RET_TRS_OBJ_ID")
	public Tiers getObjet() {
		// begin-user-code
		return objet;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theObjet the objet to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_ON1FcpNZEdygKK6Oe0tVlw?SETTER"
	 */
	public void setObjet(Tiers theObjet) {
		// begin-user-code
		objet = theObjet;
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
		equalsBuilder.append(this.getSujet(), rapportEntreTiers.getSujet());
		equalsBuilder.append(this.getDateDebut(), rapportEntreTiers.getDateDebut());
		equalsBuilder.append(this.getDateFin(), rapportEntreTiers.getDateFin());
		equalsBuilder.append(this.getObjet(), rapportEntreTiers.getObjet());
		equalsBuilder.append(this.getType(), rapportEntreTiers.getType());
		equalsBuilder.append(this.isAnnule(), rapportEntreTiers.isAnnule());
		boolean status = equalsBuilder.isEquals();
		return status;
	}

	@Transient
	public abstract TypeRapportEntreTiers getType();
}
