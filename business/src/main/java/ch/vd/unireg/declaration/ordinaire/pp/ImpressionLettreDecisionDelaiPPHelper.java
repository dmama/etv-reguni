package ch.vd.unireg.declaration.ordinaire.pp;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pp.FichierImpression;

public interface ImpressionLettreDecisionDelaiPPHelper extends EditiqueAbstractHelper {

	/**
	 * @param params détails de la demande de délai
	 * @return type de document éditique à générer
	 */
	TypeDocumentEditique getTypeDocumentEditique(ImpressionLettreDecisionDelaiPPHelperParams params);

	/**
	 * @param params détails de la demande de délai
	 * @return chaîne de caractères utilisée comme business id des échanges avec l'éditique
	 */
	String construitIdDocument(ImpressionLettreDecisionDelaiPPHelperParams params);

	/**
	 * @param params détails de la la demande de délai
	 * @return clé d'archivage pour le document éditique
	 */
	String construitIdArchivageDocument(ImpressionLettreDecisionDelaiPPHelperParams params);

	/**
	 * @param params détails de la demande de délai
	 * @param cleArchivage clé d'archivage pour le document généré
	 * @param dateExpedition
	 * @return données à envoyer à l'éditique pour le document
	 */
	FichierImpression.Document buildDocument(ImpressionLettreDecisionDelaiPPHelperParams params, String cleArchivage, RegDate dateExpedition) throws EditiqueException;
}
