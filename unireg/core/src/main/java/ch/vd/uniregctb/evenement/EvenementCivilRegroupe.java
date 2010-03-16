package ch.vd.uniregctb.evenement;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementErreur;

/**
 * <!-- begin-user-doc --> <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_53AUYMK6EdydR6r71NY4Vg"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_53AUYMK6EdydR6r71NY4Vg"
 */
@Entity
@Table(name = "EVENEMENT_CIVIL_REGROUPE")
public class EvenementCivilRegroupe extends HibernateEntity {

	/**
	 *
	 */
	private static final long serialVersionUID = 2422294829219811626L;

	/**
	 * The ID
	 */
	private Long id;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LYdY8MK7EdydR6r71NY4Vg"
	 */
	private TypeEvenementCivil type;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SdbwINSZEdyRNpOhiSbYUw"
	 */
	private EtatEvenementCivil etat = EtatEvenementCivil.A_TRAITER;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Fnzo8MK7EdydR6r71NY4Vg"
	 */
	private Date dateTraitement;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DiTVAMK7EdydR6r71NY4Vg"
	 */
	private RegDate dateEvenement;

	private Long numeroIndividuPrincipal;

	private Long numeroIndividuConjoint;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LzhfwcTpEdyVT_nuj0a-ig"
	 */
	private PersonnePhysique habitantPrincipal;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig"
	 */
	private PersonnePhysique habitantConjoint;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IGggAMK7EdydR6r71NY4Vg"
	 */
	private Integer numeroOfsCommuneAnnonce;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg"
	 */
	private Set<EvenementCivilErreur> erreurs;

	/**
	 * Constructeur (requis par Hibernate)
	 */
	public EvenementCivilRegroupe() {
	}

