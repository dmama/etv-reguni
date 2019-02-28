package ch.vd.unireg.di.view;

@SuppressWarnings("UnusedDeclaration")
public class LibererDeclarationImpotView {

	private Long idDI;
	private String motif;

	public LibererDeclarationImpotView() {
	}

	public LibererDeclarationImpotView(Long idDI, String motif) {
		this.idDI = idDI;
		this.motif = motif;
	}

	public Long getIdDI() {
		return idDI;
	}

	public void setIdDI(Long idDI) {
		this.idDI = idDI;
	}

	public String getMotif() {
		return motif;
	}

	public void setMotif(String motif) {
		this.motif = motif;
	}
}