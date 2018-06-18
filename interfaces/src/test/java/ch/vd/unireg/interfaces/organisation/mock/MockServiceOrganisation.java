package ch.vd.unireg.interfaces.organisation.mock;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.type.TypeAdresseCivil;

public abstract class MockServiceOrganisation implements ServiceOrganisationRaw {

	/**
	 * Map des organisations par numéro
	 */
	private final Map<Long, MockOrganisation> organisationMap = new HashMap<>();
	private Map<String, AnnonceIDE> annoncesIDE = new HashMap<>();
	private Map<BaseAnnonceIDE.Contenu, BaseAnnonceIDE.Statut> annoncesIDEValidations = new HashMap<>();

	/**
	 * Cette méthode initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	public MockServiceOrganisation() {
		this.init();
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		return organisationMap.get(noOrganisation);
	}

	@Override
	public Long getNoOrganisationFromNoEtablissement(Long noEtablissementCivil) throws ServiceOrganisationException {
		for (Map.Entry<Long, MockOrganisation> organisation : organisationMap.entrySet()) {
			for (EtablissementCivil etablissement : organisation.getValue().getEtablissements()) {
				if (etablissement.getNumeroEtablissement() == noEtablissementCivil) {
					return organisation.getKey();
				}
			}
		}
		return null;
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		for (Map.Entry<Long, MockOrganisation> organisation : organisationMap.entrySet()) {
			for (EtablissementCivil etablissement : organisation.getValue().getEtablissements()) {
				if (etablissement.getNumeroIDE() != null) {
					for (DateRanged<String> candidat : etablissement.getNumeroIDE()) {
						if (noide != null && noide.equals(candidat.getPayload())) {
							return new Identifiers(organisation.getKey(), etablissement.getNumeroEtablissement());
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

	protected void addOrganisation(MockOrganisation organisation) {
		organisationMap.put(organisation.getNumeroOrganisation(), organisation);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {

		final Long noticeId = query.getNoticeId();
		if (noticeId == null){
			throw new NotImplementedException("Seule la recherche par identifiant est supportée dans cette version de MockServiceOrganisation");
		}

		/*
			Implémentation partielle qui ne se préoccupe que de la recherche par identifiant.
		 */
		String userId = query.getUserId();
		if (userId == null) {
			userId = RCEntAnnonceIDEHelper.UNIREG_USER;
		}
		return new PageImpl<AnnonceIDE>(Collections.singletonList(annoncesIDE.get(noticeId + userId)));
	}

	protected void addAnnonceIDE(AnnonceIDE annonce, String userId) {
		if (userId == null) {
			userId = RCEntAnnonceIDEHelper.UNIREG_USER;
		}
		annoncesIDE.put(annonce.getNumero() + userId, annonce);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) {
		final BaseAnnonceIDE.Statut statut = annoncesIDEValidations.get(modele.getContenu());
		Assert.notNull(statut, "Objet statut introuvable pour le prototype reçu. Le statut à renvoyer pour un proto d'annonce précis doit être configuré au début du test. La comparaison se base sur ProtoAnnonceIDE.Contenu.");
		return statut;
	}

	public void addStatutAnnonceIDEAttentu(BaseAnnonceIDE modele, AnnonceIDEEnvoyee.Statut statut) {
		annoncesIDEValidations.put(modele.getContenu(), statut);
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		// un mock fonctionne toujours
	}

	protected MockOrganisation addOrganisation(long cantonalId) {
		final MockOrganisation org = new MockOrganisation(cantonalId);
		addOrganisation(org);
		return org;
	}

	protected MockEtablissementCivil addEtablissmeent(MockOrganisation organisation, long cantonalId, RegDate dateCreation,
	                                                  MockDonneesRegistreIDE donneesRegistreIDE, MockDonneesRC donneesRC, MockDonneesREE donneesREE) {
		final MockEtablissementCivil etablissement = new MockEtablissementCivil(cantonalId, donneesRegistreIDE, donneesRC, donneesREE);
		organisation.addDonneesEtablissement(etablissement);
		return etablissement;
	}

	protected void addNumeroIDE(MockOrganisation organisation, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		MockEtablissementCivil etablissement = (MockEtablissementCivil) organisation.getEtablissements().get(0);
		etablissement.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected void addNumeroIDE(MockEtablissementCivil etablissement, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		etablissement.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected MockAdresse addAdresse(MockOrganisation organisation, TypeAdresseCivil type, MockRue rue, RegDate dateDebut, @Nullable RegDate dateFin) {
		return addAdresse(organisation, type, rue, null, rue.getLocalite(), dateDebut, dateFin);
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	protected MockAdresse addAdresse(MockOrganisation organisation, TypeAdresseCivil type, @Nullable MockRue rue, @Nullable String complement,
	                                 MockLocalite localite, RegDate debutValidite, @Nullable RegDate finValidite) {
		return addAdresse(organisation, type, rue, null, complement, localite, debutValidite, finValidite);
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	protected MockAdresse addAdresse(MockOrganisation pm, TypeAdresseCivil type, @Nullable MockRue rue, @Nullable String numeroMaison, @Nullable String complement,
	                                 MockLocalite localite, RegDate debutValidite, @Nullable RegDate finValidite) {

		final MockAdresse adresse = new MockAdresse();
		adresse.setTypeAdresse(type);
		adresse.setTitre(complement);
		if (rue != null) {
			adresse.setRue(rue.getDesignationCourrier());
		}
		adresse.setNumero(numeroMaison);
		adresse.setLocalite(localite.getNomAbrege());
		adresse.setDateDebutValidite(debutValidite);
		adresse.setDateFinValidite(finValidite);

		pm.addAdresse(adresse);
		return adresse;
	}

}
