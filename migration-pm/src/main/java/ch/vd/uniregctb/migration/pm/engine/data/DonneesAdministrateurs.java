package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.MigrationContexte;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdministrateur;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFonction;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscalVD;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.RegimeFiscal;

public class DonneesAdministrateurs {

	private final List<RegpmAdministrateur> administrations;

	private static final StringRenderer<RegpmAdministrateur> FROM_INDIVIDU_RENDERER =
			adm -> String.format("%d sur l'entreprise %d (%s)", adm.getId().getSeqNo(), adm.getEntrepriseId(), adm.getFonction());

	private static final StringRenderer<RegpmAdministrateur> FROM_ENTREPRISE_RENDERER =
			adm -> String.format("%s par l'administrateur %s", adm.getFonction(), adm.getId().getIdIndividu());

	private DonneesAdministrateurs(List<RegpmAdministrateur> administrations) {
		this.administrations = administrations == null ? Collections.emptyList() : administrations;
	}

	public List<RegpmAdministrateur> getAdministrations() {
		return administrations;
	}

	public Map<Long, List<RegpmAdministrateur>> getAdministrationsParEntreprise() {
		return parEntreprise(administrations.stream());
	}

	public Map<Long, List<RegpmAdministrateur>> getAdministrationsParAdministrateur() {
		return parAdministrateur(administrations.stream());
	}

