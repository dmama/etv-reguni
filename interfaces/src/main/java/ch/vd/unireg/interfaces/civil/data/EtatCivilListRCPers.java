package ch.vd.unireg.interfaces.civil.data;

import java.util.List;

/**
 * Liste générique d'états civils qui sont triés par ordre croissant des dates de début de validité.
 */
public class EtatCivilListRCPers extends AbstractEtatCivilList {

	private static final long serialVersionUID = 2599348418123122521L;

	public EtatCivilListRCPers(List<EtatCivil> list) {
		super(list);
	}
}
