package ch.vd.uniregctb.migration.pm.engine.log;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.log.AdresseLoggedElement;
import ch.vd.uniregctb.migration.pm.log.EmptyValuedLoggedElement;
import ch.vd.uniregctb.migration.pm.log.EntrepriseLoggedElement;
import ch.vd.uniregctb.migration.pm.log.EtablissementLoggedElement;
import ch.vd.uniregctb.migration.pm.log.ForPrincipalOuvertApresFinAssujLoggedElement;
import ch.vd.uniregctb.migration.pm.log.IndividuLoggedElement;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LoggedElement;
import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;
import ch.vd.uniregctb.migration.pm.log.RapportEntreTiersLoggedElement;

public abstract class LogStructure {

	/**
	 * Map qui décrit, pour chacune des catégories de log, la récupération des données contextuelles pour chaque ligne de log de cette
	 * catégorie (= quelles seront les colonnes à remplir, et quelles en sont les valeurs courantes)
	 */
	static final Map<LogCategory, List<Function<LogContexte, LoggedElement>>> STRUCTURES_CONTEXTES = buildStructuresContextes();

	/**
	 * Implémentation du mapping entre le contexte courant et une valeur spécifique, prenant en compte une valeur
	 * par défaut si le contexte est muet sur l'élément en question
	 */
	private static final class FromContextInformationSource implements Function<LogContexte, LoggedElement> {

		private final Class<? extends LoggedElement> clazz;
		private final LoggedElement defaultValue;

		public FromContextInformationSource(Class<? extends LoggedElement> clazz, @NotNull LoggedElement defaultValue) {
			this.clazz = clazz;
			this.defaultValue = defaultValue;
		}

		@Override
		public LoggedElement apply(LogContexte contexte) {
			final LoggedElement contextValue = contexte.getContextValue(clazz);
			return contextValue == null ? defaultValue : contextValue;
		}
	}

	/**
	 * Construction de la map des structures de contexte
	 * @return la map construite
	 */
	private static Map<LogCategory, List<Function<LogContexte, LoggedElement>>> buildStructuresContextes() {

		final LoggedElement level = new EmptyValuedLoggedElement(LoggedElementAttribute.NIVEAU);
		final Function<LogContexte, LoggedElement> donneesNiveau = ctxt -> level;
		final Function<LogContexte, LoggedElement> donneesEntreprise = new FromContextInformationSource(EntrepriseLoggedElement.class, EntrepriseLoggedElement.EMPTY);
		final Function<LogContexte, LoggedElement> donneesEtablissement = new FromContextInformationSource(EtablissementLoggedElement.class, EtablissementLoggedElement.EMPTY);
		final Function<LogContexte, LoggedElement> donneesIndividu = new FromContextInformationSource(IndividuLoggedElement.class, IndividuLoggedElement.EMPTY);
		final Function<LogContexte, LoggedElement> donneesAdresse = new FromContextInformationSource(AdresseLoggedElement.class, AdresseLoggedElement.EMPTY);
		final Function<LogContexte, LoggedElement> donneesRapportEntreTiers = new FromContextInformationSource(RapportEntreTiersLoggedElement.class, RapportEntreTiersLoggedElement.EMPTY);
		final Function<LogContexte, LoggedElement> donneesForsOuvertsApresFinAssuj = new FromContextInformationSource(ForPrincipalOuvertApresFinAssujLoggedElement.class, ForPrincipalOuvertApresFinAssujLoggedElement.EMPTY);

		final Map<LogCategory, List<Function<LogContexte, LoggedElement>>> map = new EnumMap<>(LogCategory.class);

		// Dans le log des établissements, le contexte impose le niveau de log en première colonne puis les données de l'établissement
		map.put(LogCategory.ETABLISSEMENTS, Arrays.asList(donneesNiveau, donneesEtablissement));

		// Dans le log des déclarations, le contexte impose le niveau de log en première colonne puis les données de l'entreprise
		map.put(LogCategory.DECLARATIONS, Arrays.asList(donneesNiveau, donneesEntreprise));

		// Dans le log des fors, le contexte impose le niveau de log en première colonne puis les données de l'entreprise
		map.put(LogCategory.FORS, Arrays.asList(donneesNiveau, donneesEntreprise));

		// Dans le log des adresses, le contexte impose le niveau de log en première colonne puis les données de l'entreprise, de l'établissement et de l'individu (cela peut venir de l'un ou de l'autre...)
		map.put(LogCategory.ADRESSES, Arrays.asList(donneesNiveau, donneesEntreprise, donneesEtablissement, donneesIndividu, donneesAdresse));

		// Dans le log des coordonnées financières, le contexte impose le niveau de log en première colonne puis les données de l'entreprise et de l'établissement (cela peut venir de l'un ou de l'autre...)
		map.put(LogCategory.COORDONNEES_FINANCIERES, Arrays.asList(donneesNiveau, donneesEntreprise, donneesEtablissement));

		// Dans le log des individus, le contexte impose le niveau de log en première colonne puis les données de l'individu
		map.put(LogCategory.INDIVIDUS_PM, Arrays.asList(donneesNiveau, donneesIndividu));

		// Dans le log de suivi, on met tout le contexte
		map.put(LogCategory.SUIVI, Arrays.asList(donneesNiveau, donneesEntreprise, donneesEtablissement, donneesIndividu));

		// Dans le log des assujettissements, on met le contexte de l'entreprise
		map.put(LogCategory.ASSUJETTISSEMENTS, Arrays.asList(donneesNiveau, donneesEntreprise));

		// Dans le log des données civiles de regpm, on met le contexte de l'entreprise et de l'établissement
		map.put(LogCategory.DONNEES_CIVILES_REGPM, Arrays.asList(donneesNiveau, donneesEntreprise, donneesEtablissement));

		// Dans la liste des rapports entre tiers, on ne met que ce contexte-là
		map.put(LogCategory.RAPPORTS_ENTRE_TIERS, Arrays.asList(donneesNiveau, donneesRapportEntreTiers));

		// Dans la liste des entreprises de forme juridique DP_APM, on met l'entreprise seulement
		map.put(LogCategory.DP_APM, Arrays.asList(donneesNiveau, donneesEntreprise));

		// Liste des fors ouverts après la fermeture de tous les assujettissements
		map.put(LogCategory.FORS_OUVERTS_APRES_FIN_ASSUJETTISSEMENT, Arrays.asList(donneesNiveau, donneesForsOuvertsApresFinAssuj));

		// Dans le log des erreurs, on ne met aucun contexte -> seul le texte sera affiché
		map.put(LogCategory.EXCEPTIONS, Collections.singletonList(donneesNiveau));

		return map;
	}

	/**
	 * @param contexte un contexte de log (= le contexte courant...)
	 * @param cat une catégorie de log
	 * @return la liste des {@link LoggedElement} qui constituent le contexte (= les premières colonnes)
	 */
	public static List<LoggedElement> resolveContextForCategory(LogContexte contexte, LogCategory cat) {
		final List<Function<LogContexte, LoggedElement>> structure = STRUCTURES_CONTEXTES.get(cat);
		return structure.stream()
				.map(f -> f.apply(contexte))
				.collect(Collectors.toList());
	}
}