	private static Map<Long, List<RegpmAdministrateur>> parEntreprise(Stream<RegpmAdministrateur> stream) {
		return stream.collect(Collectors.toMap(RegpmAdministrateur::getEntrepriseId,
		                                       Collections::singletonList,
		                                       (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
	}

	private static Map<Long, List<RegpmAdministrateur>> parAdministrateur(Stream<RegpmAdministrateur> stream) {
		return stream.collect(Collectors.toMap(adm -> adm.getId().getIdIndividu(),
		                                       Collections::singletonList,
		                                       (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList())));
	}

	/**
	 * @param mr collecteur de messages de suivi officiel
	 * @return version "silencieuse" du collecteur de messages de suivi (= qui ne collecte pas, mais transmet le reste des appels)
	 */
	private static MigrationResultProduction silentify(MigrationResultProduction mr) {
		return new MigrationResultProduction() {
			@Override
			public void addMessage(LogCategory cat, LogLevel niveau, String msg) {
				// on ne fait rien, justement...
			}

			@Override
			public void addPostTransactionCallback(@NotNull Runnable callback) {
				mr.addPostTransactionCallback(callback);
			}

			@Override
			public <D> void addPreTransactionCommitData(@NotNull D data) {
				mr.addPreTransactionCommitData(data);
			}

			@Override
			public <T> T getExtractedData(Class<T> clazz, EntityKey key) {
				return mr.getExtractedData(clazz, key);
			}
		};
	}

	/**
	 * Construction des données d'administration depuis le côté de l'administrateur
	 * @param individu administrateur
	 * @param migrationContexte contexte de migration
	 * @param mr collecteur de messages de suivi
	 * @return les données extraites
	 */
	public static DonneesAdministrateurs fromAdministrateur(RegpmIndividu individu, MigrationContexte migrationContexte, MigrationResultProduction mr) {

		final Stream<RegpmAdministrateur> administrations = extractAdministrateursActifs(individu.getAdministrations(),
		                                                                                 migrationContexte,
		                                                                                 FROM_INDIVIDU_RENDERER,
		                                                                                 mr,
		                                                                                 LogCategory.INDIVIDUS_PM);

		// on ne logge pas de message pendant l'extraction des régimes fiscaux de l'entreprise car ces mêmes messages seront (ou ont été) de toute façon loggués quand on traite l'entreprise
		final MigrationResultProduction silentmr = silentify(mr);
		final Stream<RegpmAdministrateur> administrationsDeSocietesImmobilieres = administrations
				.filter(adm -> adm.getEntrepriseId() != null)       // dans le modèle RegPM, il y a aussi des administrateurs d'établissement (qui n'ont pas de lien vers des entreprises, donc...)
				.filter(adm -> {
					// les régimes fiscaux officiels
					final NavigableMap<RegDate, RegpmRegimeFiscalVD> regimes = migrationContexte.getRegimeFiscalHelper().buildMapRegimesFiscaux(adm.getRegimesFiscauxVD(),
					                                                                                                                            null,
					                                                                                                                            RegimeFiscal.Portee.VD,
					                                                                                                                            silentmr);
					return isSocieteImmobiliere(regimes, migrationContexte);
				});

		// comme on vient de l'administrateur, il ne faut prendre que les entreprises où il y a
		// un rôle ADMINISTRATEUR (il pourrait n'y avoir que PRESIDENT, auquel cas l'administration doit être ignorée)
		final Map<Long, List<RegpmAdministrateur>> parEntreprise = parEntreprise(administrationsDeSocietesImmobilieres);
		final List<RegpmAdministrateur> data = auMoinsRoleAdministrateur(parEntreprise);

		// on a fini ?
		return new DonneesAdministrateurs(data);
	}

	/**
	 * Construction des données d'administration depuis le côté de l'entreprise administrée
	 * @param entreprise entreprise administrée
	 * @param migrationContexte contexte de migration
	 * @param mr collecteur de messages de suivi
	 * @return les données extraites
	 */
	public static DonneesAdministrateurs fromEntrepriseAdministree(RegpmEntreprise entreprise, MigrationContexte migrationContexte, MigrationResultProduction mr) {
		final NavigableMap<RegDate, RegpmRegimeFiscalVD> vd = mr.getExtractedData(RegimesFiscauxHistoData.class, EntityKey.of(entreprise)).getVD();
		if (!isSocieteImmobiliere(vd, migrationContexte)) {
			return new DonneesAdministrateurs(null);
		}

		final Stream<RegpmAdministrateur> actifs = extractAdministrateursActifs(entreprise.getAdministrateurs(),
		                                                                        migrationContexte,
		                                                                        FROM_ENTREPRISE_RENDERER,
		                                                                        mr,
		                                                                        LogCategory.SUIVI);

		// il ne faut garder que les administrateurs qui ont ce rôle (pas ceux qui n'auraient que le rôle de PRESIDENT)
		final Map<Long, List<RegpmAdministrateur>> parAdministrateur = parAdministrateur(actifs);
		final List<RegpmAdministrateur> data = auMoinsRoleAdministrateur(parAdministrateur);

		// on a fini ?
		return new DonneesAdministrateurs(data);
	}

	/**
	 * Ne conserve que les listes dont au moins un des rôles est ADMINISTRATEUR, et applatit le tout
	 * @param map map des administrations
	 * @return la liste applatie
	 */
	private static List<RegpmAdministrateur> auMoinsRoleAdministrateur(Map<Long, List<RegpmAdministrateur>> map) {
		return map.values().stream()
				.filter(lst -> lst.stream().anyMatch(adm -> adm.getFonction() == RegpmFonction.ADMINISTRATEUR))
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	/**
	 * @param regimes la donnée officielle des régimes fiscaux vaudois de l'entreprise
	 * @param migrationContexte contexte de migration, accès aux services
	 * @return <code>true</code> si le dernier régime connu correspond à une société immobilière
	 */
	private static boolean isSocieteImmobiliere(NavigableMap<RegDate, RegpmRegimeFiscalVD> regimes,
	                                            MigrationContexte migrationContexte) {
		return !regimes.isEmpty() && migrationContexte.getRegimeFiscalHelper().isSocieteImmobiliere(regimes.lastEntry().getValue().getType());
	}

	/**
	 * Extraction de la liste des rapports d'administration actifs
	 * @param administrateurs l'ensemble des rapports d'administration
	 * @param migrationContexte contexte de migration, accès aux services et divers helpers
	 * @param renderer renderer pour les caractéristiques des liens d'administration (le besoin n'est pas le même si on vient côté entreprise ou côté individu)
	 * @param mr collecteur de messages de suivi
	 * @param logCategory catégorie de log à utiliser pour les messages de suivi
	 * @return un stream (dans le même ordre que celui présenté par l'ensemble initial) des données d'administration à prendre en compte
	 */
	private static Stream<RegpmAdministrateur> extractAdministrateursActifs(Set<RegpmAdministrateur> administrateurs,
	                                                                        MigrationContexte migrationContexte,
	                                                                        StringRenderer<? super RegpmAdministrateur> renderer,
	                                                                        MigrationResultProduction mr,
	                                                                        LogCategory logCategory) {

		return administrateurs.stream()
				.filter(adm -> !adm.isRectifiee())          // on ne migre de toute façon pas les cas annulés
				.filter(adm -> adm.getFonction() == RegpmFonction.ADMINISTRATEUR || adm.getFonction() == RegpmFonction.PRESIDENT)
				.filter(adm -> adm.getFins().stream().noneMatch(fin -> !fin.isRectifiee()))      // on ne prend en compte que les actifs (= sans fin non-annulée)
				.filter(adm -> {
			        if (adm.getDateEntreeFonction() == null) {
				        mr.addMessage(logCategory, LogLevel.ERROR,
				                      String.format("Le lien d'administration %s est ignorée car sa date de début est vide (ou antérieure au 01.08.1291).",
				                                    renderer.toString(adm)));
				        return false;
			        }
					return true;
				})
				.filter(adm -> {
					if (migrationContexte.getDateHelper().isFutureDate(adm.getDateEntreeFonction())) {
						mr.addMessage(logCategory, LogLevel.ERROR,
						              String.format("Lien d'administration %s ignoré en raison de sa date de début dans le futur (%s).",
						                            renderer.toString(adm),
						                            StringRenderers.DATE_RENDERER.toString(adm.getDateEntreeFonction())));
						return false;
					}
					return true;
				})
				.peek(adm -> migrationContexte.getDateHelper().checkDateLouche(adm.getDateEntreeFonction(),
				                                                               () -> String.format("%s avec date de début de validité", renderer.toString(adm)),
				                                                               logCategory,
				                                                               mr));
	}
}
