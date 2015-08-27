package ch.vd.uniregctb.entreprise;

import java.util.List;

import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;

/**
 *  Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 *
 */
public interface EntrepriseService {

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	EntrepriseView get(Entreprise entreprise, List<DateRanged<Etablissement>> etablissements) ;
}
