package ch.vd.uniregctb.evenement.party;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.technical.esb.util.EsbUrlDataHandler;
import ch.vd.unireg.xml.event.party.v1.Response;

public class RequestHandlerResult {

	private Response response;
	private Map<String, EsbDataHandler> attachments = new HashMap<String, EsbDataHandler>();

	public RequestHandlerResult() {
	}

	public RequestHandlerResult(Response response) {
		this.response = response;
	}

	public RequestHandlerResult(Response response, Map<String, EsbDataHandler> attachments) {
		this.response = response;
		this.attachments = attachments;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public void addAttachment(String name, EsbDataHandler attachement) {
		this.attachments.put(name, attachement);
	}

	public void addAttachment(String name, byte[] attachement) {
		addAttachment(name, new ByteArrayInputStream(attachement));
	}

	public void addAttachment(String name, InputStream attachement) {
		addAttachment(name, new EsbUrlDataHandler(attachement));
	}

	public Map<String, EsbDataHandler> getAttachments() {
		return attachments;
	}
}
