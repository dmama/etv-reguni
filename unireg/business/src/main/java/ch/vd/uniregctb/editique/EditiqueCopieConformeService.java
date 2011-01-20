package ch.vd.uniregctb.editique;

import java.io.InputStream;

/**
 * Service de récupération de copies conformes de documents éditiques archivés
 */
public interface EditiqueCopieConformeService {

	/**
	 * Récupère le document archivé (format PDF)
	 * @param numeroTiers numéro de tiers associé au document recherché
	 * @param typeDocument type de document
	 * @param nomDocument identifiant du document archivé
	 * @param contexte token identifiant le contexte de récupération de ce document depuis l'archive
	 * @return contenu (format PDF) du document archivé, ou <code>null</null> si un tel document n'existe pas
	 * @throws EditiqueException en cas de problème
	 */
	InputStream getPdfCopieConforme(long numeroTiers, String typeDocument, String nomDocument, String contexte) throws EditiqueException;
}
