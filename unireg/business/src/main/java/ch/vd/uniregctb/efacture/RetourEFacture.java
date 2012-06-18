package ch.vd.uniregctb.efacture;

public enum RetourEFacture {


	NUMERO_CTB_INCOHERENT( null,"Numéro de contribuable incohérent"),
	NUMERO_AVS_CTB_INCOHERENT(null,"Numéro AVS incohérent avec le numéro de contribuable"),
	ADRESSE_COURRIER_INEXISTANTE(null,"Aucune adresse courrier pour ce contribuable"),
	EN_ATTENTE_CONTACT(1,"Mise en attente: L'assujettissement est incoherent avec l'e-facture"),
	EN_ATTENTE_SIGNATURE(2,"En attente de confirmation d'inscritpion");


	private final Integer code;
	private final String description;


	RetourEFacture(Integer code,String motif) {
		this.code = code;
		this.description = motif;
	}

	public String getDescription() {
		return description;
	}
}
