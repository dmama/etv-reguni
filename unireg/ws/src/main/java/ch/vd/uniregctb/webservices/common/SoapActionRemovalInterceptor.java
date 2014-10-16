package ch.vd.uniregctb.webservices.common;

import java.util.List;
import java.util.Map;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * Quand un web-service SOAP attend un header SOAPAction vide ("") mais que certaines applications envoient
 * tout de même une valeur, cet intercepteur permet <i>d'effacer</i> la valeur présentée au web-service.
 * <p/>
 * Cette classe peut être utilisée par nom dans les annotations CXF {@link org.apache.cxf.interceptor.InInterceptors}.
 */
@SuppressWarnings("UnusedDeclaration")
public class SoapActionRemovalInterceptor extends AbstractSoapInterceptor {

	private static final String FORCED_SOAP_ACTION_VALUE = "\"\"";

	public SoapActionRemovalInterceptor() {
		super(Phase.READ);
		addAfter(ReadHeadersInterceptor.class.getName());       // il faut que les headers aient été lus ...
		addBefore(SoapActionInInterceptor.class.getName());     // ... mais que le traitement de la valeur du champ SOAPAction n'ait pas encore commencé
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault {
		final Map<String, List<String>> headers = CastUtils.cast((Map) message.get(Message.PROTOCOL_HEADERS));
		if (headers != null) {
			final List<String> sa = headers.get(SoapBindingConstants.SOAP_ACTION);
			if (sa != null) {
				if (sa.size() > 1 || (sa.size() == 1 && !FORCED_SOAP_ACTION_VALUE.equals(sa.get(0)))) {
					sa.clear();
					sa.add(FORCED_SOAP_ACTION_VALUE);
				}
			}
		}
	}
}
