package ch.vd.unireg.tiers.rattrapage.appariement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.interfaces.entreprise.data.DateRanged;
import ch.vd.unireg.interfaces.entreprise.data.Domicile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.data.TypeEtablissementCivil;
import ch.vd.unireg.tiers.ActiviteEconomique;
import ch.vd.unireg.tiers.DomicileEtablissement;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.LocalizedDateRange;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeRapportEntreTiers;

public class AppariementServiceImpl implements AppariementService {

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@NotNull
	@Override
	public List<CandidatAppariement> rechercheAppariementsEtablissementsSecondaires(Entreprise entreprise) {
		final EntrepriseCivile entrepriseCivile = tiersService.getEntrepriseCivile(entreprise);
		if (entrepriseCivile == null) {
			return Collections.emptyList();
		}

		// ça y est, on a une entreprise civile avec ses établissements et une entreprise, face à face...
		final Map<Long, Pair<Etablissement, List<DateRange>>> tousEtablissementsSecondaires = extractEtablissementsSecondaires(entreprise);
		final Map<Long, Pair<EtablissementCivil, List<DateRange>>> tousEtablissementsCivilsSecondaires = extractEtablissementsCivilsSecondaires(entrepriseCivile);

		// 1. récupération des établissements non-appariés de l'entreprise et des numéros cantonaux déjà connus (= à ne pas ré-utiliser...)

		final Map<Long, Pair<Etablissement, List<DateRange>>> etbsSecondairesNonApparies = new HashMap<>(tousEtablissementsSecondaires.size());
		final Set<Long> idsCantonauxApparies = new HashSet<>(tousEtablissementsSecondaires.size());
		for (Map.Entry<Long, Pair<Etablissement, List<DateRange>>> entry : tousEtablissementsSecondaires.entrySet()) {
			final Etablissement etablissement = entry.getValue().getLeft();
			if (!etablissement.isConnuAuCivil()) {
				etbsSecondairesNonApparies.put(entry.getKey(), entry.getValue());
			}
			else {
				idsCantonauxApparies.add(etablissement.getNumeroEtablissement());
			}
		}

		// 2. récupération de la liste des établissements civils secondaires de l'entreprise civile qui ne sont pas encore appariés à un établissement de chez nous

		final Map<Long, Pair<EtablissementCivil, List<DateRange>>> etablissementsSecondairesDisponibles = new HashMap<>(tousEtablissementsCivilsSecondaires);
		etablissementsSecondairesDisponibles.keySet().removeAll(idsCantonauxApparies);

		// s'il n'y a rien à faire, il n'y a rien à faire
		if (etbsSecondairesNonApparies.isEmpty() || etablissementsSecondairesDisponibles.isEmpty()) {
			return Collections.emptyList();
		}

		final List<CandidatAppariement> candidats = new LinkedList<>();

		// 3. recherche par numéro IDE d'abord...

		candidats.addAll(appariementsParNumeroIDE(etbsSecondairesNonApparies, etablissementsSecondairesDisponibles));

		// 4. ceux qui restent sont ensuite comparés par commune, raison sociale...

		candidats.addAll(appariementsParLocalisationEtRaisonSociale(etbsSecondairesNonApparies, etablissementsSecondairesDisponibles));

		// et retour
		return candidats;
	}

