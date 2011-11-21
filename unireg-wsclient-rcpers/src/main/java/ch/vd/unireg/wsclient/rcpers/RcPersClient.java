package ch.vd.unireg.wsclient.rcpers;

import java.util.Collection;

import ch.vd.evd0001.v3.ListOfPersons;
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
}
