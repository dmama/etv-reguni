package ch.vd.unireg.evenement.organisation.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseEnvoi;
import ch.vd.unireg.evenement.common.view.TiersAssocieView;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.unireg.organisation.OrganisationView;
import ch.vd.unireg.type.EtatEvenementOrganisation;
import ch.vd.unireg.type.TypeEvenementOrganisation;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 */
public class EvenementOrganisationDetailView implements Serializable {

	private static final long serialVersionUID = 4622877003735146179L;

	private Long evtId;
	private Long noEvenement;
	private RegDate evtDate;
	private TypeEvenementOrganisation evtType;
	private Long refEvtId;
	private EtatEvenementOrganisation evtEtat;
	private boolean recyclable;
	private boolean forcable;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private long annonceIDEId;
	private boolean correctionDansLePasse;
	private final List<ErreurEvenementOrganisationView> evtErreurs = new ArrayList<>();

	private Long foscNumero;
	private RegDate foscDate;
	private String foscLienDirect;

	private Long noOrganisation;
	private OrganisationView organisation;
	private String organisationError;
	private AdresseEnvoi adresse;
	private TiersAssocieView tiersAssocie;
	private final Set<String> erreursTiersAssocies = new LinkedHashSet<>();     // pour éviter les doublons mais conserver l'ordre d'insertion

	private List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation;

	private boolean embedded;

	private Long nextId;

	@SuppressWarnings("unused")
	public Long getEvtId() {
		return evtId;
	}

	public void setEvtId(Long evtId) {
		this.evtId = evtId;
	}

	public Long getNoEvenement() {
		return noEvenement;
	}

	public void setNoEvenement(Long noEvenement) {
		this.noEvenement = noEvenement;
	}

	@SuppressWarnings("unused")
	public EtatEvenementOrganisation getEvtEtat() {
		return evtEtat;
	}

	public void setEvtEtat(EtatEvenementOrganisation evtEtat) {
		this.evtEtat = evtEtat;
	}

	@SuppressWarnings("unused")
	public RegDate getEvtDate() {
		return evtDate;
	}

	public void setEvtDate(RegDate evtDate) {
		this.evtDate = evtDate;
	}

	@SuppressWarnings("unused")
	public Date getEvtDateTraitement() {
		return evtDateTraitement;
	}

	public void setEvtDateTraitement(Date evtDateTraitement) {
		this.evtDateTraitement = evtDateTraitement;
	}

	public void setEvtCommentaireTraitement(String evtCommentaireTraitement) {
		this.evtCommentaireTraitement = evtCommentaireTraitement;
	}

	@SuppressWarnings("unused")
	public String getEvtCommentaireTraitement() {
		return evtCommentaireTraitement;
	}

	@SuppressWarnings("unused")
	public List<ErreurEvenementOrganisationView> getEvtErreurs() {
		return evtErreurs;
	}

	public void addEvtErreur(ErreurEvenementOrganisationView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	@SuppressWarnings("unused")
	public TypeEvenementOrganisation getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementOrganisation evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
	public boolean isRecyclable() {
		return recyclable;
	}

	public void setRecyclable(boolean recyclable) {
		this.recyclable = recyclable;
	}

	public boolean isForcable() {
		return forcable;
	}

	public void setForcable(boolean forcable) {
		this.forcable = forcable;
	}

	@SuppressWarnings("UnusedDeclaration")
	public Long getRefEvtId() {
		return refEvtId;
	}

	public void setRefEvtId(Long refEvtId) {
		this.refEvtId = refEvtId;
	}

	@SuppressWarnings("UnusedDeclaration")
	public TiersAssocieView getTiersAssocie() {
		return tiersAssocie;
	}

	@SuppressWarnings("unused")
	public List<TiersAssocieView> getTiersAssocies() {
		return tiersAssocie == null ? Collections.emptyList() : Collections.singletonList(tiersAssocie);
	}

	public void setTiersAssocie(TiersAssocieView tiersAssocie) {
		this.tiersAssocie = tiersAssocie;
	}

	@SuppressWarnings("unused")
	public Collection<String> getErreursTiersAssocies() {
		return Collections.unmodifiableCollection(erreursTiersAssocies);
	}

	public void addErreursTiersAssocies(String message) {
		erreursTiersAssocies.add(message);
	}

	public Long getNoOrganisation() {
		return noOrganisation;
	}

	public void setNoOrganisation(Long noOrganisation) {
		this.noOrganisation = noOrganisation;
	}

	public Long getFoscNumero() {
		return foscNumero;
	}

	public void setFoscNumero(Long foscNumero) {
		this.foscNumero = foscNumero;
	}

	public RegDate getFoscDate() {
		return foscDate;
	}

	public void setFoscDate(RegDate foscDate) {
		this.foscDate = foscDate;
	}

	public String getFoscLienDirect() {
		return foscLienDirect;
	}

	public void setFoscLienDirect(String foscLienDirect) {
		this.foscLienDirect = foscLienDirect;
	}

	public OrganisationView getOrganisation() {
		return organisation;
	}

	public void setOrganisation(OrganisationView organisation) {
		this.organisation = organisation;
	}

	public AdresseEnvoi getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseEnvoi adresse) {
		this.adresse = adresse;
	}

	public void setOrganisationError(String organisationError) {
		this.organisationError = organisationError;
	}

	public String getOrganisationError() {
		return organisationError;
	}

	public long getAnnonceIDEId() {
		return annonceIDEId;
	}

	public void setAnnonceIDEId(long annonceIDEId) {
		this.annonceIDEId = annonceIDEId;
	}

	public boolean isCorrectionDansLePasse() {
		return correctionDansLePasse;
	}

	public void setCorrectionDansLePasse(boolean correctionDansLePasse) {
		this.correctionDansLePasse = correctionDansLePasse;
	}

	@SuppressWarnings("UnusedDeclaration")
	public List<EvenementOrganisationBasicInfo> getNonTraitesSurMemeOrganisation() {
		return nonTraitesSurMemeOrganisation;
	}

	public void setNonTraitesSurMemeOrganisation(List<EvenementOrganisationBasicInfo> nonTraitesSurMemeOrganisation) {
		this.nonTraitesSurMemeOrganisation = nonTraitesSurMemeOrganisation;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public Long getNextId() {
		return nextId;
	}

	public void setNextId(Long nextId) {
		this.nextId = nextId;
	}
}
