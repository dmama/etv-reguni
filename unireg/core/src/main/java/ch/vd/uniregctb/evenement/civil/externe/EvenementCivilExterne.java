package ch.vd.uniregctb.evenement.civil.externe;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
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
@Table(name = "EVENEMENT_CIVIL")
public class EvenementCivilExterne extends HibernateEntity {

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
	private Long habitantPrincipalId;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 *
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig"
	 */
	private Long habitantConjointId;

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
	private Set<EvenementCivilExterneErreur> erreurs;

	/**
	 * Constructeur (requis par Hibernate)
	 */
	public EvenementCivilExterne() {
	}

	public EvenementCivilExterne(Long id, TypeEvenementCivil type, EtatEvenementCivil etat, RegDate dateEvenement,
	                             Long numeroIndividuPrincipal, PersonnePhysique individuPrincipal, Long numeroIndividuConjoint,
	                             PersonnePhysique conjoint, Integer numeroOfsCommuneAnnonce, Set<EvenementCivilExterneErreur> erreurs) {
		this.id = id;
		this.type = type;
		this.etat = etat;
		this.dateEvenement = dateEvenement;
		this.numeroIndividuPrincipal = numeroIndividuPrincipal;
		this.habitantPrincipalId = (individuPrincipal == null ? null : individuPrincipal.getId());
		this.numeroIndividuConjoint = numeroIndividuConjoint;
		this.habitantConjointId = (conjoint == null ? null : conjoint.getId());
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
		this.erreurs = erreurs;
	}

	public EvenementCivilExterne(EvtRegCivilDocument.EvtRegCivil bean) {
		this.id = (long)bean.getNoTechnique();
		this.type = TypeEvenementCivil.valueOf(bean.getCode());
		this.etat = EtatEvenementCivil.A_TRAITER;
		this.dateEvenement = RegDate.get(bean.getDateEvenement().getTime());
		this.numeroIndividuPrincipal = (long) bean.getNoIndividu();
		this.dateTraitement = DateHelper.getCurrentDate();
		this.numeroOfsCommuneAnnonce = bean.getNumeroOFS();
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
	@Column(name = "HAB_PRINCIPAL")
	@ForeignKey(name = "FK_EV_RGR_TRS_PRC_ID")
	public Long getHabitantPrincipalId() {
		// begin-user-code
		return habitantPrincipalId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param value the individuPrincipal to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_LzhfwcTpEdyVT_nuj0a-ig?SETTER"
	 */
	public void setHabitantPrincipalId(Long value) {
		// begin-user-code
		habitantPrincipalId = value;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @return the conjoint
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig?GETTER"
	 */
	@Column(name = "HAB_CONJOINT")
	@ForeignKey(name = "FK_EV_RGR_TRS_CJT_ID")
	public Long getHabitantConjointId() {
		// begin-user-code
		return habitantConjointId;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param value the conjoint to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_lrMoYcTpEdyVT_nuj0a-ig?SETTER"
	 */
	public void setHabitantConjointId(Long value) {
		// begin-user-code
		habitantConjointId = value;
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
	public Set<EvenementCivilExterneErreur> getErreurs() {
		// begin-user-code
		return erreurs;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theErreurs the erreurs to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg?SETTER"
	 */
	public void setErreurs(Set<EvenementCivilExterneErreur> theErreurs) {
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
	@Index(name = "IDX_EV_CIV_ETAT")
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
	@Index(name = "IDX_EV_CIV_NO_IND_PR")
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
	public Set<EvenementCivilExterneErreur> getErrors() {
		Set<EvenementCivilExterneErreur> list = new LinkedHashSet<EvenementCivilExterneErreur>();
		for (EvenementCivilExterneErreur e : getErreurs()) {
			if (e.getType() == TypeEvenementErreur.ERROR) {
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
	public Set<EvenementCivilExterneErreur> getWarnings() {
		Set<EvenementCivilExterneErreur> list = new LinkedHashSet<EvenementCivilExterneErreur>();
		for (EvenementCivilExterneErreur e : getErreurs()) {
			if (e.getType() == TypeEvenementErreur.WARNING) {
				list.add(e);
			}
		}
		return list;
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addErrors(List<EvenementCivilExterneErreur> errors) {
		if (erreurs == null) {
			erreurs = new HashSet<EvenementCivilExterneErreur>();
		}
		for (EvenementCivilExterneErreur e : errors) {
			e.setType(TypeEvenementErreur.ERROR);
			erreurs.add(e);
		}
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addWarnings(List<EvenementCivilExterneErreur> warn) {
		if (erreurs == null) {
			erreurs = new HashSet<EvenementCivilExterneErreur>();
		}
		for (EvenementCivilExterneErreur w : warn) {
			w.setType(TypeEvenementErreur.WARNING);
			erreurs.add(w);
		}
	}

}
