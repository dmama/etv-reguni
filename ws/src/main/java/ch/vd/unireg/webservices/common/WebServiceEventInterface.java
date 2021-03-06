package ch.vd.unireg.webservices.common;

import java.util.Set;

public interface WebServiceEventInterface {

	/**
	 * Cette méthode est appelée lorsqu'un ou plusieurs tiers ont été ajoutés/modifiés dans la base de données. A priori, tous les tiers impactés par les changements sont présents dans l'ensemble
	 * spécifié, il n'est donc pas utile de faire des recherches supplémentaires.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param ids les numéros des tiers
	 */
	void onTiersChange(Set<Long> ids);

	/**
	 * Cette méthode est appelée lorsqu'un immeuble RF a été ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param immeubleId l'id technique Unireg de l'immeuble.
	 */
	void onImmeubleChange(long immeubleId);

	/**
	 * Cette méthode est appelée lorsqu'un bâtiment RF a été ajouté/modifié dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param batimentId l'id technique Unireg du bâtiment.
	 */
	void onBatimentChange(long batimentId);

	/**
	 * Cette méthode est appelée lorsqu'une communauté de propriétaires RF a été ajoutée/modifiée dans la base de données.
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param communauteId l'id technique Unireg de la communauté.
	 */
	void onCommunauteChange(long communauteId);

	/**
	 * Cette méthode est appelée lorsque la base de données a été entièrement vidée (avant le chargement d'un script DBunit, par exemple).
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onTruncateDatabase();

	/**
	 * Cette méthode est appelée après que la base de données ait été chargée (après le chargement d'un script DBunit, par exemple).
	 * <p/>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 */
	void onLoadDatabase();
}
