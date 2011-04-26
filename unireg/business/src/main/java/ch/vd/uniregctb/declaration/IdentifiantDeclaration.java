package ch.vd.uniregctb.declaration;

import java.util.Comparator;

/**
 * Cette structure permet de lier un couple déclaration-tiers et de faciliter la mise à disposition
 * de ces informations dans le traitement d'une exception
 */
public final class IdentifiantDeclaration {

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_DECL_ID = new Comparator<IdentifiantDeclaration>() {
		public int compare(IdentifiantDeclaration o1, IdentifiantDeclaration o2) {
			return o1.numeroDeclaration < o2.numeroDeclaration ? -1 : (o1.numeroDeclaration > o2.numeroDeclaration ? 1 : 0);
		}
	};

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_TIERS_ID = new Comparator<IdentifiantDeclaration>() {
		public int compare(IdentifiantDeclaration o1, IdentifiantDeclaration o2) {
			return o1.numeroTiers < o2.numeroTiers ? -1 : (o1.numeroTiers > o2.numeroTiers ? 1 : 0);
		}
	};

	private final long numeroDeclaration;
	private final long numeroTiers;

	public IdentifiantDeclaration(long numeroDeclaration, long numeroTiers) {
		this.numeroTiers = numeroTiers;
		this.numeroDeclaration = numeroDeclaration;
	}

	public long getNumeroDeclaration() {
		return numeroDeclaration;
	}

	public long getNumeroTiers() {
		return numeroTiers;
	}
}