	/**
	 * Algorithme d'appariement basé sur le numéro IDE
	 * @param etbsSecondairesNonApparies map des établissements secondaires disponibles à l'appariement (en sortie, cette map peut avoir été amputée des établissements appariés)
	 * @param etablissementSecondairesDisponibles map des établissements civils secondaires disponibles à l'appariement (en sortie, cette map peut avoir été amputée des établissements civils appariés)
	 * @return une liste des appariements proposés
	 */
	@NotNull
	private List<CandidatAppariement> appariementsParNumeroIDE(Map<Long, Pair<Etablissement, List<DateRange>>> etbsSecondairesNonApparies,
	                                                           Map<Long, Pair<EtablissementCivil, List<DateRange>>> etablissementSecondairesDisponibles) {

		// récupération de tous les numéros IDE connus de part et d'autre
		final Map<String, Etablissement> etablissementsParIde = buildMapEtablissementParNumeroIDE(etbsSecondairesNonApparies.values());
		final Map<String, EtablissementCivil> etablissementsCivilsParIde = buildMapEtablissementsCivilsParNumeroIDE(etablissementSecondairesDisponibles.values());

		// seuls nous intéressent ici les IDE qui sont des deux côtés...
		final Set<String> idesCommuns = new HashSet<>(etablissementsParIde.size() + etablissementsCivilsParIde.size());
		idesCommuns.addAll(etablissementsParIde.keySet());
		idesCommuns.retainAll(etablissementsCivilsParIde.keySet());

		// il y a des communs ?
		if (idesCommuns.isEmpty()) {
			return Collections.emptyList();
		}

		final List<CandidatAppariement> candidats = new ArrayList<>(idesCommuns.size());

		// oui, il y en a.., prenons-les un par un...
		for (String ide : idesCommuns) {
			final Etablissement etb = etablissementsParIde.get(ide);
			final EtablissementCivil etablissementCivil = etablissementsCivilsParIde.get(ide);

			final Localisation localisationEtablissement = extractDerniereLocalisation(etb);
			final Localisation localisationEtablissementCivil = extractDerniereLocalisation(etablissementCivil);
			if (areCompatible(localisationEtablissementCivil, localisationEtablissement)) {
				// on ajoute un candidat et on n'oublie pas de retirer les heureux élus des listes de disponibles
				candidats.add(new CandidatAppariement(etb, etablissementCivil, CandidatAppariement.CritereDecisif.IDE, localisationEtablissement));
				etbsSecondairesNonApparies.remove(etb.getNumero());
				etablissementSecondairesDisponibles.remove(etablissementCivil.getNumeroEtablissement());
			}
		}

		return candidats;
	}

	@Nullable
	private static Localisation extractDerniereLocalisation(Etablissement etablissement) {
		final List<DomicileEtablissement> domiciles = etablissement.getSortedDomiciles(false);
		if (domiciles.isEmpty()) {
			return null;
		}
		else {
			return new Localisation(CollectionsUtils.getLastElement(domiciles));
		}
	}

	@Nullable
	private static Localisation extractDerniereLocalisation(EtablissementCivil etablissementCivil) {
		final List<Domicile> domiciles = etablissementCivil.getDomicilesEnActivite();
		if (domiciles == null || domiciles.isEmpty()) {
			return null;
		}
		else {
			return new Localisation(CollectionsUtils.getLastElement(domiciles));
		}
	}

	/**
	 * Implémentation locale de l'interface {@link LocalizedDateRange} pour laquelle les méthodes {@link Object#equals(Object)} et {@link Object#hashCode()}
	 * ne sont basées que sur le type d'autorité fiscale et le numéro OFS, en aucun cas sur les dates...
	 */
	private static final class Localisation implements LocalizedDateRange {

		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final TypeAutoriteFiscale typeAutoriteFiscale;
		private final int numeroOfsAutoriteFiscale;

		public Localisation(RegDate dateDebut, @Nullable RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, int numeroOfsAutoriteFiscale) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.typeAutoriteFiscale = typeAutoriteFiscale;
			this.numeroOfsAutoriteFiscale = numeroOfsAutoriteFiscale;
		}

		public Localisation(LocalizedDateRange src) {
			this(src.getDateDebut(), src.getDateFin(), src.getTypeAutoriteFiscale(), src.getNumeroOfsAutoriteFiscale());
		}

		@Override
		public TypeAutoriteFiscale getTypeAutoriteFiscale() {
			return typeAutoriteFiscale;
		}

		@Override
		public Integer getNumeroOfsAutoriteFiscale() {
			return numeroOfsAutoriteFiscale;
		}

		@Override
		public RegDate getDateDebut() {
			return dateDebut;
		}

		@Override
		public RegDate getDateFin() {
			return dateFin;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Localisation that = (Localisation) o;
			return numeroOfsAutoriteFiscale == that.numeroOfsAutoriteFiscale && typeAutoriteFiscale == that.typeAutoriteFiscale;
		}

