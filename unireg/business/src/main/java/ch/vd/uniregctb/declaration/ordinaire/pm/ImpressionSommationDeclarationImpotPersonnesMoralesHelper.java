package ch.vd.uniregctb.declaration.ordinaire.pm;

import ch.vd.editique.unireg.FichierImpression;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.editique.EditiqueAbstractHelper;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;

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

