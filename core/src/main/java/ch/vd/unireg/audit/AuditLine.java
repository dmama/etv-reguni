/**
 *
 */
package ch.vd.unireg.audit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.common.LengthConstants;

@Entity
@Table(name = "AUDIT_LOG", indexes = @Index(name = "IDX_AUDIT_LOG_DATE", columnList = "LOG_DATE"))
public class AuditLine {

	public static final int MESSAGE_MAX_LENGTH = LengthConstants.LOG_MESSAGE;

	private Long id;
	private Long threadId;
	private Long evenementId;
	private Date date;
	private String user;
	private String message;
	private AuditLevel level;
	private Long documentId;

	public AuditLine() {
	}

	public AuditLine(long threadId, Long evtId, String user, AuditLevel level, String message, Long documentId) {
		this.threadId = threadId;
		this.evenementId = evtId;
		this.user = user;
		this.level = level;
		if (message != null && message.length() > MESSAGE_MAX_LENGTH) {
			this.message = message.substring(0, MESSAGE_MAX_LENGTH - 1);
		}
		else {
			this.message = message;
		}

		this.date = DateHelper.getCurrentDate();
		this.documentId = documentId;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "THREAD_ID")
	public Long getThreadId() {
		return threadId;
	}

	public void setThreadId(Long theThreadId) {
		threadId = theThreadId;
	}

	@Column(name = "EVT_ID")
	public Long getEvenementId() {
		return evenementId;
	}

	public void setEvenementId(Long theEvenementId) {
		evenementId = theEvenementId;
	}

	@Column(name = "LOG_DATE")
	public Date getDate() {
		return date;
	}

	public void setDate(Date theDate) {
		date = theDate;
	}

	@Column(name = "LOG_USER")
	public String getUser() {
		return user;
	}

	public void setUser(String theUser) {
		user = theUser;
	}

	@Column(name = "LOG_LEVEL", length = LengthConstants.LOG_LEVEL)
	@Type(type = "ch.vd.unireg.hibernate.AuditLevelUserType")
	public AuditLevel getLevel() {
		return level;
	}

	public void setLevel(AuditLevel theLevel) {
		level = theLevel;
	}

	@Column(name = "MESSAGE", length = LengthConstants.LOG_MESSAGE)
	public String getMessage() {
		return message;
	}

	public void setMessage(String theMessage) {
		this.message = theMessage;
	}

	@Column(name = "DOC_ID")
	public Long getDocumentId() {
		return documentId;
	}

	public void setDocumentId(Long documentId) {
		this.documentId = documentId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
