package ch.vd.unireg.servlet.remoting;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.lang.StringUtils;
import org.springframework.remoting.httpinvoker.CommonsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class BasicAuthHttpInvokerProxyFactoryBean extends HttpInvokerProxyFactoryBean {

	private int readTimeout;
	private String username;
	private String password;
	private Integer maxConnectionsPerHost;
	private CommonsHttpInvokerRequestExecutor executor = new CommonsHttpInvokerRequestExecutor();

	@Override
	public void afterPropertiesSet() {
		initExecutor(executor, readTimeout, username, password, maxConnectionsPerHost, getBeanClassLoader());
		setHttpInvokerRequestExecutor(executor);
		super.afterPropertiesSet();
	}

	public static void initExecutor(CommonsHttpInvokerRequestExecutor exec, int readTimeout, String username, String password, Integer maxConnectionsPerHost, ClassLoader beanClassLoader) {
		exec.setBeanClassLoader(beanClassLoader); // JEC: C'est fait dans la super classe par défaut, ca sert a quoi???
		exec.setReadTimeout(readTimeout);

		if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
			// Credentials
			HttpClient client = exec.getHttpClient();
			client.getParams().setAuthenticationPreemptive(true); // Envoie l'auth avant que le serveur lui demande => Perf
			Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
			client.getState().setCredentials(AuthScope.ANY, defaultcreds);

			// Auth methods
			// This will exclude the DIGETS and NTLM authentication scheme
			List<String> authPrefs = new ArrayList<String>();
			authPrefs.add(AuthPolicy.BASIC);
			client.getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);
		}

		if (maxConnectionsPerHost != null) {
			final HttpClient client = exec.getHttpClient();
			client.getHttpConnectionManager().getParams().setDefaultMaxConnectionsPerHost(maxConnectionsPerHost);
		}
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setMaxConnectionsPerHost(Integer maxConnectionsPerHost) {
		this.maxConnectionsPerHost = maxConnectionsPerHost;
	}

	public void setExecutor(CommonsHttpInvokerRequestExecutor executor) {
		this.executor = executor;
	}
}
