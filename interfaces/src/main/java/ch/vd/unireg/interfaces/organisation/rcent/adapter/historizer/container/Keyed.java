package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.container;

import java.util.Map;

/**
 * Classe utilitaire qui permet d'associé une clé (= clé d'indexation) à une donnée
 * @param <K> type de la clé
 * @param <D> type de la donnée
 */
public final class Keyed<K, D> implements Map.Entry<K, D> {

	private final K key;
	private final D payload;

	public Keyed(K key, D payload) {
		this.key = key;
		this.payload = payload;
	}

	@Override
	public K getKey() {
		return key;
	}

	@Override
	public D getValue() {
		return payload;
	}

	@Override
	public D setValue(D value) {
		throw new UnsupportedOperationException("setValue");
	}
}
