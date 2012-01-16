package ch.vd.uniregctb.evenement.civil.ech;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

@Entity
@Table(name = "EVENEMENT_CIVIL_ECH")
public class EvenementCivilEch extends HibernateEntity {

	private Long id;
	private Long refMessageId;
	private TypeEvenementCivilEch type;
	private ActionEvenementCivilEch action;
	private EtatEvenementCivil etat;
	private Date dateTraitement;
	private RegDate dateEvenement;
	private Long numeroIndividu;
	private Long numeroContribuablePersonnePhysique;
	private String commentaireTraitement;

	// TODO jde il manque encore les erreurs

	public EvenementCivilEch() {
	}

	public EvenementCivilEch(ch.vd.evd0006.v1.EventIdentification bean) {
		this.id = bean.getMessageId();
		this.refMessageId = bean.getReferenceMessageId();
		this.dateEvenement = XmlUtils.xmlcal2regdate(bean.getDate());
		this.etat = EtatEvenementCivil.A_TRAITER;
		this.type = TypeEvenementCivilEch.fromEchCode(bean.getType());
		this.action = ActionEvenementCivilEch.fromEchCode(bean.getAction());
		this.dateTraitement = null;
		this.numeroIndividu = null;
		this.numeroContribuablePersonnePhysique = null;
		this.commentaireTraitement = null;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "REF_MESSAGE_ID")
	public Long getRefMessageId() {
		return refMessageId;
	}

	public void setRefMessageId(Long refMessageId) {
		this.refMessageId = refMessageId;
	}

	@Column(name = "TYPE", length = LengthConstants.EVTCIVILECH_TYPE, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementCivilEchUserType")
	public TypeEvenementCivilEch getType() {
		return type;
	}

	public void setType(TypeEvenementCivilEch type) {
		this.type = type;
	}

	@Column(name = "ACTION_EVT", length = LengthConstants.EVTCIVILECH_ACTION, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.ActionEvenementCivilEchUserType")
	public ActionEvenementCivilEch getAction() {
		return action;
	}

	public void setAction(ActionEvenementCivilEch action) {
		this.action = action;
	}

	@Column(name = "ETAT", length = LengthConstants.EVTCIVILECH_ETAT, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatEvenementCivilUserType")
	@Index(name = "IDX_EV_CIV_ECH_ETAT")
	public EtatEvenementCivil getEtat() {
		return etat;
	}

	public void setEtat(EtatEvenementCivil etat) {
		this.etat = etat;
	}

	@Column(name = "DATE_TRAITEMENT")
	public Date getDateTraitement() {
		return dateTraitement;
	}

	public void setDateTraitement(Date dateTraitement) {
		this.dateTraitement = dateTraitement;
	}

	@Column(name = "DATE_EVENEMENT", nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	@Column(name = "NO_INDIVIDU")
	@Index(name = "IDX_EV_CIV_ECH_NO_IND")
	public Long getNumeroIndividu() {
		return numeroIndividu;
	}

	public void setNumeroIndividu(Long numeroIndividu) {
		this.numeroIndividu = numeroIndividu;
	}

	@Column(name = "PP_ID")
	@Index(name = "IDX_EV_CIV_ECH_PP")
	public Long getNumeroContribuablePersonnePhysique() {
		return numeroContribuablePersonnePhysique;
	}

	public void setNumeroContribuablePersonnePhysique(Long numeroContribuablePersonnePhysique) {
		this.numeroContribuablePersonnePhysique = numeroContribuablePersonnePhysique;
	}

	/**
	 * @return un commentaire sur la manière dont le traitement c'est effectué, ou <b>null</b> s'il n'y a rien à dire.
	 */
	@Column(name = "COMMENTAIRE_TRAITEMENT", length = LengthConstants.EVTCIVILECH_COMMENT)
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(String commentaireTraitement) {
		this.commentaireTraitement = commentaireTraitement;
	}
}
