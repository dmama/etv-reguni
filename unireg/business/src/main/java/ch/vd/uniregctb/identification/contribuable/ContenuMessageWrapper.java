package ch.vd.uniregctb.identification.contribuable;

import ch.vd.dfin.acicom.web.services.meldewesen.impl.ContenuMessage;

public class ContenuMessageWrapper implements FichierOrigine{
	protected byte[] content;
    protected String extension;
    protected String mimeType;

	/**
     * Gets the value of the content property.
     *
     * @return
     *     possible object is
     *     byte[]
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Sets the value of the content property.
     *
     * @param value
     *     allowed object is
     *     byte[]
     */
    public void setContent(byte[] value) {
        this.content = value;
    }

    /**
     * Gets the value of the extension property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Sets the value of the extension property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setExtension(String value) {
        this.extension = value;
    }

    /**
     * Gets the value of the mimeType property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the value of the mimeType property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setMimeType(String value) {
        this.mimeType = value;
    }

	public ContenuMessageWrapper(ContenuMessage contenu){
		this.content = contenu.getContent();
		this.extension = contenu.getExtension();
		this.mimeType = contenu.getMimeType();
	}


}
