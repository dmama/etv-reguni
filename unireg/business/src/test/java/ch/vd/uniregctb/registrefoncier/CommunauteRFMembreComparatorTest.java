package ch.vd.uniregctb.registrefoncier;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertTrue;

public class CommunauteRFMembreComparatorTest {

	private CommunauteRFMembreComparator comparator;
	private Map<Long, Tiers> tiersMap;
	private Map<Long, List<ForFiscalPrincipal>> forsVirtuels;
	private Map<Long, NomPrenom> nomPrenomMap;
	private Map<Long, String> raisonSocialeMap;

	@Before
	public void setUp() throws Exception {

		this.tiersMap = new HashMap<>();
		this.forsVirtuels = new HashMap<>();
		this.nomPrenomMap = new HashMap<>();
		this.raisonSocialeMap = new HashMap<>();

		final Function<Long, Tiers> tiersGetter = tiersMap::get;
		final Function<Tiers, List<ForFiscalPrincipal>> forsVirtuelsGetter = tiers -> forsVirtuels.get(tiers.getNumero());
		final Function<PersonnePhysique, NomPrenom> nomPrenomGetter = pp -> nomPrenomMap.get(pp.getNumero());
		final Function<Tiers, String> raisonSocialeGetter = tiers -> raisonSocialeMap.get(tiers.getNumero());

		this.comparator = new CommunauteRFMembreComparator(tiersGetter, forsVirtuelsGetter, nomPrenomGetter, raisonSocialeGetter);
	}

