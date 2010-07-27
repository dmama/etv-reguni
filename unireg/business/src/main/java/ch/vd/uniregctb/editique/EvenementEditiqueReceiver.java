package ch.vd.uniregctb.editique;

import ch.vd.editique.service.enumeration.TypeFormat;

public interface EvenementEditiqueReceiver {

	/**
	 * Récupère un document dans la queue de retour d'éditique. Le délai d'attente standard est appliqué si <i>appliqueDelai</i> est spécifié. Si le document n'est pas trouvé dans le queue, cette méthode
	 * retourn null.
	 *
	 * @param typeFormat    le format du document
	 * @param nomDocument   le nom du document
	 * @param appliqueDelai s'il faut applique le délai standard, ou non
	 * @return un document ou <b>null</b> si le document n'existe pas dans la queue de retour
	 * @throws Exception en cas de problème
	 */
	EditiqueResultat getDocument(TypeFormat typeFormat, String nomDocument, boolean appliqueDelai) throws Exception;
}
