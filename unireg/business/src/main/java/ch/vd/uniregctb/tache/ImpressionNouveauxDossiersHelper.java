package ch.vd.uniregctb.tache;

import java.util.List;

import noNamespace.TypFichierImpression;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.tiers.Contribuable;

public interface ImpressionNouveauxDossiersHelper {

	/**
	 * Alimente un objet Document pour l'impression des nouveaux dossiers
	 *
	 * @param contribuables
	 * @return
	 * @throws InfrastructureException
	 */
	public TypFichierImpression remplitNouveauDossier(List<Contribuable> contribuables) throws EditiqueException ;

	/**
	 * Calcul le prefixe
	 * @return
	 */

	public String calculPrefixe() ;

	/**
	 * Construit le champ idDocument
	 *
	 * @param declaration
	 * @return
	 */
	public String construitIdDocument(Contribuable contribuable) ;

}
