package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;

public class ServiceOrganisationRCEnt implements ServiceOrganisationRaw {

	private RCEntAdapter adapter;

	public ServiceOrganisationRCEnt(RCEntAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationHistory(noOrganisation);
		if (received == null) {
			return null;
		}
		sanityCheck(noOrganisation, received.getCantonalId());
		return RCEntOrganisationHelper.get(received);
	}

	@Override
	public Long getOrganisationPourSite(Long noOrganisation) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getLocation(noOrganisation);
		if (received == null) {
			return null;
		}
		return received.getCantonalId();
	}

	public void ping() throws ServiceOrganisationException {
		throw new UnsupportedOperationException(); // FIXME: Implement ping operation
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

	public void setAdapter(RCEntAdapter adapter) {
		this.adapter = adapter;
	}

}
