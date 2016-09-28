package ch.vd.uniregctb.evenement.organisation;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

@Entity
@Table(name = "EVENEMENT_ORGANISATION")
public class EvenementOrganisation extends HibernateEntity {

	private long id;
	/**
	 * ch.vd.evd0024.v3:noticeRoot:notice:noticeId
	 */
	private long noEvenement;
	/**
	 * ch.vd.evd0024.v3:noticeRoot:notice:typeOfNotice
	 */
	private TypeEvenementOrganisation type;
	/**
	 * ch.vd.evd0024.v3:noticeRoot:notice:noticeDate
	 */
	private RegDate dateEvenement;
	/**
	 * ch.vd.evd0024.v3:noticeRoot:notice:organisation[0]:Organisation:cantonalId
	 * Il peut y avoir plusieurs organisations, mais on ne prend que la première
	 */
	private long noOrganisation;

	/**
	 * ch.vd.evd0024.v3:noticeRoot:notice:noticeRequest:noticeRequestId
	 * Le numéro de l'annonce à l'IDE à l'origine de l'événement, si cette annonce provient d'Unireg.
	 */
	private Long noAnnonceIDE;

	private EtatEvenementOrganisation etat;
	private Date dateTraitement;
	private String commentaireTraitement;
	private List<EvenementOrganisationErreur> erreurs;

	/**
	  Réservé à Hibernate
	 */
	public EvenementOrganisation() {
	}

	public EvenementOrganisation(
			long noEvenement,
			TypeEvenementOrganisation type,
			RegDate dateEvenement,
			long noOrganisation,
			EtatEvenementOrganisation etat
			) {
		this.dateEvenement = dateEvenement;
		this.etat = etat;
		this.noEvenement = noEvenement;
		this.noOrganisation = noOrganisation;
		this.type = type;
	}

	@Transient
	@Override
	public Object getKey() {
		return id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}


	/**
	 * @return le numéro d'événement RCEnt
	 */
	@Column(name = "NO_EVENEMENT", nullable = false)
	@Index(name = "IDX_EV_ORGA_NO_EV")
	public long getNoEvenement() {
		return noEvenement;
	}

	public void setNoEvenement(long noEvenement) {
		this.noEvenement = noEvenement;
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

	public Long getNoAnnonceIDE() {
		return noAnnonceIDE;
	}

	@Column(name = "NO_ANNONCE_IDE")
	public void setNoAnnonceIDE(Long noAnnonceIDE) {
		this.noAnnonceIDE = noAnnonceIDE;
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
	@OrderColumn(name = "LIST_INDEX", nullable = false)
	public List<EvenementOrganisationErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(List<EvenementOrganisationErreur> erreurs) {
		this.erreurs = erreurs;
	}

	@Override
	public String toString() {
		return String.format("Evt Org n°%d, rcent:%d, orga: %d, du %s", getId(), getNoEvenement(), getNoOrganisation(), RegDateHelper.dateToDisplayString(getDateEvenement()));
	}

	public String rapportErreurs() {
		return CollectionsUtils.toString(this.getErreurs(), new StringRenderer<EvenementOrganisationErreur>() {
			@Override
			public String toString(EvenementOrganisationErreur object) {
				return object.getMessage();
			}
		}, "\n");
	}
}
