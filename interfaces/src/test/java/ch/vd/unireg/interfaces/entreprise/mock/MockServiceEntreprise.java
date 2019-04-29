package ch.vd.unireg.interfaces.entreprise.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseException;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEEnvoyee;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesREE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.rcent.RCEntAnnonceIDEHelper;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.type.TypeAdresseCivil;

public abstract class MockServiceEntreprise implements ServiceEntrepriseRaw {

	/**
	 * Map des entreprisess par numéro
	 */
	private final Map<Long, MockEntrepriseCivile> entrepriseMap = new HashMap<>();
	private Map<String, AnnonceIDE> annoncesIDE = new HashMap<>();
	private Map<BaseAnnonceIDE.Contenu, BaseAnnonceIDE.Statut> annoncesIDEValidations = new HashMap<>();

	/**
	 * Cette méthode initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	public MockServiceEntreprise() {
		this.init();
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws ServiceEntrepriseException {
		return entrepriseMap.get(noEntreprise);
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws ServiceEntrepriseException {
		for (Map.Entry<Long, MockEntrepriseCivile> entreprise : entrepriseMap.entrySet()) {
			for (EtablissementCivil etablissement : entreprise.getValue().getEtablissements()) {
				if (etablissement.getNumeroEtablissement() == noEtablissementCivil) {
					return entreprise.getKey();
				}
			}
		}
		return null;
	}

	@Override
	public Identifiers getEntrepriseByNoIde(String noide) throws ServiceEntrepriseException {
		for (Map.Entry<Long, MockEntrepriseCivile> entreprise : entrepriseMap.entrySet()) {
			for (EtablissementCivil etablissement : entreprise.getValue().getEtablissements()) {
				if (etablissement.getNumeroIDE() != null) {
					for (DateRanged<String> candidat : etablissement.getNumeroIDE()) {
						if (noide != null && noide.equals(candidat.getPayload())) {
							return new Identifiers(entreprise.getKey(), etablissement.getNumeroEtablissement());
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws ServiceEntrepriseException {
		throw new UnsupportedOperationException();
	}

	protected void addEntreprise(MockEntrepriseCivile entreprise) {
		entrepriseMap.put(entreprise.getNumeroEntreprise(), entreprise);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceEntrepriseException {

		final Long noticeId = query.getNoticeId();
		if (noticeId == null){
			throw new NotImplementedException("Seule la recherche par identifiant est supportée dans cette version de MockServiceEntreprise");
		}

		/*
			Implémentation partielle qui ne se préoccupe que de la recherche par identifiant.
		 */
		String userId = query.getUserId();
		if (userId == null) {
			userId = RCEntAnnonceIDEHelper.UNIREG_USER;
		}
		return new PageImpl<>(Collections.singletonList(annoncesIDE.get(noticeId + userId)));
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
		Assert.assertNotNull("Objet statut introuvable pour le prototype reçu. Le statut à renvoyer pour un proto d'annonce précis doit être configuré au début du test. La comparaison se base sur ProtoAnnonceIDE.Contenu.", statut);
		return statut;
	}

	public void addStatutAnnonceIDEAttentu(BaseAnnonceIDE modele, AnnonceIDEEnvoyee.Statut statut) {
		annoncesIDEValidations.put(modele.getContenu(), statut);
	}

	@Override
	public void ping() throws ServiceEntrepriseException {
		// un mock fonctionne toujours
	}

	protected MockEntrepriseCivile addEntreprise(long cantonalId) {
		final MockEntrepriseCivile ent = new MockEntrepriseCivile(cantonalId);
		addEntreprise(ent);
		return ent;
	}

	protected MockEtablissementCivil addEtablissmeent(MockEntrepriseCivile entreprise, long cantonalId, RegDate dateCreation,
	                                                  MockDonneesRegistreIDE donneesRegistreIDE, MockDonneesRC donneesRC, MockDonneesREE donneesREE) {
		final MockEtablissementCivil etablissement = new MockEtablissementCivil(cantonalId, donneesRegistreIDE, donneesRC, donneesREE);
		entreprise.addDonneesEtablissement(etablissement);
		return etablissement;
	}

	protected void addNumeroIDE(MockEntrepriseCivile entreprise, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		MockEtablissementCivil etablissement = (MockEtablissementCivil) entreprise.getEtablissements().get(0);
		etablissement.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected void addNumeroIDE(MockEtablissementCivil etablissement, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		etablissement.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected MockAdresse addAdresse(MockEntrepriseCivile entreprise, TypeAdresseCivil type, MockRue rue, RegDate dateDebut, @Nullable RegDate dateFin) {
		return addAdresse(entreprise, type, rue, null, rue.getLocalite(), dateDebut, dateFin);
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	protected MockAdresse addAdresse(MockEntrepriseCivile entreprise, TypeAdresseCivil type, @Nullable MockRue rue, @Nullable String complement,
	                                 MockLocalite localite, RegDate debutValidite, @Nullable RegDate finValidite) {
		return addAdresse(entreprise, type, rue, null, complement, localite, debutValidite, finValidite);
	}

	/**
	 * Ajoute une adresse pour la PM spécifiée.
	 */
	protected MockAdresse addAdresse(MockEntrepriseCivile pm, TypeAdresseCivil type, @Nullable MockRue rue, @Nullable String numeroMaison, @Nullable String complement,
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
