package ch.vd.uniregctb.xml;

import ch.vd.unireg.xml.exception.ServiceExceptionInfo;

public class ServiceException extends Exception {

	private ServiceExceptionInfo info;

	public ServiceException(ServiceExceptionInfo info) {
		this.info = info;
	}

	public ServiceException(String message, ServiceExceptionInfo info) {
		super(message);
		this.info = info;
	}

	public ServiceException(String message, Throwable cause, ServiceExceptionInfo info) {
		super(message, cause);
		this.info = info;
	}

	public ServiceException(Throwable cause, ServiceExceptionInfo info) {
		super(cause);
		this.info = info;
	}

	public ServiceExceptionInfo getInfo() {
		return info;
	}
}
