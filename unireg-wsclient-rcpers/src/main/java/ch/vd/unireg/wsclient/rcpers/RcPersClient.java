package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.registre.base.date.RegDate;

public interface RcPersClient {

	/**
	 * Récupère une ou plusieurs <i>personnes</i> (c'est-à-dire des individus, dans l'ancienne terminologie) dans le registre cantonal des personnes (RCPers).
	 *
	 * @param ids         les ids des personnes à retourner
	 * @param date        une date de validité (peut être nulle)
	 * @param withHistory <b>vrai</b> si les collections historisées doivent être renseignée; <b>faux</b> autrement.
	 * @return une liste de personnes
	 */
	ListOfPersons getPersons(Collection<Long> ids, RegDate date, boolean withHistory);

	/**
	 * Récupère les relations vers d'autres personnes (parents, enfants, conjoints, ...) d'une ou plusieurs <i>personnes</i>.
	 *
	 * @param ids         les ids des personnes à retourner
	 * @param date        une date de validité (peut être nulle)
	 * @param withHistory <b>vrai</b> si les collections historisées doivent être renseignée; <b>faux</b> autrement.
	 * @return une liste de relations entre personnes
	 */
	Relations getRelations(Collection<Long> ids, RegDate date, boolean withHistory);

	/**
	 * Récupère la personne liée à l'événement civil dont l'identifiant est donné
	 *
	 * @param eventId l'identifiant de l'événement civil
	 * @return la personne concernée par l'événement
	 */
	Person getPersonForEvent(long eventId);
}
