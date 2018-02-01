package ch.vd.unireg.evenement.ide;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public interface AnnonceIDEService {

	/**
	 * Expédie une annonce à l'IDE
	 *
	 * @param proto le prototype de l'annonce à publier (Son numero doit être vide)
	 * @param etablissement l'établissement concerné par l'annonce à l'IDE
	 * @return l'annonce telle qu'expédiée, avec son numéro (attention, un nouvel objet est retourné)
	 */
	AnnonceIDE emettreAnnonceIDE(BaseAnnonceIDE proto, Etablissement etablissement) throws AnnonceIDEException;

}