	@Test
	public void testCompareByForFiscalTypes() throws Exception {

		// on ajoute quelques tiers de types différents
		PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(1L);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		Entreprise pm3 = new Entreprise();
		pm3.setNumero(3L);
		pm3.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_HC, null));

		Entreprise pm4 = new Entreprise();
		pm4.setNumero(4L);
		pm4.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.PAYS_HS, null));

		tiersMap.put(pp1.getNumero(), pp1);
		tiersMap.put(pm3.getNumero(), pm3);
		tiersMap.put(pm4.getNumero(), pm4);

		nomPrenomMap.put(pp1.getNumero(), new NomPrenom(pp1.getNom(), pp1.getPrenomUsuel()));

		raisonSocialeMap.put(pm3.getNumero(), "Ma petit entreprise");
		raisonSocialeMap.put(pm4.getNumero(), "Ne connaît pas la crise");

		// on vérifie que l'ordre des types de fors fiscaux est bien respecté
		assertBefore(comparator.compare(1L, 3L));  // VD avant HC
		assertBefore(comparator.compare(1L, 4L));  // VD avant HS
		assertBefore(comparator.compare(3L, 4L));  // HC avant HS

		assertAfter(comparator.compare(3L, 1L));  // HC après VD
		assertAfter(comparator.compare(4L, 1L));  // HS après VD
		assertAfter(comparator.compare(4L, 3L));  // HS après HC
	}

	/**
	 * [SIFISC-24521] On s'assure que les fors fiscaux virtuels sont utilisés s'ils existent.
	 */
	@Test
	public void testCompareByForFiscalTypesAvecMenageCommun() throws Exception {

		// on ajoute quelques tiers VD/HC/HS en ménage commun (et donc avec des fors fiscaux virtuels)
		PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(1L);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.setForsFiscaux(Collections.emptySet());
		forsVirtuels.put(1L, Collections.singletonList(new ForFiscalPrincipalPP(null, null, null, null, null,
		                                                                        TypeAutoriteFiscale.COMMUNE_HC, null, null)));

		PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(2L);
		pp2.setPrenomUsuel("Jean");
		pp2.setNom("Routourne");
		pp2.setForsFiscaux(Collections.emptySet());
		forsVirtuels.put(2L, Collections.singletonList(new ForFiscalPrincipalPP(null, null, null, null, null,
		                                                                        TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null)));

		PersonnePhysique pp3 = new PersonnePhysique(false);
		pp3.setNumero(3L);
		pp3.setPrenomUsuel("Janine");
		pp3.setNom("Vosch");
		pp3.setForsFiscaux(Collections.emptySet());
		forsVirtuels.put(3L, Collections.singletonList(new ForFiscalPrincipalPP(null, null, null, null, null,
		                                                                        TypeAutoriteFiscale.PAYS_HS, null, null)));

		tiersMap.put(pp1.getNumero(), pp1);
		tiersMap.put(pp2.getNumero(), pp2);
		tiersMap.put(pp3.getNumero(), pp3);

		nomPrenomMap.put(pp1.getNumero(), new NomPrenom(pp1.getNom(), pp1.getPrenomUsuel()));
		nomPrenomMap.put(pp2.getNumero(), new NomPrenom(pp2.getNom(), pp2.getPrenomUsuel()));
		nomPrenomMap.put(pp3.getNumero(), new NomPrenom(pp3.getNom(), pp3.getPrenomUsuel()));

		// on vérifie que l'ordre des types de fors fiscaux est bien respecté
		assertAfter(comparator.compare(1L, 2L));   // HC après VD
		assertBefore(comparator.compare(1L, 3L));  // HC avant HS
		assertBefore(comparator.compare(2L, 3L));  // VD avant HS

		assertBefore(comparator.compare(2L, 1L));  // VD avant HC
		assertAfter(comparator.compare(3L, 1L));  // HS après HC
		assertAfter(comparator.compare(3L, 2L));  // HS après VD
	}

	@Test
	public void testCompareByTiersTypes() throws Exception {

		// on ajoute quelques tiers de types différents
		PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(1L);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		Entreprise pm3 = new Entreprise();
		pm3.setNumero(3L);
		pm3.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		CollectiviteAdministrative coll5 = new CollectiviteAdministrative();
		coll5.setNumero(5L);
		coll5.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		tiersMap.put(pp1.getNumero(), pp1);
		tiersMap.put(pm3.getNumero(), pm3);
		tiersMap.put(coll5.getNumero(), coll5);

		nomPrenomMap.put(pp1.getNumero(), new NomPrenom(pp1.getNom(), pp1.getPrenomUsuel()));

		raisonSocialeMap.put(pm3.getNumero(), "Ma petit entreprise");
		raisonSocialeMap.put(coll5.getNumero(), "Administration cantonale des impôts");

		// on vérifie que l'ordre des types de tiers est bien respecté
		assertBefore(comparator.compare(3L, 1L));  // PM avant PP
		assertBefore(comparator.compare(3L, 5L));  // PM avant Coll
		assertBefore(comparator.compare(1L, 5L));  // PP avant Coll

		assertAfter(comparator.compare(1L, 3L));  // PP après PM
		assertAfter(comparator.compare(5L, 3L));  // Coll après PM
		assertAfter(comparator.compare(5L, 1L));  // Coll après PP
	}

	@Test
	public void testCompareByNoms() throws Exception {

		// on ajoute quelques tiers de types différents
		PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(1L);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Fjjuii");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(2L);
		pp2.setPrenomUsuel("Zoltan");
		pp2.setNom("Aarau");
		pp2.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		Entreprise pm3 = new Entreprise();
		pm3.setNumero(3L);
		pm3.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		Entreprise pm4 = new Entreprise();
		pm4.setNumero(4L);
		pm4.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		CollectiviteAdministrative coll5 = new CollectiviteAdministrative();
		coll5.setNumero(5L);
		coll5.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		CollectiviteAdministrative coll6 = new CollectiviteAdministrative();
		coll6.setNumero(6L);
		coll6.addForFiscal(new ForFiscalPrincipalPM(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null));

		tiersMap.put(pp1.getNumero(), pp1);
		tiersMap.put(pp2.getNumero(), pp2);
		tiersMap.put(pm3.getNumero(), pm3);
		tiersMap.put(pm4.getNumero(), pm4);
		tiersMap.put(coll5.getNumero(), coll5);
		tiersMap.put(coll6.getNumero(), coll6);

		nomPrenomMap.put(pp1.getNumero(), new NomPrenom(pp1.getNom(), pp1.getPrenomUsuel()));
		nomPrenomMap.put(pp2.getNumero(), new NomPrenom(pp2.getNom(), pp2.getPrenomUsuel()));

		raisonSocialeMap.put(pm3.getNumero(), "Ma petit entreprise");
		raisonSocialeMap.put(pm4.getNumero(), "Ne connaît pas la crise");
		raisonSocialeMap.put(coll5.getNumero(), "Administration cantonale des impôts");
		raisonSocialeMap.put(coll6.getNumero(), "Piscine-Club");

		// on vérifie que l'ordre des types de tiers est bien respecté
		assertBefore(comparator.compare(2L, 1L));  // "Aarau" avant "Fjjuii"
		assertBefore(comparator.compare(3L, 4L));  // "Ma petite entreprise" avant "Ne connaît pas la crise"
		assertBefore(comparator.compare(5L, 6L));  // "Administration cantonale des impôts" avant "Piscine-Club"

		assertAfter(comparator.compare(1L, 2L));  // "Fjjuii" après "Aarau"
		assertAfter(comparator.compare(4L, 3L));  // "Ne connaît pas la crise" après "Ma petite entreprise"
		assertAfter(comparator.compare(6L, 5L));  // "Piscine-Club" après "Administration cantonale des impôts"
	}

	@Test
	public void testCompareByPrenoms() throws Exception {

		// on ajoute quelques tiers de types différents
		PersonnePhysique pp1 = new PersonnePhysique(false);
		pp1.setNumero(1L);
		pp1.setPrenomUsuel("Arnold");
		pp1.setNom("Bolomey");
		pp1.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		PersonnePhysique pp2 = new PersonnePhysique(false);
		pp2.setNumero(2L);
		pp2.setPrenomUsuel("Zoltan");
		pp2.setNom("Bolomey");
		pp2.addForFiscal(new ForFiscalPrincipalPP(null, null, null, null, null, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null, null));

		tiersMap.put(pp1.getNumero(), pp1);
		tiersMap.put(pp2.getNumero(), pp2);

		nomPrenomMap.put(pp1.getNumero(), new NomPrenom(pp1.getNom(), pp1.getPrenomUsuel()));
		nomPrenomMap.put(pp2.getNumero(), new NomPrenom(pp2.getNom(), pp2.getPrenomUsuel()));

		// on vérifie que l'ordre des types de tiers est bien respecté
		assertBefore(comparator.compare(1L, 2L));  // "Arnold" avant "Zoltan"
		assertAfter(comparator.compare(2L, 1L));  // "Zoltan" après "Arnold"
	}

	public static void assertBefore(int compare) {
		assertTrue(compare < 0);
	}

	public static void assertAfter(int compare) {
		assertTrue(compare > 0);
	}
}