package ch.vd.uniregctb.evenement.civil.regpp;

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

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
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
public class EvenementCivilRegPP extends HibernateEntity {

	private Long id;
	private TypeEvenementCivil type;
	private EtatEvenementCivil etat = EtatEvenementCivil.A_TRAITER;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividuPrincipal;
	private Long numeroIndividuConjoint;
	private Integer numeroOfsCommuneAnnonce;
	private String commentaireTraitement;
	private Set<EvenementCivilRegPPErreur> erreurs;

	/**
	 * Constructeur (requis par Hibernate)
	 */
	public EvenementCivilRegPP() {
	}

	public EvenementCivilRegPP(Long id, TypeEvenementCivil type, EtatEvenementCivil etat, RegDate dateEvenement, Long numeroIndividuPrincipal, Long numeroIndividuConjoint,
	                           Integer numeroOfsCommuneAnnonce, Set<EvenementCivilRegPPErreur> erreurs) {
		this.id = id;
		this.type = type;
		this.etat = etat;
		this.dateEvenement = dateEvenement;
		this.numeroIndividuPrincipal = numeroIndividuPrincipal;
		this.numeroIndividuConjoint = numeroIndividuConjoint;
		this.numeroOfsCommuneAnnonce = numeroOfsCommuneAnnonce;
		this.erreurs = erreurs;
	}

	public EvenementCivilRegPP(EvtRegCivilDocument.EvtRegCivil bean) {
		this.id = (long)bean.getNoTechnique();
		this.type = TypeEvenementCivil.valueOf(bean.getCode());
		this.etat = EtatEvenementCivil.A_TRAITER;
		this.dateEvenement = RegDateHelper.get(bean.getDateEvenement().getTime());
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
	 * @return the erreurs
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg?GETTER"
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "EVT_CIVIL_ID", nullable = false)
	@ForeignKey(name = "FK_EV_ERR_EV_RGR_ID")
	public Set<EvenementCivilRegPPErreur> getErreurs() {
		// begin-user-code
		return erreurs;
		// end-user-code
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param theErreurs the erreurs to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_R6KEAcK-EdydR6r71NY4Vg?SETTER"
	 */
	public void setErreurs(Set<EvenementCivilRegPPErreur> theErreurs) {
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
	 * @return un commentaire sur la manière dont le traitement c'est effectué, ou <b>null</b> s'il n'y a rien à dire.
	 */
	@Column(name = "COMMENTAIRE_TRAITEMENT", length = LengthConstants.EVTCIVILREG_COMMENT)
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(String value) {
		if (value != null && value.length() > LengthConstants.EVTCIVILREG_COMMENT) {
			value = value.substring(0, LengthConstants.EVTCIVILREG_COMMENT);
		}
		this.commentaireTraitement = value;
	}

	/**
	 * Ne renvoie que les VRAIES erreurs
	 *
	 * @return une liste de message d'erreur
	 */
	@Transient
	public Set<EvenementCivilRegPPErreur> getErrors() {
		Set<EvenementCivilRegPPErreur> list = new LinkedHashSet<>();
		for (EvenementCivilRegPPErreur e : getErreurs()) {
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
	public Set<EvenementCivilRegPPErreur> getWarnings() {
		Set<EvenementCivilRegPPErreur> list = new LinkedHashSet<>();
		for (EvenementCivilRegPPErreur e : getErreurs()) {
			if (e.getType() == TypeEvenementErreur.WARNING) {
				list.add(e);
			}
		}
		return list;
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addErrors(List<EvenementCivilRegPPErreur> errors) {
		if (erreurs == null) {
			erreurs = new HashSet<>();
		}
		for (EvenementCivilRegPPErreur e : errors) {
			e.setType(TypeEvenementErreur.ERROR);
			erreurs.add(e);
		}
	}

	/**
	 * Ajoute les erreurs dans la liste <pre>errors</pre>
	 */
	public void addWarnings(List<EvenementCivilRegPPErreur> warn) {
		if (erreurs == null) {
			erreurs = new HashSet<>();
		}
		for (EvenementCivilRegPPErreur w : warn) {
			w.setType(TypeEvenementErreur.WARNING);
			erreurs.add(w);
		}
	}

}
