package ch.vd.unireg.jms;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;

import org.w3c.dom.Document;

import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.OperationType;
import ch.vd.technical.esb.util.EsbDataHandler;
import ch.vd.unireg.stats.ServiceTracingRecorder;

/**
 * Façade qui trace certains appels (<i>a priori</i> consomateurs) sur l'EsbMessage
 */
public class EsbMessageTracingFacade implements EsbMessage, EsbMessageWrapper {

	private final EsbMessage target;
	private final ServiceTracingRecorder recorder;

	public EsbMessageTracingFacade(EsbMessage target, ServiceTracingRecorder recorder) {
		this.target = target;
		this.recorder = recorder;
	}

	@Override
	public EsbMessage getTarget() {
		return target;
	}

	@Override
	public EsbMessage getUltimateTarget() {
		if (target instanceof EsbMessageWrapper) {
			return ((EsbMessageWrapper) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}

	@Override
	public String getMessageId() {
		return target.getMessageId();
	}

	@Override
	public Date getMessageCreationDate() {
		return target.getMessageCreationDate();
	}

	@Override
	public Date getMessageSendDate() {
		return target.getMessageSendDate();
	}

	@Override
	public Date getMessageReceiveDate() {
		return target.getMessageReceiveDate();
	}

	@Override
	public String getServiceDestination() {
		return target.getServiceDestination();
	}

	@Override
	public void setServiceDestination(String serviceDestination) {
		target.setServiceDestination(serviceDestination);
	}

	@Override
	public String getServiceReplyTo() {
		return target.getServiceReplyTo();
	}

	@Override
	public void setServiceReplyTo(String serviceReplyTo) {
		target.setServiceReplyTo(serviceReplyTo);
	}

	@Override
	public String getBusinessUser() {
		return target.getBusinessUser();
	}

	@Override
	public void setBusinessUser(String businessUser) {
		target.setBusinessUser(businessUser);
	}

	@Override
	public String getBusinessId() {
		return target.getBusinessId();
	}

	@Override
	public void setBusinessId(String businessId) {
		target.setBusinessId(businessId);
	}

	@Override
	public String getBusinessCorrelationId() {
		return target.getBusinessCorrelationId();
	}

	@Override
	public void setBusinessCorrelationId(String businessCorrelationId) {
		target.setBusinessCorrelationId(businessCorrelationId);
	}

	@Override
	public String getDomain() {
		return target.getDomain();
	}

	@Override
	public String getApplication() {
		return target.getApplication();
	}

	@Override
	public String getContext() {
		return target.getContext();
	}

	@Override
	public void setContext(String context) {
		target.setContext(context);
	}

	@Override
	public void setOperation(OperationType operation) {
		target.setOperation(operation);
	}

	@Override
	public OperationType getOperation() {
		return target.getOperation();
	}

	@Override
	public void setProcessDefinitionId(String processDefinitionId) {
		target.setProcessDefinitionId(processDefinitionId);
	}

	@Override
	public String getProcessDefinitionId() {
		return target.getProcessDefinitionId();
	}

	@Override
	public void setProcessInstanceId(String processInstanceId) {
		target.setProcessInstanceId(processInstanceId);
	}

	@Override
	public String getProcessInstanceId() {
		return target.getProcessInstanceId();
	}

	@Override
	public Source getBodyAsSource() {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsSource();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsSource", null);
		}
	}

	@Override
	public Document getBodyAsDocument() throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsDocument();
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsDocument", null);
		}
	}

	@Override
	public byte[] getBodyAsByteArray() throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsByteArray();
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsByteArray", null);
		}
	}

	@Override
	public byte[] getBodyAsByteArray(String encoding) throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsByteArray(encoding);
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsByteArray", () -> String.format("encoding=%s", encoding));
		}
	}

	@Override
	public String getBodyAsString() throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsString();
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsString", null);
		}
	}

	@Override
	public String getBodyAsString(String encoding) throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			return target.getBodyAsString(encoding);
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "getBodyAsString", () -> String.format("encoding=%s", encoding));
		}
	}

	@Override
	public ErrorType getErrorType() throws Exception {
		return target.getErrorType();
	}

	@Override
	public String getErrorCode() throws Exception {
		return target.getErrorCode();
	}

	@Override
	public void bodyToOutputStream(OutputStream out) throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			target.bodyToOutputStream(out);
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "bodyToOutputStream", null);
		}
	}

	@Override
	public void bodyToOutputStream(OutputStream out, String encoding) throws Exception {
		Throwable t = null;
		final long start = recorder.start();
		try {
			target.bodyToOutputStream(out, encoding);
		}
		catch (Exception | Error e) {
			t = e;
			throw e;
		}
		finally {
			recorder.end(start, t, "bodyToOutputStream", () -> String.format("encoding=%s", encoding));
		}
	}

	@Override
	public void setBody(Source body) throws Exception {
		target.setBody(body);
	}

	@Override
	public void setBody(InputStream body) throws Exception {
		target.setBody(body);
	}

	@Override
	public void setBody(Document body) throws Exception {
		target.setBody(body);
	}

	@Override
	public void setBody(String body) throws Exception {
		target.setBody(body);
	}

	@Override
	public String getHeader(String header) {
		return target.getHeader(header);
	}

	@Override
	public Set<String> getHeadersNames() {
		return target.getHeadersNames();
	}

	@Override
	public void addHeader(String name, String value) throws Exception {
		target.addHeader(name, value);
	}

	@Override
	public void removeHeader(String name) {
		target.removeHeader(name);
	}

	@Override
	public EsbDataHandler getAttachment(String name) {
		return target.getAttachment(name);
	}

	@Override
	public InputStream getAttachmentAsStream(String name) throws Exception {
		return target.getAttachmentAsStream(name);
	}

	@Override
	public byte[] getAttachmentAsByteArray(String name) throws IOException {
		return target.getAttachmentAsByteArray(name);
	}

	@Override
	public String getAttachmentRef(String name) {
		return target.getAttachmentRef(name);
	}

	@Override
	public Set<String> getAttachmentsNames() {
		return target.getAttachmentsNames();
	}

	@Override
	public void addAttachment(String name, EsbDataHandler esbDataHandler) throws Exception {
		target.addAttachment(name, esbDataHandler);
	}

	@Override
	public void addAttachment(String name, InputStream data) throws Exception {
		target.addAttachment(name, data);
	}

	@Override
	public void addAttachment(String name, byte[] data) throws Exception {
		target.addAttachment(name, data);
	}

	@Override
	public void addXmlAttachment(String name, String data) throws Exception {
		target.addXmlAttachment(name, data);
	}

	@Override
	public void addXmlAttachment(String name, Document data) throws Exception {
		target.addXmlAttachment(name, data);
	}

	@Override
	public void removeAttachment(String name) {
		target.removeAttachment(name);
	}

	@Override
	public String getExceptionMessage() {
		return target.getExceptionMessage();
	}

	@Override
	public String getExceptionTrace() {
		return target.getExceptionTrace();
	}

	@Override
	public Set<String> getCustomHeadersNames() {
		return target.getCustomHeadersNames();
	}
}
