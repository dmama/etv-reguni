package ch.vd.uniregctb.identification.contribuable;

public interface FichierOrigine {

	public byte[] getContent();

	public String getExtension();

	public String getMimeType();
}
