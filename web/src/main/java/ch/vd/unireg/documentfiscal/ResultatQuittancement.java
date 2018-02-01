package ch.vd.unireg.documentfiscal;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.FormatNumeroHelper;

public class ResultatQuittancement {

	private static final ResultatQuittancement OK = new ResultatQuittancement();
	private static final ResultatQuittancement ENTREPRISE_INEXISTANTE = new ResultatQuittancement("Aucune entreprise connue avec le numéro %s.");
	private static final Map<TypeAutreDocumentFiscalQuittanceable, ResultatQuittancement> RIEN_A_QUITTANCER = buildMapRienAQuittancer();

	private static Map<TypeAutreDocumentFiscalQuittanceable, ResultatQuittancement> buildMapRienAQuittancer() {
		final Map<TypeAutreDocumentFiscalQuittanceable, ResultatQuittancement> map = new EnumMap<>(TypeAutreDocumentFiscalQuittanceable.class);
		for (TypeAutreDocumentFiscalQuittanceable type : TypeAutreDocumentFiscalQuittanceable.values()) {
			map.put(type, new ResultatQuittancement(String.format("Aucun document de type '%s' à quittancer pour l'entreprise %%s.", type.getDisplayName())));
		}
		return map;
	}

	private final boolean ok;
	@Nullable
	private final String causeErreur;

	private ResultatQuittancement() {
		this.ok = true;
		this.causeErreur = null;
	}

	private ResultatQuittancement(@NotNull String messageErreur) {
		this.ok = false;
		this.causeErreur = messageErreur;
	}

	@NotNull
	public static ResultatQuittancement entrepriseInexistante() {
		return ENTREPRISE_INEXISTANTE;
	}

	@NotNull
	public static ResultatQuittancement rienAQuittancer(TypeAutreDocumentFiscalQuittanceable type) {
		return RIEN_A_QUITTANCER.get(type);
	}

	@NotNull
	public static ResultatQuittancement ok() {
		return OK;
	}

	public boolean isOk() {
		return ok;
	}

	@Nullable("Null si ok est vrai, non-null si ok est faux")
	public String getCauseErreur(long noEntreprise) {
		return causeErreur == null ? null : String.format(causeErreur, FormatNumeroHelper.numeroCTBToDisplay(noEntreprise));
	}
}
