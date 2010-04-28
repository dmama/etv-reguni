package ch.vd.uniregctb.common;

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

	@Override
	public void afterPropertiesSet() {

		CommonsHttpInvokerRequestExecutor exec = new CommonsHttpInvokerRequestExecutor();
		exec.setBeanClassLoader(getBeanClassLoader()); // JEC: C'est fait dans la super classe par dÃ©faut, ca sert a quoi???
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

		setHttpInvokerRequestExecutor(exec);
		super.afterPropertiesSet();
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

}
