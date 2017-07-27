package ch.vd.uniregctb.validation;

import ch.vd.uniregctb.common.StringRenderer;

/**
 * Interface d'un bean de service qui permet de donner une chaîne de caractères pour un objet donné
 * (dans le contexte de la validation, il est parfois intéressant de pouvoir décrire un objet dans le message d'erreur)
 */
public interface ValidableEntityNamingService {

	/**
	 * Enregistre un nouveau {@link StringRenderer} pour le service de nommage
	 * @param clazz classe pour laquelle ce renderer est utilisable
	 * @param renderer renderer à enregistrer
	 * @param <T> type de la classe des objets à décrire
	 */
	<T> void registerEntityRenderer(Class<T> clazz, StringRenderer<? super T> renderer);

	/**
	 * @param object un objet
	 * @return une chaîne de caractères descriptive pour l'object (ou le résultat de {@link java.util.Objects#toString} en absence d'information)
	 */
	String getDisplayName(Object object);
}
