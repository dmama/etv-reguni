package ch.vd.unireg.security;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.type.Niveau;

/**
 * Interface du service de sécurité sur les dossiers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SecuriteDossierService {

	/**
	 * Retourne les droits d'accès à un dossier particulier pour l'opérateur courant. Cette méthode charge le tiers spécifié à partir de la
	 * base de données.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link SecurityProviderInterface#isGranted(Role, String, int)}.
	 *
	 * @param tiersId
	 *            l'id du tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 * @throws ObjectNotFoundException
	 *             si le tiers spécifié n'existe pas
	 */
	Niveau getAcces(long tiersId) throws ObjectNotFoundException;

	/**
	 * Retourne les droits d'accès à un dossier particulier pour l'opérateur courant.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link SecurityProviderInterface#isGranted(Role, String, int)}.
	 *
	 * @param tiers
	 *            le tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 */
	Niveau getAcces(Tiers tiers);

	/**
	 * Retourne les droits d'accès à un dossier particulier pour un opérateur particulier.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link SecurityProviderInterface#isGranted(Role, String, int)}.
	 *
	 * @param visaOperateur
	 *            le visa de l'opérateur particulier.
	 * @param tiersId
	 *            l'id du tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 */
	Niveau getAcces(@NotNull String visaOperateur, long tiersId);

	/**
	 * Retourne les droits d'accès à une liste de dossiers particuliers pour un opérateur particulier.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link SecurityProviderInterface#isGranted(Role, String, int)}.
	 *
	 * @param visa
	 *            le visa de l'opérateur particulier.
	 * @param ids
	 *            les ids des tiers associés aux dossiers pour lesquels on veut obtenir les droits d'accès. Cette liste peut contenir des
	 *            valeurs nulles.
	 * @return une liste ordonnée dans l'ordre des ids d'entrée avec <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier;
	 *         <b>LECTURE</b> si l'opérateur possède un droit d'accès en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un
	 *         droit d'accès complet au dossier.
	 */
	List<Niveau> getAcces(@NotNull String visa, List<Long> ids);
}
