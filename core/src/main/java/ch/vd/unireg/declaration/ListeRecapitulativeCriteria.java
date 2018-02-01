package ch.vd.unireg.declaration;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class ListeRecapitulativeCriteria implements Serializable {

	private static final long serialVersionUID = 4707213373784221710L;

	private PeriodiciteDecompte periodicite;
	private RegDate periode;
	private CategorieImpotSource categorie;
	private TypeEtatDocumentFiscal etat;
	private ModeCommunication modeCommunication;

	@Nullable
	public PeriodiciteDecompte getPeriodicite() {
		return periodicite;
	}

	public void setPeriodicite(@Nullable PeriodiciteDecompte periodicite) {
		this.periodicite = periodicite;
	}

	@Nullable
	public RegDate getPeriode() {
		return periode;
	}

	public void setPeriode(@Nullable RegDate periode) {
		this.periode = periode;
	}

	@Nullable
	public CategorieImpotSource getCategorie() {
		return categorie;
	}

	public void setCategorie(@Nullable CategorieImpotSource categorie) {
		this.categorie = categorie;
	}

	@Nullable
	public TypeEtatDocumentFiscal getEtat() {
		return etat;
	}

	public void setEtat(@Nullable TypeEtatDocumentFiscal etat) {
		this.etat = etat;
	}

	@Nullable
	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	public void setModeCommunication(@Nullable ModeCommunication modeCommunication) {
		this.modeCommunication = modeCommunication;
	}

	/**
	 * @return true si aucun paramètre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return modeCommunication == null && etat == null && categorie == null && periodicite == null && periode == null;
	}

}
