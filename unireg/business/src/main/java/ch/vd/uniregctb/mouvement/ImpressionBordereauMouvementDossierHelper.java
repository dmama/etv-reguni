package ch.vd.uniregctb.mouvement;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.editique.EditiqueException;

public interface ImpressionBordereauMouvementDossierHelper {

	String calculePrefixe();

	FichierImpressionDocument remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException;

	String construitIdDocument(BordereauMouvementDossier bordereau);
}
