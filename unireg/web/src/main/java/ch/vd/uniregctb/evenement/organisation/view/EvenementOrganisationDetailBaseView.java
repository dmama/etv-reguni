package ch.vd.uniregctb.evenement.organisation.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.organisation.OrganisationView;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

abstract public class EvenementOrganisationDetailBaseView implements Serializable {

	private static final long serialVersionUID = 1843719863400286679L;

	private Long evtId;
	private EtatEvenementOrganisation evtEtat;
	private RegDate evtDate;
	private Date evtDateTraitement;
	private String evtCommentaireTraitement;
	private List<ErreurEvenementOrganisationView> evtErreurs = new ArrayList<>();
	private Long noOrganisation;
	private OrganisationView organisation;
	private String organisationError;
	private AdresseEnvoi adresse;
	private List<TiersAssocieView> tiersAssocies = new ArrayList<>();
	private Set<String> erreursTiersAssocies = new LinkedHashSet<>();     // pour éviter les doublons mais conserver l'ordre d'insertion

	@SuppressWarnings("unused")
	public Long getEvtId() {
		return evtId;
	}

	public void setEvtId(Long evtId) {
		this.evtId = evtId;
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
		return Collections.unmodifiableList(evtErreurs);
	}

	public void addEvtErreur(ErreurEvenementOrganisationView evtErreur) {
		evtErreurs.add(evtErreur);
	}

	@SuppressWarnings("unused")
	public List<TiersAssocieView> getTiersAssocies() {
		return Collections.unmodifiableList(tiersAssocies);
	}

	public void addTiersAssocies(TiersAssocieView tiersAssocie) {
		tiersAssocies.add(tiersAssocie);
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
}
