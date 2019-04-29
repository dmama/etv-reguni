package ch.vd.unireg.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;

public class MockEvenementReqDesProcessor implements EvenementReqDesProcessor, InitializingBean {

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
	public void postUnitesTraitement(Collection<Long> ids) {
		collectedUniteTraitementIds.addAll(ids);
	}

	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		throw new NotImplementedException("");
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
