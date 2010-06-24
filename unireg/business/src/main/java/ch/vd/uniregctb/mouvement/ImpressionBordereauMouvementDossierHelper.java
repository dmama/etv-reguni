package ch.vd.uniregctb.mouvement;

import noNamespace.TypFichierImpression;

import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionBordereauMouvementDossierHelper {

	String calculePrefixe();

	TypFichierImpression remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException;

	String construitIdDocument(BordereauMouvementDossier bordereau);
}
