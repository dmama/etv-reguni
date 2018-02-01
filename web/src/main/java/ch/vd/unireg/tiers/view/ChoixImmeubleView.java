package ch.vd.unireg.tiers.view;

import java.util.List;
import java.util.Set;

/**
 * Entité renvoyée par le controlleur des dégrèvements et exonération quand il s'agit
 * d'afficher les immeubles filtrés d'un contribuable et de connaître les valeurs possibles
 * pour les numéros de parcelles et différents indexes
 */
public class ChoixImmeubleView {

	private final List<ImmeubleView> immeubles;
	private final Set<Integer> numerosParcelles;
	private final Set<Integer> numerosIndex1;
	private final Set<Integer> numerosIndex2;
	private final Set<Integer> numerosIndex3;

	public ChoixImmeubleView(List<ImmeubleView> immeubles, Set<Integer> numerosParcelles, Set<Integer> numerosIndex1, Set<Integer> numerosIndex2, Set<Integer> numerosIndex3) {
		this.immeubles = immeubles;
		this.numerosParcelles = numerosParcelles;
		this.numerosIndex1 = numerosIndex1;
		this.numerosIndex2 = numerosIndex2;
		this.numerosIndex3 = numerosIndex3;
	}

	public List<ImmeubleView> getImmeubles() {
		return immeubles;
	}

	public Set<Integer> getNumerosParcelles() {
		return numerosParcelles;
	}

	public Set<Integer> getNumerosIndex1() {
		return numerosIndex1;
	}

	public Set<Integer> getNumerosIndex2() {
		return numerosIndex2;
	}

	public Set<Integer> getNumerosIndex3() {
		return numerosIndex3;
	}
}
