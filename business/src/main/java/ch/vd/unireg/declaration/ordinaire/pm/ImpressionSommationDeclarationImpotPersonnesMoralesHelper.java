package ch.vd.unireg.declaration.ordinaire.pm;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.editique.EditiqueAbstractHelper;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.xml.editique.pm.FichierImpression;

public interface ImpressionSommationDeclarationImpotPersonnesMoralesHelper extends EditiqueAbstractHelper {

	TypeDocumentEditique getTypeDocumentEditique();

	/**
	 * Construction du document à envoyer à l'éditique
	 */
	FichierImpression.Document buildDocument(DeclarationImpotOrdinairePM declaration, RegDate dateSommation, RegDate dateOfficielleEnvoi, boolean batch) throws EditiqueException;

	/**
	 * Construit le champ idDocument pour la sommation de la déclaration donnée
	 */
	String construitIdDocument(DeclarationImpotOrdinairePM declaration);

	/**
	 * Construit le champ idDocument pour l'archivage de la sommation de la déclaration donnée
	 */
	String construitCleArchivageDocument(DeclarationImpotOrdinairePM declaration);

}

