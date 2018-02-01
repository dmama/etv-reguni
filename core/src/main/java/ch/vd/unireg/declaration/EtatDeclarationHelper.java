package ch.vd.unireg.declaration;

import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EtatDeclarationHelper {

	public static EtatDeclaration getInstanceOfEtatDeclaration(TypeEtatDocumentFiscal typeEtat) {
		switch (typeEtat) {
		case ECHU:
			return new EtatDeclarationEchue();
		case SOMME:
			return new EtatDeclarationSommee();
		case EMIS:
			return new EtatDeclarationEmise();
		case RETOURNE:
			return new EtatDeclarationRetournee();
		default:
			throw new IllegalArgumentException("Valeur de l'état non-supportée : " + typeEtat);
		}
	}

	public static Class<? extends EtatDeclaration> getClasseOfEtatDeclaration(TypeEtatDocumentFiscal typeEtat) {
		switch (typeEtat) {
		case ECHU:
			return EtatDeclarationEchue.class;
		case SOMME:
			return EtatDeclarationSommee.class;
		case EMIS:
			return EtatDeclarationEmise.class;
		case RETOURNE:
			return EtatDeclarationRetournee.class;
		default:
			throw new IllegalArgumentException("Valeur de l'état non-supportée : " + typeEtat);
		}
	}
}
