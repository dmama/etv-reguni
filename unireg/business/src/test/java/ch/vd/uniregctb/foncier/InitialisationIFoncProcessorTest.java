package ch.vd.uniregctb.foncier;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class InitialisationIFoncProcessorTest extends BusinessTest {

	private InitialisationIFoncProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final RapprochementRFDAO rapprochementRFDAO = getBean(RapprochementRFDAO.class, "rapprochementRFDAO");
		final RegistreFoncierService registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		processor = new InitialisationIFoncProcessor(transactionManager, hibernateTemplate, rapprochementRFDAO, registreFoncierService);
	}

	@Test
	public void testSimple() throws Exception {

		final class Ids {
			long idProprietaire;
			long idHabitant;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);
			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COPROPRIETE, proprioRF, immeuble, null);

			final PersonnePhysique habitant = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			final PersonnePhysiqueRF habitantRF = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
			addRapprochementRF(habitant, habitantRF, null, null, TypeRapprochementRF.AUTO);
			addDroitHabitationRF(null, date(2017, 3, 1), null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", null, null, habitantRF, immeuble);

			final PersonnePhysiqueRF usufruitier = addPersonnePhysiqueRF("236gzbfahécf", "Gérard", "Menfais", date(2000, 3, 1));
			addUsufruitRF(null, date(2015, 6, 1), null, null, "Succession", null, "58gfhfba", "58gfhfbb", null, null, usufruitier, immeuble);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idHabitant = habitant.getNumero();
			res.idImmeuble = immeuble.getId();
			return res;
		});

		// extraction au 01.01.2017 (tous sauf le droit d'habitation)
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.getNbImmeublesInspectes());
			Assert.assertEquals(2, results.getLignesExtraites().size());
			Assert.assertEquals(0, results.getImmeublesIgnores().size());
			Assert.assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals((Long) ids.idProprietaire, info.idContribuable);
				Assert.assertNull(info.idCommunaute);
				Assert.assertNotNull(info.identificationRF);
				Assert.assertEquals("Rouge", info.identificationRF.nom);
				Assert.assertEquals("Francis", info.identificationRF.prenom);
				Assert.assertNull(info.identificationRF.raisonSociale);
				Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				Assert.assertEquals("Achat", info.motifDebut);
				Assert.assertNull(info.motifFin);
				Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
				Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
				Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
				Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
				Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				Assert.assertNotNull(info.part);
				Assert.assertEquals(1, info.part.getNumerateur());
				Assert.assertEquals(5, info.part.getDenominateur());
				Assert.assertEquals(GenrePropriete.COPROPRIETE, info.regime);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				Assert.assertNotNull(info);
				Assert.assertNull(info.idContribuable);
				Assert.assertNull(info.idCommunaute);
				Assert.assertNotNull(info.identificationRF);
				Assert.assertEquals("Menfais", info.identificationRF.nom);
				Assert.assertEquals("Gérard", info.identificationRF.prenom);
				Assert.assertNull(info.identificationRF.raisonSociale);
				Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				Assert.assertEquals(UsufruitRF.class, info.classDroit);
				Assert.assertEquals("Succession", info.motifDebut);
				Assert.assertNull(info.motifFin);
				Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
				Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
				Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
				Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
				Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				Assert.assertNull(info.part);
				Assert.assertNull(info.regime);
			}
		}
		// extraction au 01.01.2018 (tous)
		{
			final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null);
			Assert.assertNotNull(results);
			Assert.assertEquals(1, results.getNbImmeublesInspectes());
			Assert.assertEquals(3, results.getLignesExtraites().size());
			Assert.assertEquals(0, results.getImmeublesIgnores().size());
			Assert.assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals((Long) ids.idProprietaire, info.idContribuable);
				Assert.assertNull(info.idCommunaute);
				Assert.assertNotNull(info.identificationRF);
				Assert.assertEquals("Rouge", info.identificationRF.nom);
				Assert.assertEquals("Francis", info.identificationRF.prenom);
				Assert.assertNull(info.identificationRF.raisonSociale);
				Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				Assert.assertEquals("Achat", info.motifDebut);
				Assert.assertNull(info.motifFin);
				Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
				Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
				Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
				Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
				Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				Assert.assertNotNull(info.part);
				Assert.assertEquals(1, info.part.getNumerateur());
				Assert.assertEquals(5, info.part.getDenominateur());
				Assert.assertEquals(GenrePropriete.COPROPRIETE, info.regime);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals((Long) ids.idHabitant, info.idContribuable);
				Assert.assertNull(info.idCommunaute);
				Assert.assertNotNull(info.identificationRF);
				Assert.assertEquals("Zorro", info.identificationRF.nom);
				Assert.assertEquals("Albertine", info.identificationRF.prenom);
				Assert.assertNull(info.identificationRF.raisonSociale);
				Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				Assert.assertEquals(DroitHabitationRF.class, info.classDroit);
				Assert.assertEquals("Un motif, quoi...", info.motifDebut);
				Assert.assertNull(info.motifFin);
				Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
				Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
				Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
				Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
				Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				Assert.assertNull(info.part);
				Assert.assertNull(info.regime);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(2);
				Assert.assertNotNull(info);
				Assert.assertNull(info.idContribuable);
				Assert.assertNull(info.idCommunaute);
				Assert.assertNotNull(info.identificationRF);
				Assert.assertEquals("Menfais", info.identificationRF.nom);
				Assert.assertEquals("Gérard", info.identificationRF.prenom);
				Assert.assertNull(info.identificationRF.raisonSociale);
				Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				Assert.assertEquals(UsufruitRF.class, info.classDroit);
				Assert.assertEquals("Succession", info.motifDebut);
				Assert.assertNull(info.motifFin);
				Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
				Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
				Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
				Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
				Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
				Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				Assert.assertNull(info.part);
				Assert.assertNull(info.regime);
			}
		}
	}

	@Test
	public void testAvecCommunaute() throws Exception {

		final class Ids {
			long id1;
			long id2;
			long idCommunaute;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final CommunauteRF communaute = addCommunauteRF("285t378og43t", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);

			final PersonnePhysique communiste1 = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF communisteRF1 = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			addRapprochementRF(communiste1, communisteRF1, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, communisteRF1, immeuble, communaute);

			final PersonnePhysique communiste2 = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			final PersonnePhysiqueRF communisteRF2 = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
			addRapprochementRF(communiste2, communisteRF2, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2017, 3, 1), null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(3, 8), GenrePropriete.COMMUNE, communisteRF2, immeuble, communaute);

			addDroitCommunauteRF(null, date(2016, 1, 1), null, null, "Succession", null, "478tgsbFB", "478tgsbFA", new IdentifiantAffaireRF(213, "5823g"), new Fraction(12, 50), GenrePropriete.INDIVIDUELLE, communaute, immeuble);

			final Ids res = new Ids();
			res.id1 = communiste1.getNumero();
			res.id2 = communiste2.getNumero();
			res.idCommunaute = communaute.getId();
			res.idImmeuble = immeuble.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbImmeublesInspectes());
		Assert.assertEquals(3, results.getLignesExtraites().size());
		Assert.assertEquals(0, results.getImmeublesIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals((Long) ids.id1, info.idContribuable);
			Assert.assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			Assert.assertNotNull(info.identificationRF);
			Assert.assertEquals("Rouge", info.identificationRF.nom);
			Assert.assertEquals("Francis", info.identificationRF.prenom);
			Assert.assertNull(info.identificationRF.raisonSociale);
			Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			Assert.assertEquals("Achat", info.motifDebut);
			Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNotNull(info.part);
			Assert.assertEquals(1, info.part.getNumerateur());
			Assert.assertEquals(5, info.part.getDenominateur());
			Assert.assertEquals(GenrePropriete.COMMUNE, info.regime);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals((Long) ids.id2, info.idContribuable);
			Assert.assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			Assert.assertNotNull(info.identificationRF);
			Assert.assertEquals("Zorro", info.identificationRF.nom);
			Assert.assertEquals("Albertine", info.identificationRF.prenom);
			Assert.assertNull(info.identificationRF.raisonSociale);
			Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			Assert.assertEquals("Un motif, quoi...", info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNotNull(info.part);
			Assert.assertEquals(3, info.part.getNumerateur());
			Assert.assertEquals(8, info.part.getDenominateur());
			Assert.assertEquals(GenrePropriete.COMMUNE, info.regime);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(2);
			Assert.assertNotNull(info);
			Assert.assertNull(info.idContribuable);
			Assert.assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			Assert.assertNull(info.identificationRF);
			Assert.assertEquals(CommunauteRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprieteCommunauteRF.class, info.classDroit);
			Assert.assertEquals("Succession", info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNotNull(info.part);
			Assert.assertEquals(12, info.part.getNumerateur());
			Assert.assertEquals(50, info.part.getDenominateur());
			Assert.assertEquals(GenrePropriete.INDIVIDUELLE, info.regime);
		}
	}

	@Test
	public void testImmeubleSansAucunDroit() throws Exception {

		final class Ids {
			long idProprietaire;
			long idImmeubleAvecDroit;
			long idImmeubleSansDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeuble, null);

			final BienFondRF immeubleSansDroit = addBienFondRF("5378gwfbs", "Autre EGRID", commune, 471, 3, null, null);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idImmeubleAvecDroit = immeuble.getId();
			res.idImmeubleSansDroit = immeubleSansDroit.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbImmeublesInspectes());
		Assert.assertEquals(2, results.getLignesExtraites().size());
		Assert.assertEquals(0, results.getImmeublesIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals((Long) ids.idProprietaire, info.idContribuable);
			Assert.assertNull(info.idCommunaute);
			Assert.assertNotNull(info.identificationRF);
			Assert.assertEquals("Rouge", info.identificationRF.nom);
			Assert.assertEquals("Francis", info.identificationRF.prenom);
			Assert.assertNull(info.identificationRF.raisonSociale);
			Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			Assert.assertEquals("Achat", info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeubleAvecDroit, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNotNull(info.part);
			Assert.assertEquals(1, info.part.getNumerateur());
			Assert.assertEquals(5, info.part.getDenominateur());
			Assert.assertEquals(GenrePropriete.COMMUNE, info.regime);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
			Assert.assertNotNull(info);
			Assert.assertNull(info.idContribuable);
			Assert.assertNull(info.idCommunaute);
			Assert.assertNull(info.identificationRF);
			Assert.assertNull(info.classAyantDroit);
			Assert.assertNull(info.classDroit);
			Assert.assertNull(info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeubleSansDroit, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("Autre EGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 471, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 3, info.infoImmeuble.index1);
			Assert.assertNull(info.infoImmeuble.index2);
			Assert.assertNull(info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNull(info.part);
			Assert.assertNull(info.regime);
		}
	}

	@Test
	public void testImmeubleSansDroitADateReference() throws Exception {

		final class Ids {
			long idProprietaire;
			long idImmeubleAvecDroit;
			long idImmeubleSansDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondRF immeuble = addBienFondRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2015, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeuble, null);

			final BienFondRF immeubleSansDroit = addBienFondRF("5378gwfbs", "Autre EGRID", commune, 471, 3, null, null);
			addDroitPersonnePhysiqueRF(null, date(2015, 1, 2), date(2017, 1,4), date(2016, 12, 21), "Achat", "Vente", "rqz7i3uf", "rqz7i3ue", new IdentifiantAffaireRF(213, "78rgfse"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeubleSansDroit, null);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idImmeubleAvecDroit = immeuble.getId();
			res.idImmeubleSansDroit = immeubleSansDroit.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbImmeublesInspectes());
		Assert.assertEquals(1, results.getLignesExtraites().size());
		Assert.assertEquals(1, results.getImmeublesIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals((Long) ids.idProprietaire, info.idContribuable);
			Assert.assertNull(info.idCommunaute);
			Assert.assertNotNull(info.identificationRF);
			Assert.assertEquals("Rouge", info.identificationRF.nom);
			Assert.assertEquals("Francis", info.identificationRF.prenom);
			Assert.assertNull(info.identificationRF.raisonSociale);
			Assert.assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			Assert.assertEquals("Achat", info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeubleAvecDroit, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CHEGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 4, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 2, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNotNull(info.part);
			Assert.assertEquals(1, info.part.getNumerateur());
			Assert.assertEquals(5, info.part.getDenominateur());
			Assert.assertEquals(GenrePropriete.COMMUNE, info.regime);
		}

		{
			final InitialisationIFoncResults.ImmeubleIgnore info = results.getImmeublesIgnores().get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals((Long) ids.idImmeubleSansDroit, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("Autre EGRID", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 471, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 3, info.infoImmeuble.index1);
			Assert.assertNull(info.infoImmeuble.index2);
			Assert.assertNull(info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
		}
	}
}
