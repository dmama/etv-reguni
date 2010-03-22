package ch.vd.uniregctb.editique;

import java.util.List;

import javax.jms.JMSException;

import ch.vd.editique.service.enumeration.TypeFormat;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.declaration.ordinaire.ModeleFeuilleDocumentEditique;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.TypeDocument;

/**
 * Service Editique. Ce service est dédié à la communication avec le service Editique permettant l'impression des divers documents.
 *
 * @author xcifwi (last modified by $Author: xcicfh $ @ $Date: 2007/08/15 06:14:15 $)
 * @version $Revision: 1.8 $
 */
public interface EditiqueService {

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression directe.
	 *
	 * @param object
	 *            l'object à sérialiser et à transmettre.
	 * @param nomDocument
	 *            le nom du document à transmettre à Editique.
	 * @param archive
	 * 			  indicateur d'archivage
	 * @throws EditiqueException
	 *             si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	EditiqueResultat creerDocumentImmediatement(String nomDocument, String typeDocument, TypeFormat typeFormat, Object object, boolean archive) throws EditiqueException, JMSException;

	/**
	 * Sérialise au format XML et transmet l'object en paramètre au service Editique JMS d'impression de masse.
	 *
	 * @param object
	 *            l'object à sérialiser et à transmettre.
	 * @param typeDocument
	 *            le type de document
	 * @param archive
	 * 			  indicateur d'archivage
	 * @throws EditiqueException
	 *             si un problème survient durant la génération du XML ou durant la transmission du message au serveur JMS.
	 */
	void creerDocumentParBatch(Object object, String typeDocument, boolean archive) throws EditiqueException;

	/**
	 * Obitent un document pdf, sous forme binaire, identifié par les différents paramètres.
	 *
	 * @param noContribuable
	 *            l'identifiant du contribuable.
	 * @param typeDocument
	 *            le type de document.
	 * @param nomDocument
	 *            le nom du document.
	 * @return un document pdf, sous forme binaire.
	 */
	byte[] getPDFDeDocumentDepuisArchive(Long noContribuable, String typeDocument, String nomDocument) throws EditiqueException;

}
