package ch.vd.unireg.interfaces.organisation.mock;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.uniregctb.type.TypeAdresseCivil;

public abstract class MockServiceOrganisation implements ServiceOrganisationRaw {

	/**
	 * Map des organisations par numéro
	 */
	private final Map<Long, MockOrganisation> organisationMap = new HashMap<>();

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
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		for (Map.Entry<Long, MockOrganisation> organisation : organisationMap.entrySet()) {
			for (SiteOrganisation site : organisation.getValue().getDonneesSites()) {
				if (site.getNumeroSite() == noSite) {
					return organisation.getKey();
				}
			}
		}
		return null;
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		for (Map.Entry<Long, MockOrganisation> organisation : organisationMap.entrySet()) {
			for (SiteOrganisation site : organisation.getValue().getDonneesSites()) {
				if (site.getNumeroIDE() != null) {
					for (DateRanged<String> candidat : site.getNumeroIDE()) {
						if (noide != null && noide.equals(candidat.getPayload())) {
							return new Identifiers(organisation.getKey(), site.getNumeroSite());
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

	@Override
	public void ping() throws ServiceOrganisationException {
		// un mock fonctionne toujours
	}

	protected MockOrganisation addOrganisation(long cantonalId) {
		final MockOrganisation org = new MockOrganisation(cantonalId);
		addOrganisation(org);
		return org;
	}

	protected MockSiteOrganisation addSite(MockOrganisation organisation, long cantonalId, RegDate dateCreation,
	                                       MockDonneesRegistreIDE donneesRegistreIDE, MockDonneesRC donneesRC) {
		final MockSiteOrganisation site = new MockSiteOrganisation(cantonalId, donneesRegistreIDE, donneesRC);
		organisation.addDonneesSite(site);
		return site;
	}

	protected void addNumeroIDE(MockOrganisation organisation, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		MockSiteOrganisation site = (MockSiteOrganisation) organisation.getDonneesSites().get(0);
		site.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected void addNumeroIDE(MockSiteOrganisation site, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		site.addNumeroIDE(dateDebut, dateFin, numeroIDE);
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
