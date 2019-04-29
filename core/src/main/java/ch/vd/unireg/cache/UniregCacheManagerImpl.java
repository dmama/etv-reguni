package ch.vd.unireg.cache;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.utils.LogLevel;

public class UniregCacheManagerImpl implements UniregCacheManager, DynamicMBean {

	private final Logger LOGGER = LoggerFactory.getLogger(UniregCacheManagerImpl.class);

	private final Map<String, UniregCacheInterface> map = new HashMap<>();

	@Override
	public UniregCacheInterface getCache(String name) {
		return map.get(name);
	}

	@Override
	public Collection<UniregCacheInterface> getCaches() {
		return map.values();
	}

	@Override
	public void register(UniregCacheInterface cache) {
		if (map.containsKey(cache.getName())) {
			throw new IllegalArgumentException("Cache " + cache.getName() + " déjà enregistré");
		}
		map.put(cache.getName(), cache);
	}

	@Override
	public void unregister(UniregCacheInterface cache) {
		map.remove(cache.getName());
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		final UniregCacheInterface cache = map.get(attribute);
		if (cache == null) {
			throw new AttributeNotFoundException();
		}

		return cache.buildStats().toString();
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new NotImplementedException("");
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		AttributeList list = new AttributeList(attributes.length);
		for (String a : attributes) {
			final UniregCacheInterface cache = map.get(a);
			list.add(new Attribute(a, cache.buildStats()));
		}
		return list;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		throw new NotImplementedException("");
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		try {
			if (actionName.startsWith("reset")) {
				final String cacheName = actionName.substring(5);
				if (cacheName.equals("ALL")) {
					for (UniregCacheInterface cache : map.values()) {
						cache.reset();
					}
					LOGGER.info("Tous les caches ont été resettés par JMX.");
				}
				else {
					final UniregCacheInterface cache = map.get(cacheName);
					if (cache == null) {
						throw new NoSuchMethodException(actionName);
					}
					cache.reset();
					LOGGER.info("Le cache " + cacheName + " a été resetté par JMX.");
				}
				return null;
			}
			else if (actionName.startsWith("dumpKeys")) {
				final String cacheName = actionName.substring(8);
				final UniregCacheInterface cache = map.get(cacheName);
				if (!(cache instanceof KeyDumpableCache)) {
					throw new NoSuchMethodException(actionName);
				}
				((KeyDumpableCache) cache).dumpCacheKeys(LOGGER, LogLevel.Level.INFO);
				return null;
			}
			else if (actionName.startsWith("dumpValues")) {
				final String cacheName = actionName.substring(10);
				final UniregCacheInterface cache = map.get(cacheName);
				if (!(cache instanceof KeyValueDumpableCache)) {
					throw new NoSuchMethodException(actionName);
				}
				((KeyValueDumpableCache) cache).dumpCacheContent(LOGGER, LogLevel.Level.INFO);
				return null;
			}
			else {
				throw new NoSuchMethodException(actionName);
			}
		}
		catch (NoSuchMethodException e) {
			throw new ReflectionException(e);
		}
	}

	@Override
	public MBeanInfo getMBeanInfo() {

		// Récupère la liste des caches, et on la trie pour éviter que l'ordre change entre deux appels
		final List<UniregCacheInterface> caches = new ArrayList<>(map.values());
		caches.sort(new Comparator<UniregCacheInterface>() {
			@Override
			public int compare(UniregCacheInterface o1, UniregCacheInterface o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		final MBeanAttributeInfo[] atts = new MBeanAttributeInfo[caches.size()];

		final List<MBeanOperationInfo> resets = new ArrayList<>(caches.size() + 1);
		final List<MBeanOperationInfo> dumps = new ArrayList<>(2 * caches.size());

		// Pour chacun des cache, on créé un attribut virtuel qui expose les statistiques du cache, ainsi qu'une méthode virtuelle qui permet de resetter le cache
		resets.add(new MBeanOperationInfo("resetALL", "Vide et réinitialise tous les cache pour retrouver leurs états tel qu'au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION));
		for (int i = 0, cachesSize = caches.size(); i < cachesSize; i++) {
			final UniregCacheInterface c = caches.get(i);
			atts[i] = new MBeanAttributeInfo(c.getName(), c.getClass().getName(), c.getDescription(), true, false, false);
			resets.add(new MBeanOperationInfo("reset" + c.getName(), "Vide et réinitialise le cache pour retrouver son état au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION));
			if (c instanceof KeyDumpableCache) {
				dumps.add(new MBeanOperationInfo("dumpKeys" + c.getName(), "Produit une liste des clés du cache dans les logs applicatifs", null, "void", MBeanOperationInfo.ACTION));
			}
			if (c instanceof KeyValueDumpableCache) {
				dumps.add(new MBeanOperationInfo("dumpValues" + c.getName(), "Produit une liste des clés/valeurs du cache dans les logs applicatifs", null, "void", MBeanOperationInfo.ACTION));
			}
		}

		final List<MBeanOperationInfo> allOps = new ArrayList<>(resets.size() + dumps.size());
		allOps.addAll(resets);
		allOps.addAll(dumps);
		final MBeanOperationInfo ops[] = allOps.toArray(new MBeanOperationInfo[0]);
		return new MBeanInfo(getClass().getName(), "Cache Manager d'Unireg", atts, null, ops, null);
	}
}
