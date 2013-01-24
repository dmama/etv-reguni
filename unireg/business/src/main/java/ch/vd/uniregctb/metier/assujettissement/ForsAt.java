package ch.vd.uniregctb.metier.assujettissement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structure qui contient les fors fiscaux d'un contribuables à une date donnée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ForsAt {

	public final ForFiscalPrincipal principal;
	public final ForsList<ForFiscalSecondaire> secondaires;

	/**
	 * Le nombre total de fors valides.
	 */
	public final int count;

	public ForsAt(ForFiscalPrincipal principal, List<ForFiscalSecondaire> secondaires) {
		this.principal = principal;
		if (secondaires == null || secondaires.isEmpty()) {
			this.secondaires = null;
		}
		else {
			this.secondaires = new ForsList<ForFiscalSecondaire>(secondaires);
		}
		this.count = (principal == null ? 0 : 1) + (this.secondaires == null ? 0 : this.secondaires.size());
	}

	public ForsAt(ForFiscalPrincipal principal, ForFiscalSecondaire... secondaires) {
		this.principal = principal;
		if (secondaires == null || secondaires.length == 0) {
			this.secondaires = null;
		}
		else {
			this.secondaires = new ForsList<ForFiscalSecondaire>(Arrays.asList(secondaires));
		}
		this.count = (principal == null ? 0 : 1) + (this.secondaires == null ? 0 : this.secondaires.size());
	}
}
