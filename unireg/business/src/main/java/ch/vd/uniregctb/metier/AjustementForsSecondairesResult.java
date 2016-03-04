package ch.vd.uniregctb.metier;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

/**
 * @author Raphaël Marmier, 2016-03-04, <raphael.marmier@vd.ch>
 */
public class AjustementForsSecondairesResult {
	private final List<ForFiscalSecondaire> aAnnuler;
	private final List<ForAFermer> aFermer;
	private final List<ForFiscalSecondaire> aCreer;

	public AjustementForsSecondairesResult(List<ForFiscalSecondaire> aAnnuler, List<ForAFermer> aFermer, List<ForFiscalSecondaire> aCreer) {
		this.aAnnuler = aAnnuler;
		this.aFermer = aFermer;
		this.aCreer = aCreer;
	}

	public List<ForFiscalSecondaire> getAAnnuler() {
		return aAnnuler;
	}

	public List<ForAFermer> getAFermer() {
		return aFermer;
	}

	public List<ForFiscalSecondaire> getACreer() {
		return aCreer;
	}

	public static class ForAFermer {
		private final ForFiscalSecondaire forFiscal;
		private final RegDate dateFermeture;

		public ForAFermer(ForFiscalSecondaire forFiscal, RegDate dateFermeture) {
			this.forFiscal = forFiscal;
			this.dateFermeture = dateFermeture;
		}

		public ForFiscalSecondaire getForFiscal() {
			return forFiscal;
		}

		public RegDate getDateFermeture() {
			return dateFermeture;
		}
	}
}
