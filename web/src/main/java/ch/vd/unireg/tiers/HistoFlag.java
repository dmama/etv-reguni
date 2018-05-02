package ch.vd.unireg.tiers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

public enum HistoFlag {

	ADRESSES("adressesHisto"),
	ADRESSES_CIVILES("adressesHistoCiviles"),
	ADRESSES_CIVILES_CONJOINT("adressesHistoCivilesConjoint"),
	COORDONNEES_FINANCIERES("coordonneesHisto"),
	RAISONS_SOCIALES("raisonsSocialesHisto"),
	NOMS_ADDITIONNELS("nomsAdditionnelsHisto"),
	SIEGES("siegesHisto"),
	FORMES_JURIDIQUES("formesJuridiquesHisto"),
	CAPITAUX("capitauxHisto"),
	DOMICILES("domicilesHisto"),
	RAPPORTS_ENTRE_TIERS("rapportsEntreTiersHisto"),
	ETABLISSEMENTS("etablissementsHisto"),
	RAPPORTS_PRESTATION("rapportsPrestationHisto"),
	CTB_ASSOCIE("ctbAssocieHisto"),
	LABELS("labelsHisto"),
	LABELS_CONJOINT("labelsConjointHisto"),
	MANDATAIRES_COURRIER("mandatairesCourrierHisto"),
	MANDATAIRES_PERCEPTION("mandatairesPerceptionHisto"),
	SITUATIONS_FAMILLE("situFamilleHisto"),
	ALLEGEMENTS_FISCAUX("allegementsFiscauxHisto"),
	FLAGS_ENTREPRISE_SISUP("flagsEntrepriseHisto-SI_SERVICE_UTILITE_PUBLIQUE"),
	FLAGS_ENTREPRISE_LIBRE("flagsEntrepriseHisto-LIBRE"),
	REGIMES_FISCAUX_VD("regimesFiscauxVDHisto"),
	REGIMES_FISCAUX_CH("regimesFiscauxCHHisto"),
	PERIODICITES_HISTO("periodicitesHisto"),
	REMARQUES("remarquesHisto"),
	;

	private final String paramName;

	private static final Map<String, HistoFlag> FLAGS = indexFlags();

	private static Map<String, HistoFlag> indexFlags() {
		final Map<String, HistoFlag> map = Arrays.stream(HistoFlag.values()).collect(Collectors.toMap(HistoFlag::getParamName, Function.identity()));       // ça explose ici si plusieurs modalités ont le même nom...
		return Collections.unmodifiableMap(map);
	}

	HistoFlag(String paramName) {
		this.paramName = paramName;
	}

	public String getParamName() {
		return paramName;
	}

	@Nullable
	public static HistoFlag fromName(String paramName) {
		return FLAGS.get(paramName);
	}
}
