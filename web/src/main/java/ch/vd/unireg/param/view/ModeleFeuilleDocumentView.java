package ch.vd.unireg.param.view;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.TypeDocument;


public class ModeleFeuilleDocumentView {

	private Long idFeuille;
	private ModeleFeuille modeleFeuille;
	private Long idPeriode;
	private Integer periodeAnnee;
	private Long idModele;
	private TypeDocument modeleDocumentTypeDocument;

	public ModeleFeuilleDocumentView() {
	}

	public ModeleFeuilleDocumentView(@NotNull ModeleFeuilleDocument mfd) {
		final ModeleDocument modeleDocument = mfd.getModeleDocument();
		final PeriodeFiscale periodeFiscale = modeleDocument.getPeriodeFiscale();
		final ModeleFeuille modeleFeuille = ModeleFeuille.fromNoCADEV(mfd.getNoCADEV());

		this.idFeuille = mfd.getId();
		this.modeleFeuille = modeleFeuille;
		this.idPeriode = periodeFiscale.getId();
		this.periodeAnnee = periodeFiscale.getAnnee();
		this.idModele = modeleDocument.getId();
		this.modeleDocumentTypeDocument = modeleDocument.getTypeDocument();
	}

	public void setModeleDocumentTypeDocument(TypeDocument modeleDocumentTypeDocument) {
		this.modeleDocumentTypeDocument = modeleDocumentTypeDocument;
	}
	public TypeDocument getModeleDocumentTypeDocument() {
		return modeleDocumentTypeDocument;
	}
	public Long getIdPeriode() {
		return idPeriode;
	}
	public void setIdPeriode(Long idPeriode) {
		this.idPeriode = idPeriode;
	}
	public Integer getPeriodeAnnee() {
		return periodeAnnee;
	}
	public void setPeriodeAnnee(Integer periodeAnnee) {
		this.periodeAnnee = periodeAnnee;
	}
	public Long getIdModele() {
		return idModele;
	}
	public void setIdModele(Long idModele) {
		this.idModele = idModele;
	}
	public Long getIdFeuille() {
		return idFeuille;
	}
	public void setIdFeuille(Long idFeuille) {
		this.idFeuille = idFeuille;
	}

	public ModeleFeuille getModeleFeuille() {
		return modeleFeuille;
	}

	public void setModeleFeuille(ModeleFeuille modeleFeuille) {
		this.modeleFeuille = modeleFeuille;
	}
}
