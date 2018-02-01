package ch.vd.unireg.indexer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import ch.vd.unireg.indexer.lucene.IndexProvider;

/**
 * Singleton statique du global index, c'est-à-dire qu'il peut-être partagé par plusieurs contextes Spring.
 */
public class StaticSingletonGlobalIndex implements FactoryBean<GlobalIndexInterface>, DisposableBean {

	private final IndexerReference reference;
	private final GlobalIndexInterface singleton;

	private static final Map<String, IndexerReference> SINGLETONS = new HashMap<>();

	private static class IndexerReference {
		private int count;
		private GlobalIndex index;

		private IndexerReference() {
			this.count = 0;
			this.index = null;
		}

		public synchronized GlobalIndex acquireReference(IndexProvider provider) throws Exception {
			if (index == null) {
				index = new GlobalIndex(provider);
				index.afterPropertiesSet();
			}
			++ count;
			return index;
		}

		public synchronized void releaseReference() throws Exception {
			-- count;
			if (count == 0 && index != null) {
				index.destroy();
				index = null;
			}
		}
	}

	public StaticSingletonGlobalIndex(String name, IndexProvider provider) throws Exception {
		synchronized (SINGLETONS) {
			if (SINGLETONS.containsKey(name)) {
				reference = SINGLETONS.get(name);
			}
			else {
				reference = new IndexerReference();
				SINGLETONS.put(name, reference);
			}
		}
		this.singleton = reference.acquireReference(provider);
	}

	@Override
	public void destroy() throws Exception {
		this.reference.releaseReference();
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
