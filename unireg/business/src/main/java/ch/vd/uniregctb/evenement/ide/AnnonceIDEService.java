package ch.vd.uniregctb.evenement.ide;

import ch.vd.unireg.interfaces.organisation.data.AnnonceIDERCEnt;
import ch.vd.unireg.interfaces.organisation.data.ModeleAnnonceIDE;
import ch.vd.uniregctb.tiers.Etablissement;

/**
 * @author Raphaël Marmier, 2016-08-15, <raphael.marmier@vd.ch>
 */
public interface AnnonceIDEService {

	/**
	 * Expédie une annonce à l'IDE
	 *
	 * @param modele le modèle de l'annonce à publier (Son numero doit être vide)
	 * @param etablissement l'établissement concerné par l'annonce à l'IDE
	 * @return l'annonce telle qu'expédiée, avec son numéro (attention, un nouvel objet est retourné)
	 */
	AnnonceIDERCEnt emettreAnnonceIDE(ModeleAnnonceIDE modele, Etablissement etablissement);

}