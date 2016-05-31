package ch.vd.uniregctb.migration.pm.communes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.migration.pm.utils.DataLoadHelper;

public class FusionCommunesProviderImpl implements FusionCommunesProvider, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(FusionCommunesProviderImpl.class);

	/**
	 * Nom du fichier d'entrée pour les données des mutations extraites de RefInf
	 */
	private String nomFichierSource;

	/**
	 * Map entre un identifiant OFS de commune et les mutations associées
	 */
	private final Map<Integer, NavigableMap<RegDate, DonneesMutation>> communes = new HashMap<>();

	public void setNomFichierSource(String nomFichierSource) {
		this.nomFichierSource = nomFichierSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		// on va lire le fichier d'input et remplir les données
		// le format du fichier est, colonne par colonne (séparation faite par des ";")
		// 1. le numéro OFS d'une commune
		// 2. l'identification de la mutation de création (optionnelle)
		// 3. la date de la mutation de création (obligatoire si la colonne 2 est remplie)
		// 4. l'identification de la mutation de disparition (optionnelle)
		// 5. la date de la mutation de disparition (obligatoire si la colonne 4 est remplie)
		final Pattern pattern = Pattern.compile("([0-9]+);([0-9]+)?;([0-9]{4}-[0-9]{2}-[0-9]{2})?;([0-9]+)?;([0-9]{4}-[0-9]{2}-[0-9]{2})?");

		// les données extraites du fichier...
		final List<DonneesSource> data;

		LOGGER.info("Chargement du fichier " + nomFichierSource + " pour les informations des fusions de communes passées.");
		try (FileInputStream fis = new FileInputStream(nomFichierSource);
		     Reader reader = new InputStreamReader(fis);
		     BufferedReader br = new BufferedReader(reader)) {

			data = DataLoadHelper.loadData(br, pattern, matcher -> {
				final int noOfs = Integer.valueOf(matcher.group(1));
				final Long idMutationCreation = extractOptionalLong(matcher.group(2));
				final RegDate dateMutationCreation = extractOptionalDate(matcher.group(3));
				final Long idMutationDisparition = extractOptionalLong(matcher.group(4));
				final RegDate dateMutationDisparition = extractOptionalDate(matcher.group(5));
				return new DonneesSource(noOfs, idMutationCreation, dateMutationCreation, idMutationDisparition, dateMutationDisparition);
			});
		}
		LOGGER.info(data.size() + " ligne(s) chargée(s) du fichier " + nomFichierSource + ".");

		// maintenant, on va remplir la structure de données adaptée à nos besoins

		final Map<Long, DonneesMutation> mutations = new HashMap<>();
		for (DonneesSource ds : data) {
			final NavigableMap<RegDate, DonneesMutation> mapCommune = communes.computeIfAbsent(ds.ofsCommune, k -> new TreeMap<>());
			if (ds.mutationCreation != null && ds.dateMutationCreation != null) {
				final DonneesMutation creation = mutations.computeIfAbsent(ds.mutationCreation, k -> new DonneesMutation());
				creation.addOfsApres(ds.ofsCommune);
				if (mapCommune.put(ds.dateMutationCreation, creation) != null) {
					throw new IllegalArgumentException("Plusieurs mutations à la même date pour la commune " + ds.ofsCommune);
				}
			}
			if (ds.mutationDisparition != null && ds.dateMutationDisparition != null) {
				final DonneesMutation disparition = mutations.computeIfAbsent(ds.mutationDisparition, k -> new DonneesMutation());
				disparition.addOfsAvant(ds.ofsCommune);
				if (mapCommune.put(ds.dateMutationDisparition, disparition) != null) {
					throw new IllegalArgumentException("Plusieurs mutations à la même date pour la commune " + ds.ofsCommune);
				}
			}
		}
	}

	@Nullable
	private static Long extractOptionalLong(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		return Long.valueOf(str);
	}

	/**
	 * @param str au format {@link ch.vd.registre.base.date.RegDateHelper.StringFormat#DASH}
	 * @return la date lue
	 */
	@Nullable
	private static RegDate extractOptionalDate(String str) {
		if (StringUtils.isBlank(str)) {
			return null;
		}
		return RegDateHelper.dashStringToDate(str);
	}

	@NotNull
	@Override
	public List<Integer> getCommunesAvant(int noOfs, @NotNull RegDate dateFusion) {
		final NavigableMap<RegDate, DonneesMutation> mapMutations = communes.get(noOfs);
		if (mapMutations == null) {
			return Collections.emptyList();
		}
		final Map.Entry<RegDate, DonneesMutation> entry = mapMutations.ceilingEntry(dateFusion);
		if (entry == null) {
			return Collections.emptyList();
		}
		return entry.getValue().getOfsAvant();
	}

	@NotNull
	@Override
	public List<Integer> getCommunesApres(int noOfs, @NotNull RegDate dateDisparition) {
		final NavigableMap<RegDate, DonneesMutation> mapMutations = communes.get(noOfs);
		if (mapMutations == null) {
			return Collections.emptyList();
		}
		final Map.Entry<RegDate, DonneesMutation> entry = mapMutations.floorEntry(dateDisparition);
		if (entry == null) {
			return Collections.emptyList();
		}
		return entry.getValue().getOfsApres();
	}

	/**
	 * Les données associées à une mutation
	 */
	static final class DonneesMutation {
		private final Set<Integer> noOfsAvant = new TreeSet<>();
		private final Set<Integer> noOfsApres = new TreeSet<>();

		public void addOfsAvant(int noOfs) {
			noOfsAvant.add(noOfs);
		}

		public void addOfsApres(int noOfs) {
			noOfsApres.add(noOfs);
		}

		@NotNull
		public List<Integer> getOfsAvant() {
			return noOfsAvant.isEmpty() ? Collections.emptyList() : new ArrayList<>(noOfsAvant);
		}

		@NotNull
		public List<Integer> getOfsApres() {
			return noOfsApres.isEmpty() ? Collections.emptyList() : new ArrayList<>(noOfsApres);
		}
	}

	/**
	 * Données directement extraite du fichier d'entrée
	 */
	private static final class DonneesSource {
		final int ofsCommune;
		final Long mutationCreation;
		final RegDate dateMutationCreation;
		final Long mutationDisparition;
		final RegDate dateMutationDisparition;

		public DonneesSource(int ofsCommune, Long mutationCreation, RegDate dateMutationCreation, Long mutationDisparition, RegDate dateMutationDisparition) {
			this.ofsCommune = ofsCommune;
			this.mutationCreation = mutationCreation;
			this.dateMutationCreation = dateMutationCreation;
			this.mutationDisparition = mutationDisparition;
			this.dateMutationDisparition = dateMutationDisparition;
		}
	}
}
