package ch.vd.uniregctb.evenement.externe;

import java.util.Collection;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * DAO des événements externes.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementExterneDAO extends GenericDAO<EvenementExterne, Long> {

	/**
	 * Crée une nouvelle instance transient d'un événement externe.
	 * @param text le texte correspondant à l'événement.
	 * @param correlationId identifie l'unicité de l'événement.
	 * @return Retourne une nouvelle instance.
	 */
	EvenementExterne creerEvenementExterne(String text, String correlationId);

	void traceEvenementEnError(long id, String errorMessage) ;

	void traceEvenementTraite(long id) ;

	boolean existe(String correlationId);

	public Collection<EvenementExterne> getEvenementExternes(boolean ascending, EtatEvenementExterne... etatEvenementExternes);
}

