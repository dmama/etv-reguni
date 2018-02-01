package ch.vd.unireg.evenement.reqdes.reception;

public final class ReqDesTransactionImmobiliere {

	private final String description;
	private final int ofsCommune;
	private final ModeInscriptionDansActe modeInscription;
	private final TypeInscriptionDansActe typeInscription;

	public ReqDesTransactionImmobiliere(String description, int ofsCommune, ModeInscriptionDansActe modeInscription, TypeInscriptionDansActe typeInscription) {
		this.description = description;
		this.ofsCommune = ofsCommune;
		this.modeInscription = modeInscription;
		this.typeInscription = typeInscription;
	}

	public String getDescription() {
		return description;
	}

	public int getOfsCommune() {
		return ofsCommune;
	}

	public ModeInscriptionDansActe getModeInscription() {
		return modeInscription;
	}

	public TypeInscriptionDansActe getTypeInscription() {
		return typeInscription;
	}
}
