package ch.vd.unireg.hibernate;

import ch.vd.unireg.type.MotifFor;

public class MotifForUserType extends EnumUserType<MotifFor> {

	/**
	 * Constructeur : on passe la classe de l'Enum a EnumUserType
	 */
	public MotifForUserType() {
		super(MotifFor.class);
	}

}

