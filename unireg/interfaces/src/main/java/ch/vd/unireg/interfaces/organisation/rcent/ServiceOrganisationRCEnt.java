package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.registre.base.date.RegDate;
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
	public Organisation getOrganisation(long cantonalId) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisation(cantonalId);
		if (received == null) {
			return null;
		}
		sanityCheck(cantonalId, received.getCantonalId());
		return RCEntOrganisationHelper.get(received);
	}

	@Override
	public Organisation getOrganisation(long cantonalId, RegDate date) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisation(cantonalId, date);
		if (received == null) {
			return null;
		}
		sanityCheck(cantonalId, received.getCantonalId());
		return RCEntOrganisationHelper.get(received);
	}

	@Override
	public Organisation getOrganisationHistory(long cantonalId) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationHistory(cantonalId);
		if (received == null) {
			return null;
		}
		sanityCheck(cantonalId, received.getCantonalId());
		return RCEntOrganisationHelper.get(received);
	}

	@Override
	public Long getOrganisationPourSite(Long cantonalId) throws ServiceOrganisationException {
		ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getLocation(cantonalId);
		if (received == null) {
			return null;
		}
		return received.getCantonalId();
	}

	private void sanityCheck(long cantonalId, long receivedId) {
		if (receivedId != cantonalId) {
			throw new IllegalStateException(
					String.format("Incohérence des données retournées détectée: organisation demandée = %d, organisation retournée = %d.",
					              cantonalId,
					              receivedId));
		}
	}

	public void setAdapter(RCEntAdapter adapter) {
		this.adapter = adapter;
	}

}
