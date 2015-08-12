package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ech.ech0044.v2.NamedPersonId;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v5.FoundPerson;
import ch.vd.evd0001.v5.Identity;
import ch.vd.evd0001.v5.ListOfFoundPersons;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.evd0001.v5.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.Equalator;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.fusion.FusionCommunesProvider;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;

public class IndividuMigrator extends AbstractEntityMigrator<RegpmIndividu> {

	private final TiersDAO tiersDAO;
	private final RcPersClient rcpersClient;
	private final NonHabitantIndex nonHabitantIndex;

	public IndividuMigrator(UniregStore uniregStore, ActivityManager activityManager, ServiceInfrastructureService infraService,
	                        TiersDAO tiersDAO, RcPersClient rcpersClient, NonHabitantIndex nonHabitantIndex, FusionCommunesProvider fusionCommunesProvider) {
		super(uniregStore, activityManager, infraService, fusionCommunesProvider);
		this.tiersDAO = tiersDAO;
		this.rcpersClient = rcpersClient;
		this.nonHabitantIndex = nonHabitantIndex;
	}

	private static String extractPrenomUsuel(String tousPrenoms) {
		return Arrays.stream(StringUtils.trimToEmpty(tousPrenoms).split("\\s+")).findFirst().orElse(null);
	}

