package ch.vd.unireg.tiers;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;

/**
 * Contient les fors fiscaux d'un contribuable à un moment donné, décomposés par types et triés dans l'ordre chronologique.
 */
public class ForsParTypeAt {

	public final ForFiscalPrincipal principal;
	public final List<ForFiscalSecondaire> secondaires;
	public final ForDebiteurPrestationImposable dpis;
	public final List<ForFiscalAutreElementImposable> autreElementImpot;
	public final List<ForFiscalAutreImpot> autresImpots;

	@Nullable
	private static <T extends ForFiscal> T elementAt(@NotNull Iterable<T> iterable, RegDate date) {
		return elementAt(StreamSupport.stream(iterable.spliterator(), false), date);
	}

	@Nullable
	private static <T extends ForFiscal> T elementAt(@NotNull Collection<T> collection, RegDate date) {
		return elementAt(collection.stream(), date);
	}

	@Nullable
	private static <T extends ForFiscal> T elementAt(@NotNull Stream<T> stream, RegDate date) {
		final List<T> all = elementsAt(stream, date);
		if (all.isEmpty()) {
			return null;
		}
		if (all.size() > 1) {
			throw new IllegalStateException("Found several elements for date " + date);
		}
		return all.get(0);
	}

	@NotNull
	private static <T extends ForFiscal> List<T> elementsAt(@NotNull List<T> collection, RegDate date) {
		return elementsAt(collection.stream(), date);
	}

	@NotNull
	private static <T extends ForFiscal> List<T> elementsAt(@NotNull Stream<T> stream, RegDate date) {
		return stream
				.filter(elt -> elt.isValidAt(date))
				.collect(Collectors.toList());
	}

	public ForsParTypeAt(Set<ForFiscal> forsFiscaux, RegDate date, boolean sort) {
		final ForsParType fpt = new ForsParType(forsFiscaux, sort);
		this.principal = elementAt(CollectionsUtils.merged(fpt.principauxPM, fpt.principauxPP), date);
		this.secondaires = elementsAt(fpt.secondaires, date);
		this.dpis = elementAt(fpt.dpis, date);
		this.autreElementImpot = elementsAt(fpt.autreElementImpot, date);
		this.autresImpots = elementsAt(fpt.autresImpots, date);
	}

}
