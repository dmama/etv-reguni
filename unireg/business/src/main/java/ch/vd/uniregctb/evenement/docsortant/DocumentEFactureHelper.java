package ch.vd.uniregctb.evenement.docsortant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * Quelques méthodes utilitaires autour de la fonctionnalité des documents sortants e-facture
 */
public abstract class DocumentEFactureHelper {

	private static final Pattern IDENTIFIANT_PATTERN = Pattern.compile("(\\d{1,19})\\|(.+)");

	/**
	 * @param tiers un tiers
	 * @param cleArchivage la clé d'archivage utilisée pour un document e-facture de ce tiers
	 * @return une chaîne de caractères "encodée" qui contient les deux informations
	 * @see #decodeIdentifiant(String)
	 */
	public static String encodeIdentifiant(Tiers tiers, String cleArchivage) {
		return encodeIdentifiant(tiers.getNumero(), cleArchivage);
	}

	/**
	 * @param noTiers un numéro de tiers
	 * @param cleArchivage la clé d'archivage utilisée pour un document e-facture de ce tiers
	 * @return une chaîne de caractères "encodée" qui contient les deux informations
	 * @see #decodeIdentifiant(String)
	 */
	public static String encodeIdentifiant(long noTiers, String cleArchivage) {
		return String.format("%d|%s", noTiers, cleArchivage);
	}

	/**
	 * @param id un identifiant précédemment encodé avec {@link #encodeIdentifiant(Tiers, String)}
	 * @return les numéro de tiers et clé d'archivage extraits
	 * @throws IllegalArgumentException si l'identifiant donné n'est pas au bon format
	 */
	public static Pair<Long, String> decodeIdentifiant(String id) {
		final Matcher matcher = IDENTIFIANT_PATTERN.matcher(id);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Format d'identifiant non-reconnu : " + id);
		}

		final Long idTiers = Long.valueOf(matcher.group(1));
		final String cleArchivage = matcher.group(2);
		return Pair.of(idTiers, cleArchivage);
	}
}
