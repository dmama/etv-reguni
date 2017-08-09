package ch.vd.unireg.wsclient.efacture;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

import ch.vd.evd0025.v1.PayerSearchResult;
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
	public PayerWithHistory getHistory(long ctbId, String billerId) {
		final WebClient wc = createWebClient(60000); // 1 minute
		wc.path(historyPath);
		wc.path(billerId);
		wc.path(String.valueOf(ctbId));
		try {
			final PayerSearchResult payerSearchResult =  wc.get(PayerSearchResult.class);
			if (payerSearchResult == null){
				return null;
			}
			return payerSearchResult.getPayerWithHistory();
		}
		catch (WebApplicationException e) {
			if (e.getResponse().getStatus() == 404) {
				return null;
			}
			throw e;
		}
	}

	private WebClient createWebClient(int receiveTimeout) {
		final WebClient wc = WebClient.create(baseUrl, username, password, null);
		final HTTPConduit conduit = (HTTPConduit) WebClient.getConfig(wc).getConduit();
		conduit.getClient().setReceiveTimeout(receiveTimeout);
		return wc;
	}
}
