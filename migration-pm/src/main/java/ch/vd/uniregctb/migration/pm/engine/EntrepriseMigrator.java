package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.common.HibernateDateRangeEntity;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEmise;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.extractor.IbanExtractor;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.rcent.model.Organisation;
import ch.vd.uniregctb.migration.pm.rcent.service.RCEntService;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDemandeDelaiSommation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmExerciceCommercial;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForPrincipal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmForSecondaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRegimeFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDemandeDelai;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeRegimeFiscal;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.Bouclement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.RegimeFiscal;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.type.TypeRegimeFiscal;

public class EntrepriseMigrator extends AbstractEntityMigrator<RegpmEntreprise> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseMigrator.class);

	/**
	 * La valeur à mettre dans le champ "source" d'un état de DI retournée lors de la migration
	 */
	private static final String SOURCE_RETOUR_DI_MIGREE = "SDI";

	private final BouclementService bouclementService;
	private final RCEntService rcEntService;
	private final AdresseHelper adresseHelper;

	public EntrepriseMigrator(UniregStore uniregStore, BouclementService bouclementService, RCEntService rcEntService, AdresseHelper adresseHelper) {
		super(uniregStore);
		this.bouclementService = bouclementService;
		this.rcEntService = rcEntService;
		this.adresseHelper = adresseHelper;
	}

	@Nullable
	@Override
	protected String getMessagePrefix(RegpmEntreprise entity) {
		return String.format("Entreprise %d", entity.getId());
	}

	private static Entreprise createEntreprise(RegpmEntreprise regpm) {
		final Entreprise unireg = new Entreprise(regpm.getId());
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	private static class ControleForsSecondairesData {
		final Set<RegpmForSecondaire> regpm;
		final KeyedSupplier<Entreprise> entrepriseSupplier;

		public ControleForsSecondairesData(Set<RegpmForSecondaire> regpm, KeyedSupplier<Entreprise> entrepriseSupplier) {
			this.regpm = regpm;
			this.entrepriseSupplier = entrepriseSupplier;
		}
	}

	@Override
	public void initMigrationResult(MigrationResult mr) {
		super.initMigrationResult(mr);

		// enregistrement de la consolidation pour la constitution des fors "immeuble"
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Immeuble.class,
		                                        MigrationConstants.PHASE_FORS_IMMEUBLES,
		                                        k -> k.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Immeuble(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesImmeuble(d, mr));

		// enregistrement d'un callback pour le contrôle des fors secondaires après création
		mr.registerPreTransactionCommitCallback(ControleForsSecondairesData.class,
		                                        MigrationConstants.PHASE_CONTROLE_FORS_SECONDAIRES,
		                                        k -> k.entrepriseSupplier,
		                                        (d1, d2) -> { throw new IllegalArgumentException("une seule donnée par entreprise, donc pas de raison d'appeler le merger..."); },
		                                        d -> controleForsSecondaires(d, mr));
	}

	/**
	 * Consolidation de toutes les migrations de PM par rapport au contrôle final des fors secondaires
	 * @param data données de l'entreprise
	 * @param masterMr collecteur de messages de suivi
	 */
	private void controleForsSecondaires(ControleForsSecondairesData data, MigrationResultProduction masterMr) {
		final EntityKey keyEntiteJuridique = data.entrepriseSupplier.getKey();
		final MigrationResultProduction mr = masterMr.withMessagePrefix(String.format("%s %d", keyEntiteJuridique.getType().getDisplayName(), keyEntiteJuridique.getId()));
		final Entreprise entreprise = data.entrepriseSupplier.get();

		// on va construire des périodes par commune (no OFS), et vérifier qu'on a bien les mêmes des deux côtés
		final Map<Integer, List<DateRange>> avantMigration = data.regpm.stream()
				.collect(Collectors.toMap(fs -> fs.getCommune().getNoOfs(), Collections::singletonList, DATE_RANGE_LIST_MERGER));
		final Set<ForFiscal> forsFiscaux = Optional.ofNullable(entreprise.getForsFiscaux()).orElse(Collections.emptySet());    // en cas de nouvelle entreprise, la collection est nulle
		final Map<Integer, List<DateRange>> apresMigration = forsFiscaux.stream()
				.filter(f -> f instanceof ForFiscalSecondaire)
				.collect(Collectors.toMap(ForFiscal::getNumeroOfsAutoriteFiscale, Collections::singletonList, DATE_RANGE_LIST_MERGER));

		// rien avant, rien après, pas la peine de continuer...
		if (avantMigration.isEmpty() && apresMigration.isEmpty()) {
			return;
		}

		// des communes présentes d'un côté et pas du tout de l'autre ?
		final Set<Integer> ofsSeulementAvant = avantMigration.keySet().stream().filter(ofs -> !apresMigration.containsKey(ofs)).collect(Collectors.toSet());
		final Set<Integer> ofsSeulementApres = apresMigration.keySet().stream().filter(ofs -> !avantMigration.containsKey(ofs)).collect(Collectors.toSet());
		if (!ofsSeulementAvant.isEmpty()) {
			for (Integer ofs : ofsSeulementAvant) {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
				              String.format("Il n'y a plus de fors secondaires sur la commune OFS %d (avant : %s).", ofs, toDisplayString(avantMigration.get(ofs))));
			}
		}
		if (!ofsSeulementApres.isEmpty()) {
			for (Integer ofs : ofsSeulementApres) {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
				              String.format("Il n'y avait pas de fors secondaires sur la commune OFS %d (maintenant : %s).", ofs, toDisplayString(apresMigration.get(ofs))));
			}
		}

		// et sur les communes effectivement en commun, il faut comparer les périodes
		final Set<Integer> ofsCommunes = avantMigration.keySet().stream().filter(apresMigration::containsKey).collect(Collectors.toSet());
		if (!ofsCommunes.isEmpty()) {
			for (Integer ofs : ofsCommunes) {
				final List<DateRange> rangesAvant = avantMigration.get(ofs);
				final List<DateRange> rangesApres = apresMigration.get(ofs);
				if (!sameDateRanges(rangesAvant, rangesApres)) {
					mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
					              String.format("Sur la commune OFS %d, la couverture des fors secondaires n'est plus la même : avant (%s) et après (%s).",
					                            ofs, toDisplayString(rangesAvant), toDisplayString(rangesApres)));
				}
			}
		}
	}

	/**
	 * @param list une liste de ranges
	 * @return une représentation String de cette liste
	 */
	private static String toDisplayString(List<? extends DateRange> list) {
		return list.stream().map(DateRangeHelper::toDisplayString).collect(Collectors.joining(", "));
	}

	/**
	 * Les listes de ranges en entrée sont supposés triés
	 * @param l1 une liste de ranges
	 * @param l2 une autre liste de ranges
	 * @return <code>true</code> si les listes contiennent les mêmes plages de dates
	 */
	private static boolean sameDateRanges(List<DateRange> l1, List<DateRange> l2) {
		boolean same = l1.size() == l2.size();
		if (same) {
			for (Iterator<DateRange> i1 = l1.iterator(), i2 = l2.iterator(); i1.hasNext() && i2.hasNext() && same; ) {
				final DateRange r1 = i1.next();
				final DateRange r2 = i2.next();
				same = DateRangeHelper.equals(r1, r2);
			}
		}
		return same;
	}

	/**
	 * Consolidation de toutes les demandes de créations de fors secondaires "immeuble" pour une PM
	 * @param data les données consolidées des communes/dates sur lesquels les fors doivent être créés
	 * @param masterMr le collecteur de messages de suivi
	 */
	private void createForsSecondairesImmeuble(ForsSecondairesData.Immeuble data, MigrationResultProduction masterMr) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		final MigrationResultProduction mr = masterMr.withMessagePrefix(String.format("%s %d", keyEntiteJuridique.getType().getDisplayName(), keyEntiteJuridique.getId()));
		final Tiers entiteJuridique = data.entiteJuridiqueSupplier.get();
		for (Map.Entry<RegpmCommune, List<DateRange>> communeData : data.communes.entrySet()) {

			final RegpmCommune commune = communeData.getKey();
			if (commune.getCanton() != RegpmCanton.VD) {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
				              String.format("Immeuble(s) sur la commune de %s (%d) sise dans le canton %s -> pas de for secondaire créé.", commune.getNom(), commune.getNoOfs(), commune.getCanton()));
			}
			else {
				for (DateRange dates : communeData.getValue()) {
					final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
					ffs.setDateDebut(dates.getDateDebut());
					ffs.setDateFin(dates.getDateFin());
					ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
					ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					ffs.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
					ffs.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
					ffs.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
					ffs.setMotifFermeture(dates.getDateFin() != null ? MotifFor.VENTE_IMMOBILIER : null);
					ffs.setTiers(entiteJuridique);
					entiteJuridique.addForFiscal(ffs);

					mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.INFO, String.format("For secondaire 'immeuble' %s ajouté sur la commune %d.",
					                                                                                                            DateRangeHelper.toDisplayString(dates),
					                                                                                                            commune.getNoOfs()));
				}
			}
		}
	}

	@Override
	protected void doMigrate(RegpmEntreprise regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {

		if (idMapper.hasMappingForEntreprise(regpm.getId())) {
			// l'entreprise a déjà été migrée... pas la peine d'aller plus loin, ou bien ?
			return;
		}

		// TODO: Déterminer si le numéro cantonal existe dans RegPM. S'il n'existe pas, migration directe des données civiles.
		// Accès à RCEnt au moyen du numéro cantonal. Une exception est lancée s'il n'existe pas dans RCEnt
		try {
			Organisation rcent = rcEntService.getOrganisation(regpm.getNumeroCantonal());
		}
		catch (Exception e) { // A voir si on implemente une RCEntServiceException
			LOGGER.warn("Erreur lors de la recherche RCEnt. Organisation cantonalId: {}", regpm.getNumeroCantonal());
			LOGGER.info(e.getMessage());
			LOGGER.debug("", e);
		}

		// Les entreprises conservent leur numéro comme numéro de contribuable
		Entreprise unireg = uniregStore.getEntityFromDb(Entreprise.class, regpm.getId());
		if (unireg == null) {
			mr.addMessage(MigrationResultMessage.CategorieListe.PM_MIGREE, MigrationResultMessage.Niveau.WARN, "L'entreprise n'existait pas dans Unireg avec ce numéro de contribuable.");
			unireg = uniregStore.saveEntityToDb(createEntreprise(regpm));
		}
		idMapper.addEntreprise(regpm, unireg);

		// enregistrement de cette entreprise pour un contrôle final des fors secondaires (une fois que tous les immeubles et établissements ont été visés)
		mr.addPreTransactionCommitData(new ControleForsSecondairesData(regpm.getForsSecondaires(), new KeyedSupplier<>(EntityKey.of(regpm), getEntrepriseByUniregIdSupplier(unireg.getId()))));

		// TODO générer l'établissement principal (= siège...)
		// TODO migrer les bouclements, les adresses, les documents...

		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, unireg, mr);

		migrateRegimesFiscaux(regpm, unireg, mr);
		migrateExercicesCommerciaux(regpm, unireg, mr);
		migrateDeclarations(regpm, unireg, mr);
		migrateForsPrincipaux(regpm, unireg, mr);
		migrateImmeubles(regpm, unireg, mr);

		migrateMandataires(regpm, mr, linkCollector, idMapper);
		migrateFusionsApres(regpm, linkCollector, idMapper);
	}

	/**
	 * Migration des liens de fusions (ceux qui clôturent l'entreprise en question, les autres étant traités au moment de la migration des entreprises précédentes)
	 * @param regpm l'entreprise qui va disparaître dans la fusion
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateFusionsApres(RegpmEntreprise regpm, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des fusions (cette entreprise étant la source)
		regpm.getFusionsApres().forEach(apres -> {
			// TODO et les autres informations de la fusion (forme, date de contrat, date de bilan... ?)
			final Supplier<Entreprise> apresFusion = getEntrepriseByRegpmIdSupplier(idMapper, apres.getEntrepriseApres().getId());
			linkCollector.addLink(new EntityLinkCollector.FusionEntreprisesLink(moi, apresFusion, apres.getDateInscription(), null));
		});
	}

	/**
	 * Migration des mandataires d'une entreprise
	 * @param regpm entreprise à migrer
	 * @param mr collecteur de messages, de données à logguer...
	 * @param linkCollector collecteur de liens à créer
	 * @param idMapper mapper des identifiants RegPM -> Unireg
	 */
	private void migrateMandataires(RegpmEntreprise regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// un supplier qui va renvoyer l'entreprise en cours de migration
		final Supplier<Entreprise> moi = getEntrepriseByRegpmIdSupplier(idMapper, regpm.getId());

		// migration des mandataires -> liens à créer par la suite
		regpm.getMandataires().forEach(mandat -> {

			// récupération du mandataire qui peut être une autre entreprise, un établissement ou un individu
			final Supplier<? extends Contribuable> mandataire = getPolymorphicSupplier(idMapper, mandat::getMandataireEntreprise, mandat::getMandataireEtablissement, mandat::getMandataireIndividu);
			if (mandataire == null) {
				mr.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.WARN, "Le mandat " + mandat.getId() + " n'a pas de mandataire.");
				return;
			}

			// TODO ne faut-il vraiment migrer que les mandats généraux ?

			// on ne migre que les mandats généraux pour le moment
			if (mandat.getType() != RegpmTypeMandat.GENERAL) {
				mr.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.WARN, "Le mandat " + mandat.getId() + " de type " + mandat.getType() + " est ignoré dans la migration.");
				return;
			}

			final TypeMandat typeMandat = TypeMandat.GENERAL;
			final String bicSwift = mandat.getBicSwift();
			String iban;
			try {
				iban = IbanExtractor.extractIban(mandat);
			}
			catch (IbanExtractor.IbanExtratorException e) {
				mr.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.ERROR, "Impossible d'extraire un IBAN du mandat " + mandat.getId() + " (" + e.getMessage() + ")");
				iban = null;
			}

			// ajout du lien entre l'entreprise et son mandataire
			linkCollector.addLink(new EntityLinkCollector.MandantMandataireLink<>(moi, mandataire, mandat.getDateAttribution(), mandat.getDateResiliation(), typeMandat, iban, bicSwift));
		});
	}

	private void migrateExercicesCommerciaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final RegDate dateBouclementFutur = regpm.getDateBouclementFutur();
		final List<RegDate> datesBouclements;
		final SortedSet<RegpmExerciceCommercial> exercicesCommerciaux = regpm.getExercicesCommerciaux();
		if (exercicesCommerciaux != null && !exercicesCommerciaux.isEmpty()) {
			final RegpmExerciceCommercial dernierConnu = exercicesCommerciaux.last();
			final RegDate dateFinDernierExercice = dernierConnu.getDateFin();

			// c'est apparemment le cas qui apparaît tout le temps :
			// la déclaration (= dossier fiscal) de la PF précédente a déjà été envoyée, mais aucun exercice commercial
			// n'a encore été généré, et la date de bouclement futur correspond déjà à la fin de la PF courante)
			// -> je dois bien créer un exercice commercial dans Unireg entre les deux (= pour la DI envoyée)

			// TODO que faire si la date de bouclement futur est nulle ?

			final Stream.Builder<RegDate> additionalDatesStreamBuilder = Stream.builder();
			if (dateBouclementFutur != null) {
				if (dateFinDernierExercice.addYears(1).compareTo(dateBouclementFutur) < 0) {
					additionalDatesStreamBuilder.accept(dateFinDernierExercice.addYears(1));
				}
				additionalDatesStreamBuilder.accept(dateBouclementFutur);
			}
			final Stream<RegDate> additionalDatesStream = additionalDatesStreamBuilder.build();

			// la liste des dates à prendre en compte pour le calcul des bouclements à la sauce Unireg
			datesBouclements = Stream.concat(exercicesCommerciaux.stream().map(RegpmExerciceCommercial::getDateFin), additionalDatesStream)
					.collect(Collectors.toList());
		}
		else if (dateBouclementFutur != null) {
			// TODO aucun exercice commercial... comment trouver la date de début de l'exercice en cours ?
			datesBouclements = Collections.singletonList(dateBouclementFutur);
		}
		else {
			// TODO que faire pour les entreprises qui n'ont ni exercices commerciaux ni date de bouclement futur ?
			datesBouclements = Collections.emptyList();
		}

		// calcul des périodicités...
		final List<Bouclement> bouclements = bouclementService.extractBouclementsDepuisDates(datesBouclements, 12);

		// TODO sauvegarder ces bouclements et les associer à l'entreprise dans Unireg

	}

	@NotNull
	private PeriodeFiscale getPeriodeFiscaleByYear(int year) {
		// critère sur l'année
		final Map<String, Object> params = new HashMap<>(1);
		params.put("annee", year);

		// récupération des données
		final List<PeriodeFiscale> pfs = uniregStore.getEntitiesFromDb(PeriodeFiscale.class, params);
		if (pfs == null || pfs.isEmpty()) {
			throw new IllegalStateException("La période fiscale " + year + " n'existe pas dans Unireg.");
		}
		if (pfs.size() > 1) {
			throw new IllegalStateException("Plusieurs périodes fiscales trouvées dans Unireg pour l'année " + year);
		}

		// la seule donnée
		final PeriodeFiscale pf = pfs.get(0);
		if (pf == null) {
			throw new IllegalStateException("La période fiscale " + year + " n'existe pas dans Unireg.");
		}
		return pf;
	}

	private Declaration migrateDeclaration(RegpmDossierFiscal dossier, RegDate dateDebut, RegDate dateFin) {
		final PeriodeFiscale pf = getPeriodeFiscaleByYear(dossier.getPf());

		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();
		copyCreationMutation(dossier, di);
		di.setDateDebut(dateDebut);
		di.setDateFin(dateFin);
		di.setDelais(migrateDelaisDeclaration(dossier, di));
		di.setEtats(migrateEtatsDeclaration(dossier, di));
		di.setNumero(dossier.getNoParAnnee());
		di.setPeriode(pf);

		if (dossier.getEtat() == RegpmTypeEtatDossierFiscal.ANNULE) {
			di.setAnnulationUser(Optional.ofNullable(dossier.getLastMutationOperator()).orElse(AuthenticationHelper.getCurrentPrincipal()));
			di.setAnnulationDate(Optional.ofNullable((Date) dossier.getLastMutationTimestamp()).orElseGet(DateHelper::getCurrentDate));
		}
		return di;
	}

	/**
	 * Migration des déclarations d'impôts, de leurs états, délais...
	 */
	private void migrateDeclarations(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final Set<RegpmDossierFiscal> dossiersFiscauxAttribuesAuxExercicesCommerciaux = new HashSet<>(regpm.getDossiersFiscaux().size());

		// boucle sur chacun des exercices commerciaux
		regpm.getExercicesCommerciaux().forEach(exercice -> {

			final RegpmDossierFiscal dossier = exercice.getDossierFiscal();
			if (dossier != null) {

				// on collecte les dossiers fiscaux attachés aux exercices commerciaux
				// pour trouver au final ceux qui ne le sont pas (= les déclarations envoyées mais pas encore traitées ???)
				dossiersFiscauxAttribuesAuxExercicesCommerciaux.add(dossier);

				final Declaration di = migrateDeclaration(dossier, exercice.getDateDebut(), exercice.getDateFin());
				unireg.addDeclaration(di);
			}
		});

		// ensuite, il faut éventuellement trouver une déclaration envoyée mais pour laquelle je n'ai pas encore
		// d'entrée dans la table des exercices commerciaux
		regpm.getDossiersFiscaux().stream()
				.filter(dossier -> !dossiersFiscauxAttribuesAuxExercicesCommerciaux.contains(dossier))
				.forEach(dossier -> {

					// si la PF est celle de la prochaine date de bouclement, alors on peut la créer...
					if (regpm.getDateBouclementFutur() != null && regpm.getDateBouclementFutur().year() == dossier.getPf()) {

						// TODO comment évaluer la date de début ?
						final RegDate dateDebut = null;
						final Declaration di = migrateDeclaration(dossier, dateDebut, regpm.getDateBouclementFutur());
						unireg.addDeclaration(di);
					}
					else {
						// TODO que faire avec ces dossiers ? Ils correspondent pourtant à une déclaration envoyée, mais pourquoi n'y a-t-il pas d'exercice commercial associé ?
						mr.addMessage(MigrationResultMessage.CategorieListe.DECLARATIONS, MigrationResultMessage.Niveau.WARN,
						              String.format("Dossier fiscal %d/%d sans exercice commercial associé.", dossier.getPf(), dossier.getNoParAnnee()));
					}
				});
	}

	/**
	 * Génération des délais de dépôt
	 */
	private static Set<DelaiDeclaration> migrateDelaisDeclaration(RegpmDossierFiscal dossier, Declaration di) {

		final Set<DelaiDeclaration> delais = new LinkedHashSet<>();

		// délai initial
		if (dossier.getDelaiRetour() != null) {
			final DelaiDeclaration delai = new DelaiDeclaration();
			copyCreationMutation(dossier, delai);
			delai.setConfirmationEcrite(false);
			delai.setDateDemande(dossier.getDateEnvoi());           // TODO le délai initial est "demandé" à la date d'envoi, non ?
			delai.setDateTraitement(dossier.getDateEnvoi());
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(dossier.getDelaiRetour());
			delais.add(delai);
		}

		// fonction de conversion
		final Function<RegpmDemandeDelaiSommation, DelaiDeclaration> mapper = regpm -> {
			final DelaiDeclaration delai = new DelaiDeclaration();
			copyCreationMutation(regpm, delai);
			delai.setConfirmationEcrite(false);                             // les documents ne doivent pas être retrouvés dans Unireg, mais par le DPerm s'il le faut
			delai.setDateDemande(regpm.getDateDemande());
			delai.setDateTraitement(regpm.getDateReception());              // TODO on est sûr ce de mapping ?
			delai.setDeclaration(di);
			delai.setDelaiAccordeAu(regpm.getDelaiAccorde());
			return delai;
		};

		// TODO que fait-on avec les demandes de délai en cours d'analyse (etat = DEMANDEE) ?
		// TODO que fait-on avec les demandes de délai refusées (etat = REFUSEE) ?
		// TODO que fait-on avec les demandes de délai après sommation (type = APRES_SOMMATION) ?

		// demandes ultérieures
		dossier.getDemandesDelai().stream()
				.filter(demande -> demande.getType() == RegpmTypeDemandeDelai.AVANT_SOMMATION)
				.filter(demande -> demande.getEtat() == RegpmTypeEtatDemandeDelai.ACCORDEE)
				.map(mapper)
				.forEach(delais::add);
		return delais;
	}

	/**
	 * Génération des états d'une déclaration
	 */
	private static Set<EtatDeclaration> migrateEtatsDeclaration(RegpmDossierFiscal dossier, Declaration di) {

		final Set<EtatDeclaration> etats = new LinkedHashSet<>();

		// envoi
		if (dossier.getDateEnvoi() != null) {
			etats.add(new EtatDeclarationEmise(dossier.getDateEnvoi()));
		}

		// sommation
		if (dossier.getDateEnvoiSommation() != null) {
			etats.add(new EtatDeclarationSommee(dossier.getDateEnvoiSommation(), dossier.getDateEnvoiSommation()));
		}

		// retour
		if (dossier.getDateRetour() != null) {
			etats.add(new EtatDeclarationRetournee(dossier.getDateRetour(), SOURCE_RETOUR_DI_MIGREE));
		}

		// TODO la taxation d'office (= échéance, au sens Unireg) existait-elle ?

		return etats;
	}

	private void migrateForsPrincipaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		final Function<RegpmForPrincipal, Optional<ForFiscalPrincipalPM>> mapper = f -> {
			final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM();
			copyCreationMutation(f, ffp);
			ffp.setDateDebut(f.getDateValidite());
			ffp.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
			ffp.setMotifRattachement(MotifRattachement.DOMICILE);
			if (f.getCommune() != null) {
				final RegpmCommune commune = f.getCommune();
				ffp.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
				ffp.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			}
			else if (f.getOfsPays() != null) {
				if (f.getOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
					mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.ERROR, String.format("For principal %s sans commune mais sur Suisse", f.getId()));
					return Optional.empty();
				}
				ffp.setNumeroOfsAutoriteFiscale(f.getOfsPays());
				ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			}
			else {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.ERROR, String.format("For principal %s sans autorité fiscale", f.getId()));
				return Optional.empty();
			}
			ffp.setTiers(unireg);
			return Optional.of(ffp);
		};

		final List<ForFiscalPrincipalPM> liste = regpm.getForsPrincipaux().stream()
				.map(mapper)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.sorted(Comparator.comparing(ForFiscalPrincipal::getDateDebut))
				.collect(Collectors.toList());

		// TODO la date de fin du dernier for est-elle la date de fin fiscale ?
		// assignation des dates de fin
		assigneDatesFin(regpm.getDateFinFiscale(), liste);

		// assignation des motifs
		final MovingWindow<ForFiscalPrincipalPM> wnd = new MovingWindow<>(liste);
		while (wnd.hasNext()) {
			final MovingWindow.Snapshot<ForFiscalPrincipalPM> snap = wnd.next();
			final ForFiscalPrincipal current = snap.getCurrent();
			final ForFiscalPrincipal previous = snap.getPrevious();
			final ForFiscalPrincipal next = snap.getNext();

			// TODO les motifs doivent peut-être venir des inscriptions/radiations au RC

			// le tout premier for a un motif d'ouverture indéterminé
			if (previous == null) {
				current.setMotifOuverture(MotifFor.INDETERMINE);
			}

			// le tout dernier for a un motif de fermeture indéterminé si la date de fermeture est non-nulle
			if (next == null && current.getDateFin() != null) {
				current.setMotifFermeture(MotifFor.INDETERMINE);
			}

			// comparaison des types d'autorité fiscales pour les mutations
			if (next != null) {
				final TypeAutoriteFiscale currentTAF = current.getTypeAutoriteFiscale();
				final TypeAutoriteFiscale nextTAF = next.getTypeAutoriteFiscale();
				final MotifFor motif;
				if (currentTAF == nextTAF) {
					// TODO il y a sans doute d'autres possibilités, comme une fusion de communes...
					motif = MotifFor.DEMENAGEMENT_VD;
				}
				else if (nextTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.DEPART_HS;
				}
				else if (currentTAF == TypeAutoriteFiscale.PAYS_HS) {
					motif = MotifFor.ARRIVEE_HS;
				}
				else if (nextTAF == TypeAutoriteFiscale.COMMUNE_HC) {
					motif = MotifFor.DEPART_HC;
				}
				else {
					motif = MotifFor.ARRIVEE_HC;
				}

				current.setMotifFermeture(motif);
				next.setMotifOuverture(motif);
			}
		}

		// on les ajoute au tiers
		liste.forEach(unireg::addForFiscal);
	}

	private void migrateImmeubles(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {
		// les fors secondaires devront être créés sur l'entreprise migrée
		final KeyedSupplier<Entreprise> moi = new KeyedSupplier<>(EntityKey.of(regpm), getEntrepriseByUniregIdSupplier(unireg.getId()));

		// les immeubles en possession directe
		final Map<RegpmCommune, List<DateRange>> mapDirecte = couvertureDepuisRattachementsProprietaires(regpm.getRattachementsProprietaires());
	    if (!mapDirecte.isEmpty()) {
		    mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(moi, mapDirecte));
	    }

		// les immeubles en possession via un groupe
		final Map<RegpmCommune, List<DateRange>> mapViaGroupe = couvertureDepuisAppartenancesGroupeProprietaire(regpm.getAppartenancesGroupeProprietaire());
		if (!mapViaGroupe.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData.Immeuble(moi, mapViaGroupe));
		}
	}

	/**
	 * Attribution des dates de fin en suivant les principes que
	 * <ul>
	 *     <li>la liste des éléments est triée dans l'ordre chronologique des dates de début</li>
	 *     <li>les éléments ne se chevauchent pas</li>
	 * </ul>
	 * @param derniereDateFin date de fin à appliquer au dernier éléments de la liste
	 * @param listeTriee liste dont les éléments doivent se voir assigner une date de fin
	 * @param <T> type des éléments de la liste
	 */
	private static <T extends HibernateDateRangeEntity> void assigneDatesFin(@Nullable RegDate derniereDateFin, List<T> listeTriee) {
		RegDate dateFinCourante = derniereDateFin;
		for (T ffp : CollectionsUtils.revertedOrder(listeTriee)) {
			ffp.setDateFin(dateFinCourante);
			dateFinCourante = ffp.getDateDebut().getOneDayBefore();
		}
	}

	private static TypeRegimeFiscal mapTypeRegimeFiscal(RegpmTypeRegimeFiscal type) {
		// TODO il va falloir trouver un mapping un peu plus touffu...
		return TypeRegimeFiscal.ORDINAIRE;
	}

	private static RegimeFiscal mapRegimeFiscal(RegimeFiscal.Portee portee, RegpmRegimeFiscal rf) {
		final RegimeFiscal unireg = new RegimeFiscal();
		unireg.setDateDebut(rf.getDateDebut());
		unireg.setDateFin(null);
		unireg.setPortee(portee);
		unireg.setType(mapTypeRegimeFiscal(rf.getType()));
		return unireg;
	}

	private static <T extends RegpmRegimeFiscal> List<RegimeFiscal> mapRegimesFiscaux(RegimeFiscal.Portee portee, SortedSet<T> regimesRegpm, @Nullable RegDate dateFinRegimes) {
		// collecte des régimes fiscaux CH sans date de fin d'abord...
		final List<RegimeFiscal> liste = regimesRegpm.stream()
				.filter(r -> r.getDateAnnulation() == null)         // on ne migre pas les régimes fiscaux annulés
				.map(r -> mapRegimeFiscal(portee, r))
				.collect(Collectors.toList());

		// ... puis attribution des dates de fin
		assigneDatesFin(dateFinRegimes, liste);
		return liste;
	}

	private void migrateRegimesFiscaux(RegpmEntreprise regpm, Entreprise unireg, MigrationResultProduction mr) {

		// TODO la date de fin du dernier régime fiscal CH est-elle la date de fin fiscale ?
		// collecte des régimes fiscaux CH...
		final List<RegimeFiscal> listeCH = mapRegimesFiscaux(RegimeFiscal.Portee.CH, regpm.getRegimesFiscauxCH(), regpm.getDateFinFiscale());

		// TODO la date de fin du dernier régime fiscal VD est-elle la date de fin fiscale ?
		// ... puis des règimes fiscaux VD
		final List<RegimeFiscal> listeVD = mapRegimesFiscaux(RegimeFiscal.Portee.VD, regpm.getRegimesFiscauxVD(), regpm.getDateFinFiscale());

		// et finalement on ajoute tout ça dans l'entreprise
		Stream.concat(listeCH.stream(), listeVD.stream()).forEach(unireg::addRegimeFiscal);
	}
}
