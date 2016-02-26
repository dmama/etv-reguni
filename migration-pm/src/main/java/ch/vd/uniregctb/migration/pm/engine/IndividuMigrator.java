package ch.vd.uniregctb.migration.pm.engine;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import ch.ech.ech0044.v2.NamedPersonId;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v5.FoundPerson;
import ch.vd.evd0001.v5.ListOfFoundPersons;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.evd0001.v5.Person;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.Equalator;
import ch.vd.uniregctb.adresse.AdresseMandataire;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.migration.pm.MigrationResultContextManipulation;
import ch.vd.uniregctb.migration.pm.MigrationResultInitialization;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.collector.NeutralizedLinkAction;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesAdministrateurs;
import ch.vd.uniregctb.migration.pm.engine.data.DonneesMandats;
import ch.vd.uniregctb.migration.pm.engine.helpers.StringRenderers;
import ch.vd.uniregctb.migration.pm.indexeur.NonHabitantIndex;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAdresseIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAdresseIndividu;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class IndividuMigrator extends AbstractEntityMigrator<RegpmIndividu> {

	private static final Equalator<RegDate> DATE_EQUALATOR = (d1, d2) -> d1 == d2 || (d1 != null && d2 != null && (d1.isPartial() || d2.isPartial()) && d1.compareTo(d2) == 0);
	private static final Equalator<String> NAME_EQUALATOR = (d1, d2) -> (d1 == null && d2 == null) || (d1 != null && d1.equalsIgnoreCase(d2));
	private static final Equalator<Enum<?>> ENUM_EQUALATOR = (e1, e2) -> e1 == e2;

	public IndividuMigrator(MigrationContexte migrationContexte) {
		super(migrationContexte);
	}

	private static String extractPrenomUsuel(String tousPrenoms) {
		return Arrays.stream(StringUtils.trimToEmpty(tousPrenoms).split("\\s+")).findFirst().orElse(null);
	}

	@Override
	public void initMigrationResult(MigrationResultInitialization mr, IdMapping idMapper) {
		super.initMigrationResult(mr, idMapper);

		// données des mandats
		mr.registerDataExtractor(DonneesMandats.class,
		                         null,
		                         null,
		                         i -> extractDonneesMandats(i, mr, idMapper));

		// données des administrations
		mr.registerDataExtractor(DonneesAdministrateurs.class,
		                         null,
		                         null,
		                         i -> extractDonneesAdministrateurs(i, mr, idMapper));
	}

	@NotNull
	private DonneesMandats extractDonneesMandats(RegpmIndividu i, MigrationResultContextManipulation mr, IdMapping idMapper) {
		return extractDonneesMandats(buildIndividuKey(i), i.getMandants(), null, mr, LogCategory.INDIVIDUS_PM, idMapper);
	}

	@NotNull
	private DonneesAdministrateurs extractDonneesAdministrateurs(RegpmIndividu i, MigrationResultContextManipulation mr, IdMapping idMapper) {
		final EntityKey moi = buildIndividuKey(i);
		return doInLogContext(moi, mr, idMapper, () -> DonneesAdministrateurs.fromAdministrateur(i, migrationContexte, mr));
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
		final NomPrenom npRegpm = new NomPrenom(regpm.getNom(), regpm.getPrenom());
		final NomPrenom npRcpers = new NomPrenom(rcpers.getIdentity().getOfficialName(), rcpers.getIdentity().getFirstNames());
		final boolean equals = checkEquality(npRegpm.getNom(), npRcpers.getNom(), NAME_EQUALATOR, false)
				&& (checkEquality(npRegpm.getPrenom(), npRcpers.getPrenom(), NAME_EQUALATOR, false) || checkEquality(npRegpm.getPrenom(), extractPrenomUsuel(npRcpers.getPrenom()), NAME_EQUALATOR, false));
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Noms différents dans RegPM (%s) et RCPers (%s).", npRegpm, npRcpers));
		}
		return equals;
	}

	private static boolean checkSexe(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		final Sexe sRegpm = regpm.getSexe();
		final Sexe sRcpers = EchHelper.sexeFromEch44(rcpers.getIdentity().getSex());
		final boolean equals = checkEquality(sRegpm, sRcpers, ENUM_EQUALATOR, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Sexes différents dans RegPM (%s) et RCPers (%s).", sRegpm, sRcpers));
		}
		return equals;
	}

	private static boolean checkDateNaissance(RegpmIndividu regpm, Person rcpers, MigrationResultProduction mr) {
		final RegDate dRegpm = regpm.getDateNaissance();
		final RegDate dRcpers = EchHelper.partialDateFromEch44(rcpers.getIdentity().getDateOfBirth());
		final boolean equals = checkEquality(dRegpm, dRcpers, DATE_EQUALATOR, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Dates de naissance différentes dans RegPM (%s) et RCPers (%s).", dRegpm, dRcpers));
		}
		return equals;
	}

	private static boolean checkIdentite(RegpmIndividu regpm, PersonnePhysique unireg, MigrationResultProduction mr) {
		boolean ok = checkNomPrenom(regpm, unireg, mr);
		ok = checkSexe(regpm, unireg, mr) && ok;
		ok = checkDateNaissance(regpm, unireg, mr) && ok;
		if (!ok) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, "Correspondance avec personne trouvée dans les non-habitants d'Unireg ignorée.");
		}
		return ok;
	}

	private static boolean checkNomPrenom(RegpmIndividu regpm, PersonnePhysique unireg, MigrationResultProduction mr) {
		final NomPrenom npRegpm = new NomPrenom(regpm.getNom(), regpm.getPrenom());
		final NomPrenom npUnireg = new NomPrenom(unireg.getNom(), unireg.getTousPrenoms());
		final boolean equals = checkEquality(npRegpm.getNom(), npUnireg.getNom(), NAME_EQUALATOR, false)
				&& (checkEquality(npRegpm.getPrenom(), npUnireg.getPrenom(), NAME_EQUALATOR, false) || checkEquality(npRegpm.getPrenom(), unireg.getPrenomUsuel(), NAME_EQUALATOR, false));
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Noms différents dans RegPM (%s) et Unireg (%s).", npRegpm, npUnireg));
		}
		return equals;
	}

	private static boolean checkSexe(RegpmIndividu regpm, PersonnePhysique unireg, MigrationResultProduction mr) {
		final Sexe sRegpm = regpm.getSexe();
		final Sexe sUnireg = unireg.getSexe();
		final boolean equals = checkEquality(sRegpm, sUnireg, ENUM_EQUALATOR, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Sexes différents dans RegPM (%s) et Unireg (%s).", sRegpm, sUnireg));
		}
		return equals;
	}

	private static boolean checkDateNaissance(RegpmIndividu regpm, PersonnePhysique unireg, MigrationResultProduction mr) {
		final RegDate dRegpm = regpm.getDateNaissance();
		final RegDate dUnireg = unireg.getDateNaissance();
		final boolean equals = checkEquality(dRegpm, dUnireg, DATE_EQUALATOR, false);
		if (!equals) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Dates de naissance différentes dans RegPM (%s) et Unireg (%s).", dRegpm, dUnireg));
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

		// si la personne physique n'est pas directement mandataire ni administrateur actif d'une société immobilière, on ne la migre pas
		final boolean hasMandats = hasMandats(regpm, mr);
		final boolean isAdministrateur = isAdministrateur(regpm, mr);
		if (!hasMandats && !isAdministrateur) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, "Individu PM ignoré car n'a pas de rôle de mandataire ni d'administrateur.");
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
			ppExistant = migrationContexte.getTiersDAO().getPPByNumeroIndividu(trouveRCPersId.getLeft());
		}
		else {
			ppExistant = rechercheNonHabitantExistant(regpm, mr);
		}

		// [SIFISC-16858] On ne crée pas de nouvel individu...
		if (ppExistant != null) {
			idMapper.addIndividu(regpm, ppExistant);
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Trouvé personne physique existante %s.", FormatNumeroHelper.numeroCTBToDisplay(ppExistant.getNumero())));

			// log de suivi à la fin des opérations pour cet individu
			mr.addMessage(LogCategory.SUIVI, LogLevel.INFO, String.format("Individu migré : %s.", FormatNumeroHelper.numeroCTBToDisplay(ppExistant.getNumero())));
		}
		else {

			// si on est ici, c'est que l'individu a au moins un mandant récent (ou est administrateur), mais on ne le retrouve pas...
			// comme on ne doit pas le créer, il ne faut pas oublier de neutraliser la création des liens avec cet individu,,,
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.ERROR, "Individu non migré car aucune correspondance univoque n'a pu être trouvée avec une personne physique existante dans Unireg.");

			// on ne doit finalement générer aucun des liens avec l'individu
			linkCollector.addNeutralizedEntity(buildIndividuKey(regpm), buildActionRecopieAdresseMandataire(regpm));
		}
	}

	private NeutralizedLinkAction buildActionRecopieAdresseMandataire(RegpmIndividu regpm) {
		return (link, neutralizationReason, mr, idMapper) -> {

			if (link.getType() == EntityLinkCollector.LinkType.MANDANT_MANDATAIRE && !neutralizationReason.isSourceNeutralisee()) {
				// c'est donc que nous sommes sur une relation mandant/mandataire dont seul le mandataire a été neutralisé
				final Contribuable mandant = (Contribuable) link.resolveSource();

				// on pose un contexte de log car ce code ne sera exécuté que beaucoup plus tard, hors de toute migration individuelle identifiée
				doInLogContext(buildIndividuKey(regpm),
				               mr,
				               idMapper,
				               () -> doInLogContext(link.getSourceKey(),
				                                    mr,
				                                    idMapper,
				                                    () -> regpm.getAdresses().stream()
						                                    .filter(a -> a.getType() == RegpmTypeAdresseIndividu.COURRIER)
						                                    .filter(a -> a.getDateAnnulation() == null)
						                                    .map(a -> Pair.<RegpmAdresseIndividu, DateRange>of(a, new DateRangeHelper.Range(a.getDateDebut(), a.getDateFin())))
						                                    .filter(pair -> DateRangeHelper.intersect(link, pair.getRight()))
						                                    .map(Pair::getLeft)
						                                    .sorted(Comparator.comparing(RegpmAdresseIndividu::getDateDebut, NullDateBehavior.EARLIEST::compare))
						                                    .map(a -> {
							                                    final AdresseMandataire am = migrationContexte.getAdresseHelper().buildAdresseMandataire(a, mr, null);
							                                    if (am == null) {
								                                    mr.addMessage(LogCategory.SUIVI, LogLevel.ERROR, "Aucune adresse trouvée pour le mandat vers l'individu.");
							                                    }
							                                    return am;
						                                    })
						                                    .filter(Objects::nonNull)
						                                    .forEach(am -> {
							                                    am.setDateDebut(link.getDateDebut());
							                                    am.setDateFin(link.getDateFin());
							                                    am.setNomDestinataire(new NomPrenom(regpm.getNom(), regpm.getPrenom()).getNomPrenom());
							                                    am.setTypeMandat(((EntityLinkCollector.MandantMandataireLink) link).getTypeMandat());

							                                    mr.addMessage(LogCategory.SUIVI, LogLevel.INFO,
							                                                  String.format("Mandat vers l'individu migré en tant que simple adresse mandataire sur la période %s.",
							                                                                StringRenderers.DATE_RANGE_RENDERER.toString(am)));

							                                    mandant.addAdresseMandataire(am);
						                                    }))
				);
			}
		};
	}

	/**
	 * @param regpm un individu de RegPM
	 * @param mr le collecteur des messages de suivi
	 * @return <code>true</code> si l'individu est bénéficiaire d'au moins un mandat récent
	 */
	private boolean hasMandats(RegpmIndividu regpm, MigrationResultProduction mr) {
		final DonneesMandats donneesMandats = mr.getExtractedData(DonneesMandats.class, buildIndividuKey(regpm));
		return donneesMandats.isMandataire();
	}

	/**
	 * @param regpm un individu de RegPM
	 * @param mr le collecteur des messages de suivi
	 * @return <code>true</code> si l'individu est administrateur actif d'au moins une société immobilière
	 */
	private boolean isAdministrateur(RegpmIndividu regpm, MigrationResultProduction mr) {
		final DonneesAdministrateurs donneesAdministrateurs = mr.getExtractedData(DonneesAdministrateurs.class, buildIndividuKey(regpm));
		return !donneesAdministrateurs.getAdministrations().isEmpty();
	}

	@Nullable
	private PersonnePhysique rechercheNonHabitantExistant(RegpmIndividu regpm, MigrationResultProduction mr) {

		final PersonnePhysique ppExistant;
		final NonHabitantIndex.NonHabitantSearchParameters params = new NonHabitantIndex.NonHabitantSearchParameters(regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), regpm.getDateNaissance());
		if (params.isEmpty()) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.ERROR, "Individu RegPM sans nom, prenom, sexe ni date de naissance.");
			ppExistant = null;
		}
		else {
			final List<Long> idsPP = migrationContexte.getNonHabitantIndex().search(params, Integer.MAX_VALUE);
			if (idsPP == null || idsPP.isEmpty()) {
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Aucun non-habitant trouvé dans Unireg avec ces nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
				                                                                     regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), StringRenderers.DATE_RENDERER.toString(regpm.getDateNaissance())));
				ppExistant = null;
			}
			else if (idsPP.size() > 1) {
				mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, String.format("Plusieurs non-habitants trouvés dans Unireg avec ces nom (%s), prénom (%s), sexe (%s) et date de naissance (%s) : %s.",
				                                                                     regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), StringRenderers.DATE_RENDERER.toString(regpm.getDateNaissance()),
				                                                                     Arrays.toString(idsPP.toArray(new Long[idsPP.size()]))));
				ppExistant = null;
			}
			else {
				// [SIFISC-18034][SIFISC-17354] Vérification additionnelle
				final PersonnePhysique candidatExistant = (PersonnePhysique) migrationContexte.getTiersDAO().get(idsPP.get(0));
				final boolean ok = candidatExistant != null && checkIdentite(regpm, candidatExistant, mr);
				if (ok) {
					ppExistant = candidatExistant;
				}
				else {
					mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.WARN, String.format("Candidat non-habitant Unireg trouvé mais sans correspondance exacte avec les données de nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
					                                                                     regpm.getNom(), regpm.getPrenom(), regpm.getSexe(), StringRenderers.DATE_RENDERER.toString(regpm.getDateNaissance())));
					ppExistant = null;
				}
			}
		}
		return ppExistant;
	}

	@Nullable
	private Person getFromRCPersWithId(MigrationResultProduction mr, long id) {
		final ListOfPersons list = migrationContexte.getRcpersClient().getPersons(Collections.singletonList(id), RegDate.get(), false);
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
		final ListOfFoundPersons found = migrationContexte.getRcpersClient().findPersons(EchHelper.sexeToEch44(sexe), prenom, nom, null, null, null, null, Boolean.TRUE,
		                                                                                 null, null, null, null, null, null, null, null, dateNaissanceMin, dateNaissanceMax);

		// aucun résultat
		if (found == null || found.getNumberOfResults().equals(BigInteger.ZERO)) {
			mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Aucun résultat dans RCPers pour le nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
			                                                                     nom, prenom, sexe, StringRenderers.DATE_RENDERER.toString(dateNaissance)));
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
			                                                                     nom, prenom, sexe, StringRenderers.DATE_RENDERER.toString(dateNaissance)));
			return null;
		}

		// un seul résultat -> ok
		final FoundPerson fp = found.getListOfResults().getFoundPerson().get(0);
		final long id = idExtractor.apply(fp);
		mr.addMessage(LogCategory.INDIVIDUS_PM, LogLevel.INFO, String.format("Trouvé un individu (%d) de RCPers pour le nom (%s), prénom (%s), sexe (%s) et date de naissance (%s).",
		                                                                     id, nom, prenom, sexe, StringRenderers.DATE_RENDERER.toString(dateNaissance)));

		return Pair.of(id, fp.getResidence() != null);
	}
}