	public EvenementCivilRegroupe(Long id, TypeEvenementCivil type, EtatEvenementCivil etat, RegDate dateEvenement,
			Long numeroIndividuPrincipal, PersonnePhysique individuPrincipal, Long numeroIndividuConjoint,
			PersonnePhysique conjoint, Integer numeroOfsCommuneAnnonce, Set<EvenementCivilErreur> erreurs) {
		this.id = id;
		this.type = type;
		this.etat = etat;
		this.dateEvenement = dateEvenement;
		this.numeroIndividuPrincipal = numeroIndividuPrincipal;
		this.habitantPrincipal = individuPrincipal;
		this.numeroIndividuConjoint = numeroIndividuConjoint;
		this.habitantConjoint = conjoint;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
		this.erreurs = erreurs;
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
	//@GeneratedValue(strategy = GenerationType.AUTO)
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
	 * @return the dateEvenement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DiTVAMK7EdydR6r71NY4Vg?GETTER"
	 */
	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		// begin-user-code
		return dateEvenement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDateEvenement the dateEvenement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_DiTVAMK7EdydR6r71NY4Vg?SETTER"
	 */
	public void setDateEvenement(RegDate theDateEvenement) {
		// begin-user-code
		dateEvenement = theDateEvenement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the dateTraitement
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Fnzo8MK7EdydR6r71NY4Vg?GETTER"
	 */
	@Column(name = "DATE_TRAITEMENT")
	public Date getDateTraitement() {
		// begin-user-code
		return dateTraitement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theDateTraitement the dateTraitement to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_Fnzo8MK7EdydR6r71NY4Vg?SETTER"
	 */
	public void setDateTraitement(Date theDateTraitement) {
		// begin-user-code
		dateTraitement = theDateTraitement;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the numeroOfsCommuneAnnonce
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IGggAMK7EdydR6r71NY4Vg?GETTER"
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
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_IGggAMK7EdydR6r71NY4Vg?SETTER"
	 */
	public void setNumeroOfsCommuneAnnonce(Integer theNumeroOfsCommuneAnnonce) {
		// begin-user-code
		numeroOfsCommuneAnnonce = theNumeroOfsCommuneAnnonce;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the type
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LYdY8MK7EdydR6r71NY4Vg?GETTER"
	 */
	@Column(name = "TYPE", length = LengthConstants.EVTCIVILREG_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementCivilUserType")
	public TypeEvenementCivil getType() {
		// begin-user-code
		return type;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theType the type to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LYdY8MK7EdydR6r71NY4Vg?SETTER"
	 */
	public void setType(TypeEvenementCivil theType) {
		// begin-user-code
		type = theType;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the individuPrincipal
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LzhfwcTpEdyVT_nuj0a-ig?GETTER"
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "HAB_PRINCIPAL")
	@ForeignKey(name = "FK_EV_RGR_TRS_PRC_ID")
	public PersonnePhysique getHabitantPrincipal() {
		// begin-user-code
		return habitantPrincipal;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theIndividuPrincipal the individuPrincipal to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LzhfwcTpEdyVT_nuj0a-ig?SETTER"
	 */
	public void setHabitantPrincipal(PersonnePhysique theIndividuPrincipal) {
		// begin-user-code
		habitantPrincipal = theIndividuPrincipal;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the conjoint
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig?GETTER"
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "HAB_CONJOINT")
	@ForeignKey(name = "FK_EV_RGR_TRS_CJT_ID")
	public PersonnePhysique getHabitantConjoint() {
		// begin-user-code
		return habitantConjoint;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theConjoint the conjoint to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig?SETTER"
	 */
	public void setHabitantConjoint(PersonnePhysique theConjoint) {
		// begin-user-code
		habitantConjoint = theConjoint;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the erreurs
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinColumn(name = "EVT_CIVIL_ID", nullable = false)
	@ForeignKey(name = "FK_EV_ERR_EV_RGR_ID")
	@Cascade(value = org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	public Set<EvenementCivilErreur> getErreurs() {
		// begin-user-code
		return erreurs;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theErreurs the erreurs to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg?SETTER"
	 */
	public void setErreurs(Set<EvenementCivilErreur> theErreurs) {
		// begin-user-code
		erreurs = theErreurs;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the etat
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SdbwINSZEdyRNpOhiSbYUw?GETTER"
	 */
	@Column(name = "ETAT", length = LengthConstants.EVTCIVILREG_ETAT)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatEvenementCivilUserType")
	public EtatEvenementCivil getEtat() {
		// begin-user-code
		return etat;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theEtat the etat to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_SdbwINSZEdyRNpOhiSbYUw?SETTER"
	 */
	public void setEtat(EtatEvenementCivil theEtat) {
		// begin-user-code
		etat = theEtat;
		// end-user-code
	}

	@Column(name = "NO_INDIVIDU_PRINCIPAL")
	public Long getNumeroIndividuPrincipal() {
		return numeroIndividuPrincipal;
	}

	public void setNumeroIndividuPrincipal(Long numeroIndividuPrincipal) {
		this.numeroIndividuPrincipal = numeroIndividuPrincipal;
	}

	@Column(name = "NO_INDIVIDU_CONJOINT")
	public Long getNumeroIndividuConjoint() {
		return numeroIndividuConjoint;
	}

	public void setNumeroIndividuConjoint(Long numeroIndividuConjoint) {
		this.numeroIndividuConjoint = numeroIndividuConjoint;
	}

	/**
	 * Ne renvoie que les VRAIES erreurs
	 *
	 * @return une liste de message d'erreur
	 */
	@Transient
	public Set<EvenementCivilErreur> getErrors() {
		Set<EvenementCivilErreur> list = new LinkedHashSet<EvenementCivilErreur>();
		for (EvenementCivilErreur e : getErreurs()) {
			if (e.getType().equals(TypeEvenementErreur.ERROR)) {
				list.add(e);
			}
		}
		return list;
	}

	/**
	 * Ne renvoie que les VRAIES warnings
	 *
	 * @return une liste de message d'erreur
	 */
	@Transient
	public Set<EvenementCivilErreur> getWarnings() {
		Set<EvenementCivilErreur> list = new LinkedHashSet<EvenementCivilErreur>();
		for (EvenementCivilErreur e : getErreurs()) {
			if (e.getType().equals(TypeEvenementErreur.WARNING)) {
				list.add(e);
			}
		}
		return list;
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addErrors(List<EvenementCivilErreur> errors) {
		if (erreurs == null) {
			erreurs = new HashSet<EvenementCivilErreur>();
		}
		for (EvenementCivilErreur e : errors) {
			e.setType(TypeEvenementErreur.ERROR);
			erreurs.add(e);
		}
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addWarnings(List<EvenementCivilErreur> warn) {
		if (erreurs == null) {
			erreurs = new HashSet<EvenementCivilErreur>();
		}
		for (EvenementCivilErreur w : warn) {
			w.setType(TypeEvenementErreur.WARNING);
			erreurs.add(w);
		}
	}

}
