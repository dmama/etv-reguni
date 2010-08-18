package ch.vd.uniregctb.evenement.jms;

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
import ch.vd.technical.esb.util.EsbDataHandler;

public class MockEsbMessage implements EsbMessage {

	private String body;

	private String businessUser= "visa mutation";


	public MockEsbMessage(String body) {
		this.body = body;
	}

	public String getMessageId() {
		throw new NotImplementedException();
	}

	public Date getMessageCreationDate() {
		throw new NotImplementedException();
	}

	public Date getMessageSendDate() {
		throw new NotImplementedException();
	}

	public Date getMessageReceiveDate() {
		throw new NotImplementedException();
	}

	public String getServiceDestination() {
		throw new NotImplementedException();
	}

	public void setServiceDestination(String serviceDestination) {
		throw new NotImplementedException();
	}

	public String getServiceReplyTo() {
		throw new NotImplementedException();
	}

	public void setServiceReplyTo(String serviceReplyTo) {
		throw new NotImplementedException();
	}

	public String getBusinessUser() {
		return businessUser;
	}

	public void setBusinessUser(String businessUser) {
		this.businessUser = businessUser;
	}

	public String getBusinessId() {
		throw new NotImplementedException();
	}

	public void setBusinessId(String businessId) {
		throw new NotImplementedException();
	}

	public String getBusinessCorrelationId() {
		throw new NotImplementedException();
	}

	public void setBusinessCorrelationId(String businessCorrelationId) {
		throw new NotImplementedException();
	}

	public String getDomain() {
		throw new NotImplementedException();
	}

	public void setDomain(String domain) {
		throw new NotImplementedException();
	}

	public String getContext() {
		throw new NotImplementedException();
	}

	public void setContext(String context) {
		throw new NotImplementedException();
	}

	public String getApplication() {
		throw new NotImplementedException();
	}

	public void setApplication(String application) {
		throw new NotImplementedException();
	}

	public Source getBodyAsSource() {
		throw new NotImplementedException();
	}

	public Document getBodyAsDocument() throws Exception {
		throw new NotImplementedException();
	}

	public byte[] getBodyAsByteArray() throws Exception {
		throw new NotImplementedException();
	}

	public byte[] getBodyAsByteArray(String encoding) throws Exception {
		throw new NotImplementedException();
	}

	public String getBodyAsString() throws Exception {
		return body;
	}

	public String getBodyAsString(String encoding) throws Exception {
		throw new NotImplementedException();
	}

	public ErrorType getErrorType() throws Exception {
		throw new NotImplementedException();
	}

	public String getErrorCode() throws Exception {
		throw new NotImplementedException();
	}

	public boolean isError() throws Exception {
		throw new NotImplementedException();
	}

	public void bodyToOutputStream(OutputStream out) throws Exception {
		throw new NotImplementedException();
	}

	public void bodyToOutputStream(OutputStream out, String encoding) throws Exception {
		throw new NotImplementedException();
	}

	public void setBody(Source body) throws Exception {
		throw new NotImplementedException();
	}

	public void setBody(InputStream body) throws Exception {
		throw new NotImplementedException();
	}

	public void setBody(Document body) throws Exception {
		throw new NotImplementedException();
	}

	public void setBody(String body) throws Exception {
		throw new NotImplementedException();
	}

	public String getHeader(String header) {
		throw new NotImplementedException();
	}

	public Set<String> getHeadersNames() {
		throw new NotImplementedException();
	}

	public void addHeader(String name, String value) throws Exception {
		throw new NotImplementedException();
	}

	public void removeHeader(String name) {
		throw new NotImplementedException();
	}

	public EsbDataHandler getAttachment(String name) {
		throw new NotImplementedException();
	}

	public InputStream getAttachmentAsStream(String name) throws Exception {
		throw new NotImplementedException();
	}

	public byte[] getAttachmentAsByteArray(String name) throws IOException {
		throw new NotImplementedException();
	}

	public String getAttachmentRef(String name) {
		throw new NotImplementedException();
	}

	public Set<String> getAttachmentsNames() {
		throw new NotImplementedException();
	}

	public void addAttachment(String name, EsbDataHandler esbDataHandler) throws Exception {
		throw new NotImplementedException();
	}

	public void addAttachment(String name, InputStream data) throws Exception {
		throw new NotImplementedException();
	}

	public void addAttachment(String name, byte[] data) throws Exception {
		throw new NotImplementedException();
	}

	public void addXmlAttachment(String name, String data) throws Exception {
		throw new NotImplementedException();
	}

	public void addXmlAttachment(String name, Document data) throws Exception {
		throw new NotImplementedException();
	}

	public void removeAttachment(String name) {
		throw new NotImplementedException();
	}

	public String getExceptionMessage() {
		throw new NotImplementedException();
	}

	public String getExceptionTrace() {
		throw new NotImplementedException();
	}
}
