package ch.vd.uniregctb.admin;

import java.util.Date;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.uniregctb.audit.AuditLevel;
import ch.vd.uniregctb.audit.AuditLine;
import ch.vd.uniregctb.audit.AuditLineCriteria;
import ch.vd.uniregctb.document.Document;

/**
 * Contient une liste des lignes d'audits.
 */
public class AuditLogBean {

	public static class AuditView {
		private final Long id;
		private final Long threadId;
		private final Integer evenementId;
		private final Date date;
		private final String user;
		private final String message;
		private final AuditLevel level;
		private final Document document;

		public AuditView(AuditLine line, HibernateTemplate hibernateTemplate) {
			this.id = line.getId();
			this.threadId = line.getThreadId();
			this.evenementId = line.getEvenementId();
			this.date = line.getDate();
			this.user = line.getUser();
			this.message = line.getMessage();
			this.level = line.getLevel();

			final Long documentId = line.getDocumentId();
			if (documentId == null) {
				this.document = null;
			}
			else {
				this.document = hibernateTemplate.get(Document.class, documentId);
			}
		}

		public Long getId() {
			return id;
		}

		public Long getThreadId() {
			return threadId;
		}

		public Integer getEvenementId() {
			return evenementId;
		}

		public Date getDate() {
			return date;
		}

		public String getUser() {
			return user;
		}

		public String getMessage() {
			return message;
		}

		public AuditLevel getLevel() {
			return level;
		}

		public Document getDocument() {
			return document;
		}
	}

	private int totalSize;
	private List<AuditView> list;
	private final AuditLineCriteria criteria = new AuditLineCriteria();

	public int getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}

	public List<AuditView> getList() {
		return list;
	}

	public void setList(List<AuditView> list) {
		this.list = list;
	}

	public boolean isShowInfo() {
		return criteria.isShowInfo();
	}

	public void setShowInfo(boolean showInfo) {
		criteria.setShowInfo(showInfo);
	}

	public boolean isShowWarning() {
		return criteria.isShowWarning();
	}

	public void setShowWarning(boolean showWarning) {
		criteria.setShowWarning(showWarning);
	}

	public boolean isShowError() {
		return criteria.isShowError();
	}

	public void setShowError(boolean showError) {
		criteria.setShowError(showError);
	}

	public boolean isShowSuccess() {
		return criteria.isShowSuccess();
	}

	public void setShowSuccess(boolean showSuccess) {
		criteria.setShowSuccess(showSuccess);
	}

	public boolean isShowEvCivil() {
		return criteria.isShowEvCivil();
	}

	public void setShowEvCivil(boolean showEvCivil) {
		this.criteria.setShowEvCivil(showEvCivil);
	}

	public AuditLineCriteria getCriteria() {
		return criteria;
	}
}
