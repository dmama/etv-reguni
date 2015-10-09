package ch.vd.uniregctb.interfaces.service.mock;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationServiceWrapper;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
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
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getOrganisationPourSite(noSite);
	}

	@Override
	public AdressesCivilesHistoriques getAdressesOrganisationHisto(long noOrganisation) throws ServiceOrganisationException {
		assertTargetNotNull();
		return service.getAdressesOrganisationHisto(noOrganisation);
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
