package ch.vd.uniregctb.editique;

/**
 * Représente le résultat retourné par Editique suite à une demande de création de document.
 */
public interface EditiqueResultat {

	/**
	 * @return la chaîne de caractère qui identifie un document de manière unique à travers une requête à éditique et la réponse d'éditique.
	 */
	String getIdDocument();
}
