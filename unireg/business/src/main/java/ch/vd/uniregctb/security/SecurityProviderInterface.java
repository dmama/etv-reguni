package ch.vd.uniregctb.security;

import java.util.List;

import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Niveau;

/**
 * Interface du provider de sécurité qui regroupe la sécurité d'IFOSec (authentification) et celle d'Unireg (accès aux dossiers).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SecurityProviderInterface {

	/**
	 * Vérifie que l'opérateur spécifié possède le rôle spécifié.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un dossier particulier. Pour
	 * cela, il faut utiliser la méthode {@link #getDroitAcces(Tiers)}.
	 *
	 * @param role
	 *            le rôle dont on veut vérifier l'allocation.
	 * @param visaOperateur
	 *            le visa de l'opérateur
	 * @param codeCollectivite
	 *            le code de la collectivité de l'opérateur
	 *
	 * @return <b>vrai</b> si le rôle spécifié est alloué; <b>faux</b> autrement.
	 */
	public boolean isGranted(Role role, String visaOperateur, int codeCollectivite);

	/**
	 * Retourne les droits d'accès à un dossier particulier pour un opérateur particulier. Cette méthode charge le tiers spécifié à partir
	 * de la base de données.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link #isGranted(Role)}.
	 *
	 * @param visaOperateur
	 *            le visa de l'opérateur particulier.
	 * @param tiersId
	 *            l'id du tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 * @throws ObjectNotFoundException
	 *             si le tiers spécifié n'existe pas
	 */
	public Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException;

	/**
	 * Retourne les droits d'accès à une liste de dossiers particuliers pour un opérateur particulier. Cette méthode charge les tiers spécifiés à partir
	 * de la base de données.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link #isGranted(Role)}.
	 *
	 * @param visaOperateur
	 *            le visa de l'opérateur particulier.
	 * @param ids
	 *            les ids des tiers associés aux dossiers pour lesquels on veut obtenir les droits d'accès. Cette liste peut contenir des
	 *            valeurs nulles.
	 * @return une liste ordonnée dans l'ordre des ids d'entrée avec <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier;
	 *         <b>LECTURE</b> si l'opérateur possède un droit d'accès en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un
	 *         droit d'accès complet au dossier.
	 */
	public List<Niveau> getDroitAcces(String visa, List<Long> ids);
}
