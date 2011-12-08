package ch.vd.uniregctb.webservices.batch;

import javax.xml.ws.WebFault;

@WebFault(name="ServiceFault" )
public class BatchWSException extends Exception {

	private static final long serialVersionUID = -8102L;

	public BatchWSException(String message) {
		super(message);
	}

	public BatchWSException(String message, Exception e) {
		super(message + " : " + e.getMessage());
	}

}
