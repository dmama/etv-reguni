package ch.vd.unireg.mouvement;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionBordereauMouvementDossierHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	FichierImpressionDocument remplitBordereau(ImpressionBordereauMouvementDossierHelperParams params) throws EditiqueException;

	String construitIdDocument(BordereauMouvementDossier bordereau);
}
