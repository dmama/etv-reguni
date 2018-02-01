package ch.vd.unireg.xml;

import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;

public class ServiceException extends Exception {

	private final ServiceExceptionInfo info;

	public ServiceException(ServiceExceptionInfo info) {
		super(info.getMessage());
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
