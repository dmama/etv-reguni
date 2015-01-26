package ch.vd.unireg.wsclient.rcpers;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class WebClientPool {

	private String baseUrl;
	private String username;
	private String password;

	private final PooledObjectFactory<WebClient> factory = new BasePooledObjectFactory<WebClient>() {
		@Override
		public WebClient create() throws Exception {
			return WebClient.create(baseUrl, username, password, null);
		}

		@Override
		public PooledObject<WebClient> wrap(WebClient webClient) {
			return new DefaultPooledObject<>(webClient);
		}
	};

	private final GenericObjectPool<WebClient> pool = new GenericObjectPool<>(factory);

	public void init() {
		pool.setMaxTotal(40);
		pool.setBlockWhenExhausted(true);
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

	public void setMaxTotal(int maxTotal) {
		this.pool.setMaxTotal(maxTotal);
	}

	public void setBlockWhenExhausted(boolean block) {
		this.pool.setBlockWhenExhausted(block);
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
			wc = pool.borrowObject();
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
