package ch.vd.uniregctb.declaration;

import ch.vd.registre.base.utils.Pair;


public class DeclarationImpotCriteria {

	private static final String TOUS = "TOUS";

	private Integer annee;

	private String officeImpot;

	private String etat;

	private Long contribuable;

	private Pair<Integer, Integer> anneeRange;

	public Integer getAnnee() {
		return annee;
	}

	public void setAnnee(Integer annee) {
		this.annee = annee;
	}

	public String getEtat() {
		return etat;
	}

	public void setEtat(String etat) {
		this.etat = etat;
	}

	public String getOfficeImpot() {
		return officeImpot;
	}

	public void setOfficeImpot(String officeImpot) {
		this.officeImpot = officeImpot;
	}

	public Long getContribuable() {
		return contribuable;
	}

	public void setContribuable(Long contribuable) {
		this.contribuable = contribuable;
	}

	public Pair<Integer, Integer> getAnneeRange() {
		return anneeRange;
	}

	public void setAnneeRange(Pair<Integer, Integer> anneeRange) {
		this.anneeRange = anneeRange;
	}

	/**
	 * @return true si aucun paramétre de recherche n'est renseigné. false
	 *         autrement.
	 */
	public boolean isEmpty() {
		return annee == null && (officeImpot == null || TOUS.equals(officeImpot)) && (contribuable == null)
				&& (anneeRange == null);
	}

}
