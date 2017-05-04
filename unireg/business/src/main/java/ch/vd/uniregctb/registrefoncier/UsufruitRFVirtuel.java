package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;


/**
 * Usufruit virtuel générée à la volée pour un tiers RF donné. Cet usufruit possède les caractéristiques suivantes :
 * <ul>
 *     <li>il n'est pas persisté</li>
 *     <li>il n'a qu'un seul ayant-droit : le tiers RF donné</li>
 *     <li>il n'a qu'un seul immeuble : l'immeuble pointé par le chemin</li>
 *     <li>l'identificant du droit est toujours nul</li>
 *     <li>le numéro d'affaire est toujours nul</li>
 *     <li>il possède en plus le chemin vers l'immeuble concernée</li>
 * </ul>
 */
public class UsufruitRFVirtuel extends ServitudeRF {

	/**
	 * La liste des droits qui mène de l'ayant-droit à l'immeuble.
	 */
	private List<DroitRF> chemin;

	public List<DroitRF> getChemin() {
		return chemin;
	}

	public void setChemin(List<DroitRF> chemin) {
		this.chemin = chemin;
	}

	@Override
	public ServitudeRF duplicate() {
		throw new NotImplementedException();
	}
}
