package ch.vd.uniregctb.evenement.reqdes;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.reqdes.engine.EvenementReqDesProcessor;

public class MockReqDesEventProcessor implements EvenementReqDesProcessor, InitializingBean {

	private final List<Long> collectedUniteTraitementIds = new LinkedList<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		collectedUniteTraitementIds.clear();
	}

	@Override
	public void postUniteTraitement(long id) {
		collectedUniteTraitementIds.add(id);
	}

	@Override
	public ListenerHandle registerListener(Listener listener) {
		throw new NotImplementedException();
	}

	@Override
	public void unregisterListener(ListenerHandle handle) {
		throw new NotImplementedException();
	}

	/**
	 * @return le contenu de la collection des IDs collectés (et vide la collection pour les futures collectes)
	 */
	public List<Long> drainCollectedUniteTraitementIds() {
		final List<Long> result = new ArrayList<>(collectedUniteTraitementIds);
		collectedUniteTraitementIds.clear();
		return result;
	}

	public boolean hasCollectedIds() {
		return !collectedUniteTraitementIds.isEmpty();
	}
}
