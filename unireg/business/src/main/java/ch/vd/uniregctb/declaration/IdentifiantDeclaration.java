package ch.vd.uniregctb.declaration;

/**
 *Cette structure permet de lier un couple declaration-tiers et de faciliter la mis à disposition   
 */
public class IdentifiantDeclaration {
	private Long numeroDeclaration;
	private Long numeroTiers;

	public IdentifiantDeclaration() {
	}

	public IdentifiantDeclaration(Long numeroDeclaration, Long numeroTiers) {
		this.numeroTiers = numeroTiers;
		this.numeroDeclaration = numeroDeclaration;
	}

	public Long getNumeroDeclaration() {
		return numeroDeclaration;
	}

	public void setNumeroDeclaration(Long numeroDeclaration) {
		this.numeroDeclaration = numeroDeclaration;
	}

	public Long getNumeroTiers() {
		return numeroTiers;
	}

	public void setNumeroTiers(Long numeroTiers) {
		this.numeroTiers = numeroTiers;
	}
}
