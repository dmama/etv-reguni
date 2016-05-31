package ch.vd.uniregctb.declaration.snc;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.QuestionnaireSNC;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

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
