package ch.vd.uniregctb.migration.pm;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.CollatableDateRange;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCanton;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCommune;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDomicileEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissementStable;
import ch.vd.uniregctb.migration.pm.utils.EntityKey;
import ch.vd.uniregctb.migration.pm.utils.EntityLinkCollector;
import ch.vd.uniregctb.migration.pm.utils.IdMapper;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class EtablissementMigrator extends AbstractEntityMigrator<RegpmEtablissement> {

	private static final BinaryOperator<List<DateRange>> LIST_MERGER = (l1, l2) -> {
		final List<DateRange> liste = Stream.concat(l1.stream(), l2.stream())
				.sorted(new DateRangeComparator<>())
				.collect(Collectors.toList());
		return DateRangeHelper.merge(liste);
	};

	private static final BinaryOperator<Map<RegpmCommune, List<DateRange>>> MAP_MERGER = (m1, m2) -> {
		//noinspection CodeBlock2Expr
		return Stream.concat(m1.entrySet().stream(), m2.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, LIST_MERGER));
	};

	public EtablissementMigrator(SessionFactory uniregSessionFactory, StreetDataMigrator streetDataMigrator, TiersDAO tiersDAO) {
		super(uniregSessionFactory, streetDataMigrator, tiersDAO);
	}

	private static List<Pair<RegpmCommune, CollatableDateRange>> buildPeriodesForsSecondaires(NavigableMap<RegDate, RegpmDomicileEtablissement> domicilesValides, DateRange range, MigrationResultProduction mr) {
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileDebut = domicilesValides.floorEntry(range.getDateDebut());
		final Map.Entry<RegDate, RegpmDomicileEtablissement> domicileFin = range.getDateFin() != null ? domicilesValides.floorEntry(range.getDateFin()) : domicilesValides.lastEntry();

		// si l'une ou l'autre des entrées est nulle, c'est que le range demandé est plus grand que le range couvert par les domicile...
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
			return String.format("Etablissement %d de l'individu %d", entity.getId(), entity.getIndividu().getId());
		}
		return String.format("Etablissement %d", entity.getId());
	}

	@Override
	public void initMigrationResult(MigrationResult mr) {
		super.initMigrationResult(mr);

		// on va regrouper les données (communes et dates) par entité juridique afin de créer,
		// pour chacune d'entre elles, les fors secondaires qui vont bien
		mr.registerPreTransactionCommitCallback(ForsSecondairesData.class,
		                                        d -> d.entiteJuridiqueSupplier,
		                                        (d1, d2) -> new ForsSecondairesData(d1.entiteJuridiqueSupplier, MAP_MERGER.apply(d1.communes, d2.communes)),
		                                        d -> createForsSecondaires(d, mr));
	}

	/**
	 * Appelé pour la consolidation des données de fors secondaires par entité juridique
	 * @param data les données collectées pour une entité juridique
	 * @param masterMr le collecteur de messages de suivi
	 */
	private void createForsSecondaires(ForsSecondairesData data, MigrationResultProduction masterMr) {
		final EntityKey keyEntiteJuridique = data.entiteJuridiqueSupplier.getKey();
		final MigrationResultProduction mr = masterMr.withMessagePrefix(String.format("%s %d", keyEntiteJuridique.getType().getDisplayName(), keyEntiteJuridique.getId()));
		final Tiers entiteJuridique = data.entiteJuridiqueSupplier.get();
		for (Map.Entry<RegpmCommune, List<DateRange>> communeData : data.communes.entrySet()) {

			// TODO attention, dans le cas d'un individu, les fors secondaires peuvent devoir être créés sur un couple !!
			// TODO il convient peut-être également de vérifier que des fors secondaires équivalents ne sont pas déjà là... (y compris sur les entreprises, puisque l'on migre les fors secondaires directement également)
			// TODO l'établissement principal doit-il générer un for secondaire ?

			final RegpmCommune commune = communeData.getKey();
			if (commune.getCanton() != RegpmCanton.VD) {
				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.WARN,
				              String.format("For(s) secondaire(s) sur la commune de %s (%d) sise dans le canton %s.", commune.getNom(), commune.getNoOfs(), commune.getCanton()));
			}

			for (DateRange dates : communeData.getValue()) {
				final ForFiscalSecondaire ffs = new ForFiscalSecondaire();
				ffs.setDateDebut(dates.getDateDebut());
				ffs.setDateFin(dates.getDateFin());
				ffs.setGenreImpot(GenreImpot.BENEFICE_CAPITAL);
				ffs.setTypeAutoriteFiscale(commune.getCanton() == RegpmCanton.VD ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
				ffs.setNumeroOfsAutoriteFiscale(commune.getNoOfs());
				ffs.setMotifRattachement(MotifRattachement.ETABLISSEMENT_STABLE);
				ffs.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
				ffs.setMotifFermeture(dates.getDateFin() != null ? MotifFor.FIN_EXPLOITATION : null);
				ffs.setTiers(entiteJuridique);
				entiteJuridique.addForFiscal(ffs);

				mr.addMessage(MigrationResultMessage.CategorieListe.FORS, MigrationResultMessage.Niveau.INFO, String.format("For secondaire %s ajouté sur la commune %d.", DateRangeHelper.toDisplayString(dates), commune.getNoOfs()));
			}
		}
	}

	private static Etablissement createEtablissement(RegpmEtablissement regpm) {
		final Etablissement unireg = new Etablissement();
		copyCreationMutation(regpm, unireg);
		return unireg;
	}

	@Override
	protected void doMigrate(RegpmEtablissement regpm, MigrationResultProduction mr, EntityLinkCollector linkCollector, IdMapper idMapper) {
		// TODO à un moment, il faudra quand-même se demander comment cela se passe avec RCEnt, non ?

		// on crée forcément un nouvel établissement
		final Etablissement unireg = saveEntityToDb(createEtablissement(regpm));
		idMapper.addEtablissement(regpm, unireg);

		// TODO vérifier que l'on doit bien prendre en compte les liens vers les individus... (si c'est le cas, les graphes ne doivent pas être construits seulement à partir des identifiants d'entreprises)
		// on crée les liens vers l'entreprise ou l'individu avec les dates d'établissements stables
		final KeyedSupplier<? extends Tiers> entiteJuridique = getPolymorphicSupplier(idMapper, regpm::getEntreprise, null, regpm::getIndividu);
		if (entiteJuridique == null) {
			mr.addMessage(MigrationResultMessage.CategorieListe.ETABLISSEMENTS, MigrationResultMessage.Niveau.ERROR, "Etablissement sans lien vers une entreprise ou un individu.");
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

		// TODO que fait-on des "succursales" d'un établissement ?
		// TODO migrer l'enseigne, les coordonnées financières...
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
				                          LIST_MERGER));

		// s'il y a des données relatives à des fors secondaires, on les envoie...
		if (!mapFors.isEmpty()) {
			mr.addPreTransactionCommitData(new ForsSecondairesData(entiteJuridique, mapFors));
		}
	}

	protected static final class ForsSecondairesData {

		final KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier;
		final Map<RegpmCommune, List<DateRange>> communes;

		public ForsSecondairesData(KeyedSupplier<? extends Tiers> entiteJuridiqueSupplier,
		                           Map<RegpmCommune, List<DateRange>> communes) {
			this.entiteJuridiqueSupplier = entiteJuridiqueSupplier;
			this.communes = communes;
		}
	}
}
