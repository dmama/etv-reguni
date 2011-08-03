package ch.vd.uniregctb.declaration;

import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EtatDeclarationHelper {

	public static EtatDeclaration getInstanceOfEtatDeclaration(TypeEtatDeclaration typeEtat) {
		switch (typeEtat) {
		case ECHUE:
			return new EtatDeclarationEchue();
		case SOMMEE:
			return new EtatDeclarationSommee();
		case EMISE:
			return new EtatDeclarationEmise();
		case RETOURNEE:
			return new EtatDeclarationRetournee();
		default:
			throw new IllegalArgumentException("Valeur de l'état non-supportée : " + typeEtat);
		}
	}

	public static Class<? extends EtatDeclaration> getClasseOfEtatDeclaration(TypeEtatDeclaration typeEtat) {
		switch (typeEtat) {
		case ECHUE:
			return EtatDeclarationEchue.class;
		case SOMMEE:
			return EtatDeclarationSommee.class;
		case EMISE:
			return EtatDeclarationEmise.class;
		case RETOURNEE:
			return EtatDeclarationRetournee.class;
		default:
			throw new IllegalArgumentException("Valeur de l'état non-supportée : " + typeEtat);
		}
	}
}
