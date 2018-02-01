package ch.vd.unireg.extraction.entreprise.photosimpa;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.registre.base.date.RegDate;

public class OutputData {

	final int noEntreprise;
	final String noIDE;
	final String raisonSociale;
	final Pair<Integer, String> noOfsNomDernierForPrincipal;
	final Pair<Integer, String> npaNoPolice;
	final Pair<String, String> codeLibelleFormeJuridique;
	final RegDate depuisLe;         // ????
	final Pair<String, String> codeLibelleRegimeFiscalCH;
	final Pair<String, String> codeLibelleRegimeFiscalVD;
	final RegDate debutAssujettissementICC;
	final RegDate finAssujettissementICC;

	public OutputData(int noEntreprise, String noIDE, String raisonSociale, Pair<Integer, String> noOfsNomDernierForPrincipal, Pair<Integer, String> npaNoPolice,
	                  Pair<String, String> codeLibelleFormeJuridique, RegDate depuisLe, Pair<String, String> codeLibelleRegimeFiscalCH, Pair<String, String> codeLibelleRegimeFiscalVD, RegDate debutAssujettissementICC,
	                  RegDate finAssujettissementICC) {
		this.noEntreprise = noEntreprise;
		this.noIDE = noIDE;
		this.raisonSociale = raisonSociale;
		this.noOfsNomDernierForPrincipal = noOfsNomDernierForPrincipal;
		this.npaNoPolice = npaNoPolice;
		this.codeLibelleFormeJuridique = codeLibelleFormeJuridique;
		this.depuisLe = depuisLe;
		this.codeLibelleRegimeFiscalCH = codeLibelleRegimeFiscalCH;
		this.codeLibelleRegimeFiscalVD = codeLibelleRegimeFiscalVD;
		this.debutAssujettissementICC = debutAssujettissementICC;
		this.finAssujettissementICC = finAssujettissementICC;
	}

	public int getNoEntreprise() {
		return noEntreprise;
	}

	public String getNoIDE() {
		return noIDE;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public Pair<Integer, String> getNoOfsNomDernierForPrincipal() {
		return noOfsNomDernierForPrincipal;
	}

	public Pair<Integer, String> getNpaNoPolice() {
		return npaNoPolice;
	}

	public Pair<String, String> getCodeLibelleFormeJuridique() {
		return codeLibelleFormeJuridique;
	}

	public RegDate getDepuisLe() {
		return depuisLe;
	}

	public Pair<String, String> getCodeLibelleRegimeFiscalCH() {
		return codeLibelleRegimeFiscalCH;
	}

	public Pair<String, String> getCodeLibelleRegimeFiscalVD() {
		return codeLibelleRegimeFiscalVD;
	}

	public RegDate getDebutAssujettissementICC() {
		return debutAssujettissementICC;
	}

	public RegDate getFinAssujettissementICC() {
		return finAssujettissementICC;
	}
}
