package ch.vd.unireg.declaration.snc;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionRappelQuestionnaireSNCHelper extends EditiqueAbstractHelper {

	/**
	 * Construction d'un champ IDDocument (qui sert de businessID dans l'envoi ESB)
	 */
	String getIdDocument(QuestionnaireSNC questionnaire);

	/**
	 * Construction d'une clé d'archivage pour le document de rappel
	 */
	String construitCleArchivageDocument(QuestionnaireSNC questionnaire);

	/**
	 * @param questionnaire un questionnaire SNC
	 * @return le type de document tel que vu par Editique
	 */
	TypeDocumentEditique getTypeDocumentEditique(QuestionnaireSNC questionnaire);

	/**
	 * @param questionnaire une déclaration d'impôt PM
	 * @return un document Editique correspondant à cette déclaration
	 */
	FichierImpression.Document buildDocument(QuestionnaireSNC questionnaire, RegDate dateRappel, RegDate dateEnvoiCourrier) throws EditiqueException;

}
