package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.uniregctb.common.AnnulableHelper;

/**
 * Contient les fors fiscaux d'un contribuable décomposés par types et triés dans l'ordre chronologique.
 */
public class ForsParType {

	private static final List<Class<? extends ForFiscal>> CLASSES = Arrays.asList(ForFiscalPrincipalPP.class,
	                                                                              ForFiscalPrincipalPM.class,
	                                                                              ForFiscalSecondaire.class,
	                                                                              ForDebiteurPrestationImposable.class,
	                                                                              ForFiscalAutreElementImposable.class,
	                                                                              ForFiscalAutreImpot.class);

	public final List<ForFiscalPrincipalPP> principauxPP;
	public final List<ForFiscalPrincipalPM> principauxPM;
	public final List<ForFiscalSecondaire> secondaires;
	public final List<ForDebiteurPrestationImposable> dpis;
	public final List<ForFiscalAutreElementImposable> autreElementImpot;
	public final List<ForFiscalAutreImpot> autresImpots;

	public ForsParType(Set<ForFiscal> forsFiscaux, boolean sort) {
		this(buildMapFors(forsFiscaux, sort));
	}

	@NotNull
	private static Map<Class<? extends ForFiscal>, List<? extends ForFiscal>> buildMapFors(Set<ForFiscal> forsFiscaux, boolean sort) {
		if (forsFiscaux == null || forsFiscaux.isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<Class<? extends ForFiscal>, List<? extends ForFiscal>> map = forsFiscaux.stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toMap(ForsParType::extractClass,
				                          Collections::singletonList,
				                          ListUtils::union));
		if (sort) {
			final DateRangeComparator<ForFiscal> comparator = new DateRangeComparator<>();
			map.values().forEach(list -> list.sort(comparator));
		}
		return map;
	}

	@NotNull
	private static Class<? extends ForFiscal> extractClass(ForFiscal ff) {
		return CLASSES.stream()
				.filter(clazz -> clazz.isInstance(ff))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Type de for fiscal inconnu : " + ff.getClass().getName()));
	}

	@NotNull
	private <T extends ForFiscal> List<T> extractList(Map<Class<? extends ForFiscal>, List<? extends ForFiscal>> map, Class<T> clazz) {
		//noinspection unchecked
		final List<T> found = (List<T>) map.get(clazz);
		return found != null ? new ArrayList<>(found) : new ArrayList<>();          // on veut fournir des listes éditables (pour les fors fictifs du calcul d'assujettissement par exemple)
	}

	private ForsParType(Map<Class<? extends ForFiscal>, List<? extends ForFiscal>> map) {
		this.principauxPP = extractList(map, ForFiscalPrincipalPP.class);
		this.principauxPM = extractList(map, ForFiscalPrincipalPM.class);
		this.secondaires = extractList(map, ForFiscalSecondaire.class);
		this.dpis = extractList(map, ForDebiteurPrestationImposable.class);
		this.autreElementImpot = extractList(map, ForFiscalAutreElementImposable.class);
		this.autresImpots = extractList(map, ForFiscalAutreImpot.class);
	}

	public final boolean isEmpty() {
		return principauxPP.isEmpty() && principauxPM.isEmpty() && secondaires.isEmpty() && dpis.isEmpty() && autreElementImpot.isEmpty() && autresImpots.isEmpty();
	}
}
