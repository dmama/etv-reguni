package ch.vd.uniregctb.evenement.changement.nom;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.evenement.EvenementAdapterException;
import ch.vd.uniregctb.evenement.EvenementCivilData;
import ch.vd.uniregctb.evenement.GenericEvenementAdapter;
import ch.vd.uniregctb.evenement.common.EvenementCivilContext;

/**
 * Modélise un événement de changement de nom.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class ChangementNomAdapter extends GenericEvenementAdapter{// implements ChangementNom {

	protected static Logger LOGGER = Logger.getLogger(ChangementNomAdapter.class);

	/**
	 * Le nouveau nom.
	 */
	//private String nouveauNom;

	/**
	 * Le nouveau prénom.
	 */
	//private String nouveauPrenom;

	/**
	 * @return le nouveau nom.
	 */
	/*public String getNouveauNom() {
		return nouveauNom;
	}*/

	/**
	 * @param nouveauNom
	 *            The nouveauNom to set.
	 */
	/*public void setNouveauNom(String nouveauNom) {
		this.nouveauNom = nouveauNom;
	}*/

	/**
	 * @return Returns the nouveauPrenom.
	 */
/*	public String getNouveauPrenom() {
		return nouveauPrenom;
	}*/

	/**
	 * @param nouveauPrenom
	 *            The nouveauPrenom to set.
	 */
	/*public void setNouveauPrenom(String nouveauPrenom) {
		this.nouveauPrenom = nouveauPrenom;
	}*/

	protected ChangementNomAdapter(EvenementCivilData evenement, EvenementCivilContext context) throws EvenementAdapterException {
		super(evenement, context);
	}
}
