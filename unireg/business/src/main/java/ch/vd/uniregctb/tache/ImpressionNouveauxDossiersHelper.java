package ch.vd.uniregctb.tache;

import java.util.List;

import noNamespace.FichierImpressionDocument;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.TypeDocumentEditique;
import ch.vd.uniregctb.tiers.Contribuable;

public interface ImpressionNouveauxDossiersHelper {

	/**
	 * Alimente un objet Document pour l'impression des nouveaux dossiers
	 *
	 * @param contribuables
	 * @return
	 * @throws ServiceInfrastructureException
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
