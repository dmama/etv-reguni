package ch.vd.uniregctb.declaration;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class ListeRecapCriteria implements Serializable {

	private static final long serialVersionUID = -6003619288262554985L;

	private PeriodiciteDecompte periodicite;

	private RegDate periode;

	private CategorieImpotSource categorie;

	private TypeEtatDeclaration etat;

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
	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public void setEtat(@Nullable TypeEtatDeclaration etat) {
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
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return modeCommunication == null && etat == null && categorie == null && periodicite == null && periode == null;
	}

}
