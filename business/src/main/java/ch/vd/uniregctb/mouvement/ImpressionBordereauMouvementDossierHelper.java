package ch.vd.uniregctb.mouvement;

import noNamespace.FichierImpressionDocument;

import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

public interface ImpressionBordereauMouvementDossierHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException;

	String construitIdDocument(BordereauMouvementDossier bordereau);
}
