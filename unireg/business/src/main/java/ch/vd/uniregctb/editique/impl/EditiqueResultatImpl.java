package ch.vd.uniregctb.editique.impl;

import ch.vd.uniregctb.editique.EditiqueResultat;

/**
 * default implementation de l'interface {@link EditiqueResultat}.
 *
 * @author xcicfh (last modified by $Author: xciflm $ @ $Date: 2007/09/13 06:36:24 $)
 * @version $Revision: 1.2 $
 */
public final class EditiqueResultatImpl implements EditiqueResultat {

	private byte[] document;
	private String error;
	private String documentType;
	private String idDocument;
	private String contentType;
	private long timestampReceived;

	/**
	 * {@inheritDoc}
	 */
	public byte[] getDocument() {
		return document;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getError() {
		return error;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getDocumentType() {
		return documentType;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasError() {
		return error != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDocument(byte[] document) {
		this.document = document;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdDocument() {
		return idDocument;
	}

	public void setIdDocument(String idDocument) {
		this.idDocument = idDocument;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getContentType() {
		return contentType;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * Obtient le timestamp de la reception du document.
	 *
	 * @return Retourne le timestamp de la reception du document.
	 */
	public long getTimestampReceived() {
		return timestampReceived;
	}

	/**
	 * Définit le timestamp de la reception du document.
	 *
	 * @param timestampReceived
	 *            le timestamp de la reception du document.
	 */
	public void setTimestampReceived(long timestampReceived) {
		this.timestampReceived = timestampReceived;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("EditiqueResultat:\n");
		buffer.append("EditiqueResultat.documentType: ");
		buffer.append(this.getDocumentType());
		buffer.append('\n');
		buffer.append("EditiqueResultat.nomDocument: ");
		buffer.append(this.getIdDocument());
		buffer.append('\n');
		buffer.append("EditiqueResultat.content: ");
		if (this.getDocument() == null || this.getDocument().length == 0) {
			buffer.append("empty");
		}
		else {
			buffer.append(this.getDocument().length);
		}
		buffer.append('\n');
		buffer.append("EditiqueResultat.timestamp: ");
		buffer.append(this.getTimestampReceived());
		buffer.append('\n');
		if (hasError()) {
			buffer.append("EditiqueResultat.error: ");
			buffer.append(this.getError());
			buffer.append('\n');
		}
		return buffer.toString();
	}

}
