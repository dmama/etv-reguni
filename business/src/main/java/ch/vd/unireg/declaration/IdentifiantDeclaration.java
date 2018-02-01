package ch.vd.unireg.declaration;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

/**
 * Cette structure permet de lier un couple déclaration-tiers et de faciliter la mise à disposition
 * de ces informations dans le traitement d'une exception
 */
public final class IdentifiantDeclaration {

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_BY_DECL_ID = Comparator.comparingLong(o -> o.idDeclaration);

	public static final Comparator<IdentifiantDeclaration> COMPARATOR_NATUREL = Comparator.<IdentifiantDeclaration>comparingLong(o -> o.numeroTiers).thenComparingLong(o -> o.idDeclaration);

	private final long idDeclaration;
	private final long numeroTiers;
	private final Integer numeroOID;

	public IdentifiantDeclaration(long idDeclaration, long numeroTiers) {
		this(idDeclaration, numeroTiers, null);
	}

	public IdentifiantDeclaration(long idDeclaration, long numeroTiers, @Nullable Integer numeroOID) {
		this.numeroTiers = numeroTiers;
		this.idDeclaration = idDeclaration;
		this.numeroOID = numeroOID;
	}

	public IdentifiantDeclaration(Declaration declaration, @Nullable Integer numeroOID) {
		this(declaration.getId(), declaration.getTiers().getNumero(), numeroOID);
	}

	public long getIdDeclaration() {
		return idDeclaration;
	}

	public long getNumeroTiers() {
		return numeroTiers;
	}

	@Nullable
	public Integer getNumeroOID() {
		return numeroOID;
	}

	@Override
	public String toString() {
		return "IdentifiantDeclaration{" +
				"idDeclaration=" + idDeclaration +
				", numeroTiers=" + numeroTiers +
				", numeroOID=" + numeroOID +
				'}';
	}
}
