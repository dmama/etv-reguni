package ch.vd.unireg.wsclient.rcent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.transport.http.HTTPConduit;

import ch.vd.evd0022.v3.OrganisationData;

public class WebClientPool {

	private String baseUrl;
	private String username;
	private String password;

	private boolean enableValidation;
	private List<String> schemasLocations = new ArrayList<>();

	private final PooledObjectFactory<WebClient> factory = new BasePooledObjectFactory<WebClient>() {
		@Override
		public WebClient create() throws Exception {
			JAXBElementProvider<OrganisationData> prov = new JAXBElementProvider<>();

			// Supporter l'absence de @XmlRootElement sur OrganisationData
			prov.setJaxbElementClassMap(Collections.singletonMap(OrganisationData.class.getName(), ""));

			// Supporter la validation du xml entrant
			if (enableValidation) {
				prov.setSchemaLocations(schemasLocations);
			}

			final WebClient client = WebClient.create(baseUrl, Collections.singletonList(prov), null);

			// on ajoute un intercepteur d'URL pour enrichir les messages en cas d'erreur
			final ClientConfiguration config = WebClient.getConfig(client);
			config.getOutInterceptors().add(new URLKeeperInterceptor());

			return client;
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

	public void setEnableValidation(boolean enableValidation) {
		this.enableValidation = enableValidation;
	}

	public void setSchemasLocations(List<String> schemasLocations) {
		this.schemasLocations = schemasLocations;
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
		AuthorizationPolicy authorization = new AuthorizationPolicy();
		authorization.setUserName(username);
		authorization.setPassword(password);
		conduit.setAuthorization(authorization);
		conduit.getClient().setAutoRedirect(true);
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
