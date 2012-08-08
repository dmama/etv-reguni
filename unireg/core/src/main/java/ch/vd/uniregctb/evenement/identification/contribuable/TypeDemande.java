package ch.vd.uniregctb.evenement.identification.contribuable;

/**
 @author
 */
public enum TypeDemande {
	MELDEWESEN("MELDEWESEN"),
	NCS("NCS"),
	EMPACI("EMPACI"),
	E_FACTURE("E_FACTURE");


	private final String name;

	TypeDemande(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
