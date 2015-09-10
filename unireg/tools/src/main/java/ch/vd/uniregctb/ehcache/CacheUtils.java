package ch.vd.uniregctb.ehcache;

import java.io.InputStream;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import ch.vd.unireg.xml.party.v3.Party;

public class CacheUtils {
	private static final CacheManager cacheManager;
	private Ehcache uniregCache;
	static {

		ClassLoader contextClassLoader = Thread.currentThread()
				.getContextClassLoader();
		InputStream resourceAsStream = contextClassLoader
				.getResourceAsStream("ehcache.xml");
		cacheManager = CacheManager.create(resourceAsStream);
	}

	public CacheUtils() {
		uniregCache = cacheManager.getEhcache("webService5");
	}

	/**
	 *
	 * @param
	 * @param tiers
	 */
	public void addTiers(Party tiers) {
		final KeyGetParty key = new KeyGetParty(tiers.getNumber());
		Element element = uniregCache.get(key);
		if (element == null) {
			element = new Element(key, tiers);
			uniregCache.put(element);
		}

	}

	public void evictTiers(int numero) {
		final List<?> keys = uniregCache.getKeys();
		for (Object k : keys) {
			if (k instanceof KeyGetParty && ((KeyGetParty) k).id == numero) {
				uniregCache.remove(k);
			}
		}
	}

	/**
	 * Recuperer un tiers
	 *@param id
	 * @return
	 */
	public Party getTiersFromCache(int id) {
		Element element = uniregCache.get(new KeyGetParty(id));
		if (element != null) {
			return (Party) element.getObjectValue();
		}
		return null;
	}

	public boolean isEmpty(){
		return uniregCache.getSize() == 0;
	}

	private static class KeyGetParty {
		final int id;

		public KeyGetParty(Party p) {
			this.id = p.getNumber();
		}

		private KeyGetParty(int id) {
			this.id = id;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			KeyGetParty other = (KeyGetParty) obj;
			return id == other.id;
		}

		@Override
		public String toString() {
			return "KeyGetParty{" +
					"id=" + id +
					'}';
		}
	}
}
