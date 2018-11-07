package ch.vd.unireg.declaration.snc;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionDelaiQuestionnaireSNCHelper extends EditiqueAbstractHelper {

	/**
	 * @param delai                de déclaration du questionnaire SNC
	 * @param cleArchivageDocument clé d'archivage du document
	 * @param dateExpedition date expedition du document
	 * @return un document Editique correspondant à cette déclaration
	 */
	FichierImpression.Document buildDocument(DelaiDeclaration delai, String cleArchivageDocument, RegDate dateExpedition) throws EditiqueException;

	/**
	 * @param delai de déclaration du questionnaire SNC
	 * @return le type de document tel que vu par Editique
	 */
	TypeDocumentEditique getTypeDocumentEditique(DelaiDeclaration delai);

	String construitCleArchivageDocument(DelaiDeclaration delai);

	/**
	 * Construction d'un champ IDDocument (qui sert de businessID dans l'envoi ESB)
	 */
	String getIdDocument(DelaiDeclaration delai);

	String getDescriptionDocument(DelaiDeclaration delai);
}
