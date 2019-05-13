package ch.vd.unireg.common;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public abstract class PaginationHelper {
	/**
	 * Construit une page d'une liste d'éléments.
	 *
	 * @param list        la liste des éléments source
	 * @param currentPage l'index de la page courante (0-based)
	 * @param pageSize    la taille de la page
	 * @param total       le nombre total de résultats.
	 * @param sort        l'ordre de tri demandé pour les résultats
	 * @return la page correspondant aux paramètres spécifiés.
	 */
	public static <T> Page<T> buildPage(List<T> list, int currentPage, int pageSize, long total, Sort sort) {

		if (currentPage < 0) {
			throw new IllegalArgumentException("L'index de la page doit être >= 0");
		}
		if (pageSize <= 0) {
			throw new IllegalArgumentException("La taille de la page doit être > 0");
		}
		if (total < 0) {
			throw new IllegalArgumentException("Le nombre total d'élément doit être >= 0");
		}

		// on construit la structure de résultat
		return new PageImpl<>(list,
		                      PageRequest.of(currentPage, pageSize, sort),
		                      total);
	}
}
