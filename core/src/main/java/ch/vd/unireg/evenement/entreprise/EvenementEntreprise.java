package ch.vd.unireg.evenement.entreprise;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.evenement.ide.ReferenceAnnonceIDE;
import ch.vd.unireg.type.EtatEvenementEntreprise;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeEvenementEntreprise;

@Entity
@Table(name = "EVENEMENT_ORGANISATION")
public class EvenementEntreprise extends HibernateEntity {

	private long id;
	/**
	 * Identifiant de l'annonce (noticeId de la norme)
	 */
	private long noEvenement;
	/**
	 * Type de l'annonce
	 */
	private TypeEvenementEntreprise type;
	/**
	 * Date de l'annonce (noticeDate de la norme)
	 */
	private RegDate dateEvenement;
	/**
	 * Identifiant cantonal de l'entreprise dans le registre civil des entreprises
	 */
	private long noEntrepriseCivile;

	/**
	 * La référence à l'annonce à l'IDE émise par Unireg et dont ce message découle.
	 */
	private ReferenceAnnonceIDE referenceAnnonceIDE;

	/**
	 * Flag signalant le fait que cet événement est valable à une date/heure précise antérieure à celle du dernier événement reçu dans l'historique d'une entreprise.
	 */
	private boolean correctionDansLePasse;

	/**
	 * En tête "businessId" du message dans l'ESB, identifiant métier permettant de détecter les messages en double.
	 */
	private String businessId;

	/**
	 * Forme juridique de l'entreprise des suites de l'événement
	 */
	private FormeJuridiqueEntreprise formeJuridique;

	private EtatEvenementEntreprise etat;
	private Date dateTraitement;
	private String commentaireTraitement;
	private List<EvenementEntrepriseErreur> erreurs;

	/**
	 * Réservé à Hibernate
	 */
	public EvenementEntreprise() {
	}

	public EvenementEntreprise(
			long noEvenement,
			TypeEvenementEntreprise type,
			RegDate dateEvenement,
			long noEntrepriseCivile,
			EtatEvenementEntreprise etat
	) {
		this.dateEvenement = dateEvenement;
		this.etat = etat;
		this.noEvenement = noEvenement;
		this.noEntrepriseCivile = noEntrepriseCivile;
		this.type = type;
		this.correctionDansLePasse = false;
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
	@Enumerated(EnumType.STRING)
	public TypeEvenementEntreprise getType() {
		return type;
	}

	public void setType(@NotNull TypeEvenementEntreprise type) {
		this.type = type;
	}

	@Column(name = "ETAT", length = LengthConstants.EVTORGANISATION_ETAT, nullable = false)
	@Enumerated(EnumType.STRING)
	@Index(name = "IDX_EV_ORGA_ETAT")
	public EtatEvenementEntreprise getEtat() {
		return etat;
	}

	public void setEtat(@NotNull EtatEvenementEntreprise etat) {
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
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateEvenement() {
		return dateEvenement;
	}

	public void setDateEvenement(@NotNull RegDate dateEvenement) {
		this.dateEvenement = dateEvenement;
	}

	@Column(name = "NO_ORGANISATION", nullable = false)
	@Index(name = "IDX_EV_ORGA_NO_ORGA")
	public long getNoEntrepriseCivile() {
		return noEntrepriseCivile;
	}

	public void setNoEntrepriseCivile(long noEntrepriseCivile) {
		this.noEntrepriseCivile = noEntrepriseCivile;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "NO_ANNONCE_IDE")
	@ForeignKey(name = "FK_EV_ORG_REFANNIDE_ID")
	public ReferenceAnnonceIDE getReferenceAnnonceIDE() {
		return referenceAnnonceIDE;
	}

	public void setReferenceAnnonceIDE(ReferenceAnnonceIDE referenceAnnonceIDE) {
		this.referenceAnnonceIDE = referenceAnnonceIDE;
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

	@Column(name = "CORRECTION_DANS_PASSE")
	public boolean getCorrectionDansLePasse() {
		return correctionDansLePasse;
	}

	public void setCorrectionDansLePasse(boolean correctionDansLePasse) {
		this.correctionDansLePasse = correctionDansLePasse;
	}

	@Column(name = "BUSINESS_ID", length = LengthConstants.EVTORGANISATION_BUSINESS_ID)
	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String messageId) {
		this.businessId = messageId;
	}

	@Column(name = "FORME_JURIDIQUE", length = LengthConstants.PM_FORME)
	@Index(name = "IDX_EV_ORGA_FORME_JUR")
	@Enumerated(EnumType.STRING)
	public FormeJuridiqueEntreprise getFormeJuridique() {
		return formeJuridique;
	}

	public void setFormeJuridique(FormeJuridiqueEntreprise formeJuridique) {
		this.formeJuridique = formeJuridique;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "EVT_ORGANISATION_ID", nullable = false)
	@ForeignKey(name = "FK_EV_ERR_EV_ORGA_ID")
	@OrderColumn(name = "LIST_INDEX", nullable = false)
	public List<EvenementEntrepriseErreur> getErreurs() {
		return erreurs;
	}

	public void setErreurs(List<EvenementEntrepriseErreur> erreurs) {
		this.erreurs = erreurs;
	}

	@Override
	public String toString() {
		return String.format("Evt Org n°%d, rcent:%d, orga: %d, du %s", getId(), getNoEvenement(), getNoEntrepriseCivile(), RegDateHelper.dateToDisplayString(getDateEvenement()));
	}

	public String rapportErreurs() {
		return CollectionsUtils.toString(this.getErreurs(), EvenementEntrepriseErreur::getMessage, "\n");
	}

	@Transient
	public String getCleLienPublicationFosc() {
		if (type.getSource() != TypeEvenementEntreprise.Source.FOSC) {
			return null;
		}
		return isAnnonceFosc2() ? "extprop.annonce.rcent.fosc2.url.consultation.publication" : "extprop.annonce.rcent.fosc1.url.consultation.publication";
	}

	@Transient
	private boolean isAnnonceFosc2() {
		//la date de référence du début des annonces FOSC 2 est le 03.09.2019
		return type.getSource() == TypeEvenementEntreprise.Source.FOSC && dateEvenement.isAfterOrEqual(RegDate.get(2018, 9, 3));
	}
}
