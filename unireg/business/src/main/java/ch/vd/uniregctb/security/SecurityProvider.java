package ch.vd.uniregctb.security;

import java.util.List;

import org.springframework.util.Assert;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Niveau;

/**
 * Provider de sécurité qui regroupe la sécurité d'IFOSec (authentification) et celle d'Unireg (accès aux dossiers) et l'expose avec une
 * interface statique.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class SecurityProvider {

	private static SecurityProviderInterface provider;

	public void setProvider(SecurityProviderInterface provider) {
		SecurityProvider.provider = provider;
	}

	// pour le testing uniquement
	public static SecurityProviderInterface getProvider() {
		return provider;
	}

	/**
	 * Vérifie que l'opérateur courant possède le rôle spécifié.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un dossier particulier. Pour
	 * cela, il faut utiliser la méthode {@link #getDroitAcces(Tiers)}.
	 *
	 * @param role
	 *            le rôle dont on veut vérifier l'allocation.
	 *
	 * @return <b>vrai</b> si le rôle spécifié est alloué; <b>faux</b> autrement.
	 */
	public static boolean isGranted(Role role) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer code = AuthenticationHelper.getCurrentOID();
		if (code == null) {
			return false;
		}
		return provider.isGranted(role, visa, code);
	}

	/**
	 * Vérifie si l'opérateur courant possède au moins un des rôles spécifiés.
	 * <p/>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un dossier particulier. Pour cela, il faut utiliser la méthode {@link #getDroitAcces(Tiers)}.
	 *
	 * @param roles les rôles dont on veut vérifier l'allocation.
	 * @return <b>vrai</b> si l'opérateur courant possède au moins un des rôles spécifiés; <b>faux</b> autrement.
	 */
	public static boolean isAnyGranted(Role... roles) {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		final Integer code = AuthenticationHelper.getCurrentOID();
		if (code == null) {
			return false;
		}
		for (Role role : roles) {
			if (provider.isGranted(role, visa, code)) {
				return true;
			}
		}
		return false;
	}

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
	public static boolean isGranted(Role role, String visaOperateur, int codeCollectivite) {
		return provider.isGranted(role, visaOperateur, codeCollectivite);
	}

	/**
	 * Retourne les droits d'accès à un dossier particulier pour l'opérateur courant. Cette méthode charge le tiers spécifié à partir de la
	 * base de données.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link #isGranted(Role)}.
	 *
	 * @param tiersId
	 *            l'id du tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 * @throws ObjectNotFoundException
	 *             si le tiers spécifié n'existe pas
	 */
	public static Niveau getDroitAcces(long tiersId) throws ObjectNotFoundException {
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		return provider.getDroitAcces(visa, tiersId);
	}

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
	public static Niveau getDroitAcces(String visaOperateur, long tiersId) throws ObjectNotFoundException {
		return provider.getDroitAcces(visaOperateur, tiersId);
	}

	/**
	 * Retourne les droits d'accès à un dossier particulier pour l'opérateur courant.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link #isGranted(Role)}.
	 *
	 * @param tiers
	 *            le tiers associé au dossier pour lequel on veut obtenir les droits d'accès.
	 * @return <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier; <b>LECTURE</b> si l'opérateur possède un droit d'accès
	 *         en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un droit d'accès complet au dossier.
	 */
	public static Niveau getDroitAcces(Tiers tiers) {
		Assert.notNull(tiers);
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		return provider.getDroitAcces(visa, tiers.getNumero());
	}

	/**
	 * Retourne les droits d'accès à une liste de dossiers particuliers pour l'opérateur courant.
	 * <p>
	 * <b>Attention !</b> Cette vérification ne tient pas compte d'une éventuelle restriction d'accès pour un rôle particulier. Pour cela,
	 * il faut utiliser la méthode {@link #isGranted(Role)}.
	 *
	 * @param ids
	 *            les ids des tiers associés aux dossiers pour lesquels on veut obtenir les droits d'accès. Cette liste peut contenir des
	 *            valeurs nulles.
	 * @return une liste ordonnée dans l'ordre des ids d'entrée avec <b>null</b> si l'opérateur ne possède aucun droit d'accès au dossier;
	 *         <b>LECTURE</b> si l'opérateur possède un droit d'accès en lecture seulement; et <b>ECRITURE</b> si l'opérateur possède un
	 *         droit d'accès complet au dossier.
	 */
	public static List<Niveau> getDroitsAcces(List<Long> ids) {
		Assert.notNull(ids);
		final String visa = AuthenticationHelper.getCurrentPrincipal();
		return provider.getDroitAcces(visa, ids);
	}
}
