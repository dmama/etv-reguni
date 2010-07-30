package ch.vd.uniregctb.param.view;

import ch.vd.uniregctb.type.TypeDocument;

public class ModeleDocumentView {
	
	private Long idPeriode;
	private Long idModele;
	
	private TypeDocument typeDocument;

	public Long getIdPeriode() {
		return idPeriode;
	}

	public void setIdPeriode(Long idPeriode) {
		this.idPeriode = idPeriode;
	}

	public Long getIdModele() {
		return idModele;
	}

	public void setIdModele(Long idModele) {
		this.idModele = idModele;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}
	
	

}
