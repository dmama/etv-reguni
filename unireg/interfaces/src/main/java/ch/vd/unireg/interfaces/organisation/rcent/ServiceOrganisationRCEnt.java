package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;

public class ServiceOrganisationRCEnt implements ServiceOrganisationRaw {

	private final RCEntAdapter adapter;
	private final ServiceInfrastructureRaw infraService;

	public ServiceOrganisationRCEnt(RCEntAdapter adapter, ServiceInfrastructureRaw infraService) {
		this.adapter = adapter;
		this.infraService = infraService;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationHistory(noOrganisation);
		if (received == null) {
			return null;
		}
		sanityCheck(noOrganisation, received.getCantonalId());
		return RCEntOrganisationHelper.get(received, infraService);
	}

	@Override
	public Long getOrganisationPourSite(Long noOrganisation) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getLocation(noOrganisation);
		if (received == null) {
			return null;
		}
		return received.getCantonalId();
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		long noOrganisation = 12345678L;
		final Organisation organisation = getOrganisationHistory(noOrganisation);
		if (organisation == null) {
			throw new ServiceOrganisationException(String.format("L'organisation n°%s est introuvable", noOrganisation));
		}
		if (organisation.getNumeroOrganisation() != noOrganisation) {
			throw new ServiceOrganisationException(String.format("Demandé l'organisation n°%s, reçu l'organisation n°%s!", noOrganisation, organisation.getNumeroOrganisation()));
		}
	}

	private void sanityCheck(long noOrganisation, long receivedId) {
		if (receivedId != noOrganisation) {
			throw new IllegalStateException(
					String.format("Incohérence des données retournées détectée: organisation demandée = %d, organisation retournée = %d. " +
							              "Verifiez que le numéro soumis est bien l'identifiant cantonal d'une organisation et non " +
							              "celui d'un site.",
					              noOrganisation,
					              receivedId));
		}
	}
}
