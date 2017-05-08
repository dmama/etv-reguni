package ch.vd.uniregctb.mandataire;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.mock.MockGenreImpotMandataire;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TypeTiers;
import ch.vd.uniregctb.type.TypeMandat;
import ch.vd.uniregctb.utils.UniregProperties;

public class ConfigurationMandataireTest extends WithoutSpringTest {

	private static final Set<GenreImpotMandataire> GENRES_IMPOT = buildGenresImpot();
	private static final Map<TypeTiers, Supplier<Tiers>> SUPPLIERS = buildSuppliers();

	private static Map<TypeTiers, Supplier<Tiers>> buildSuppliers() {
		final Map<TypeTiers, Supplier<Tiers>> map = new EnumMap<>(TypeTiers.class);
		map.put(TypeTiers.AUTRE_COMMUNAUTE, AutreCommunaute::new);
		map.put(TypeTiers.COLLECTIVITE_ADMINISTRATIVE, CollectiviteAdministrative::new);
		map.put(TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE, DebiteurPrestationImposable::new);
		map.put(TypeTiers.ENTREPRISE, Entreprise::new);
		map.put(TypeTiers.ETABLISSEMENT, Etablissement::new);
		map.put(TypeTiers.MENAGE_COMMUN, MenageCommun::new);
		map.put(TypeTiers.PERSONNE_PHYSIQUE, PersonnePhysique::new);
		return Collections.unmodifiableMap(map);
	}

	private static GenreImpotMandataire buildDummyGenreImpot(String code) {
		return new GenreImpotMandataire() {
			@Override
			public String getCode() {
				return code;
			}

			@Override
			public String getLibelle() {
				return "Libellé " + code;
			}
		};
	}

	private static Set<GenreImpotMandataire> buildGenresImpot() {
		return Stream.concat(Stream.of(MockGenreImpotMandataire.ALL), Stream.of(buildDummyGenreImpot("TRUC")))
				.collect(Collectors.toSet());
	}

	private static UniregProperties buildUniregProperties(Consumer<Map<String, String>> filler) {
		final Map<String, String> properties = new LinkedHashMap<>();
		filler.accept(properties);

		return new UniregProperties() {
			@Override
			public Map<String, String> getAllProperties() {
				return Collections.unmodifiableMap(properties);
			}

			@Override
			public String getProperty(String key) {
				return properties.get(key);
			}
		};
	}

	private static ConfigurationMandataire buildConfigurationMandataire(UniregProperties properties, String prefix) throws Exception {
		final ConfigurationMandataireImpl impl = new ConfigurationMandataireImpl();
		impl.setProperties(properties);
		impl.setPropertyNamePrefix(prefix);
		impl.afterPropertiesSet();
		return impl;
	}

