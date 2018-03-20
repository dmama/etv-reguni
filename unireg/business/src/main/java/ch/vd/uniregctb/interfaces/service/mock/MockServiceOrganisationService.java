package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHisto;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;

/**
 * Mock du service organisation qui par défaut ne fait rien et retourne toujours null.
 */
public class MockServiceOrganisationService implements ServiceOrganisationService {
	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		return null;
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		return null;
	}

	@Override
	public ServiceOrganisationRaw.Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		return null;
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		return null;
	}

	@Override
	public AdressesCivilesHisto getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
		return null;
	}

	@Nullable
	@Override
	public AdressesCivilesHisto getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException {
		return null;
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(long numero) {
		return null;
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws
			ServiceOrganisationException {
		return null;
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
		return null;
	}

	@NotNull
	@Override
	public String createOrganisationDescription(Organisation organisation, RegDate date) {
		return null;
	}

	@Override
	public String afficheAttributsSite(@Nullable SiteOrganisation site, @Nullable RegDate date) {
		return null;
	}
}
