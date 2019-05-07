package ch.vd.unireg.evenement.civil.regpp;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
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

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeEvenementErreur;

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

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	/**
	 * @return the id
	 */
	@Id
	//@GeneratedValue(generator = "defaultGenerator")
	//@SequenceGenerator(name = "defaultGenerator", sequenceName = "hibernate_sequence", allocationSize = 1)
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

	@Column(name = "DATE_EVENEMENT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate theDateEvenement) {
		dateEvenement = theDateEvenement;
	}

	@Column(name = "DATE_TRAITEMENT")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date theDateTraitement) {
		dateTraitement = theDateTraitement;
	}

	@Column(name = "NUMERO_OFS_ANNONCE")
	public Integer getNumeroOfsCommuneAnnonce() {
		return numeroOfsCommuneAnnonce;
	}

	public void setNumeroOfsCommuneAnnonce(Integer theNumeroOfsCommuneAnnonce) {
		numeroOfsCommuneAnnonce = theNumeroOfsCommuneAnnonce;
	}

	@Column(name = "TYPE", length = LengthConstants.EVTCIVILREG_TYPE)
	@Type(type = "ch.vd.unireg.hibernate.TypeEvenementCivilUserType")
	public TypeEvenementCivil getType() {
		return type;
	}

	public void setType(TypeEvenementCivil theType) {
		type = theType;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "EVT_CIVIL_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_EV_ERR_EV_RGR_ID"))
	public Set<EvenementCivilRegPPErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(Set<EvenementCivilRegPPErreur> theErreurs) {
		erreurs = theErreurs;
	}

	@Column(name = "ETAT", length = LengthConstants.EVTCIVILREG_ETAT)
	@Type(type = "ch.vd.unireg.hibernate.EtatEvenementCivilUserType")
	@Index(name = "IDX_EV_CIV_ETAT")
	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementCivil theEtat) {
		etat = theEtat;
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
