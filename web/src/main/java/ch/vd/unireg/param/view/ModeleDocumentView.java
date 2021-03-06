package ch.vd.unireg.param.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.type.TypeDocument;

public class ModeleDocumentView {
	
	private Long idPeriode;
	private Long idModele;
	private Integer anneePeriodeFiscale;

	private TypeDocument typeDocument;

	public ModeleDocumentView() {
	}

	public ModeleDocumentView(@NotNull PeriodeFiscale pf) {
		this.idPeriode = pf.getId();
		this.anneePeriodeFiscale = pf.getAnnee();
	}

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

	public Integer getAnneePeriodeFiscale() {
		return anneePeriodeFiscale;
	}

	public void setAnneePeriodeFiscale(Integer anneePeriodeFiscale) {
		this.anneePeriodeFiscale = anneePeriodeFiscale;
	}
}
