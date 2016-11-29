package ch.vd.uniregctb.role;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.AnnulableHelper;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public abstract class RolePopulationExtractorImpl<T extends Contribuable> implements RolePopulationExtractor<T> {

	@NotNull
	private final Set<MotifFor> motifsFermetureFractionnementAssujettissement;

	public RolePopulationExtractorImpl(Set<MotifFor> motifsFermetureFractionnementAssujettissement) {
		this.motifsFermetureFractionnementAssujettissement = Optional.ofNullable(motifsFermetureFractionnementAssujettissement)
				.map(Collections::unmodifiableSet)
				.orElseGet(Collections::emptySet);
	}

	/**
	 * A implémenter par les sous-classes pour déterminer si un for fiscal doit être pris en compte ou pas
	 * @param ff for fiscal à tester
	 * @return <code>true</code> si on doit prendre en compte le for, <code>false</code> sinon
	 */
	protected abstract boolean isForAPrendreEnCompte(ForFiscal ff);

	@Nullable
	@Override
	public Integer getCommunePourRoles(int annee, T contribuable) {
		final RegDate debutAnnee = RegDate.get(annee, 1, 1);
		final RegDate finAnnee = RegDate.get(annee, 12, 31);

		final MovingWindow.Snapshot<ForFiscalPrincipal> ffp = getDernierForPrincipalAvant(contribuable, finAnnee);
		if (ffp == null || RegDateHelper.isBefore(ffp.getCurrent().getDateFin(), debutAnnee, NullDateBehavior.LATEST)) {
			// rien... -> pas dans les rôles
			return null;
		}

		// trois possibilités : VD, HC ou HS
		if (ffp.getCurrent().getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return getCommunePourForPrincipalVaudois(ffp, finAnnee);
		}

		// le dernier jour de validité du for principal dans l'année des rôles
		final RegDate dateReferenceDansAnnee = RegDateHelper.minimum(finAnnee, ffp.getCurrent().getDateFin(), NullDateBehavior.LATEST);

		// fors secondaires qui intersectent la période de l'année des rôles
		final DateRange rangeReference = new DateRangeHelper.Range(debutAnnee, dateReferenceDansAnnee);
		final List<ForFiscalSecondaire> forsSecondaires = contribuable.getForsFiscaux().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(this::isForAPrendreEnCompte)
				.filter(ff -> ff instanceof ForFiscalSecondaire)
				.map(ff -> (ForFiscalSecondaire) ff)
				.filter(ff -> DateRangeHelper.intersect(ff, rangeReference))
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());

		// si un des for secondaire au moins est encore ouvert à la date de référence, alors ce sont
		// les fors secondaires qui sont prépondérants sur le for principal vaudois éventuel présent
		// avant le for hors-Suisse
		final List<ForFiscalSecondaire> fsActifsFin = DateRangeHelper.rangesAt(forsSecondaires, dateReferenceDansAnnee);
		if (!fsActifsFin.isEmpty()) {
			// le for "prépondérant" est l'un des actifs (= le plus vieux)
			return fsActifsFin.stream()
					.min(RolePopulationExtractorImpl::compareForsSecondaires)
					.map(ForFiscalSecondaire::getNumeroOfsAutoriteFiscale)
					.get();
		}

		// il faut chercher la date de fermeture du dernier for vaudois sur la période
		final RegDate dateFermetureDernierForVaudois = Stream.concat(forsSecondaires.stream(), ffp.getAllPrevious().stream().filter(ff -> ff.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD))
				.map(ForFiscal::getDateFin)
				.max(Comparator.naturalOrder())
				.orElse(null);

		// aucun for vaudois avant, ou il y a bien trop longtemps... -> absent des rôles
		if (dateFermetureDernierForVaudois == null || dateFermetureDernierForVaudois.isBefore(debutAnnee)) {
			return null;
		}

		// si c'est le for principal vaudois qui est encore présent à la date de fermeture du dernier for vaudois,
		// ça dépend ensuite du type de fermeture...
		final ForFiscalPrincipal ffpvd = contribuable.getForFiscalPrincipalAt(dateFermetureDernierForVaudois);
		final TypeAutoriteFiscale taf = ffpvd.getTypeAutoriteFiscale();
		if (taf == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snap = getSnapshot(getForsPrincipaux(contribuable), ffpvd);
			final MotifFor motifFermeture = getMotifFermeture(snap);
			if (motifsFermetureFractionnementAssujettissement.contains(motifFermeture)) {
				return ffpvd.getNumeroOfsAutoriteFiscale();
			}
		}
		else if (taf == TypeAutoriteFiscale.PAYS_HS) {
			// ici, le dernier for vaudois ouvert n'était pas principal
			// il faut donc trouver lequel, parmi les secondaires encore valides à la date de clôture, était le plus vieux
			// et quel était le type d'autorité fiscale du for principal à ce moment-là :
			//      - HC -> tout est perdu
			//      - HS -> on conserve la commune du dernier for
			final List<ForFiscalSecondaire> fsActifs = DateRangeHelper.rangesAt(forsSecondaires, dateFermetureDernierForVaudois);
			return fsActifs.stream()
					.min(RolePopulationExtractorImpl::compareForsSecondaires)
					.map(ForFiscalSecondaire::getNumeroOfsAutoriteFiscale)
					.get();
		}

		// rien (en général des considérations HC...)
		return null;
	}

	/**
	 * Comparaison entre deux fors secondaires&nbsp;:<br/>
	 * <ul>
	 *     <li>par date de début croissante</li>
	 *     <li>à dates de début égales, par numéro OFS croissant</li>
	 *     <li>à dates de début égales et numéros OFS égaux, par identifiant croissant</li>
	 * </ul>
	 * @param fs1 un for secondaire à comparer
	 * @param fs2 un autre for secondaire à comparer au premier
	 * @return &lt; 0 si fs1 &lt; fs2, 0 s'ils sont égaux, et &gt; 0 si fs1 &gt; fs2
	 */
	private static int compareForsSecondaires(@NotNull ForFiscalSecondaire fs1, @NotNull ForFiscalSecondaire fs2) {
		int comparison = NullDateBehavior.EARLIEST.compare(fs1.getDateDebut(), fs2.getDateDebut());
		if (comparison == 0) {
			comparison = Integer.compare(fs1.getNumeroOfsAutoriteFiscale(), fs2.getNumeroOfsAutoriteFiscale());
			if (comparison == 0) {
				comparison = Long.compare(fs1.getId(), fs2.getId());
			}
		}
		return comparison;
	}

	@NotNull
	private static MovingWindow.Snapshot<ForFiscalPrincipal> getSnapshot(List<? extends ForFiscalPrincipal> tous, ForFiscalPrincipal elt) {
		final MovingWindow<ForFiscalPrincipal> wnd = new MovingWindow<>(tous);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snap = wnd.next();
			if (snap.getCurrent() == elt) {
				return snap;
			}
		}
		throw new IllegalArgumentException("L'élément proposé n'est pas dans la collection fournie...");
	}

	/**
	 * Méthode qui va chercher les fors principaux intéressants
	 * @param ctb contribuable concerné
	 * @return une liste éventuellement épurée des fors fiscaux principaux triés du contribuable
	 */
	private List<ForFiscalPrincipal> getForsPrincipaux(Contribuable ctb) {
		return ctb.getForsFiscauxPrincipauxActifsSorted().stream()
				.filter(this::isForAPrendreEnCompte)
				.collect(Collectors.toList());
	}

	@Nullable
	private MovingWindow.Snapshot<ForFiscalPrincipal> getDernierForPrincipalAvant(Contribuable contribuable, RegDate date) {
		final List<ForFiscalPrincipal> forsAPrendreEnCompte = getForsPrincipaux(contribuable);
		final MovingWindow<ForFiscalPrincipal> wnd = new MovingWindow<>(forsAPrendreEnCompte);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipal> snap = wnd.next();
			final ForFiscalPrincipal current = snap.getCurrent();
			if (current.isValidAt(date)
					|| (snap.getNext() == null && RegDateHelper.isBefore(current.getDateFin(), date, NullDateBehavior.LATEST))
					|| (snap.getNext() != null && RegDateHelper.isBefore(current.getDateFin(), date, NullDateBehavior.LATEST) && RegDateHelper.isAfter(snap.getNext().getDateDebut(), date, NullDateBehavior.LATEST))) {
				return snap;
			}
			else if (RegDateHelper.isAfter(current.getDateDebut(), date, NullDateBehavior.LATEST)) {
				// pas la peine d'itérer davantage, les fors sont triés en entrée
				break;
			}
		}
		return null;
	}

	@Nullable
	private static MotifFor getMotifFermeture(MovingWindow.Snapshot<ForFiscalPrincipal> snap) {
		final ForFiscalPrincipal current = snap.getCurrent();
		if (current.getDateFin() == null) {
			return null;
		}

		final ForFiscalPrincipal next = snap.getNext();
		if (next == null || next.getDateDebut().getOneDayBefore() != current.getDateFin()) {
			// on fait confiance, pas le choix...
			return current.getMotifFermeture();
		}

		final TypeAutoriteFiscale avant = current.getTypeAutoriteFiscale();
		final TypeAutoriteFiscale apres = next.getTypeAutoriteFiscale();
		if (avant == apres) {
			return MotifFor.DEMENAGEMENT_VD;
		}
		else if (apres == TypeAutoriteFiscale.PAYS_HS) {
			return MotifFor.DEPART_HS;
		}
		else if (avant == TypeAutoriteFiscale.PAYS_HS) {
			return MotifFor.ARRIVEE_HS;
		}
		else if (apres == TypeAutoriteFiscale.COMMUNE_HC) {
			return MotifFor.DEPART_HC;
		}
		else {
			return MotifFor.ARRIVEE_HC;
		}
	}

	@Nullable
	private Integer getCommunePourForPrincipalVaudois(MovingWindow.Snapshot<ForFiscalPrincipal> ffp, RegDate finAnnee) {
		final ForFiscalPrincipal current = ffp.getCurrent();
		if (current.isValidAt(finAnnee) || motifsFermetureFractionnementAssujettissement.contains(getMotifFermeture(ffp))) {
			return ffp.getCurrent().getNumeroOfsAutoriteFiscale();
		}

		// cas du mariage, de la séparation, du départ HC... -> rien sur la PF
		return null;
	}

}
