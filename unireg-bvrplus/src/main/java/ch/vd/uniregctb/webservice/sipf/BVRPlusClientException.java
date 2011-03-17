package ch.vd.uniregctb.webservice.sipf;

/**
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class BVRPlusClientException extends RuntimeException {

	public BVRPlusClientException() {
	}

	public BVRPlusClientException(String message) {
		super(message);
	}

	public BVRPlusClientException(String message, Throwable cause) {
		super(message, cause);
	}

	public BVRPlusClientException(Throwable cause) {
		super(cause);
	}
}
