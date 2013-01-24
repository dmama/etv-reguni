package ch.vd.unireg.wsclient.rcpers;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class WebClientPool {

	private String baseUrl;
	private String username;
	private String password;

	private final PoolableObjectFactory factory = new PoolableObjectFactory() {
		@Override
		public Object makeObject() throws Exception {
			return WebClient.create(baseUrl, username, password, null);
		}

		@Override
		public void destroyObject(Object o) throws Exception {
			// nothing to do
		}

		@Override
		public boolean validateObject(Object o) {
			return o instanceof WebClient;
		}

		@Override
		public void activateObject(Object o) throws Exception {
			// nothing to do
		}

		@Override
		public void passivateObject(Object o) throws Exception {
			// nothing to do
		}
	};
	private final GenericObjectPool pool = new GenericObjectPool();

	public void init() {
		pool.setFactory(factory);
		pool.setMaxActive(40);
		pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
		pool.setMaxIdle(10);
		pool.setMinIdle(0);
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMaxActive(int maxActive) {
		this.pool.setMaxActive(maxActive);
	}

	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		this.pool.setWhenExhaustedAction(whenExhaustedAction);
	}

	public void setMaxIdle(int maxIdle) {
		this.pool.setMaxIdle(maxIdle);
	}

	public void setMinIdle(int minIdle) {
		this.pool.setMinIdle(minIdle);
	}

	public WebClient borrowClient(int receiveTimeout) {
		final WebClient wc;
		try {
			wc = (WebClient) pool.borrowObject();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		// set the timeout
		wc.reset();
		final HTTPConduit conduit = (HTTPConduit) WebClient.getConfig(wc).getConduit();
		conduit.getClient().setReceiveTimeout(receiveTimeout);

		return wc;
	}

	public void returnClient(WebClient obj) {
		try {
			pool.returnObject(obj);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
