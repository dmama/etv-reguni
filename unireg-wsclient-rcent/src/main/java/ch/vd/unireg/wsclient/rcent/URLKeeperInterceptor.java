package ch.vd.unireg.wsclient.rcent;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Intercepteur CXF qui mémorise la dernière adresse appelée dans un thread-local. Ce qui permet de logger l'appel effectif ou de l'ajouter dans le message d'erreur si l'appel a retourné une erreur.
 */
public class URLKeeperInterceptor extends AbstractPhaseInterceptor<Message> {

	private static ThreadLocal<String> lastUrl = new ThreadLocal<String>();

	public URLKeeperInterceptor() {
		super(Phase.PRE_STREAM);
	}

	@Override
	public void handleMessage(Message message) throws Fault {
		String address = (String) message.get(Message.ENDPOINT_ADDRESS);
		lastUrl.set(address);
	}

	public static String getLastUrl() {
		return lastUrl.get();
	}
}
