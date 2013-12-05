package ch.vd.uniregctb.indexer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import ch.vd.uniregctb.indexer.lucene.IndexProvider;

/**
 * Singleton statique du global index, c'est-à-dire qu'il peut-être partagé par plusieurs contextes Spring.
 */
public class StaticSingletonGlobalIndex implements FactoryBean<GlobalIndexInterface>, DisposableBean {

	private static final MutableInt count = new MutableInt(0);
	private static GlobalIndex singleton;

	public StaticSingletonGlobalIndex(IndexProvider provider) throws Exception {
		synchronized (count) {
			if (singleton == null) {
				singleton = new GlobalIndex(provider);
				singleton.afterPropertiesSet();
			}
			count.increment();
		}
	}

	@Override
	public void destroy() throws Exception {
		synchronized (count) {
			count.decrement();
			if (count.intValue() == 0 && singleton != null) {
				singleton.destroy();
				singleton = null;
			}
		}
	}

	@Override
	public GlobalIndexInterface getObject() throws Exception {
		return singleton;
	}

	@Override
	public Class<?> getObjectType() {
		return GlobalIndexInterface.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
