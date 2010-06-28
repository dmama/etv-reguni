package ch.vd.uniregctb.evenement.changement.sexe;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.type.Sexe;
/**
 * Modèlise un événement de changement de sexe
 * @author <a href="mailto:baba-issa.ngom@vd.ch">Baba Issa NGOM </a>
 *
 */
public interface ChangementSexe extends EvenementCivil {
/**
 *
 * @return le nouveau sexe de l'individu
 */
	public Sexe getNouveauSexe();
}
