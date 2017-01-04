package ch.vd.uniregctb.interfaces.service.mock;

import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationServiceWrapper;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.uniregctb.interfaces.model.AdressesCivilesHistoriques;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationImpl;
import ch.vd.uniregctb.interfaces.service.ServiceOrganisationService;

/**
 * Proxy du service organisation à enregistrer dans l'application context et permettant à chaque test unitaire de spécifier précisemment l'instance
 * du service organisation à utiliser.
 */
public class ProxyServiceOrganisation implements ServiceOrganisationService, ServiceOrganisationServiceWrapper {

	private ServiceOrganisationRaw target;
	private final ServiceOrganisationImpl service;

	public ProxyServiceOrganisation(ServiceInfrastructureService serviceInfra) {
		this.service = new ServiceOrganisationImpl(serviceInfra);
	}

	public void setUp(ServiceOrganisationRaw target) {
		this.target = target;
		this.service.setTarget(target);
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getOrganisationHistory(noOrganisation);
	}

	@Override
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getOrganisationEvent(noEvenement);
	}

	@Override
	public ServiceOrganisationRaw.Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getOrganisationByNoIde(noide);
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getOrganisationPourSite(noSite);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getAdressesOrganisationHisto(noOrganisation);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesSiteOrganisationHisto(long noSite) throws ServiceOrganisationException {
		return service.getAdressesSiteOrganisationHisto(noSite);
	}

	@Nullable
	@Override
	public AnnonceIDE getAnnonceIDE(Long numero) {
		return target.getAnnonceIDE(numero);
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
		return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE annonceIDE) {
		return target.validerAnnonceIDE(annonceIDE);
	}

	@NotNull
	@Override
	public String createOrganisationDescription(Organisation organisation, RegDate date) {
		return service.createOrganisationDescription(organisation, date);
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Le service organisation n'a pas été défini !");
	}

	@Override
	public ServiceOrganisationRaw getTarget() {
		return target;
	}

	@Override
	public ServiceOrganisationRaw getUltimateTarget() {
		if (target instanceof ServiceOrganisationServiceWrapper) {
			return ((ServiceOrganisationServiceWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}
}
