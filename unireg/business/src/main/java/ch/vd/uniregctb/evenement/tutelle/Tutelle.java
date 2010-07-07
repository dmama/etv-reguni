package ch.vd.uniregctb.evenement.tutelle;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TuteurGeneral;

/**
 * Modélise un événement de mise sous tutelle, curatelle ou conseil
 * legal.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public interface Tutelle extends EvenementCivil {

	/**
	 * Récupère le tuteur général.
	 * @return le tuteur général
	 */
	TuteurGeneral getTuteurGeneral();

	/**
	 * Récupère le tuteur du pupille.
	 *
	 * @return le tuteur du pupille
	 */
	Individu getTuteur();

	/**
	 * Récupère le type de la tutelle.
	 *
	 * @return le type de la tutelle
	 */
	TypeTutelle getTypeTutelle();
}
