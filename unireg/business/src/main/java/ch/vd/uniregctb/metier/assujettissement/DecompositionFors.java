package ch.vd.uniregctb.metier.assujettissement;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.declaration.ordinaire.ForsList;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;

/**
 * Décomposition des fors d'un contribuable par type sur une période donnée.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class DecompositionFors implements DateRange {

	public final Contribuable contribuable;
	public final RegDate debut;
	public final RegDate fin;

	/** For principal valide à la fin de la période considérée */
	public final ForFiscalPrincipal principal;

	/** Liste des fors principaux valides à la fin de la période considérée */
	public final ForsList<ForFiscalSecondaire> secondaires = new ForsList<ForFiscalSecondaire>();

	/** Liste des fors principaux existants tout au long de la période considérée */
	public final ForsList<ForFiscalPrincipal> principauxDansLaPeriode = new ForsList<ForFiscalPrincipal>();

	/** Liste des fors secondaires existants tout au long de la période considérée */
	public final ForsList<ForFiscalSecondaire> secondairesDansLaPeriode = new ForsList<ForFiscalSecondaire>();

	/** For principal valide immédiatement avant la période considérée */
	public final ForFiscalPrincipal principalAvantLaPeriode;

	/** For principal valide immédiatement après la période considérée */
	public final ForFiscalPrincipal principalApresLaPeriode;

	/** Liste des fors secondaires existants immédiatement avant la période considérée */
	public final ForsList<ForFiscalSecondaire> secondairesAvantLaPeriode = new ForsList<ForFiscalSecondaire>();

	/** Liste des fors secondaires existants immédiatement après la période considérée */
	public final ForsList<ForFiscalSecondaire> secondairesApresLaPeriode = new ForsList<ForFiscalSecondaire>();

	public DecompositionFors(Contribuable contribuable, RegDate debut, RegDate fin) {
		this.contribuable = contribuable;
		this.debut = debut;
		this.fin = fin;

		/**
		 * Extraction des fors principaux et secondaires courants
		 */
		final Range rangePeriode = new Range(debut, fin);
		final List<ForFiscal> fors = contribuable.getForsFiscauxSorted();

		ForFiscalPrincipal forPrincipal = null;
		ForFiscalPrincipal forPrincipalAvant = null;
		ForFiscalPrincipal forPrincipalApres = null;

		if (fors != null) {
			for (ForFiscal f : fors) {
				if (f.isAnnule()) {
					continue;
				}

				if (f.isValidAt(fin)) {
					// Le for est valide à la fin de la période
					if (f.isPrincipal()) {
						if (forPrincipal != null) {
							Assert.fail("Le contribuable n°" + contribuable.getNumero()
									+ " possède plus d'un for principal valide à la date = " + fin);
						}
						forPrincipal = (ForFiscalPrincipal) f;
						this.principauxDansLaPeriode.add(forPrincipal);
					}
					else if (f instanceof ForFiscalSecondaire) {
						this.secondaires.add((ForFiscalSecondaire) f);
						this.secondairesDansLaPeriode.add((ForFiscalSecondaire) f);
					}
				}
				else if (DateRangeHelper.intersect(f, rangePeriode)) {
					// Le for est valide dans la période
					if (f.isPrincipal()) {
						this.principauxDansLaPeriode.add((ForFiscalPrincipal) f);
					}
					else {
						this.secondairesDansLaPeriode.add((ForFiscalSecondaire) f);
					}
				}

				if (debut != null && f.isValidAt(debut.getOneDayBefore())) {
					// le for est valide juste avant la période
					if (f instanceof ForFiscalPrincipal) {
						Assert.isNull(forPrincipalAvant);
						forPrincipalAvant = (ForFiscalPrincipal) f;
					}
					else if (f instanceof ForFiscalSecondaire) {
						this.secondairesAvantLaPeriode.add((ForFiscalSecondaire) f);
					}
				}

				if (fin != null && f.isValidAt(fin.getOneDayAfter())) {
					// le for est valide juste après la période
					if (f instanceof ForFiscalPrincipal) {
						Assert.isNull(forPrincipalApres);
						forPrincipalApres = (ForFiscalPrincipal) f;
					}
					else if (f instanceof ForFiscalSecondaire) {
						this.secondairesApresLaPeriode.add((ForFiscalSecondaire) f);
					}
				}
			}
		}

		this.principal = forPrincipal;
		this.principalAvantLaPeriode = forPrincipalAvant;
		this.principalApresLaPeriode = forPrincipalApres;
	}

	/**
	 * @return la date de début de la période considérée
	 */
	public RegDate getDateDebut() {
		return debut;
	}

	/**
	 * @return la date de fin de la période considérée
	 */
	public RegDate getDateFin() {
		return fin;
	}

	/**
	 * @return vrai s'il n'y a pas de for actif le dernier jour de la période.
	 */
	public boolean isEmpty() {
		return principal == null && secondaires.isEmpty();
	}

	/**
	 * @return vrai s'il n'y a pas su tout de for actif sur toute la période.
	 */
	public boolean isFullyEmpty() {
		return principauxDansLaPeriode.isEmpty() && secondairesDansLaPeriode.isEmpty();
	}

	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, getDateDebut(), getDateFin(), NullDateBehavior.LATEST);
	}
}