		@Override
		public int hashCode() {
			int result = typeAutoriteFiscale != null ? typeAutoriteFiscale.hashCode() : 0;
			result = 31 * result + numeroOfsAutoriteFiscale;
			return result;
		}
	}

	/**
	 * @param localisationCivile localisation "civile"
	 * @param localisationFiscale localisation "fiscale"
	 * @return <code>true</code> si les deux localisations sont non-nulles et compatibles, <code>false</code> dans les cas contraires
	 */
	private static boolean areCompatible(@Nullable Localisation localisationCivile, @Nullable Localisation localisationFiscale) {
		if (localisationCivile == null || localisationFiscale == null) {
			return false;
		}

		// TODO peut-être faut-il être un peu plus souple sur la notion de compatibilité...
		// TODO utiliser le {@link Object#equals()} à la place ?
		return localisationCivile.getTypeAutoriteFiscale() == localisationFiscale.getTypeAutoriteFiscale()
				&& Objects.equals(localisationCivile.getNumeroOfsAutoriteFiscale(), localisationFiscale.getNumeroOfsAutoriteFiscale());
	}

	/**
	 * Algorithme d'appariement basé sur les localisations et, potentiellement, les raisons sociales
	 * @param etbsSecondairesNonApparies map des établissements secondaires disponibles à l'appariement (en sortie, cette map peut avoir été amputée des établissements appariés)
	 * @param etablissementsSecondairesDisponibles map des établissements civils secondaires disponibles à l'appariement (en sortie, cette map peut avoir été amputée des établissements civils appariés)
	 * @return une liste des appariements proposés
	 */
	@NotNull
	private List<CandidatAppariement> appariementsParLocalisationEtRaisonSociale(Map<Long, Pair<Etablissement, List<DateRange>>> etbsSecondairesNonApparies,
	                                                                             Map<Long, Pair<EtablissementCivil, List<DateRange>>> etablissementsSecondairesDisponibles) {

		final DataExtractor<Etablissement, Localisation> etbLocalisationExtractor = AppariementServiceImpl::extractDerniereLocalisation;
		final DataExtractor<EtablissementCivil, Localisation> etablissementLocalisationExtractor = AppariementServiceImpl::extractDerniereLocalisation;

		// on va regrouper les établissements et les établissements civils par dernière localisation
		final Map<Boolean, Map<Localisation, List<Etablissement>>> etbsParLocalisationEtActivite = extractAccordingToLocalisationActivityState(etbsSecondairesNonApparies.values(), etbLocalisationExtractor);
		final Map<Boolean, Map<Localisation, List<EtablissementCivil>>> etablissementsCivilsParLocalisationEtActivite = extractAccordingToLocalisationActivityState(etablissementsSecondairesDisponibles.values(), etablissementLocalisationExtractor);

		// le container résultant
		final List<CandidatAppariement> candidats = new LinkedList<>();

		// en raison du critère de "compatibilité" de la localisation potentiellement un peu plus
		// large qu'une bête égalité (si un jour on parvient à gérer les différences civil/fiscal autour des fractions/fusions de communes...)
		// on ne peut pas simplement travailler sur l'intersection des ensembles de clés des deux maps
		for (Boolean active : Arrays.asList(Boolean.FALSE, Boolean.TRUE)) {

			final Map<Localisation, List<Etablissement>> etbsParLocalisation = etbsParLocalisationEtActivite.get(active);
			final Map<Localisation, List<EtablissementCivil>> etablissementCivilParLocalisation = etablissementsCivilsParLocalisationEtActivite.get(active);

			for (Map.Entry<Localisation, List<Etablissement>> etbEntry : etbsParLocalisation.entrySet()) {

				// on va construire une liste des établissements civils à la localisation compatible
				final List<EtablissementCivil> etablissementsCivilsCompatibles = new ArrayList<>(etablissementsSecondairesDisponibles.size());
				for (Map.Entry<Localisation, List<EtablissementCivil>> etablissementEntry : etablissementCivilParLocalisation.entrySet()) {
					if (areCompatible(etablissementEntry.getKey(), etbEntry.getKey())) {
						etablissementsCivilsCompatibles.addAll(etablissementEntry.getValue());
					}
				}

				// si on en a un de chaque côté, on ne va pas plus loin, on apparie !
				final List<Etablissement> etablissementsCompatibles = etbEntry.getValue();
				if (etablissementsCivilsCompatibles.size() == 1 && etablissementsCompatibles.size() == 1) {
					final Etablissement etbCandidat = etablissementsCompatibles.get(0);
					final EtablissementCivil etablissementCivilCandidat = etablissementsCivilsCompatibles.get(0);

					candidats.add(new CandidatAppariement(etbCandidat, etablissementCivilCandidat, CandidatAppariement.CritereDecisif.LOCALISATION, etbEntry.getKey()));
					etbsSecondairesNonApparies.remove(etbCandidat.getNumero());
					etablissementsSecondairesDisponibles.remove(etablissementCivilCandidat.getNumeroEtablissement());

					// il faut également enlever l'établissement civil élu de la map des établissements civils par localisation
					final Localisation localisationEtablissementElu = etablissementLocalisationExtractor.extract(etablissementCivilCandidat);
					etablissementCivilParLocalisation.remove(localisationEtablissementElu);
				}
			}
		}

		return candidats;
	}

	/**
	 * En Java8, ça s'appelle une java.util.function.Function...
	 */
	private interface DataExtractor<U, V> {
		V extract(U source);
	}

	/**
	 * @param source une collection de données liées à des plages de validités (triées par ordre chronologique)
	 * @param localisationExtractor un extracteur de localisation depuis les données de la collection
	 * @param <T> le type de la donnée
	 * @return une map (clé = activité, valeur = map des données par localisation)
	 */
	@NotNull
	private static <T> Map<Boolean, Map<Localisation, List<T>>> extractAccordingToLocalisationActivityState(Collection<Pair<T, List<DateRange>>> source,
	                                                                                                        DataExtractor<? super T, Localisation> localisationExtractor) {

		final Map<Boolean, Map<Localisation, List<T>>> map = new HashMap<>(2);
		map.put(Boolean.TRUE, new HashMap<>(source.size()));
		map.put(Boolean.FALSE, new HashMap<>(source.size()));

		for (Pair<T, List<DateRange>> pair : source) {
			final Boolean active = CollectionsUtils.getLastElement(pair.getRight()).getDateFin() == null ? Boolean.TRUE : Boolean.FALSE;
			final Map<Localisation, List<T>> submap = map.get(active);
			final T data = pair.getLeft();
			final Localisation localisation = localisationExtractor.extract(data);
			final List<T> onLocalisation = submap.computeIfAbsent(localisation, k -> new ArrayList<>(source.size()));
			onLocalisation.add(data);
		}
		return map;
	}

	@NotNull
	private static <K, V> Map<K, List<V>> dispatchToMap(Collection<? extends V> collection, DataExtractor<? super V, ? extends K> keyExtractor) {
		final Map<K, List<V>> map = new HashMap<>(collection.size());
		for (V data : collection) {
			final K key = keyExtractor.extract(data);
			final List<V> list = map.computeIfAbsent(key, k -> new ArrayList<>(collection.size()));
			list.add(data);
		}
		return map;
	}

	@NotNull
	private static <T> Map<String, EtablissementCivil> buildMapEtablissementsCivilsParNumeroIDE(Collection<Pair<EtablissementCivil, T>> col) {
		final Set<String> badIDEs = new HashSet<>(col.size());
		final Map<String, EtablissementCivil> parIde = new HashMap<>(col.size());
		for (Pair<EtablissementCivil, T> pair : col) {
			final EtablissementCivil etablissementCivil = pair.getLeft();
			final List<DateRanged<String>> ides = etablissementCivil.getNumeroIDE();
			if (ides != null && !ides.isEmpty()) {
				final Set<String> localIdes = new HashSet<>(ides.size());
				for (DateRanged<String> rangedIde : ides) {
					localIdes.add(rangedIde.getPayload());
				}
				for (String ide : localIdes) {
					if (!badIDEs.contains(ide)) {
						if (parIde.containsKey(ide)) {
							// doublon de numéro IDE sur des établissements différents ???
							badIDEs.add(ide);
							parIde.remove(ide);
						}
						parIde.put(ide, etablissementCivil);
					}
				}
			}
		}
		return parIde;
	}

	@NotNull
	private static <T> Map<String, Etablissement> buildMapEtablissementParNumeroIDE(Collection<Pair<Etablissement, T>> col) {
		final Set<String> badIDEs = new HashSet<>(col.size());
		final Map<String, Etablissement> parIde = new HashMap<>(col.size());
		for (Pair<Etablissement, T> pair : col) {
			final Etablissement etb = pair.getLeft();
			final Set<IdentificationEntreprise> identifications = etb.getIdentificationsEntreprise();
			if (identifications != null && !identifications.isEmpty()) {
				final Set<String> localIdes = new HashSet<>(identifications.size());
				for (IdentificationEntreprise identification : identifications) {
					final String ide = identification.getNumeroIde();
					if (!identification.isAnnule() && StringUtils.isNotBlank(ide)) {
						localIdes.add(ide);
					}
				}
				for (String ide : localIdes) {
					if (!badIDEs.contains(ide)) {
						if (parIde.containsKey(ide)) {
							// doublons de numéro IDE sur des établissements différents ???
							badIDEs.add(ide);
							parIde.remove(ide);
						}
						else {
							parIde.put(ide, etb);
						}
					}
				}
			}
		}
		return parIde;
	}

	@NotNull
	private static Map<Long, Pair<EtablissementCivil, List<DateRange>>> extractEtablissementsCivilsSecondaires(EntrepriseCivile entrepriseCivile) {
		final List<EtablissementCivil> etablissements = entrepriseCivile.getEtablissements();
		final Map<Long, Pair<EtablissementCivil, List<DateRange>>> map = new HashMap<>(etablissements.size());
		for (EtablissementCivil etablissement : etablissements) {
			final List<DateRange> periodesSecondaires = getPeriodesEtablissementSecondaire(etablissement);
			if (!periodesSecondaires.isEmpty()) {
				map.put(etablissement.getNumeroEtablissement(), Pair.of(etablissement, periodesSecondaires));
			}
		}
		return map;
	}

	@NotNull
	private static List<DateRange> getPeriodesEtablissementSecondaire(EtablissementCivil etablissement) {
		final List<DateRanged<TypeEtablissementCivil>> typesEtablissement = etablissement.getTypesEtablissement();
		if (typesEtablissement == null || typesEtablissement.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DateRange> ranges = new ArrayList<>(typesEtablissement.size());
		for (DateRanged<TypeEtablissementCivil> type : typesEtablissement) {
			if (type.getPayload() == TypeEtablissementCivil.ETABLISSEMENT_SECONDAIRE) {
				ranges.add(type);
			}
		}
		return ranges;
	}

	@NotNull
	private Map<Long, Pair<Etablissement, List<DateRange>>> extractEtablissementsSecondaires(Entreprise entreprise) {
		final Set<RapportEntreTiers> rapports = entreprise.getRapportsSujet();
		final List<Pair<Etablissement, DateRange>> liste = new ArrayList<>(rapports.size());
		for (RapportEntreTiers rapport : rapports) {
			if (!rapport.isAnnule() && rapport.getType() == TypeRapportEntreTiers.ACTIVITE_ECONOMIQUE && !((ActiviteEconomique) rapport).isPrincipal()) {
				final Etablissement etb = (Etablissement) tiersService.getTiers(rapport.getObjetId());
				liste.add(Pair.of(etb, rapport));
			}
		}

		if (liste.isEmpty()) {
			return Collections.emptyMap();
		}

		liste.sort(new Comparator<Pair<Etablissement, DateRange>>() {
			@Override
			public int compare(Pair<Etablissement, DateRange> o1, Pair<Etablissement, DateRange> o2) {
				int comparison = Long.compare(o1.getLeft().getNumero(), o2.getLeft().getNumero());
				if (comparison == 0) {
					comparison = DateRangeComparator.compareRanges(o1.getRight(), o2.getRight());
				}
				return comparison;
			}
		});
		final Map<Long, Pair<Etablissement, List<DateRange>>> map = new HashMap<>(liste.size());
		for (Pair<Etablissement, DateRange> pair : liste) {
			final Long id = pair.getLeft().getNumero();
			final Pair<Etablissement, List<DateRange>> eltExistant = map.get(id);
			final List<DateRange> eltListe;
			if (eltExistant == null) {
				eltListe = new ArrayList<>();
				map.put(id, Pair.of(pair.getLeft(), eltListe));
			}
			else {
				eltListe = eltExistant.getRight();
			}
			eltListe.add(pair.getRight());
		}
		return map;
	}
}
