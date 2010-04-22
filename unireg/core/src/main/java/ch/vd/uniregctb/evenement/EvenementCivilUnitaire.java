package ch.vd.uniregctb.evenement;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * <HTML><HEAD>
 * <META content="MSHTML 6.00.2900.3199" name=GENERATOR></HEAD>
 * <BODY>
 * <P>Modélise un événement civil pour un individu. Il fera l'objet d'un regroupement pour recomposer l'événement civil regroupé avec tous les individus concernés par l'événement.<BR>(ex.: le déménagement d'une famille de 4 personnes sera composé de 4 événements unitaires et au final 1 seul événement regroupé.)</P></BODY></HTML>
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xLk6cGamEdy266g-CTSpFw"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_xLk6cGamEdy266g-CTSpFw"
 */
@Entity
@Table(name = "EVENEMENT_CIVIL_UNITAIRE")
public class EvenementCivilUnitaire extends HibernateEntity {

	/**
	 *
	 */
	private static final long serialVersionUID = -263463155542462309L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-dCJcGamEdy266g-CTSpFw"
	 */
	private RegDate dateEvenement;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uZILgGanEdy266g-CTSpFw"
	 */
	private Integer numeroOfsCommuneAnnonce;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Na31MNSZEdyRNpOhiSbYUw"
	 */
	private EtatEvenementCivil etat;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_soedkMK6EdydR6r71NY4Vg"
	 */
	private Long numeroIndividu;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_GWSIMGaoEdy266g-CTSpFw"
	 */
	private TypeEvenementCivil type;

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	//@GeneratedValue(strategy = GenerationType.AUTO)
	public Long getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theId the id to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_adc3kMK6EdydR6r71NY4Vg?SETTER"
	 */
	public void setId(Long theId) {
		// begin-user-code
		id = theId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the dateEvenement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-dCJcGamEdy266g-CTSpFw?GETTER"
	 */
	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		// begin-user-code
		return dateEvenement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theDateEvenement the dateEvenement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_-dCJcGamEdy266g-CTSpFw?SETTER"
	 */
	public void setDateEvenement(RegDate theDateEvenement) {
		// begin-user-code
		dateEvenement = theDateEvenement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroOfsCommuneAnnonce
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uZILgGanEdy266g-CTSpFw?GETTER"
	 */
	@Column(name = "NUMERO_OFS_ANNONCE")
	public Integer getNumeroOfsCommuneAnnonce() {
		// begin-user-code
		return numeroOfsCommuneAnnonce;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroOfsCommuneAnnonce the numeroOfsCommuneAnnonce to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_uZILgGanEdy266g-CTSpFw?SETTER"
	 */
	public void setNumeroOfsCommuneAnnonce(Integer theNumeroOfsCommuneAnnonce) {
		// begin-user-code
		numeroOfsCommuneAnnonce = theNumeroOfsCommuneAnnonce;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the etat
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Na31MNSZEdyRNpOhiSbYUw?GETTER"
	 */
	@Column(name = "ETAT", length = LengthConstants.EVTCIVILUNIT_ETAT)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatEvenementCivilUserType")
	public EtatEvenementCivil getEtat() {
		// begin-user-code
		return etat;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theEtat the etat to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Na31MNSZEdyRNpOhiSbYUw?SETTER"
	 */
	public void setEtat(EtatEvenementCivil theEtat) {
		// begin-user-code
		etat = theEtat;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the numeroIndividu
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_soedkMK6EdydR6r71NY4Vg?GETTER"
	 */
	@Column(name = "NUMERO_INDIVIDU")
	public Long getNumeroIndividu() {
		// begin-user-code
		return numeroIndividu;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theNumeroIndividu the numeroIndividu to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_soedkMK6EdydR6r71NY4Vg?SETTER"
	 */
	public void setNumeroIndividu(Long theNumeroIndividu) {
		// begin-user-code
		numeroIndividu = theNumeroIndividu;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_GWSIMGaoEdy266g-CTSpFw?GETTER"
	 */
	@Column(name = "TYPE", length = LengthConstants.EVTCIVILUNIT_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementCivilUserType")
	public TypeEvenementCivil getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theType the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_GWSIMGaoEdy266g-CTSpFw?SETTER"
	 */
	public void setType(TypeEvenementCivil theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}

}