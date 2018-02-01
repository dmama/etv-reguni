package ch.vd.unireg.declaration;

import java.util.Collection;
import java.util.Set;

import ch.vd.registre.base.dao.GenericDAO;

/**
 * Interface de base des DAO des objets "déclaration"
 * @param <T> type de déclaration
 */
public interface DeclarationDAO<T extends Declaration> extends GenericDAO<T, Long> {

	/**
	 * @param clazz classe de déclaration spécifique
	 * @param ids identifiants de déclarations
	 * @param <U> type spécifique de déclaration
	 * @return les déclarations chargées (avec leurs délais et états) d'après les identifiants fournis
	 */
	<U extends T> Set<U> getDeclarationsAvecDelaisEtEtats(Class<U> clazz, Collection<Long> ids);

	/**
	 * @param ids identifiants de déclarations
	 * @return les déclarations chargées (avec leurs délais et états) d'après les identifiants fournis
	 */
	Set<T> getDeclarationsAvecDelaisEtEtats(Collection<Long> ids);

}
