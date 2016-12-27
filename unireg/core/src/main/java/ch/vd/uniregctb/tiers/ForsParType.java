package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.registre.base.date.DateRangeComparator;

/**
 * Contient les fors fiscaux d'un contribuable décomposés par types et triés dans l'ordre chronologique.
 */
public class ForsParType {

	public List<ForFiscalPrincipalPP> principauxPP = Collections.emptyList();
	public List<ForFiscalPrincipalPM> principauxPM = Collections.emptyList();
	public List<ForFiscalSecondaire> secondaires = Collections.emptyList();
	public List<ForDebiteurPrestationImposable> dpis = Collections.emptyList();
	public List<ForFiscalAutreElementImposable> autreElementImpot = Collections.emptyList();
	public List<ForFiscalAutreImpot> autresImpots = Collections.emptyList();

	public ForsParType(Set<ForFiscal> forsFiscaux, boolean sort) {

		principauxPP = Collections.emptyList();
		principauxPM = Collections.emptyList();
		secondaires = Collections.emptyList();
		dpis = Collections.emptyList();
		autreElementImpot = Collections.emptyList();
		autresImpots = Collections.emptyList();

		if (forsFiscaux == null) {
			return;
		}

		for (ForFiscal ff : forsFiscaux) {
			if (ff.isAnnule()) {
				continue;
			}

			if (ff instanceof ForFiscalPrincipalPP) {
				if (principauxPP == Collections.EMPTY_LIST) {
					principauxPP = new ArrayList<>();
				}
				principauxPP.add((ForFiscalPrincipalPP) ff);
			}
			else if (ff instanceof ForFiscalPrincipalPM) {
				if (principauxPM == Collections.EMPTY_LIST) {
					principauxPM = new ArrayList<>();
				}
				principauxPM.add((ForFiscalPrincipalPM) ff);
			}
			else if (ff instanceof ForFiscalSecondaire) {
				if (secondaires == Collections.EMPTY_LIST) {
					secondaires = new ArrayList<>();
				}
				secondaires.add((ForFiscalSecondaire) ff);
			}
			else if (ff instanceof ForDebiteurPrestationImposable) {
				if (dpis == Collections.EMPTY_LIST) {
					dpis = new ArrayList<>();
				}
				dpis.add((ForDebiteurPrestationImposable) ff);
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
			if (principauxPP != Collections.EMPTY_LIST) {
				Collections.sort(principauxPP, new DateRangeComparator<>());
			}
			if (principauxPM != Collections.EMPTY_LIST) {
				Collections.sort(principauxPM, new DateRangeComparator<>());
			}
			if (secondaires != Collections.EMPTY_LIST) {
				Collections.sort(secondaires, new DateRangeComparator<>());
			}
			if (dpis != Collections.EMPTY_LIST) {
				Collections.sort(dpis, new DateRangeComparator<>());
			}
			if (autreElementImpot != Collections.EMPTY_LIST) {
				Collections.sort(autreElementImpot, new DateRangeComparator<>());
			}
			if (autresImpots != Collections.EMPTY_LIST) {
				Collections.sort(autresImpots, new DateRangeComparator<>());
			}
		}
	}

	public final boolean isEmpty() {
		return principauxPP.isEmpty() && principauxPM.isEmpty() && secondaires.isEmpty() && dpis.isEmpty() && autreElementImpot.isEmpty() && autresImpots.isEmpty();
	}
}
