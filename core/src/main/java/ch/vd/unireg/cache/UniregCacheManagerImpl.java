package ch.vd.unireg.cache;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.utils.LogLevel;

public class UniregCacheManagerImpl implements UniregCacheManager, DynamicMBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(UniregCacheManagerImpl.class);

	@NotNull
	private final Map<String, UniregCacheInterface> map;

	public UniregCacheManagerImpl(@NotNull Map<String, UniregCacheInterface> map) {
		this.map = Collections.unmodifiableMap(map);
	}

	@Override
	public UniregCacheInterface getCache(@NotNull String name) {
		return map.get(name);
	}

	@NotNull
	public Map<String, UniregCacheInterface> getCaches() {
		return map;
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException {
		final UniregCacheInterface cache = map.get(attribute);
		if (cache == null) {
			throw new AttributeNotFoundException();
		}

		return cache.buildStats().toString();
	}

	@Override
	public void setAttribute(Attribute attribute) {
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
	public Object invoke(String actionName, Object[] params, String[] signature) throws ReflectionException {
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
		final List<Map.Entry<String, UniregCacheInterface>> caches = map.entrySet()
				.stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.collect(Collectors.toList());

		final MBeanAttributeInfo[] atts = new MBeanAttributeInfo[caches.size()];

		final List<MBeanOperationInfo> resets = new ArrayList<>(caches.size() + 1);
		final List<MBeanOperationInfo> dumps = new ArrayList<>(2 * caches.size());

		// Pour chacun des cache, on créé un attribut virtuel qui expose les statistiques du cache, ainsi qu'une méthode virtuelle qui permet de resetter le cache
		resets.add(new MBeanOperationInfo("resetALL", "Vide et réinitialise tous les cache pour retrouver leurs états tel qu'au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION));
		for (int i = 0, cachesSize = caches.size(); i < cachesSize; i++) {
			final Map.Entry<String, UniregCacheInterface> entry = caches.get(i);
			final String cacheName = entry.getKey();
			final UniregCacheInterface cache = entry.getValue();
			atts[i] = new MBeanAttributeInfo(cacheName, cache.getClass().getName(), cache.getDescription(), true, false, false);
			resets.add(new MBeanOperationInfo("reset" + cacheName, "Vide et réinitialise le cache pour retrouver son état au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION));
			if (cache instanceof KeyDumpableCache) {
				dumps.add(new MBeanOperationInfo("dumpKeys" + cacheName, "Produit une liste des clés du cache dans les logs applicatifs", null, "void", MBeanOperationInfo.ACTION));
			}
			if (cache instanceof KeyValueDumpableCache) {
				dumps.add(new MBeanOperationInfo("dumpValues" + cacheName, "Produit une liste des clés/valeurs du cache dans les logs applicatifs", null, "void", MBeanOperationInfo.ACTION));
			}
		}

		final List<MBeanOperationInfo> allOps = new ArrayList<>(resets.size() + dumps.size());
		allOps.addAll(resets);
		allOps.addAll(dumps);
		final MBeanOperationInfo ops[] = allOps.toArray(new MBeanOperationInfo[0]);
		return new MBeanInfo(getClass().getName(), "Cache Manager d'Unireg", atts, null, ops, null);
	}
}
