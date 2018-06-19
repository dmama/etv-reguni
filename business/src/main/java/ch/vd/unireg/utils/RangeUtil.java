package ch.vd.unireg.utils;

import java.util.List;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.CollectionsUtils;

/**
 * @author Raphaël Marmier, 2016-04-19, <raphael.marmier@vd.ch>
 */
public class RangeUtil {
	/**
	 * Obtenir le dernier range, en s'assurant que la date fournie en fait partie. Cela permet de garantir qu'on
	 * ne travail pas dans le passé lorsqu'on traite, par exemple, un événement entreprise. Si une ou des périodes existes
	 * ultérieurement à la date fournie, cela signifie que la situation a évolué depuis la date et qu'on ne peut plus opérer
	 * sur la base de celle-ci.
	 * @return le dernier range
	 * @throws RangeUtilException en cas d'erreur de données ou de paramètre
	 */
	public static <T extends DateRange> T getAssertLast(List<T> entites, RegDate date) throws RangeUtilException {
		if (entites != null && !entites.isEmpty()) {
			T lastRange = CollectionsUtils.getLastElement(entites);
			if (lastRange == null) {
				throw new RangeUtilException(String.format("Erreur de données: null trouvé dans une collection de périodes %s", entites.getClass()));
			}
			if (lastRange.getDateDebut() == null) {
				throw new RangeUtilException("Erreur de données: la date de début de la dernière période est nulle");
			}
			if (lastRange.getDateDebut().isAfter(date)) {
				throw new RangeUtilException(String.format("La période valide à la date demandée %s n'est pas la dernière de l'historique!", RegDateHelper.dateToDisplayString(date)));
			}
			return lastRange;
		}
		return null;
	}

	public static class RangeUtilException extends RuntimeException {
		public RangeUtilException(String message) {
			super(message);
		}
	}
}
