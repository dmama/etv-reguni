package ch.vd.uniregctb.declaration;

import java.util.Comparator;

/**
 * Cette structure permet de lier un couple déclaration-tiers et de faciliter la mise à disposition
 * de ces informations dans le traitement d'une exception
 */
public final class IdentifiantDeclaration {

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_DECL_ID = new Comparator<IdentifiantDeclaration>() {
		@Override
		public int compare(IdentifiantDeclaration o1, IdentifiantDeclaration o2) {
			return o1.idDeclaration < o2.idDeclaration ? -1 : (o1.idDeclaration > o2.idDeclaration ? 1 : 0);
		}
	};

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_TIERS_ID = new Comparator<IdentifiantDeclaration>() {
		@Override
		public int compare(IdentifiantDeclaration o1, IdentifiantDeclaration o2) {
			return o1.numeroTiers < o2.numeroTiers ? -1 : (o1.numeroTiers > o2.numeroTiers ? 1 : 0);
		}
	};

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_OID_ID = new Comparator<IdentifiantDeclaration>() {
		@Override
		public int compare(IdentifiantDeclaration o1, IdentifiantDeclaration o2) {
			return o1.numeroOID < o2.numeroOID ? -1 : (o1.numeroOID > o2.numeroOID ? 1 : 0);
		}
	};

	private final long idDeclaration;
	private final long numeroTiers;
	private final int numeroOID;

	public IdentifiantDeclaration(long idDeclaration, long numeroTiers) {
		this.numeroTiers = numeroTiers;
		this.idDeclaration = idDeclaration;
		this.numeroOID=0;
	}

	public IdentifiantDeclaration(long idDeclaration, long numeroTiers,int numeroOID) {
		this.numeroTiers = numeroTiers;
		this.idDeclaration = idDeclaration;
		this.numeroOID = numeroOID;
	}


	public long getIdDeclaration() {
		return idDeclaration;
	}

	public long getNumeroTiers() {
		return numeroTiers;
	}

	public int getNumeroOID() {
		return numeroOID;
	}
}
