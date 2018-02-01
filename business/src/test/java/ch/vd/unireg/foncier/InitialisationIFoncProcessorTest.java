package ch.vd.unireg.foncier;

import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitHabitationRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;

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

		final long noRfProprietaire = 5753865L;
		final long noRfHabitant = 432784237L;
		final long noRfUsufruitier = 32432L;

		final class Ids {
			long idProprietaire;
			long idHabitant;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			proprioRF.setNoRF(noRfProprietaire);
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);
			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COPROPRIETE, proprioRF, immeuble, null);

			final PersonnePhysique habitant = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			final PersonnePhysiqueRF habitantRF = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
			habitantRF.setNoRF(noRfHabitant);
			addRapprochementRF(habitant, habitantRF, null, null, TypeRapprochementRF.AUTO);
			addDroitHabitationRF(null, date(2017, 3, 1), null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", null, null, habitantRF, immeuble);

			final PersonnePhysiqueRF usufruitier = addPersonnePhysiqueRF("236gzbfahécf", "Gérard", "Menfais", date(2000, 3, 1));
			usufruitier.setNoRF(noRfUsufruitier);
			addUsufruitRF(null, date(2015, 6, 1), null, null, "Succession", null, "58gfhfba", "58gfhfbb", null, null, usufruitier, immeuble);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idHabitant = habitant.getNumero();
			res.idImmeuble = immeuble.getId();
			return res;
		});

		// extraction au 01.01.2017 (tous sauf le droit d'habitation)
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null, null);
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
				Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
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
				Assert.assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("236gzbfahécf", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
			}
		}
		// extraction au 01.01.2018 (tous)
		{
			final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null, null);
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
				Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
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
				Assert.assertEquals(date(1979, 6, 1), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("5w47tgtflbsfg", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfHabitant, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
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
				Assert.assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("236gzbfahécf", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
			}
		}
	}

	@Test
	public void testSimpleSurCommuneCiblee() throws Exception {

		final long noRfProprietaire = 5753865L;
		final long noRfHabitant = 432784237L;
		final long noRfUsufruitier = 32432L;

		final class Ids {
			long idProprietaire;
			long idHabitant;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
			addEstimationFiscale(date(2016, 5, 1), date(2017, 1, 1), null, false, 42000L, "2016", immeuble);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			proprioRF.setNoRF(noRfProprietaire);
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);
			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COPROPRIETE, proprioRF, immeuble, null);

			final PersonnePhysique habitant = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			final PersonnePhysiqueRF habitantRF = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
			habitantRF.setNoRF(noRfHabitant);
			addRapprochementRF(habitant, habitantRF, null, null, TypeRapprochementRF.AUTO);
			addDroitHabitationRF(null, date(2017, 3, 1), null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", null, null, habitantRF, immeuble);

			final PersonnePhysiqueRF usufruitier = addPersonnePhysiqueRF("236gzbfahécf", "Gérard", "Menfais", date(2000, 3, 1));
			usufruitier.setNoRF(noRfUsufruitier);
			addUsufruitRF(null, date(2015, 6, 1), null, null, "Succession", null, "58gfhfba", "58gfhfbb", null, null, usufruitier, immeuble);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idHabitant = habitant.getNumero();
			res.idImmeuble = immeuble.getId();
			return res;
		});

		// extraction sur la commune de Morges (qui n'est pas Echallens -> on ne devrait rien avoir)
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, MockCommune.Morges.getNoOFS(), null);
			Assert.assertNotNull(results);
			Assert.assertEquals(0, results.getNbImmeublesInspectes());
			Assert.assertEquals(0, results.getLignesExtraites().size());
			Assert.assertEquals(0, results.getImmeublesIgnores().size());
			Assert.assertEquals(0, results.getErreurs().size());
		}

		// extraction sur la commune d'Echallens
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, MockCommune.Echallens.getNoOFS(), null);
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
				Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
				Assert.assertEquals((Long) 42000L, info.infoImmeuble.montantEstimationFiscale);
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
				Assert.assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
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
				Assert.assertEquals("236gzbfahécf", info.idRFAyantDroit);
				Assert.assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				Assert.assertNull(info.idImmeubleBeneficiaire);
				Assert.assertEquals((Long) 42000L, info.infoImmeuble.montantEstimationFiscale);
			}
		}
	}

	@Test
	public void testAvecCommunaute() throws Exception {

		final long noRfCommuniste1 = 43724L;
		final long noRfCommuniste2 = 437823L;

		final class Ids {
			long id1;
			long id2;
			long idCommunaute;
			long idImmeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final CommunauteRF communaute = addCommunauteRF("285t378og43t", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);

			final PersonnePhysique communiste1 = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF communisteRF1 = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			communisteRF1.setNoRF(noRfCommuniste1);
			addRapprochementRF(communiste1, communisteRF1, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, communisteRF1, immeuble, communaute);

			final PersonnePhysique communiste2 = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			final PersonnePhysiqueRF communisteRF2 = addPersonnePhysiqueRF("5w47tgtflbsfg", "Albertine", "Zorro", date(1979, 6, 1));
			communisteRF2.setNoRF(noRfCommuniste2);
			addRapprochementRF(communiste2, communisteRF2, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2017, 3, 1), null, null, "Un motif, quoi...", null, "5378tgzufbs", "5378tgzufbr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(3, 8), GenrePropriete.COMMUNE, communisteRF2, immeuble, communaute);

			addDroitCommunauteRF(null, date(2016, 1, 1), null, null, "Succession", null, "478tgsbFB", "478tgsbFA", new IdentifiantAffaireRF(213, "5823g"), new Fraction(12, 50), GenrePropriete.INDIVIDUELLE, communaute, immeuble);

			final ModeleCommunauteRF modele = addModeleCommunauteRF(communisteRF1, communisteRF2);
			addRegroupementRF(communaute, modele, date(2016, 5, 2), null);

			final Ids res = new Ids();
			res.id1 = communiste1.getNumero();
			res.id2 = communiste2.getNumero();
			res.idCommunaute = communaute.getId();
			res.idImmeuble = immeuble.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null, null);
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
			Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
			Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			Assert.assertEquals((Long) noRfCommuniste1, info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
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
			Assert.assertEquals(date(1979, 6, 1), info.identificationRF.dateNaissance);
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
			Assert.assertEquals("5w47tgtflbsfg", info.idRFAyantDroit);
			Assert.assertEquals((Long) noRfCommuniste2, info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
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
			Assert.assertEquals("285t378og43t", info.idRFAyantDroit);
			Assert.assertNull(info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
		}
	}

	@Test
	public void testImmeubleSansAucunDroit() throws Exception {

		final long noRfProprietaire = 4625237L;

		final class Ids {
			long idProprietaire;
			long idImmeubleAvecDroit;
			long idImmeubleSansDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			proprioRF.setNoRF(noRfProprietaire);
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2016, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeuble, null);

			final BienFondsRF immeubleSansDroit = addBienFondsRF("5378gwfbs", "Autre EGRID", commune, 471, 3, null, null);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idImmeubleAvecDroit = immeuble.getId();
			res.idImmeubleSansDroit = immeubleSansDroit.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null, null);
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
			Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
			Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			Assert.assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
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
			Assert.assertNull(info.idRFAyantDroit);
			Assert.assertNull(info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
		}
	}

	@Test
	public void testImmeubleSansDroitADateReference() throws Exception {

		final long noRfProprietaire = 23478234L;

		final class Ids {
			long idProprietaire;
			long idImmeubleAvecDroit;
			long idImmeubleSansDroit;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			proprioRF.setNoRF(noRfProprietaire);
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);

			addDroitPersonnePhysiqueRF(null, date(2015, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeuble, null);

			final BienFondsRF immeubleSansDroit = addBienFondsRF("5378gwfbs", "Autre EGRID", commune, 471, 3, null, null);
			addDroitPersonnePhysiqueRF(null, date(2015, 1, 2), date(2017, 1,4), date(2016, 12, 21), "Achat", "Vente", "rqz7i3uf", "rqz7i3ue", new IdentifiantAffaireRF(213, "78rgfse"), new Fraction(1, 5), GenrePropriete.COMMUNE, proprioRF, immeubleSansDroit, null);

			final Ids res = new Ids();
			res.idProprietaire = proprio.getNumero();
			res.idImmeubleAvecDroit = immeuble.getId();
			res.idImmeubleSansDroit = immeubleSansDroit.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null, null);
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
			Assert.assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
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
			Assert.assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			Assert.assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
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

	@Test
	public void testImmeubleAvecDroitSurImmeuble() throws Exception {

		final class Ids {
			long idImmeublePossede;
			long idImmeubleBeneficiaire;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {

			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeublePossede = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);
			final BienFondsRF immeubleBeneficiaire = addBienFondsRF("4678536545hjksdf", "CDGHJSFDG", commune, 48415, 6, 1, 7);

			final ImmeubleBeneficiaireRF ayantDroit = addImmeubleBeneficiaireRF(immeubleBeneficiaire);
			addDroitImmeubleRF(null, date(2014, 6, 12), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.FONDS_DOMINANT, ayantDroit, immeublePossede);

			final Ids res = new Ids();
			res.idImmeublePossede = immeublePossede.getId();
			res.idImmeubleBeneficiaire = immeubleBeneficiaire.getId();
			return res;
		});

		final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(2, results.getNbImmeublesInspectes());
		Assert.assertEquals(2, results.getLignesExtraites().size());
		Assert.assertEquals(0, results.getImmeublesIgnores().size());
		Assert.assertEquals(0, results.getErreurs().size());

		final List<InitialisationIFoncResults.InfoExtraction> lignesExtraites = results.getLignesExtraites();
		lignesExtraites.sort(Comparator.comparing(info -> info.infoImmeuble.egrid));
		{
			final InitialisationIFoncResults.InfoExtraction info = lignesExtraites.get(0);
			Assert.assertNotNull(info);
			Assert.assertNull(info.idContribuable);
			Assert.assertNull(info.idCommunaute);
			Assert.assertNull(info.identificationRF);
			Assert.assertNull(info.classAyantDroit);
			Assert.assertNull(info.classDroit);
			Assert.assertNull(info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeubleBeneficiaire, info.infoImmeuble.idImmeuble);
			Assert.assertEquals("CDGHJSFDG", info.infoImmeuble.egrid);
			Assert.assertEquals((Integer) 48415, info.infoImmeuble.noParcelle);
			Assert.assertEquals((Integer) 6, info.infoImmeuble.index1);
			Assert.assertEquals((Integer) 1, info.infoImmeuble.index2);
			Assert.assertEquals((Integer) 7, info.infoImmeuble.index3);
			Assert.assertEquals("Echallens", info.infoImmeuble.nomCommune);
			Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			Assert.assertNull(info.part);
			Assert.assertNull(info.regime);
			Assert.assertNull(info.idRFAyantDroit);
			Assert.assertNull(info.noRFAyantDroit);
			Assert.assertNull(info.idImmeubleBeneficiaire);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = lignesExtraites.get(1);
			Assert.assertNotNull(info);
			Assert.assertNull(info.idContribuable);
			Assert.assertNull(info.idCommunaute);
			Assert.assertNull(info.identificationRF);
			Assert.assertEquals(ImmeubleBeneficiaireRF.class, info.classAyantDroit);
			Assert.assertEquals(DroitProprieteImmeubleRF.class, info.classDroit);
			Assert.assertEquals("Achat", info.motifDebut);
			Assert.assertNull(info.motifFin);
			Assert.assertEquals((Long) ids.idImmeublePossede, info.infoImmeuble.idImmeuble);
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
			Assert.assertEquals(GenrePropriete.FONDS_DOMINANT, info.regime);
			Assert.assertEquals("4678536545hjksdf", info.idRFAyantDroit);
			Assert.assertNull(info.noRFAyantDroit);
			Assert.assertEquals((Long) ids.idImmeubleBeneficiaire, info.idImmeubleBeneficiaire);
		}
	}
}
