package ch.vd.unireg.declaration.ordinaire.pm;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;

public interface ImpressionLettreDecisionDelaiPMHelper extends EditiqueAbstractHelper {

	/**
	 * @param params détails de la demande de délai
	 * @return type de document éditique à générer
	 */
	TypeDocumentEditique getTypeDocumentEditique(ImpressionLettreDecisionDelaiPMHelperParams params);

	/**
	 * @param params détails de la demande de délai
	 * @return chaîne de caractères utilisée comme business id des échanges avec l'éditique
	 */
	String construitIdDocument(ImpressionLettreDecisionDelaiPMHelperParams params);

	/**
	 * @param params détails de la la demande de délai
	 * @return clé d'archivage pour le document éditique
	 */
	String construitIdArchivageDocument(ImpressionLettreDecisionDelaiPMHelperParams params);

	/**
	 * @param params détails de la demande de délai
	 * @param cleArchivage clé d'archivage pour le document généré
	 * @param dateExpedition
	 * @return données à envoyer à l'éditique pour le document
	 */
	FichierImpression.Document buildDocument(ImpressionLettreDecisionDelaiPMHelperParams params, String cleArchivage, RegDate dateExpedition) throws EditiqueException;
}