	@Test
	public void testTousTypesTiersDansSuppliers() throws Exception {
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Supplier<Tiers> supplier = SUPPLIERS.get(typeTiers);
			Assert.assertNotNull(typeTiers.name(), supplier);

			final Tiers tiers = supplier.get();
			Assert.assertNotNull(typeTiers.name(), tiers);
			Assert.assertEquals(typeTiers, tiers.getType());
		}
	}

	@Test
	public void testEmpty() throws Exception {
		final UniregProperties props = buildUniregProperties(map -> {});        // vide, c'est vide...
		final ConfigurationMandataire config = buildConfigurationMandataire(props, "extprop.mandataires");

		// aucun mandat général
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);
			Assert.assertEquals(ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatGeneral(tiers));
		}

		// aucun mandat tiers
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);
			Assert.assertEquals(ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatTiers(tiers));
		}

		// aucun mandat spécial
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);
			for (GenreImpotMandataire genreImpot : GENRES_IMPOT) {
				Assert.assertEquals(ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatSpecial(tiers, genreImpot));
			}
		}
	}

	private static String buildKey(String prefix, TypeMandat typeMandat, TypeTiers typeTiers) {
		return prefix + "." + typeMandat.name() + "." + typeTiers.name();
	}

	private static String buildKeySpecial(String prefix, TypeTiers typeTiers, String codeGenreImpot) {
		return prefix + "." + TypeMandat.SPECIAL.name() + "." + typeTiers.name() + "." + codeGenreImpot;
	}

	@Test
	public void testConfigurationLibre() throws Exception {

		final String prefix = "extprop.mandataires.affichage";

		final UniregProperties props = buildUniregProperties(map -> {
			map.put(buildKey(prefix, TypeMandat.GENERAL, TypeTiers.ENTREPRISE), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKey(prefix, TypeMandat.TIERS, TypeTiers.ENTREPRISE), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.ENTREPRISE, MockGenreImpotMandataire.DM.getCode()), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.ENTREPRISE, MockGenreImpotMandataire.GI.getCode()), ConfigurationMandataire.Acces.VISUALISATION_SEULE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.ENTREPRISE, MockGenreImpotMandataire.DON.getCode()), ConfigurationMandataire.Acces.AUCUN.name());
			// oups, j'ai oublié TRUC et SUCC

			map.put(buildKey(prefix, TypeMandat.GENERAL, TypeTiers.PERSONNE_PHYSIQUE), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.PERSONNE_PHYSIQUE, MockGenreImpotMandataire.DM.getCode()), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.PERSONNE_PHYSIQUE, MockGenreImpotMandataire.GI.getCode()), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.PERSONNE_PHYSIQUE, MockGenreImpotMandataire.DON.getCode()), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.PERSONNE_PHYSIQUE, MockGenreImpotMandataire.SUCC.getCode()), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
			map.put(buildKeySpecial(prefix, TypeTiers.PERSONNE_PHYSIQUE, "TRUC"), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());

			map.put(buildKey(prefix, TypeMandat.GENERAL, TypeTiers.MENAGE_COMMUN), ConfigurationMandataire.Acces.EDITION_POSSIBLE.name());
		});
		final ConfigurationMandataire config = buildConfigurationMandataire(props, prefix);

		// mandat général
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);
			if (typeTiers == TypeTiers.ENTREPRISE || typeTiers == TypeTiers.PERSONNE_PHYSIQUE || typeTiers == TypeTiers.MENAGE_COMMUN) {
				Assert.assertEquals(typeTiers.name(), ConfigurationMandataire.Acces.EDITION_POSSIBLE, config.getAffichageMandatGeneral(tiers));
			}
			else {
				Assert.assertEquals(typeTiers.name(), ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatGeneral(tiers));
			}
		}

		// mandat tiers
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);
			if (typeTiers == TypeTiers.ENTREPRISE) {
				Assert.assertEquals(typeTiers.name(), ConfigurationMandataire.Acces.EDITION_POSSIBLE, config.getAffichageMandatTiers(tiers));
			}
			else {
				Assert.assertEquals(typeTiers.name(), ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatTiers(tiers));
			}
		}

		// mandat spécial
		for (TypeTiers typeTiers : TypeTiers.values()) {
			final Tiers tiers = SUPPLIERS.get(typeTiers).get();
			Assert.assertNotNull(tiers);

			for (GenreImpotMandataire gim : GENRES_IMPOT) {
				if (typeTiers == TypeTiers.ENTREPRISE) {
					switch (gim.getCode()) {
					case "DM":
						Assert.assertEquals(ConfigurationMandataire.Acces.EDITION_POSSIBLE, config.getAffichageMandatSpecial(tiers, gim));
						break;
					case "GI":
						Assert.assertEquals(ConfigurationMandataire.Acces.VISUALISATION_SEULE, config.getAffichageMandatSpecial(tiers, gim));
						break;
					default:
						Assert.assertEquals(gim.getCode(), ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatSpecial(tiers, gim));
						break;
					}
				}
				else if (typeTiers == TypeTiers.PERSONNE_PHYSIQUE) {
					Assert.assertEquals(gim.getCode(), ConfigurationMandataire.Acces.EDITION_POSSIBLE, config.getAffichageMandatSpecial(tiers, gim));
				}
				else {
					Assert.assertEquals(typeTiers.name() + "/" + gim.getCode(), ConfigurationMandataire.Acces.AUCUN, config.getAffichageMandatSpecial(tiers, gim));
				}
			}
		}
	}
}
