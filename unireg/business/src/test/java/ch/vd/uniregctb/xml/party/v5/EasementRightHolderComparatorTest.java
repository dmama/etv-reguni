package ch.vd.uniregctb.xml.party.v5;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPM;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.uniregctb.registrefoncier.CommunauteRFMembreComparatorTest.assertAfter;
import static ch.vd.uniregctb.registrefoncier.CommunauteRFMembreComparatorTest.assertBefore;

@SuppressWarnings("Duplicates")
public class EasementRightHolderComparatorTest {

	private EasementRightHolderComparator comparator;
	private Map<Long, Tiers> tiersMap;
	private Map<Long, NomPrenom> nomPrenomMap;
	private Map<Long, String> raisonSocialeMap;

	@Before
	public void setUp() throws Exception {

		this.tiersMap = new HashMap<>();
		this.nomPrenomMap = new HashMap<>();
		this.raisonSocialeMap = new HashMap<>();

		final Function<Long, Tiers> tiersGetter = tiersMap::get;
		final Function<PersonnePhysique, NomPrenom> nomPrenomGetter = pp -> nomPrenomMap.get(pp.getNumero());
		final Function<Tiers, String> raisonSocialeGetter = tiers -> raisonSocialeMap.get(tiers.getNumero());

		this.comparator = new EasementRightHolderComparator(tiersGetter, nomPrenomGetter, raisonSocialeGetter);
	}

	@Test
	public void testCompareByHolderType() throws Exception {
		assertBefore(comparator.compare(newCtb(1), newImmeuble(2L)));   // CTB avant Immeuble
		assertBefore(comparator.compare(newCtb(1), newNonRapproche(3)));               // CTB avant Non-rapproché
		assertAfter(comparator.compare(newImmeuble(2L), newCtb(1)));    // Immeuble après CTB
		assertAfter(comparator.compare(newNonRapproche(3), newCtb(1)));                // Non-rapproché après CTB
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
		assertBefore(comparator.compare(newCtb(1), newCtb(3)));  // VD avant HC
		assertBefore(comparator.compare(newCtb(1), newCtb(4)));  // VD avant HS
		assertBefore(comparator.compare(newCtb(3), newCtb(4)));  // HC avant HS
		assertAfter(comparator.compare(newCtb(3), newCtb(1)));  // HC après VD
		assertAfter(comparator.compare(newCtb(4), newCtb(1)));  // HS après VD
		assertAfter(comparator.compare(newCtb(4), newCtb(3)));  // HS après HC
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
		assertBefore(comparator.compare(newCtb(1), newCtb(2)));  // "Arnold" avant "Zoltan"
		assertAfter(comparator.compare(newCtb(2), newCtb(1)));  // "Zoltan" après "Arnold"
	}


	@NotNull
	private static RightHolder newCtb(int taxPayerNumber) {
		return new RightHolder(taxPayerNumber, null, null, 1, null);
	}


	@NotNull
	private static RightHolder newImmeuble(long immovablePropertyId) {
		return new RightHolder(null, immovablePropertyId, null, 0, null);
	}

	@NotNull
	private static RightHolder newNonRapproche(int id) {
		return new RightHolder(null, null, new NaturalPersonIdentity(id, null, null, null, 0, null), 0, null);
	}
}