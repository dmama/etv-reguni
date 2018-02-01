package ch.vd.unireg.declaration.snc;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionQuestionnaireSNCHelper extends EditiqueAbstractHelper {

	/**
	 * Construction d'un champ IDDocument (qui sert de businessID dans l'envoi ESB)
	 */
	String getIdDocument(QuestionnaireSNC questionnaire);

	/**
	 * @param questionnaire un questionnaire SNC
	 * @return le type de document tel que vu par Editique
	 */
	TypeDocumentEditique getTypeDocumentEditique(QuestionnaireSNC questionnaire);

	/**
	 * @param questionnaire une déclaration d'impôt PM
	 * @return un document Editique correspondant à cette déclaration
	 */
	FichierImpression.Document buildDocument(QuestionnaireSNC questionnaire) throws EditiqueException;

}
