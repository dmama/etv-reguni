package ch.vd.uniregctb.evenement.civil.externe.jms;

import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;

import org.w3c.dom.Document;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.technical.esb.ErrorType;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.OperationType;
import ch.vd.technical.esb.util.EsbDataHandler;

public class MockEsbMessage implements EsbMessage {

	private String body;

	private String businessUser= "visa mutation";


	public MockEsbMessage(String body) {
		this.body = body;
	}

	@Override
	public String getMessageId() {
		throw new NotImplementedException();
	}

	@Override
	public Date getMessageCreationDate() {
		throw new NotImplementedException();
	}

	@Override
	public Date getMessageSendDate() {
		throw new NotImplementedException();
	}

	@Override
	public Date getMessageReceiveDate() {
		throw new NotImplementedException();
	}

	@Override
	public String getServiceDestination() {
		throw new NotImplementedException();
	}

	@Override
	public void setServiceDestination(String serviceDestination) {
		throw new NotImplementedException();
	}

	@Override
	public String getServiceReplyTo() {
		throw new NotImplementedException();
	}

	@Override
	public void setServiceReplyTo(String serviceReplyTo) {
		throw new NotImplementedException();
	}

	@Override
	public String getBusinessUser() {
		return businessUser;
	}

	@Override
	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	@Override
	public String getBusinessId() {
		throw new NotImplementedException();
	}

	@Override
	public void setBusinessId(String businessId) {
		throw new NotImplementedException();
	}

	@Override
	public String getBusinessCorrelationId() {
		throw new NotImplementedException();
	}

	@Override
	public void setBusinessCorrelationId(String businessCorrelationId) {
		throw new NotImplementedException();
	}

	@Override
	public String getDomain() {
		throw new NotImplementedException();
	}

	@Override
	public void setDomain(String domain) {
		throw new NotImplementedException();
	}

	@Override
	public String getContext() {
		throw new NotImplementedException();
	}

	@Override
	public void setContext(String context) {
		throw new NotImplementedException();
	}

	@Override
	public String getApplication() {
		throw new NotImplementedException();
	}

	@Override
	public void setApplication(String application) {
		throw new NotImplementedException();
	}

	@Override
	public Source getBodyAsSource() {
		throw new NotImplementedException();
	}

	@Override
	public Document getBodyAsDocument() throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public byte[] getBodyAsByteArray() throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public byte[] getBodyAsByteArray(String encoding) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String getBodyAsString() throws Exception {
		return body;
	}

	@Override
	public String getBodyAsString(String encoding) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public ErrorType getErrorType() throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String getErrorCode() throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public boolean isError() throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void bodyToOutputStream(OutputStream out) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void bodyToOutputStream(OutputStream out, String encoding) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void setBody(Source body) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void setBody(InputStream body) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void setBody(Document body) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void setBody(String body) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public String getHeader(String header) {
		throw new NotImplementedException();
	}

	@Override
	public Set<String> getHeadersNames() {
		throw new NotImplementedException();
	}

	@Override
	public void addHeader(String name, String value) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void removeHeader(String name) {
		throw new NotImplementedException();
	}

	@Override
	public EsbDataHandler getAttachment(String name) {
		throw new NotImplementedException();
	}

	@Override
	public InputStream getAttachmentAsStream(String name) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public byte[] getAttachmentAsByteArray(String name) throws IOException {
		throw new NotImplementedException();
	}

	@Override
	public String getAttachmentRef(String name) {
		throw new NotImplementedException();
	}

	@Override
	public Set<String> getAttachmentsNames() {
		throw new NotImplementedException();
	}

	@Override
	public void addAttachment(String name, EsbDataHandler esbDataHandler) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void addAttachment(String name, InputStream data) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void addAttachment(String name, byte[] data) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void addXmlAttachment(String name, String data) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void addXmlAttachment(String name, Document data) throws Exception {
		throw new NotImplementedException();
	}

	@Override
	public void removeAttachment(String name) {
		throw new NotImplementedException();
	}

	@Override
	public String getExceptionMessage() {
		throw new NotImplementedException();
	}

	@Override
	public String getExceptionTrace() {
		throw new NotImplementedException();
	}

	@Override
	public void setOperation(OperationType operationType) {
		throw new NotImplementedException();
	}

	@Override
	public OperationType getOperation() {
		throw new NotImplementedException();
	}

	@Override
	public void setProcessDefinitionId(String s) {
		throw new NotImplementedException();
	}

	@Override
	public String getProcessDefinitionId() {
		throw new NotImplementedException();
	}

	@Override
	public void setProcessInstanceId(String s) {
		throw new NotImplementedException();
	}

	@Override
	public String getProcessInstanceId() {
		throw new NotImplementedException();
	}

	@Override
	public Set<String> getCustomHeadersNames() {
		throw new NotImplementedException();
	}
}
