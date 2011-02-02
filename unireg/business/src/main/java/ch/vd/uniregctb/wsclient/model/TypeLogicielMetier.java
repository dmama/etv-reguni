package ch.vd.uniregctb.wsclient.model;

import ch.vd.uniregctb.type.LogicielMetier;

public enum TypeLogicielMetier {
	ECH_99(ch.vd.uniregctb.type.LogicielMetier.ECH_99),
	EMPACI(ch.vd.uniregctb.type.LogicielMetier.EMPACI);

	private ch.vd.uniregctb.type.LogicielMetier core;

	TypeLogicielMetier(LogicielMetier core) {
		this.core = core;
	}

	public static TypeLogicielMetier get(ch.vd.fidor.ws.v2.LogicielMetier right){
		if (right == null) {
			return null;
		}

		switch(right){
		case ECH_99:
				return TypeLogicielMetier.ECH_99;
		case EMPACI:
				return TypeLogicielMetier.EMPACI;
		 default:
			    throw new IllegalArgumentException("Valeur de logicielMetier non-supportée : " + right);

		}


	}
}
