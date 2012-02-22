package ch.vd.uniregctb.interfaces.service.rcpers;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.interfaces.model.Individu;

public class ProxyRcPersClientHelper implements RcPersClientHelper {
	
	private RcPersClientHelper target;

	public void setup(RcPersClientHelper target) {
		this.target = target;
	}

	public void tearDown() {
		this.target = null;
	}

	private void assertTargetNotNull() {
		Assert.notNull(target, "Helper du ClientRcPers non défini !");
	}

	@Override
	public Individu getIndividuFromEvent(long eventId) {
		assertTargetNotNull();
		return target.getIndividuFromEvent(eventId);
	}
}
