package ch.vd.unireg.di.view;

public class LibererDocumentFiscalView {

	private Long idDocument;
	private String motif;

	public LibererDocumentFiscalView() {
	}

	public LibererDocumentFiscalView(Long idDI, String motif) {
		this.idDocument = idDI;
		this.motif = motif;
	}

	public Long getIdDocument() {
		return idDocument;
	}

	public void setIdDocument(Long idDI) {
		this.idDocument = idDI;
	}

	public String getMotif() {
		return motif;
	}

	public void setMotif(String motif) {
		this.motif = motif;
	}
}