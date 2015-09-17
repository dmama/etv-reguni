package ch.vd.unireg.interfaces.organisation.mock;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.DonneesRC;
import ch.vd.unireg.interfaces.organisation.data.DonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;

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
		throw new UnsupportedOperationException();
	}

	protected void addOrganisation(MockOrganisation organisation) {
		organisationMap.put(organisation.getNumeroOrganisation(), organisation);
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		// un mock fonctionne toujours
	}

	protected MockOrganisation addOrganisation(long cantonalId, RegDate dateCreation, String nom, FormeLegale formeLegale) {
		final MockOrganisation org = new MockOrganisation(cantonalId, dateCreation, nom, formeLegale);
		addOrganisation(org);
		return org;
	}

	protected MockSiteOrganisation addSite(MockOrganisation organisation, long cantonalId, RegDate dateCreation,
	                                       DonneesRegistreIDE donneesRegistreIDE, DonneesRC donneesRC) {
		final MockSiteOrganisation site = new MockSiteOrganisation(cantonalId, donneesRegistreIDE, donneesRC);
		organisation.addSiteId(dateCreation, null, cantonalId);
		organisation.addDonneesSite(site);
		return site;
	}

	protected void addNumeroIDE(MockOrganisation organisation, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		organisation.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}

	protected void addNumeroIDE(MockSiteOrganisation site, String numeroIDE, RegDate dateDebut, RegDate dateFin) {
		site.addNumeroIDE(dateDebut, dateFin, numeroIDE);
	}
}
