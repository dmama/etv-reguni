package ch.vd.uniregctb.foncier.migration.mandataire;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.uniregctb.adresse.AdresseMandataireSuisse;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeMandat;

public class MigrationMandatImporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationMandatImporter.class);

	private static final int BATCH_SIZE = 10;

	private final ServiceInfrastructureService infraService;
	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;

	public MigrationMandatImporter(ServiceInfrastructureService infraService, PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate) {
		this.infraService = infraService;
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	private static class NpaLocalite {
		private final int npa;
		private final String localite;

		public NpaLocalite(int npa, String localite) {
			this.npa = npa;
			this.localite = localite;
		}

		public NpaLocalite(Localite localite) {
			this.npa = localite.getNPA();
			this.localite = localite.getNom();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final NpaLocalite that = (NpaLocalite) o;
			return npa == that.npa && (localite != null ? localite.equalsIgnoreCase(that.localite) : that.localite == null);
		}

		@Override
		public int hashCode() {
			int result = npa;
			result = 31 * result + (localite != null ? localite.toLowerCase().hashCode() : 0);
			return result;
		}
	}

	public MigrationMandatImporterResults importData(List<DonneesMandat> mandats, RegDate dateDebutMandats, GenreImpotMandataire genreImpot, StatusManager statusManager) {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);
		final List<Localite> localites = infraService.getLocalites();
		final Map<NpaLocalite, Integer> onrp = localites.stream()
				.sorted(Comparator.comparing(Localite::getDateDebut, NullDateBehavior.EARLIEST::compare))
				.collect(Collectors.toMap(NpaLocalite::new,
				                          Localite::getNoOrdre,
				                          (onrp1, onrp2) -> onrp2));
		final Map<Integer, Set<NpaLocalite>> byNPA = localites.stream()
				.collect(Collectors.toMap(Localite::getNPA,
				                          localite -> Collections.singleton(new NpaLocalite(localite)),
				                          (s1, s2) -> Stream.concat(s1.stream(), s2.stream()).collect(Collectors.toSet())));

		final BatchTransactionTemplateWithResults<DonneesMandat, MigrationMandatImporterResults> template = new BatchTransactionTemplateWithResults<>(mandats.iterator(),
		                                                                                                                                              mandats.size(),
		                                                                                                                                              BATCH_SIZE,
		                                                                                                                                              Behavior.REPRISE_AUTOMATIQUE,
		                                                                                                                                              transactionManager,
		                                                                                                                                              status);
		final MigrationMandatImporterResults rapportFinal = new MigrationMandatImporterResults(dateDebutMandats, genreImpot);
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		template.execute(rapportFinal, new BatchWithResultsCallback<DonneesMandat, MigrationMandatImporterResults>() {
			@Override
			public boolean doInTransaction(List<DonneesMandat> batch, MigrationMandatImporterResults rapport) throws Exception {
				status.setMessage("Importation des mandats", progressMonitor.getProgressInPercent());
				for (DonneesMandat mandat : batch) {
					final Contribuable ctb = hibernateTemplate.get(Contribuable.class, mandat.getNoContribuable());
					if (ctb == null) {
						rapport.addContribuableInconnu(mandat);
						continue;
					}

					if (!(ctb instanceof PersonnePhysique) && !(ctb instanceof Entreprise)) {
						rapport.addContribuableNonAcceptable(ctb, mandat);
						continue;
					}

					// on essaie d'abord de trouver le numéro d'ordre poste depuis le NPA et le nom exact de la localité
					final NpaLocalite npaLocalite = new NpaLocalite(mandat.getNpa(), mandat.getLocalite());
					Integer zipCodeId = onrp.get(npaLocalite);
					if (zipCodeId == null) {

						// pas trouvé avec le nom exact... peut-être qu'il est juste un peu mal écrit ?
						// - s'il n'y a qu'une seule localité avec le NPA donné, on prend celle-là
						// - s'il y en plus d'une, il va falloir creuser...
						final Set<NpaLocalite> npaLocaliteByNpa = byNPA.get(mandat.getNpa());
						if (npaLocaliteByNpa == null || npaLocaliteByNpa.isEmpty()) {
							rapport.addNpaLocaliteInconnu(mandat);
							continue;
						}

						// il n'y a qu'une localité avec ce numéro d'ordre poste
						if (npaLocaliteByNpa.size() == 1) {
							final NpaLocalite only = npaLocaliteByNpa.iterator().next();
							zipCodeId = onrp.get(only);
							if (zipCodeId != null) {
								// un peu de log...
								LOGGER.info("NPA Localité du mandat (" + mandat.getNpa() + " " + mandat.getLocalite() + ") -> (" + only.npa + " " + only.localite + ") -> ONRP " + zipCodeId);
							}
						}
						else {
							final Pattern pattern = Pattern.compile("^" + Pattern.quote(mandat.getLocalite()) + "\\b", Pattern.CASE_INSENSITIVE);

							// On tri ici par ordre alphabétique du nom de la localité pour que les "Renens VD 1" et "Renens VD 2" passent après "Renens VD"
							final SortedSet<NpaLocalite> sortedNpaLocaliteForNpa = new TreeSet<>(Comparator.comparing(e -> e.localite));
							sortedNpaLocaliteForNpa.addAll(npaLocaliteByNpa);
							for (NpaLocalite npaLoc : sortedNpaLocaliteForNpa) {
								final Matcher matcher = pattern.matcher(npaLoc.localite);
								if (matcher.find()) {
									zipCodeId = onrp.get(npaLoc);
									if (zipCodeId != null) {
										// un peu de log...
										LOGGER.info("NPA Localité du mandat (" + mandat.getNpa() + " " + mandat.getLocalite() + ") -> (" + npaLoc.npa + " " + npaLoc.localite + ") -> ONRP " + zipCodeId);
									}
									break;
								}
							}
						}

						if (zipCodeId == null) {
							rapport.addNpaLocaliteInconnu(mandat);
							continue;
						}
					}

					final AdresseMandataireSuisse adresseMandataire = new AdresseMandataireSuisse();
					final String nom = Stream.of(mandat.getNom1(), mandat.getNom2())
							.map(StringUtils::trimToNull)
							.filter(Objects::nonNull)
							.collect(Collectors.joining(" "));
					adresseMandataire.setCivilite(mandat.getFormulePolitesse());
					adresseMandataire.setCodeGenreImpot(genreImpot.getCode());
					adresseMandataire.setDateDebut(dateDebutMandats);
					adresseMandataire.setComplement(mandat.getAttentionDe());
					adresseMandataire.setMandant(ctb);
					adresseMandataire.setNomDestinataire(nom);
					adresseMandataire.setNoTelephoneContact(mandat.getNoTelephone());
					adresseMandataire.setNumeroOrdrePoste(zipCodeId);
					adresseMandataire.setRue(mandat.getRue());
					adresseMandataire.setTypeMandat(TypeMandat.SPECIAL);
					adresseMandataire.setWithCopy(mandat.isAvecCourrier());
					ctb.addAdresseMandataire(adresseMandataire);

					rapport.addAdresseMandataireCree(mandat);
				}
				return !status.interrupted();
			}

			@Override
			public MigrationMandatImporterResults createSubRapport() {
				return new MigrationMandatImporterResults(dateDebutMandats, genreImpot);
			}
		}, progressMonitor);

		rapportFinal.end();
		status.setMessage("Importation terminée.");
		return rapportFinal;
	}
}
