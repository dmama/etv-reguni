package ch.vd.uniregctb.migration.pm.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.migration.pm.MigrationConstants;
import ch.vd.uniregctb.migration.pm.MigrationResult;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.engine.helpers.AdresseHelper;
import ch.vd.uniregctb.migration.pm.historizer.collector.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.mapping.IdMapping;
import ch.vd.uniregctb.migration.pm.rcent.service.RCEntService;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissementStable;
import ch.vd.uniregctb.migration.pm.store.UniregStore;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DomicileEtablissement;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EtablissementMigrator extends AbstractEntityMigrator<RegpmEtablissement> {

	private final RCEntService rcEntService;
	private final AdresseHelper adresseHelper;

	public EtablissementMigrator(UniregStore uniregStore, TiersDAO tiersDAO, RCEntService rcEntService, AdresseHelper adresseHelper) {
		super(uniregStore, tiersDAO);
		this.rcEntService = rcEntService;
		this.adresseHelper = adresseHelper;
	}

	private static List<Pair<RegpmCommune, CollatableDateRange>> buildPeriodesForsSecondaires(NavigableMap<RegDate, RegpmDomicileEtablissement> domicilesValides, DateRange range, MigrationResultProduction mr) {
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebut = domicilesValides.floorEntry(range.getDateDebut());
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileFin = range.getDateFin() != null ? domicilesValides.floorEntry(range.getDateFin()) : domicilesValides.lastEntry();

		// si l'une ou l'autre des entrées est nulle, c'est que le range demandé est plus grand que le range couvert par les domiciles...
		if (domicileFin == null) {
			// fin == null -> il n'y a absolument rien qui couvre le range demandé
			mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.ERROR, String.format("L'établissement stable %s n'intersecte aucun domicile.", DateRangeHelper.toDisplayString(range)));
			return Collections.emptyList();
		}

		// début == null mais fin != null -> on a une intersection, il faut donc raboter un peu
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebutEffectif;
		if (domicileDebut == null) {
			domicileDebutEffectif = domicilesValides.ceilingEntry(range.getDateDebut());        // il y en a forcément un, puisque domicileFin != null
			mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.WARN, String.format("L'établissement stable %s n'est couvert par les domiciles qu'à partir du %s.",
			                                                                                                                      DateRangeHelper.toDisplayString(range), RegDateHelper.dateToDisplayString(domicileDebutEffectif.getKey())));
		}
		else {
			domicileDebutEffectif = domicileDebut;
		}

		// s'il n'y a pas eu de changemenent de commune entre les deux dates, ces entrées sont normalement les mêmes
		// (comme je ne sais pas si les Map.Entry sont des constructions pour l'extérieur ou des externalisations de données internes, je préfère juste comparer la clé)
		if (domicileDebutEffectif.getKey() == domicileFin.getKey()) {
			return Collections.singletonList(Pair.<RegpmCommune, CollatableDateRange>of(domicileDebutEffectif.getValue().getCommune(), new DateRangeHelper.Range(domicileDebutEffectif.getKey(), range.getDateFin())));
		}
		else {
			// il y a eu changement de communes... il faut donc préparer plusieurs cas
			final List<Pair<RegpmCommune, CollatableDateRange>> liste = new LinkedList<>();
			RegDate cursor = range.getDateFin();
			for (Map.Entry<RegDate, RegpmDomicileEtablissement> step : domicilesValides.subMap(domicileDebutEffectif.getKey(), true, domicileFin.getKey(), true).descendingMap().entrySet()) {
				final RegDate dateDebut = RegDateHelper.maximum(range.getDateDebut(), step.getKey(), NullDateBehavior.EARLIEST);
				liste.add(0, Pair.of(step.getValue().getCommune(), new DateRangeHelper.Range(dateDebut, cursor)));
				cursor = dateDebut.getOneDayBefore();
			}
			return liste;
		}
	}

	@Nullable
	@Override
	protected String getMessagePrefix(RegpmEtablissement entity) {
		if (entity.getEntreprise() != null) {
			return String.format("Etablissement %d de l'entreprise %d", entity.getId(), entity.getEntreprise().getId());
		}
		if (entity.getIndividu() != null) {
			return String.format("Etablissement %d de l'entreprise %d", entity.getId(), entity.getIndividu().getId());
		}
		return String.format("Etablissement %d", entity.getId());
	}

	@Override
	public void initMigrationResult(MigrationResult mr) {
		super.initMigrationResult(mr);

		// on va regrouper les données (communes et dates) par entité juridique afin de créer,
		// pour chacune d'entre elles, les fors secondaires "activité" qui vont bien
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.Activite.class,
		                                        MigrationConstants.PHASE_FORS_ACTIVITE,
		                                        d -> d.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData.Activite(d1.entiteJuridiqueSupplier, DATE_RANGE_MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondairesEtablissement(d, mr));
	}

	/**
	 * Appelé pour la consolidation des données de fors secondaires par entité juridique
	 * @param data les données collectées pour une entité juridique
	 * @param masterMr le collecteur de messages de suivi
	 */
	private void createForsSecondairesEtablissement(ForsSecondairesData.Activite data, MigrationResultProduction masterMr) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		final MigrationResultProduction mr = masterMr.withMessagePrefix(String.format("%s %d", keyEntiteJuridique.getType().getDisplayName(), keyEntiteJuridique.getId()));
		final Tiers entiteJuridique = data.entiteJuridiqueSupplier.get();
		for (Map.Entry<RegpmCommune, List<DateRange>> communeData : data.communes.entrySet()) {

			// TODO attention, dans le cas d'un individu, les fors secondaires peuvent devoir être créés sur un couple !!
			// TODO l'établissement principal doit-il générer un for secondaire ?

			final RegpmCommune commune = communeData.getKey();
			if (commune.getCanton() != RegpmCanton.VD) {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
				              String.format("Etablissement(s) sur la commune de %s (%d) sise dans le canton %s -> pas de for secondaire créé.", commune.getNom(), commune.getNoOfs(), commune.getCanton()));
			}
			else {
				for (DateRange dates : communeData.getValue()) {
					final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
					ffs.setDateDebut(dates.getDateDebut());
					ffs.setDateFin(dates.getDateFin());
					ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
					ffs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
					ffs.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
					ffs.setMotifRattachement(MotifRattachement.ETABLISSEMENT_STABLE);
					ffs.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
					ffs.setMotifFermeture(dates.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null);
					ffs.setTiers(entiteJuridique);
					entiteJuridique.addForFiscal(ffs);

					mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.INFO, String.format("For secondaire 'activité' %s ajouté sur la commune %d.",
					                                                                                                            DateRangeHelper.toDisplayString(dates),
					                                                                                                            commune.getNoOfs()));
				}
			}
		}
	}

	private static Etablissement createEtablissement(RegpmEtablissement regpm) {
		final Etablissement unireg = new Etablissement();
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	@Override
	protected void doMigrate(RegpmEtablissement regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapping idMapper) {
		// TODO à un moment, il faudra quand-même se demander comment cela se passe avec RCEnt, non ?

		// Attention, il y a des cas où on ne doit pas aveuglément créer un établissement
		// (quand l'établissement apparaît comme mandataire de deux entreprises présentes dans deux graphes distincts, par exemple,
		// ou suite à une reprise sur incident...)
		if (idMapper.hasMappingForEtablissement(regpm.getId())) {
			// l'établissement a déjà été migré rien à faire...
			return;
		}

		// on crée forcément un nouvel établissement
		final Etablissement unireg = uniregStore.saveEntityToDb(createEtablissement(regpm));
		idMapper.addEtablissement(regpm, unireg);

		// les liens vers les individus (= activités indépendantes) doivent bien être pris en compte pour les mandataires, par exemple.
		// en revanche, cela ne signifie pas que l'on doivent aller remplir les graphes de départ avec les établissements d'individus
		// pour eux-mêmes (-> on ne traite les activités indépendantes "PP" que dans le cas où elles sont mandatrices de quelque chose...)

		// on crée les liens vers l'entreprise ou l'individu avec les dates d'établissements stables
		final KeyedSupplier<? extends Contribuable> entiteJuridique = getPolymorphicSupplier(idMapper, regpm::getEntreprise, null, regpm::getIndividu);
		if (entiteJuridique == null) {
			mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.ERROR, "Etablissement sans lien vers une migration ou un individu.");
		}
		else {
			final Supplier<Etablissement> moi = getEtablissementByRegpmIdSupplier(idMapper, regpm.getId());
			final Collection<RegpmEtablissementStable> etablissementsStables = regpm.getEtablissementsStables();
			if (etablissementsStables != null && !etablissementsStables.isEmpty()) {
				// création des liens (= rapports entre tiers)
				etablissementsStables.stream()
						.map(range -> new EntityLinkCollector.EtablissementEntiteJuridiqueLink<>(moi, entiteJuridique, range.getDateDebut(), range.getDateFin()))
						.forEach(linkCollector::addLink);

				// génération de l'information pour la création des fors secondaires associés à ces établissements stables
				enregistrerDemandesForsSecondaires(entiteJuridique, regpm.getDomicilesEtablissements(), mr, etablissementsStables);
			}
			else {
				mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.ERROR, "Etablissement sans aucune période de validité d'un établissement stable.");
			}
		}

		// on ne fait rien des "succursales" d'un établissement, car il n'y en a aucune dans le modèle RegPM

		// on ne fait rien non plus avec les rattachements propriétaires (directs ou via groupe) des établissements, car ceux-ci n'ont pas la personalité juridique
		// (mais on loggue les cas)
		regpm.getRattachementsProprietaires().stream()
				.map(AbstractEntityMigrator::couvertureDepuisRattachementProprietaire)
				.flatMap(Function.<Stream<Pair<RegpmCommune, DateRange>>>identity())
				.map(Pair::getKey)
				.distinct()
				.map(c -> String.format("Etablissement avec rattachement propriétaire direct sur la commune %s/%d.", c.getNom(), c.getNoOfs()))
				.forEach(msg -> mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.WARN, msg));
		regpm.getAppartenancesGroupeProprietaire().stream()
				.map(AbstractEntityMigrator::couvertureDepuisAppartenanceGroupeProprietaire)
				.flatMap(Function.<Stream<Pair<RegpmCommune, DateRange>>>identity())
				.map(Pair::getKey)
				.distinct()
				.map(c -> String.format("Etablissement avec rattachement propriétaire (via groupe) sur la commune %s/%d.", c.getNom(), c.getNoOfs()))
				.forEach(msg -> mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.WARN, msg));

		// adresse
		// TODO usage de l'adresse = COURRIER ou plutôt DOMICILE ?
		// TODO adresse permanente ou pas ?
		// TODO enseigne dans le complément d'adresse ?
		final AdresseTiers adresse = adresseHelper.buildAdresse(regpm.getAdresse(), mr, regpm::getEnseigne, false);
		if (adresse != null) {
			adresse.setUsage(TypeAdresseTiers.COURRIER);
			unireg.addAdresseTiers(adresse);
		}

		// coordonnées financières
		migrateCoordonneesFinancieres(regpm::getCoordonneesFinancieres, unireg, mr);

		// données de base : enseigne, flag "principal" (aucun de ceux qui viennent de RegPM ne le sont, normalement)
		unireg.setEnseigne(regpm.getEnseigne());
		unireg.setPrincipal(false);
		unireg.setNumeroEtablissement(null);        // TODO à voir avec RCEnt

		// domiciles de l'établissement
		migrateDomiciles(regpm, unireg, mr);
	}

	/**
	 * Migration des domiciles de l'établissement
	 * @param regpm établissement RegPM
	 * @param unireg établissement Unireg
	 * @param mr collecteur de messages de suivi
	 */
	private void migrateDomiciles(RegpmEtablissement regpm, Etablissement unireg, MigrationResultProduction mr) {
		// communes de l'établissement (dans la base du mainframe au 12.05.2015, aucun établissement n'a plus d'un domicile non-rectifié)
		// -> en fait, il n'y a toujours qu'au plus une seule commune...

		// création d'un domicile Unireg depuis un domicile RegPM
		final Function<RegpmDomicileEtablissement, DomicileEtablissement> mapper = d -> {
			final DomicileEtablissement domicile = new DomicileEtablissement();
			domicile.setDateDebut(d.getDateValidite());

			final RegpmCommune commune = d.getCommune();
			domicile.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
			domicile.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
			return domicile;
		};

		// liste des domiciles Unireg (sans dates de fin pour le moment, elles seront assignées juste après)
		final List<DomicileEtablissement> domiciles = regpm.getDomicilesEtablissements().stream()
				.filter(domicile -> !domicile.isRectifiee())
				.map(mapper)
				.sorted(Comparator.comparing(DomicileEtablissement::getDateDebut))
				.collect(Collectors.toList());

		// assignation des dates de fin (la dernière date de fin est par défaut à null, et sera mise à autre chose éventuellement lors de l'intersection
		// avec les dates des établissements stables plus bas)
		RegDate dateFinCourante = null;
		for (DomicileEtablissement domicile : CollectionsUtils.revertedOrder(domiciles)) {
			domicile.setDateFin(dateFinCourante);
			dateFinCourante = domicile.getDateDebut().getOneDayBefore();
		}

		// maintenant, on a des domiciles à mettre en regard des établissements stables
		// (en partie pour ajouter une date de fin au dernier domicile le cas échéant)
		final List<DateRange> etablissementsStables = DateRangeHelper.merge(regpm.getEtablissementsStables().stream()
				                                                                           .sorted(DateRangeComparator::compareRanges)
				                                                                           .map(DateRangeHelper.Range::new)
				                                                                           .collect(Collectors.toList()));
		final List<DomicileEtablissement> domicilesStables = domiciles.stream()
				.map(domicile -> Pair.of(domicile, DateRangeHelper.intersections(domicile, etablissementsStables)))     // intersection avec les établissements stables
				.filter(pair -> pair.getValue() != null && !pair.getValue().isEmpty())                      // filtrage des domiciles qui n'ont pas d'intersection avec les établissements stables
				.map(pair -> pair.getValue().stream().map(range -> Pair.of(pair.getKey(), range)))          // duplication en cas d'intersections disjointes
				.flatMap(Function.identity())
				.map(pair -> new DomicileEtablissement(pair.getValue().getDateDebut(),                      // ajustement des dates selon les dates d'intersection
				                                       pair.getValue().getDateFin(),
				                                       pair.getKey().getTypeAutoriteFiscale(),
				                                       pair.getKey().getNumeroOfsAutoriteFiscale(),
				                                       null))
				.collect(Collectors.toList());

		// log ou ajout des domiciles dans l'établissement...
		if (domicilesStables.isEmpty()) {
			mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.ERROR, "Etablissement sans domicile.");
		}
		else {
			domicilesStables.forEach(unireg::addDomicile);
		}
	}

	private void enregistrerDemandesForsSecondaires(KeyedSupplier<? extends Tiers> entiteJuridique,
	                                                Set<RegpmDomicileEtablissement> domiciles,
	                                                MigrationResultProduction mr,
	                                                Collection<RegpmEtablissementStable> etablissementsStables) {

		// les domiciles avec leurs dates d'établissement
		final NavigableMap<RegDate, RegpmDomicileEtablissement> mapDomiciles;
		if (domiciles != null && !domiciles.isEmpty()) {
			// on trie les entités avant de les collecter en map afin de s'assurer que, à dates égales,
			// c'est le dernier qui aura raison...
			mapDomiciles = domiciles.stream()
					.filter(de -> !de.isRectifiee())
					.sorted()
					.collect(Collectors.toMap(RegpmDomicileEtablissement::getDateValidite, Function.identity(), (u, v) -> v, TreeMap::new));
		}
		else {
			mapDomiciles = Collections.emptyNavigableMap();
		}

		// les informations, par communes, des périodes concernées
		final Map<RegpmCommune, List<DateRange>> mapFors = etablissementsStables.stream()
				.map(range -> buildPeriodesForsSecondaires(mapDomiciles, range, mr))
				.flatMap(List::stream)
				.collect(Collectors.toMap(Pair::getKey,
				                          pair -> Collections.singletonList(pair.getValue()),
				                          DATE_RANGE_LIST_MERGER));

		// s'il y a des données relatives à des fors secondaires, on les envoie...
		if (!mapFors.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData.Activite(entiteJuridique, mapFors));
		}
	}
}