	/**
	 * @return <code>true</code> si la personne rcpers correspond raisonablement bien à l'individu regpm (au cas où l'identifiant regpm aurait été ré-utilisé plus tard dans RCPers)
	 */
	private static boolean checkIdentite(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		boolean ok = checkNomPrenom(regpm, rcpers, mr);
		ok = checkSexe(regpm, rcpers, mr) && ok;
		ok = checkDateNaissance(regpm, rcpers, mr) && ok;
		if (!ok) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, "Correspondance d'identifiants avec individu RCPers ignorée.");
		}
		return ok;
	}

	private static boolean checkNomPrenom(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		final Equalator<String> nameEqualator = (d1, d2) -> (d1 == null && d2 == null) || (d1 != null && d1.equalsIgnoreCase(d2));
		final NomPrenom npRegpm = new NomPrenom(regpm.getNom(), regpm.getPrenom());
		final NomPrenom npRcpers = new NomPrenom(rcpers.getIdentity().getOfficialName(), rcpers.getIdentity().getFirstNames());
		final boolean equals = checkEquality(npRegpm.getNom(), npRcpers.getNom(), nameEqualator, false)
				&& (checkEquality(npRegpm.getPrenom(), npRcpers.getPrenom(), nameEqualator, false) || checkEquality(npRegpm.getPrenom(), extractPrenomUsuel(npRcpers.getPrenom()), nameEqualator, false));
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Noms différents dans RegPM (%s) et RCPers (%s).", npRegpm, npRcpers));
		}
		return equals;
	}

	private static boolean checkSexe(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		final Equalator<Enum> enumEqualator = (e1, e2) -> e1 == e2;
		final Sexe sRegpm = regpm.getSexe();
		final Sexe sRcpers = EchHelper.sexeFromEch44(rcpers.getIdentity().getSex());
		final boolean equals = checkEquality(sRegpm, sRcpers, enumEqualator, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Sexes différents dans RegPM (%s) et RCPers (%s).", sRegpm, sRcpers));
		}
		return equals;
	}

	private static boolean checkDateNaissance(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		final Equalator<RegDate> dateEqualator = (d1, d2) -> d1 == d2 || (d1 != null && d2 != null && (d1.isPartial() || d2.isPartial()) && d1.compareTo(d2) == 0);
		final RegDate dRegpm = regpm.getDateNaissance();
		final RegDate dRcpers = EchHelper.partialDateFromEch44(rcpers.getIdentity().getDateOfBirth());
		final boolean equals = checkEquality(dRegpm, dRcpers, dateEqualator, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Dates de naissance différentes dans RegPM (%s) et RCPers (%s).", dRegpm, dRcpers));
		}
		return equals;
	}

	/**
	 * @return <code>true</code> si les deux données sont nulles, ou égales (au sens de l'équalator donné) ou, si oneNullIsOk est <code>true</code>, si l'une des valeurs est nulle
	 */
	private static <T> boolean checkEquality(@Nullable T data1, @Nullable T data2, @NotNull Equalator<? super T> equalator, boolean oneNullIsOk) {
		if (oneNullIsOk && (data1 == null || data2 == null)) {
			return true;
		}

		if (data1 == data2) {
			return true;
		}
		return !(data1 == null || data2 == null) && equalator.test(data1, data2);
	}

	/**
	 * Extraction du numéro d'individu "RCPers" de la liste des identifiants d'une personne renvoyée par RCPers
	 * @param ids liste des identifiants
	 * @return le numéro d'individu, au sens "unireg"
	 */
	private static long getRcPersId(List<NamedPersonId> ids) {
		return ids.stream()
				.filter(id -> "CT.VD.RCPERS".equals(id.getPersonIdCategory()))
				.map(NamedPersonId::getPersonId)
				.map(Long::parseLong)
				.findAny()
				.orElseThrow(() -> new IllegalStateException("Individu RCPers sans identifiant CT.VD.RCPERS (" + Arrays.toString(ids.toArray(new NamedPersonId[ids.size()])) + ")"));
	}

	@NotNull
	@Override
	protected EntityKey buildEntityKey(RegpmIndividu entity) {
		return buildIndividuKey(entity);
	}

	@Override
	protected void doMigrate(RegpmIndividu regpm, MigrationResultContextManipulation mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		if (idMapper.hasMappingForIndividu(regpm.getId())) {
			// l'individu a déjà été migré, pas la peine d'aller plus loin
			return;
		}

		// individu migré à l'époque dans RCPers avec le numéro
		final Person migreRCPers = getFromRCPersWithId(mr, regpm.getId());
		final Pair<Long, Boolean> trouveRCPersId;       // long = ID, boolean = flag habitant
		if (migreRCPers != null && checkIdentite(regpm, migreRCPers, mr)) {
			// cet individu a toutes les caractéristiques, on va dire que c'est lui
			trouveRCPersId = Pair.of(getRcPersId(migreRCPers.getIdentity().getOtherPersonId()), !migreRCPers.getCurrentResidence().isEmpty());
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, "Individu trouvé avec le même identifiant et la même identité dans RCPers.");
		}
		else {
			trouveRCPersId = searchDansRCPers(regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), regpm.getDateNaissance(), mr);
		}

		// passage du civil au fiscal
		final PersonnePhysique ppExistant;
		if (trouveRCPersId != null) {
			ppExistant = tiersDAO.getPPByNumeroIndividu(trouveRCPersId.getLeft());
		}
		else {
			ppExistant = rechercheNonHabitantExistant(regpm, mr);
		}

		// numéro de contribuable pour le log final
		final long noCtbPersonnePhysique;

		// création d'une personne physique au besoin (= réelle migration)
		if (ppExistant == null) {
			final PersonnePhysique pp = new PersonnePhysique();
			if (trouveRCPersId != null) {
				pp.setNumeroIndividu(trouveRCPersId.getLeft());
				pp.setHabitant(trouveRCPersId.getRight());
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, String.format("Individu %d trouvé dans RCPers sans équivalent dans Unireg...", trouveRCPersId.getLeft()));

				// Dans le cas d'un non-habitant (= ancien habitant, donc...), il faut recopier certaines données dans Unireg
				if (!pp.isHabitantVD()) {
					final Person rcpers = getFromRCPersWithId(mr, trouveRCPersId.getLeft());
					if (rcpers == null) {
						// bizarre, RCPers ne nous renvoie rien... on prend les données de RegPM...
						pp.setNom(regpm.getNom());
						pp.setTousPrenoms(regpm.getPrenom());
						pp.setPrenomUsuel(extractPrenomUsuel(regpm.getPrenom()));
						pp.setSexe(regpm.getSexe());
						pp.setDateNaissance(regpm.getDateNaissance());
						pp.setDateDeces(regpm.getDateDeces());
					}
					else {
						// on recopie les données (minimales) de RCPers
						final Identity identity = rcpers.getIdentity();
						pp.setNom(identity.getOfficialName());
						pp.setTousPrenoms(identity.getFirstNames());
						if (StringUtils.isNotBlank(identity.getCallName())) {
							pp.setPrenomUsuel(identity.getCallName());
						}
						else {
							pp.setPrenomUsuel(extractPrenomUsuel(identity.getFirstNames()));
						}
						pp.setSexe(EchHelper.sexeFromEch44(identity.getSex()));
						pp.setDateNaissance(EchHelper.partialDateFromEch44(identity.getDateOfBirth()));
						pp.setDateDeces(XmlUtils.xmlcal2regdate(rcpers.getDateOfDeath()));
					}
				}
			}
			else {
				pp.setNom(regpm.getNom());
				pp.setTousPrenoms(regpm.getPrenom());
				pp.setPrenomUsuel(extractPrenomUsuel(regpm.getPrenom()));
				pp.setSexe(regpm.getSexe());
				pp.setDateNaissance(regpm.getDateNaissance());
				pp.setDateDeces(regpm.getDateDeces());
				pp.setHabitant(Boolean.FALSE);
			}

			// TODO il y a sûrement d'autres choses à migrer (adresses...)
			// TODO si l'individu est indiqué comme marié dans regpm/rcpers, ne faudrait-il pas créer un ménage commun aussi ?

			final PersonnePhysique saved = uniregStore.saveEntityToDb(pp);
			idMapper.addIndividu(regpm, saved);
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO,
			              String.format("Création de la personne physique %s pour correspondre à l'individu RegPM.", FormatNumeroHelper.numeroCTBToDisplay(saved.getId())));

			// capture du nouveau numéro de contribuable
			noCtbPersonnePhysique = saved.getId();

			// enregistrement d'un callback appelé une fois la transaction committée, afin de placer cette nouvelle personne physique dans l'indexeur ad'hoc
			// (ceci fonctionne sans création d'une nouvelle transaction car l'accès aux données à indexer ne nécessite pas de nouvel accès en base)
			mr.addPostTransactionCallback(() -> nonHabitantIndex.index(saved, regpm.getId()));
		}
		else {
			idMapper.addIndividu(regpm, ppExistant);
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Trouvé personne physique existante %s.", FormatNumeroHelper.numeroCTBToDisplay(ppExistant.getId())));

			// capture du numéro de contribuable utilisé
			noCtbPersonnePhysique = ppExistant.getId();

			// enregistrement d'un callback appelé une fois la transaction committée, afin d'associer personne physique existante avec le numéro RegPM dans l'indexeur ad'hoc
			// (ceci fonctionne sans création d'une nouvelle transaction car l'accès aux données à indexer ne nécessite pas de nouvel accès en base)
			mr.addPostTransactionCallback(() -> nonHabitantIndex.reindex(ppExistant, regpm.getId()));
		}

		// log de suivi à la fin des opérations pour cet individu
		mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Individu migré : %s.", FormatNumeroHelper.numeroCTBToDisplay(noCtbPersonnePhysique)));
	}

	@Nullable
	private PersonnePhysique rechercheNonHabitantExistant(RegpmIndividu regpm, MigrationResultProduction mr) {

		// on recherche éventuellement d'abord par idRegPM
		final NonHabitantIndex.NonHabitantSearchParameters paramsIdRegPm = new NonHabitantIndex.NonHabitantSearchParameters(null, null, null, null, regpm.getId());
		if (!paramsIdRegPm.isEmpty()) {
			final List<Long> idsPP = nonHabitantIndex.search(paramsIdRegPm, Integer.MAX_VALUE);
			if (idsPP != null && idsPP.size() == 1) {
				final long id = idsPP.get(0);
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Non-habitant %s déjà mappé sur le même individu PM dans une transaction précédente.", FormatNumeroHelper.numeroCTBToDisplay(id)));
				return (PersonnePhysique) tiersDAO.get(id);
			}
		}

		// rien trouvé directement par idRegPM -> on recherche par les autres critères
		final PersonnePhysique ppExistant;
		final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), regpm.getDateNaissance(), null);
		if (params.isEmpty()) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.ERROR, "Individu RegPM sans nom, prenom, sexe ni date de naissance.");
			ppExistant = null;
		}
		else {
			final List<Long> idsPP = nonHabitantIndex.search(params, Integer.MAX_VALUE);
			if (idsPP == null || idsPP.isEmpty()) {
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Aucun non-habitant trouvé dans Unireg avec ces nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
				                                                                                 regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), regpm.getDateNaissance()));
				ppExistant = null;
			}
			else if (idsPP.size() > 1) {
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, String.format("Plusieurs non-habitants trouvés dans Unireg avec ces nom (%s), prénom (%s), sexe (%s) et date de naissance (%s) : %s.",
				                                                                                 regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), regpm.getDateNaissance(),
				                                                                                 Arrays.toString(idsPP.toArray(new Long[idsPP.size()]))));
				ppExistant = null;
			}
			else {
				ppExistant = (PersonnePhysique) tiersDAO.get(idsPP.get(0));
			}
		}
		return ppExistant;
	}

	@Nullable
	private Person getFromRCPersWithId(MigrationResultProduction mr, long id) {
		final ListOfPersons list = rcpersClient.getPersons(Collections.singletonList(id), RegDate.get(), false);
		if (list == null || list.getNumberOfResults().equals(BigInteger.ZERO)) {
			// pas trouvé dans la liste des résidents RCPers...
			return null;
		}
		else if (!list.getNumberOfResults().equals(BigInteger.ONE)) {
			// plusieurs individus civils pour un numéro !!! -> erreur
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.ERROR, "L'identifiant " + id + " correspond à plus d'un individu dans RCPers.");
			return null;
		}

		// il n'y a bien qu'un seul résultat... voyons voir
		final ListOfPersons.ListOfResults.Result res = list.getListOfResults().getResult().get(0);
		if (res.getNotReturnedPersonReason() != null) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, "L'individu RCPers " + id + " ne peut être renvoyé (" + res.getNotReturnedPersonReason().getMessage() + ").");
		}

		return res.getPerson();
	}

	/**
	 * Recherche dans RCPers un individu correspondant aux caractèristiques données
	 * @param nom nom recherché
	 * @param prenom prénom recherché
	 * @param sexe sexe recherché
	 * @param dateNaissance date de naissance recherchée
	 * @param mr collecteur de messages de migration
	 * @return un couple (id,flagHabitant) si une seule personne a été trouvée, <code>null</code> sinon
	 */
	@Nullable
	private Pair<Long, Boolean> searchDansRCPers(String nom, String prenom, Sexe sexe, RegDate dateNaissance, MigrationResultProduction mr) {
		final RegDate dateNaissanceMin;
		final RegDate dateNaissanceMax;
		if (dateNaissance == null) {
			dateNaissanceMin = null;
			dateNaissanceMax = null;
		}
		else if (dateNaissance.isPartial()) {
			final RegDate[] range = dateNaissance.getPartialDateRange();
			dateNaissanceMin = range[0];
			dateNaissanceMax = range[1];
		}
		else {
			dateNaissanceMin = dateNaissance;
			dateNaissanceMax = dateNaissance;
		}
		final ListOfFoundPersons found = rcpersClient.findPersons(EchHelper.sexeToEch44(sexe), prenom, nom, null, null, null, null, Boolean.TRUE,
		                                                          null, null, null, null, null, null, null, null, dateNaissanceMin, dateNaissanceMax);

		// aucun résultat
		if (found == null || found.getNumberOfResults().equals(BigInteger.ZERO)) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Aucun résultat dans RCPers pour le nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
			                                                                                 nom, prenom, sexe, dateNaissance));
			return null;
		}

		// extraire le numéro RCPers
		final Function<FoundPerson, Long> idExtractor = fp -> getRcPersId(fp.getPerson().getIdentification().getOtherPersonId());

		// trop de résultats
		if (!found.getNumberOfResults().equals(BigInteger.ONE)) {
			final List<Long> ids = found.getListOfResults().getFoundPerson().stream()
					.map(idExtractor)
					.collect(Collectors.toList());
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, String.format("Plusieurs (%d -> %s) résultats trouvés dans RCPers pour le nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
			                                                                                 found.getNumberOfResults(), Arrays.toString(ids.toArray(new Long[ids.size()])),
			                                                                                 nom, prenom, sexe, dateNaissance));
			return null;
		}

		// un seul résultat -> ok
		final FoundPerson fp = found.getListOfResults().getFoundPerson().get(0);
		final long id = idExtractor.apply(fp);
		mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Trouvé un individu (%d) de RCPers pour le nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
		                                                                                 id, nom, prenom, sexe, dateNaissance));

		return Pair.of(id, fp.getResidence() != null);
	}
}
