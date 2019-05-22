package ch.vd.unireg.tache;

import java.util.List;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.TypeDocumentEditique;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.tiers.Contribuable;

public interface ImpressionNouveauxDossiersHelper {

	/**
	 * Alimente un objet Document pour l'impression des nouveaux dossiers
	 *
	 * @param contribuables
	 * @return
	 * @throws InfrastructureException
	 */
	FichierImpressionDocument remplitNouveauDossier(List<Contribuable> contribuables) throws EditiqueException ;

	/**
	 * Calcul le prefixe
	 * @return
	 */

	TypeDocumentEditique getTypeDocumentEditique() ;

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	String construitIdDocument(Contribuable contribuable) ;

}
