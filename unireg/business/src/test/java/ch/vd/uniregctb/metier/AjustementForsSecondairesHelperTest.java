package ch.vd.uniregctb.metier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static ch.vd.unireg.interfaces.infra.mock.MockCommune.Echallens;
import static ch.vd.uniregctb.type.MotifFor.DEBUT_EXPLOITATION;
import static ch.vd.uniregctb.type.MotifFor.FIN_EXPLOITATION;
import static ch.vd.uniregctb.type.MotifRattachement.ETABLISSEMENT_STABLE;
import static ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-03-04, <raphael.marmier@vd.ch>
 */
public class AjustementForsSecondairesHelperTest extends WithoutSpringTest {

	private Map<Integer, List<Domicile>> tousLesDomicilesVD;
	private Map<Integer, List<ForFiscalSecondaire>> tousLesForsFiscauxSecondairesParCommune;

	@Before
	public void setup() {
		tousLesDomicilesVD = new HashMap<>();
		tousLesForsFiscauxSecondairesParCommune = new HashMap<>();
	}

	@Test
	public void testNouvelEtablissement() throws MetierServiceException {

		addDomicile(date(2015, 1, 1), date(2015, 1, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		assertTrue(resultatAjustementForsSecondaires.getAAnnuler().isEmpty());
		assertTrue(resultatAjustementForsSecondaires.getAFermer().isEmpty());
		assertTrue(resultatAjustementForsSecondaires.getACreer().size() == 1);
		final ForFiscalSecondaire aCreer = resultatAjustementForsSecondaires.getACreer().get(0);
		assertEquals(date(2015, 1, 1), aCreer.getDateDebut());
		assertEquals(date(2015, 1, 31), aCreer.getDateFin());
	}

	@Test
	public void testEtablissementAnnule() throws MetierServiceException {


		ForFiscalSecondaire seraAnnule1 = addFor(date(2015, 1, 1), DEBUT_EXPLOITATION, date(2015, 1, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);
		ForFiscalSecondaire seraAnnule2 = addFor(date(2015, 3, 1), DEBUT_EXPLOITATION, date(2015, 3, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		final List<ForFiscalSecondaire> aAnnuler = resultatAjustementForsSecondaires.getAAnnuler();
		assertNotNull(aAnnuler);
		assertEquals(2, aAnnuler.size());
		assertTrue(seraAnnule1 == aAnnuler.get(0));
		assertTrue(seraAnnule2 == aAnnuler.get(1));

		assertTrue(resultatAjustementForsSecondaires.getAFermer().isEmpty());
		assertTrue(resultatAjustementForsSecondaires.getACreer().isEmpty());
	}

	@Test
	public void testEtablisssmentFerme() throws MetierServiceException {

		addDomicile(date(2015, 1, 1), date(2015, 1, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());
		addDomicile(date(2015, 3, 1), date(2015, 3, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());

		addFor(date(2015, 1, 1), DEBUT_EXPLOITATION, date(2015, 1, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);
		ForFiscalSecondaire seraFerme = addFor(date(2015, 3, 1), DEBUT_EXPLOITATION, null, FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		assertTrue(resultatAjustementForsSecondaires.getAAnnuler().isEmpty());
		final List<AjustementForsSecondairesResult.ForAFermer> aFermer = resultatAjustementForsSecondaires.getAFermer();
		assertNotNull(aFermer);
		assertEquals(1, aFermer.size());
		final AjustementForsSecondairesResult.ForAFermer forAFermer = aFermer.get(0);
		assertEquals(date(2015, 3, 31), forAFermer.getDateFermeture());
		assertTrue(seraFerme == forAFermer.getForFiscal());
		assertTrue(resultatAjustementForsSecondaires.getACreer().isEmpty());
	}

	@Test
	public void testDeuxPresencesRienNeChange() throws MetierServiceException {

		addDomicile(date(2015, 1, 1), date(2015, 1, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());
		addDomicile(date(2015, 3, 1), date(2015, 3, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());

		addFor(date(2015, 1, 1), DEBUT_EXPLOITATION, date(2015, 1, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);
		addFor(date(2015, 3, 1), DEBUT_EXPLOITATION, date(2015, 3, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		assertTrue(resultatAjustementForsSecondaires.getAAnnuler().isEmpty());
		assertTrue(resultatAjustementForsSecondaires.getAFermer().isEmpty());
		assertTrue(resultatAjustementForsSecondaires.getACreer().isEmpty());
	}

	@Test
	public void testDeuxPresencesPremierNouveauEmpiete() throws MetierServiceException {

		addDomicile(date(2015, 1, 1), date(2015, 1, 19), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());
		addDomicile(date(2015, 1, 25), date(2015, 3, 10), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());
		addDomicile(date(2015, 3, 1), date(2015, 3, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());

		ForFiscalSecondaire seraAnnule1 = addFor(date(2015, 1, 1), DEBUT_EXPLOITATION, date(2015, 1, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);
		ForFiscalSecondaire seraAnnule2 = addFor(date(2015, 3, 1), DEBUT_EXPLOITATION, date(2015, 3, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		final List<ForFiscalSecondaire> aAnnuler = resultatAjustementForsSecondaires.getAAnnuler();
		assertNotNull(aAnnuler);
		assertEquals(2, aAnnuler.size());
		assertTrue(seraAnnule1 == aAnnuler.get(0));
		assertTrue(seraAnnule2 == aAnnuler.get(1));
		assertEquals(0, resultatAjustementForsSecondaires.getAFermer().size());
		final ForFiscalSecondaire aCreer1 = resultatAjustementForsSecondaires.getACreer().get(0);
		assertEquals(date(2015, 1, 1), aCreer1.getDateDebut());
		assertEquals(date(2015, 1, 19), aCreer1.getDateFin());
		final ForFiscalSecondaire aCreer2 = resultatAjustementForsSecondaires.getACreer().get(1);
		assertEquals(date(2015, 1, 25), aCreer2.getDateDebut());
		assertEquals(date(2015, 3, 31), aCreer2.getDateFin());
	}

	@Test
	public void testDeuxPresencesNouveauEmpieteDeuxiemeFerme() throws MetierServiceException {

		addDomicile(date(2015, 1, 1), date(2015, 1, 31), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());
		addDomicile(date(2015, 3, 1), date(2015, 4, 10), COMMUNE_OU_FRACTION_VD, Echallens.getNoOFS());

		addFor(date(2015, 1, 1), DEBUT_EXPLOITATION, date(2015, 1, 31), FIN_EXPLOITATION, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);
		ForFiscalSecondaire seraFerme = addFor(date(2015, 3, 1), DEBUT_EXPLOITATION, null, null, Echallens.getNoOFS(), COMMUNE_OU_FRACTION_VD, ETABLISSEMENT_STABLE);

		final AjustementForsSecondairesResult resultatAjustementForsSecondaires =
				AjustementForsSecondairesHelper.getResultatAjustementForsSecondaires(tousLesDomicilesVD, tousLesForsFiscauxSecondairesParCommune, null);

		assertEquals(0, resultatAjustementForsSecondaires.getAAnnuler().size());
		final List<AjustementForsSecondairesResult.ForAFermer> aFermer = resultatAjustementForsSecondaires.getAFermer();
		assertNotNull(aFermer);
		assertEquals(1, aFermer.size());
		final AjustementForsSecondairesResult.ForAFermer forAFermer = aFermer.get(0);
		assertEquals(date(2015, 4, 10), forAFermer.getDateFermeture());
		assertTrue(seraFerme == forAFermer.getForFiscal());
		assertEquals(0, resultatAjustementForsSecondaires.getACreer().size());
	}

	protected Domicile addDomicile(RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutoriteFiscale, Integer noOfs) {
		List<Domicile> domicileHistos = tousLesDomicilesVD.get(noOfs);
		if (domicileHistos == null) {
			domicileHistos = new ArrayList<>();
			tousLesDomicilesVD.put(noOfs, domicileHistos);
		}
		final Domicile domicile = new Domicile(dateDebut, dateFin, typeAutoriteFiscale, noOfs);
		domicileHistos.add(domicile);
		return domicile;
	}

	protected ForFiscalSecondaire addFor(RegDate dateDebut, MotifFor motifOuverture, RegDate dateFin, MotifFor motifFermeture, Integer noOfs, TypeAutoriteFiscale typeAutoriteFiscale, MotifRattachement motifRattachement) {
		List<ForFiscalSecondaire> forFiscalSecondaires = tousLesForsFiscauxSecondairesParCommune.get(noOfs);
		if (forFiscalSecondaires == null) {
			forFiscalSecondaires = new ArrayList<>();
			tousLesForsFiscauxSecondairesParCommune.put(noOfs, forFiscalSecondaires);
		}
		final ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire(dateDebut, motifOuverture, dateFin, motifFermeture, noOfs, typeAutoriteFiscale, motifRattachement);
		forFiscalSecondaires.add(forFiscalSecondaire);
		return forFiscalSecondaire;
	}
}