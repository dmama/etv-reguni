package ch.vd.uniregctb.cache;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.NotImplementedException;

public class UniregCacheManagerImpl implements UniregCacheManager, DynamicMBean {

	private final Logger LOGGER = Logger.getLogger(UniregCacheManagerImpl.class);

	private final Map<String, UniregCacheInterface> map = new HashMap<String, UniregCacheInterface>();

	public UniregCacheInterface getCache(String name) {
		return map.get(name);
	}

	public Collection<UniregCacheInterface> getCaches() {
		return map.values();
	}

	public void register(UniregCacheInterface cache) {
		Assert.isFalse(map.containsKey(cache.getName()));
		map.put(cache.getName(), cache);
	}

	public void unregister(UniregCacheInterface cache) {
		map.remove(cache);
	}

	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		final UniregCacheInterface cache = map.get(attribute);
		if (cache == null) {
			throw new AttributeNotFoundException();
		}

		return cache.buildStats().toString();
	}

	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new NotImplementedException();
	}

	public AttributeList getAttributes(String[] attributes) {
		AttributeList list = new AttributeList(attributes.length);
		for (String a : attributes) {
			final UniregCacheInterface cache = map.get(a);
			list.add(new Attribute(a, cache.buildStats()));
		}
		return list;
	}

	public AttributeList setAttributes(AttributeList attributes) {
		throw new NotImplementedException();
	}

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
			else {
				throw new NoSuchMethodException(actionName);
			}
		}
		catch (NoSuchMethodException e) {
			throw new ReflectionException(e);
		}
	}

	public MBeanInfo getMBeanInfo() {

		// Récupère la liste des caches, et on la trie pour éviter que l'ordre change entre deux appels
		final List<UniregCacheInterface> caches = new ArrayList<UniregCacheInterface>(map.values());
		Collections.sort(caches, new Comparator<UniregCacheInterface>() {
			public int compare(UniregCacheInterface o1, UniregCacheInterface o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		final MBeanAttributeInfo[] atts = new MBeanAttributeInfo[caches.size()];
		final MBeanOperationInfo[] ops = new MBeanOperationInfo[caches.size() + 1];

		ops[0] = new MBeanOperationInfo("resetALL", "Vide et réinitialise tous les cache pour retrouver leurs états tel qu'au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION);

		// Pour chacun des cache, on créé un attribut virtuel qui expose les statistiques du cache, ainsi qu'une méthode virtuelle qui permet de resetter le cache
		for (int i = 0, cachesSize = caches.size(); i < cachesSize; i++) {
			final UniregCacheInterface c = caches.get(i);
			atts[i] = new MBeanAttributeInfo(c.getName(), c.getClass().getName(), c.getDescription(), true, false, false);
			ops[i + 1] = new MBeanOperationInfo("reset" + c.getName(), "Vide et réinitialise le cache pour retrouver son état au démarrage de l'application", null, "void", MBeanOperationInfo.ACTION);
		}

		return new MBeanInfo(getClass().getName(), "Cache Manager d'Unireg", atts, null, ops, null);
	}
}
