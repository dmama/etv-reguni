package ch.vd.uniregctb.evenement.organisation;

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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0022.v1.Header;
import ch.vd.evd0022.v1.Notice;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

@Entity
@Table(name = "EVENEMENT_ORGANISATION")
public class EvenementOrganisation extends HibernateEntity {

	/**
	 * ch.vd.evd0024.v1:noticeRoot:header:notice:noticeId
	 */
	private long id;
	/**
	 * ch.vd.evd0024.v1:noticeRoot:header:senderIdentification
	 */
	private EmetteurEvenementOrganisation identiteEmetteur;
	/**
	 * ch.vd.evd0024.v1:noticeRoot:header:senderReferenceData
	 */
	private String refDataEmetteur;
	/**
	 * ch.vd.evd0024.v1:noticeRoot:header:notice:typeOfNotice
	 */
	private TypeEvenementOrganisation type;
	/**
	 * ch.vd.evd0024.v1:noticeRoot:header:notice:noticeDate
	 */
	private RegDate dateEvenement;
	/**
	 * ch.vd.evd0024.v1:noticeRoot:noticeOrganisation:organisationIdentification:cantonalId
	 */
	private long noOrganisation;

	private EtatEvenementOrganisation etat;
	private Date dateTraitement;
	private String commentaireTraitement;
	private Set<EvenementOrganisationErreur> erreurs;

	/**
	  Réservé à Hibernate
	 */
	public EvenementOrganisation() {
	}

	public EvenementOrganisation(ch.vd.evd0022.v1.NoticeRoot message) {
		Header header = message.getHeader();
		Notice notice = header.getNotice();
		this.id = notice.getNoticeId().longValue();
		this.identiteEmetteur = EmetteurEvenementOrganisation.valueOf(header.getSenderIdentification().value());
		this.refDataEmetteur = header.getSenderReferenceData();
		this.dateEvenement = notice.getNoticeDate();
		this.etat = EtatEvenementOrganisation.A_TRAITER;
		this.type = TypeEvenementOrganisation.valueOf(notice.getTypeOfNotice().value());
		this.dateTraitement = null;
		this.noOrganisation = message.getNoticeOrganisation().get(0).getOrganisationIdentification().getCantonalId().longValue();
		this.commentaireTraitement = null;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column(name = "IDENT_EMETTEUR", length = LengthConstants.EVTORGANISATION_IDENTITEEMETTEUR, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.EmetteurEvenementOrganisationUserType")
	public EmetteurEvenementOrganisation getIdentiteEmetteur() {
		return identiteEmetteur;
	}

	public void setIdentiteEmetteur(@NotNull EmetteurEvenementOrganisation identiteEmetteur) {
		this.identiteEmetteur = identiteEmetteur;
	}

	@Column(name = "REFDATA_EMETTEUR", length = LengthConstants.EVTORGANISATION_REFDATAEMETTEUR)
	public String getRefDataEmetteur() {
		return refDataEmetteur;
	}

	public void setRefDataEmetteur(String refDataEmetteur) {
		this.refDataEmetteur = refDataEmetteur;
	}

	@Column(name = "TYPE", length = LengthConstants.EVTORGANISATION_TYPE, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeEvenementOrganisationUserType")
	public TypeEvenementOrganisation getType() {
		return type;
	}

	public void setType(@NotNull TypeEvenementOrganisation type) {
		this.type = type;
	}

	@Column(name = "ETAT", length = LengthConstants.EVTORGANISATION_ETAT, nullable = false)
	@Type(type = "ch.vd.uniregctb.hibernate.EtatEvenementOrganisationUserType")
	@Index(name = "IDX_EV_ORGA_ETAT")
	public EtatEvenementOrganisation getEtat() {
		return etat;
	}

	public void setEtat(@NotNull EtatEvenementOrganisation etat) {
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

	public void setDateEvenement(@NotNull RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	@Column(name = "NO_ORGANISATION", nullable = false)
	@Index(name = "IDX_EV_ORGA_NO_ORGA")
	public long getNoOrganisation() {
		return noOrganisation;
	}

	public void setNoOrganisation(long noOrganisation) {
		this.noOrganisation = noOrganisation;
	}

	/**
	 * @return un commentaire sur la manière dont le traitement c'est effectué, ou <b>null</b> s'il n'y a rien à dire.
	 */
	@Column(name = "COMMENTAIRE_TRAITEMENT", length = LengthConstants.EVTORGANISATION_COMMENT)
	public String getCommentaireTraitement() {
		return commentaireTraitement;
	}

	public void setCommentaireTraitement(@Nullable String commentaireTraitement) {
		this.commentaireTraitement = StringUtils.abbreviate(commentaireTraitement, LengthConstants.EVTORGANISATION_COMMENT);
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "EVT_ORGANISATION_ID", nullable = false)
	@ForeignKey(name = "FK_EV_ERR_EV_ORGA_ID")
	public Set<EvenementOrganisationErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(Set<EvenementOrganisationErreur> erreurs) {
		this.erreurs = erreurs;
	}
}
