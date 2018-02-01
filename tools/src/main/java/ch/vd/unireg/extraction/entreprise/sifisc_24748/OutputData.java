package ch.vd.unireg.extraction.entreprise.sifisc_24748;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;

public class OutputData {

	final int noCtb;
	final String raisonSociale;
	final String noIDE;
	final String codeRegimeFiscalVD;
	final String libelleRegimeFiscalVD;
	final RegDate debutAssujettissement;
	final LiabilityChangeReason motifDebutAssujettissement;
	final RegDate finAssujettissement;
	final LiabilityChangeReason motifFinAssujettissement;
	final boolean illimite;
	final TaxationAuthorityType tafForPrincipal;
	final Integer noOfsForPrincipal;
	final String nomForPrincipal;

	public OutputData(int noCtb, String raisonSociale, String noIDE, String codeRegimeFiscalVD, String libelleRegimeFiscalVD,
	                  RegDate debutAssujettissement, LiabilityChangeReason motifDebutAssujettissement, RegDate finAssujettissement, LiabilityChangeReason motifFinAssujettissement,
	                  boolean illimite, TaxationAuthorityType tafForPrincipal, Integer noOfsForPrincipal, String nomForPrincipal) {
		this.noCtb = noCtb;
		this.raisonSociale = raisonSociale;
		this.noIDE = noIDE;
		this.codeRegimeFiscalVD = codeRegimeFiscalVD;
		this.libelleRegimeFiscalVD = libelleRegimeFiscalVD;
		this.debutAssujettissement = debutAssujettissement;
		this.motifDebutAssujettissement = motifDebutAssujettissement;
		this.finAssujettissement = finAssujettissement;
		this.motifFinAssujettissement = motifFinAssujettissement;
		this.illimite = illimite;
		this.tafForPrincipal = tafForPrincipal;
		this.noOfsForPrincipal = noOfsForPrincipal;
		this.nomForPrincipal = nomForPrincipal;
	}
}
