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

	public List<ForFiscalPrincipal> principaux = Collections.emptyList();
	public List<ForFiscalSecondaire> secondaires = Collections.emptyList();
	public List<ForDebiteurPrestationImposable> dpis = Collections.emptyList();
	public List<ForFiscalAutreElementImposable> autreElementImpot = Collections.emptyList();
	public List<ForFiscalAutreImpot> autresImpots = Collections.emptyList();

	public ForsParType(Set<ForFiscal> forsFiscaux, boolean sort) {

		principaux = Collections.emptyList();
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

			if (ff instanceof ForFiscalPrincipal) {
				if (principaux == Collections.EMPTY_LIST) {
					principaux = new ArrayList<ForFiscalPrincipal>();
				}
				principaux.add((ForFiscalPrincipal) ff);
			}
			else if (ff instanceof ForFiscalSecondaire) {
				if (secondaires == Collections.EMPTY_LIST) {
					secondaires = new ArrayList<ForFiscalSecondaire>();
				}
				secondaires.add((ForFiscalSecondaire) ff);
			}
			else if (ff instanceof ForDebiteurPrestationImposable) {
				if (dpis == Collections.EMPTY_LIST) {
					dpis = new ArrayList<ForDebiteurPrestationImposable>();
				}
				dpis.add((ForDebiteurPrestationImposable) ff);
			}
			else if (ff instanceof ForFiscalAutreElementImposable) {
				if (autreElementImpot == Collections.EMPTY_LIST) {
					autreElementImpot = new ArrayList<ForFiscalAutreElementImposable>();
				}
				autreElementImpot.add((ForFiscalAutreElementImposable) ff);
			}
			else if (ff instanceof ForFiscalAutreImpot) {
				if (autresImpots == Collections.EMPTY_LIST) {
					autresImpots = new ArrayList<ForFiscalAutreImpot>();
				}
				autresImpots.add((ForFiscalAutreImpot) ff);
			}
			else {
				throw new IllegalArgumentException("Type de for fiscal inconnu = [" + ff.getClass() + ']');
			}
		}

		if (sort) {
			if (principaux != Collections.EMPTY_LIST) {
				Collections.sort(principaux, new DateRangeComparator<ForFiscalPrincipal>());
			}
			if (secondaires != Collections.EMPTY_LIST) {
				Collections.sort(secondaires, new DateRangeComparator<ForFiscalSecondaire>());
			}
			if (dpis != Collections.EMPTY_LIST) {
				Collections.sort(dpis, new DateRangeComparator<ForDebiteurPrestationImposable>());
			}
			if (autreElementImpot != Collections.EMPTY_LIST) {
				Collections.sort(autreElementImpot, new DateRangeComparator<ForFiscalAutreElementImposable>());
			}
			if (autresImpots != Collections.EMPTY_LIST) {
				Collections.sort(autresImpots, new DateRangeComparator<ForFiscalAutreImpot>());
			}
		}
	}

	public final boolean isEmpty() {
		return principaux.isEmpty() && secondaires.isEmpty() && dpis.isEmpty() && autreElementImpot.isEmpty() && autresImpots.isEmpty();
	}
}
