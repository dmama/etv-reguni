package ch.vd.unireg.wsclient.efacture;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import ch.vd.evd0025.v1.PayerWithHistory;

@SuppressWarnings("UnusedDeclaration")
public class EFactureClientImpl implements EFactureClient {

	private String baseUrl;
	private String username;
	private String password;
	private String historyPath;

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setHistoryPath(String historyPath) {
		this.historyPath = historyPath;
	}

	@Override
	public PayerWithHistory getHistory(long ctbId) {
		final WebClient wc = createWebClient(600000); // 10 minutes
		wc.path(historyPath);
		wc.path(String.valueOf(ctbId));
		return wc.get(PayerWithHistory.class);
	}

	private WebClient createWebClient(int receiveTimeout) {
		final WebClient wc = WebClient.create(baseUrl, username, password, null);
		final HTTPConduit conduit = (HTTPConduit) WebClient.getConfig(wc).getConduit();
		conduit.getClient().setReceiveTimeout(receiveTimeout);
		return wc;
	}
}
