package ch.vd.uniregctb.mouvement;

import noNamespace.TypFichierImpression;
import ch.vd.uniregctb.editique.EditiqueException;

/**
 * Interface définissant les méthodes pour l'éditique
 *
 * @author xcifde
 *
 */
public interface ImpressionBordereauEnvoiHelper {

	/**
	 * Forme le préfixe
	 * @return le prefixe
	 */
	public String calculPrefixe();

	/**
	 * Alimente la classe principale de mouvement de dossier
	 *
	 * @param mouvementDossier
	 * @param anneeFiscale
	 * @return un objet
	 * @throws EditiqueException
	 */
	public TypFichierImpression remplitBordereauEnvoi(MouvementDossier mouvementDossier, Long anneeFiscale) throws EditiqueException;

	/**
	 * Construit l'id du document
	 * @param mouvementDossier
	 * @return l'id
	 */
	public String construitIdDocument(MouvementDossier mouvementDossier);


}
