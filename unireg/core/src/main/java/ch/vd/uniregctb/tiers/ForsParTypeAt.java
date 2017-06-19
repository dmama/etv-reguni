package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;

/**
 * Contient les fors fiscaux d'un contribuable à un moment donné, décomposés par types et triés dans l'ordre chronologique.
 */
public class ForsParTypeAt {

	public ForFiscalPrincipal principal;
	public List<ForFiscalSecondaire> secondaires = new ArrayList<>();
	public ForDebiteurPrestationImposable dpis;
	public List<ForFiscalAutreElementImposable> autreElementImpot = new ArrayList<>();
	public List<ForFiscalAutreImpot> autresImpots = new ArrayList<>();

	public ForsParTypeAt(Set<ForFiscal> forsFiscaux, RegDate date, boolean sort) {

		principal = null;
		secondaires = Collections.emptyList();
		dpis = null;
		autreElementImpot = Collections.emptyList();
		autresImpots = Collections.emptyList();

		if (forsFiscaux == null) {
			return;
		}

		for (ForFiscal ff : forsFiscaux) {
			if (!ff.isValidAt(date)) {
				continue;
			}

			if (ff instanceof ForFiscalPrincipal) {
				Assert.isNull(principal);
				principal = (ForFiscalPrincipal) ff;
			}
			else if (ff instanceof ForFiscalSecondaire) {
				if (secondaires == Collections.EMPTY_LIST) {
					secondaires = new ArrayList<>();
				}
				secondaires.add((ForFiscalSecondaire) ff);
			}
			else if (ff instanceof ForDebiteurPrestationImposable) {
				Assert.isNull(dpis);
				dpis = (ForDebiteurPrestationImposable) ff;
			}
			else if (ff instanceof ForFiscalAutreElementImposable) {
				if (autreElementImpot == Collections.EMPTY_LIST) {
					autreElementImpot = new ArrayList<>();
				}
				autreElementImpot.add((ForFiscalAutreElementImposable) ff);
			}
			else if (ff instanceof ForFiscalAutreImpot) {
				if (autresImpots == Collections.EMPTY_LIST) {
					autresImpots = new ArrayList<>();
				}
				autresImpots.add((ForFiscalAutreImpot) ff);
			}
			else {
				throw new IllegalArgumentException("Type de for fiscal inconnu = [" + ff.getClass() + ']');
			}
		}

		if (sort) {
			secondaires.sort(new DateRangeComparator<>());
			autreElementImpot.sort(new DateRangeComparator<>());
			autresImpots.sort(new DateRangeComparator<>());
		}
	}

}
