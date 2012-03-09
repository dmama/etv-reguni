package ch.vd.uniregctb.interfaces.service.rcpers;

import ch.vd.evd0006.v1.Event;
import ch.vd.evd0006.v1.EventIdentification;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class RcPersClientHelperImpl implements RcPersClientHelper {

	private RcPersClient rcPersClient;
	private ServiceInfrastructureService infraService;

	@SuppressWarnings("UnusedDeclaration")
	public void setRcPersClient(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		final Event ref = rcPersClient.getEvent(eventId);
		if (ref != null) {
			final Event.PersonAfterEvent personAfterEvent = ref.getPersonAfterEvent();
			final Individu individu = IndividuRCPers.get(personAfterEvent.getPerson(), personAfterEvent.getRelations(), infraService);
			if (individu != null) {
				final EventIdentification idtf = ref.getIdentification();
				final Long refMessageId = idtf.getReferenceMessageId();
				final RegDate dateEvt = XmlUtils.xmlcal2regdate(idtf.getDate());
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.fromEchCode(idtf.getType());
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.fromEchCode(idtf.getAction());
				return new IndividuApresEvenement(individu, dateEvt, type, action, refMessageId);
			}
		}
		return null;
	}
}
