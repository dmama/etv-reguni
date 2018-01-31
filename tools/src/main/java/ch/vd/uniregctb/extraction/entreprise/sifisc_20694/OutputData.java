package ch.vd.uniregctb.extraction.entreprise.sifisc_20694;

import ch.vd.registre.base.date.RegDate;

public class OutputData {

	final InputData input;
	final String raisonSociale;
	final String noIDE;
	final String codeFormeJuridique;
	final String libelleFormeJuridique;
	final String categorieEntreprise;
	final String codeRegimeFiscalVD;
	final String libelleRegimeFiscalVD;
	final String codeRegimeFiscalCH;
	final String libelleRegimeFiscalCH;
	final RegDate dateDebut;
	final RegDate dateFin;

	public OutputData(InputData input, String raisonSociale, String noIDE, String codeFormeJuridique, String libelleFormeJuridique, String categorieEntreprise, String codeRegimeFiscalVD, String libelleRegimeFiscalVD, String codeRegimeFiscalCH,
	                  String libelleRegimeFiscalCH, RegDate dateDebut, RegDate dateFin) {
		this.input = input;
		this.raisonSociale = raisonSociale;
		this.noIDE = noIDE;
		this.codeFormeJuridique = codeFormeJuridique;
		this.libelleFormeJuridique = libelleFormeJuridique;
		this.categorieEntreprise = categorieEntreprise;
		this.codeRegimeFiscalVD = codeRegimeFiscalVD;
		this.libelleRegimeFiscalVD = libelleRegimeFiscalVD;
		this.codeRegimeFiscalCH = codeRegimeFiscalCH;
		this.libelleRegimeFiscalCH = libelleRegimeFiscalCH;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}
}
