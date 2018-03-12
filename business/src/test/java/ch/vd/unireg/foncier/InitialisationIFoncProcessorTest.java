package ch.vd.unireg.foncier;

import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.DroitHabitationRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitVirtuelHeriteRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ModeleCommunauteRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InitialisationIFoncProcessorTest extends BusinessTest {

	private InitialisationIFoncProcessor processor;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final RegistreFoncierService registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		processor = new InitialisationIFoncProcessor(transactionManager, hibernateTemplate, registreFoncierService);
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
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(2, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.idProprietaire, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Rouge", info.identificationRF.nom);
				assertEquals("Francis", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				assertEquals("Achat", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				assertNotNull(info);
				assertNull(info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Menfais", info.identificationRF.nom);
				assertEquals("Gérard", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(UsufruitRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNull(info.part);
				assertNull(info.regime);
				assertEquals("236gzbfahécf", info.idRFAyantDroit);
				assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
		}
		// extraction au 01.01.2018 (tous)
		{
			final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null, null);
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(3, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.idProprietaire, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Rouge", info.identificationRF.nom);
				assertEquals("Francis", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				assertEquals("Achat", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				assertNotNull(info);
				assertEquals((Long) ids.idHabitant, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Zorro", info.identificationRF.nom);
				assertEquals("Albertine", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(1979, 6, 1), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(DroitHabitationRF.class, info.classDroit);
				assertEquals("Un motif, quoi...", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNull(info.part);
				assertNull(info.regime);
				assertEquals("5w47tgtflbsfg", info.idRFAyantDroit);
				assertEquals((Long) noRfHabitant, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(2);
				assertNotNull(info);
				assertNull(info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Menfais", info.identificationRF.nom);
				assertEquals("Gérard", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(UsufruitRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNull(info.part);
				assertNull(info.regime);
				assertEquals("236gzbfahécf", info.idRFAyantDroit);
				assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
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
			assertNotNull(results);
			assertEquals(0, results.getNbImmeublesInspectes());
			assertEquals(0, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());
		}

		// extraction sur la commune d'Echallens
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, MockCommune.Echallens.getNoOFS(), null);
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(2, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.idProprietaire, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Rouge", info.identificationRF.nom);
				assertEquals("Francis", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				assertEquals("Achat", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
				assertEquals((Long) 42000L, info.infoImmeuble.montantEstimationFiscale);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				assertNotNull(info);
				assertNull(info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Menfais", info.identificationRF.nom);
				assertEquals("Gérard", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(2000, 3, 1), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(UsufruitRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNull(info.part);
				assertNull(info.regime);
				assertEquals("236gzbfahécf", info.idRFAyantDroit);
				assertEquals((Long) noRfUsufruitier, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
				assertEquals((Long) 42000L, info.infoImmeuble.montantEstimationFiscale);
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
		assertNotNull(results);
		assertEquals(1, results.getNbImmeublesInspectes());
		assertEquals(3, results.getLignesExtraites().size());
		assertEquals(0, results.getImmeublesIgnores().size());
		assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			assertNotNull(info);
			assertEquals((Long) ids.id1, info.idContribuable);
			assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			assertNotNull(info.identificationRF);
			assertEquals("Rouge", info.identificationRF.nom);
			assertEquals("Francis", info.identificationRF.prenom);
			assertNull(info.identificationRF.raisonSociale);
			assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
			assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			assertEquals("Achat", info.motifDebut);
			assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertNull(info.motifFin);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(1, info.part.getNumerateur());
			assertEquals(5, info.part.getDenominateur());
			assertEquals(GenrePropriete.COMMUNE, info.regime);
			assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			assertEquals((Long) noRfCommuniste1, info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
			assertNotNull(info);
			assertEquals((Long) ids.id2, info.idContribuable);
			assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			assertNotNull(info.identificationRF);
			assertEquals("Zorro", info.identificationRF.nom);
			assertEquals("Albertine", info.identificationRF.prenom);
			assertNull(info.identificationRF.raisonSociale);
			assertEquals(date(1979, 6, 1), info.identificationRF.dateNaissance);
			assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			assertEquals("Un motif, quoi...", info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(3, info.part.getNumerateur());
			assertEquals(8, info.part.getDenominateur());
			assertEquals(GenrePropriete.COMMUNE, info.regime);
			assertEquals("5w47tgtflbsfg", info.idRFAyantDroit);
			assertEquals((Long) noRfCommuniste2, info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(2);
			assertNotNull(info);
			assertNull(info.idContribuable);
			assertEquals((Long) ids.idCommunaute, info.idCommunaute);
			assertNull(info.identificationRF);
			assertEquals(CommunauteRF.class, info.classAyantDroit);
			assertEquals(DroitProprieteCommunauteRF.class, info.classDroit);
			assertEquals("Succession", info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeuble, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(12, info.part.getNumerateur());
			assertEquals(50, info.part.getDenominateur());
			assertEquals(GenrePropriete.INDIVIDUELLE, info.regime);
			assertEquals("285t378og43t", info.idRFAyantDroit);
			assertNull(info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
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
		assertNotNull(results);
		assertEquals(2, results.getNbImmeublesInspectes());
		assertEquals(2, results.getLignesExtraites().size());
		assertEquals(0, results.getImmeublesIgnores().size());
		assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			assertNotNull(info);
			assertEquals((Long) ids.idProprietaire, info.idContribuable);
			assertNull(info.idCommunaute);
			assertNotNull(info.identificationRF);
			assertEquals("Rouge", info.identificationRF.nom);
			assertEquals("Francis", info.identificationRF.prenom);
			assertNull(info.identificationRF.raisonSociale);
			assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
			assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			assertEquals("Achat", info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeubleAvecDroit, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(1, info.part.getNumerateur());
			assertEquals(5, info.part.getDenominateur());
			assertEquals(GenrePropriete.COMMUNE, info.regime);
			assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
			assertNotNull(info);
			assertNull(info.idContribuable);
			assertNull(info.idCommunaute);
			assertNull(info.identificationRF);
			assertNull(info.classAyantDroit);
			assertNull(info.classDroit);
			assertNull(info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeubleSansDroit, info.infoImmeuble.idImmeuble);
			assertEquals("Autre EGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 471, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 3, info.infoImmeuble.index1);
			assertNull(info.infoImmeuble.index2);
			assertNull(info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNull(info.part);
			assertNull(info.regime);
			assertNull(info.idRFAyantDroit);
			assertNull(info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
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
		assertNotNull(results);
		assertEquals(2, results.getNbImmeublesInspectes());
		assertEquals(1, results.getLignesExtraites().size());
		assertEquals(1, results.getImmeublesIgnores().size());
		assertEquals(0, results.getErreurs().size());

		{
			final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
			assertNotNull(info);
			assertEquals((Long) ids.idProprietaire, info.idContribuable);
			assertNull(info.idCommunaute);
			assertNotNull(info.identificationRF);
			assertEquals("Rouge", info.identificationRF.nom);
			assertEquals("Francis", info.identificationRF.prenom);
			assertNull(info.identificationRF.raisonSociale);
			assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
			assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
			assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
			assertEquals("Achat", info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeubleAvecDroit, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(1, info.part.getNumerateur());
			assertEquals(5, info.part.getDenominateur());
			assertEquals(GenrePropriete.COMMUNE, info.regime);
			assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
			assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
		}

		{
			final InitialisationIFoncResults.ImmeubleIgnore info = results.getImmeublesIgnores().get(0);
			assertNotNull(info);
			assertEquals((Long) ids.idImmeubleSansDroit, info.infoImmeuble.idImmeuble);
			assertEquals("Autre EGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 471, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 3, info.infoImmeuble.index1);
			assertNull(info.infoImmeuble.index2);
			assertNull(info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
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
		assertNotNull(results);
		assertEquals(2, results.getNbImmeublesInspectes());
		assertEquals(2, results.getLignesExtraites().size());
		assertEquals(0, results.getImmeublesIgnores().size());
		assertEquals(0, results.getErreurs().size());

		final List<InitialisationIFoncResults.InfoExtraction> lignesExtraites = results.getLignesExtraites();
		lignesExtraites.sort(Comparator.comparing(info -> info.infoImmeuble.egrid));
		{
			final InitialisationIFoncResults.InfoExtraction info = lignesExtraites.get(0);
			assertNotNull(info);
			assertNull(info.idContribuable);
			assertNull(info.idCommunaute);
			assertNull(info.identificationRF);
			assertNull(info.classAyantDroit);
			assertNull(info.classDroit);
			assertNull(info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeubleBeneficiaire, info.infoImmeuble.idImmeuble);
			assertEquals("CDGHJSFDG", info.infoImmeuble.egrid);
			assertEquals((Integer) 48415, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 6, info.infoImmeuble.index1);
			assertEquals((Integer) 1, info.infoImmeuble.index2);
			assertEquals((Integer) 7, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNull(info.part);
			assertNull(info.regime);
			assertNull(info.idRFAyantDroit);
			assertNull(info.noRFAyantDroit);
			assertNull(info.idImmeubleBeneficiaire);
		}
		{
			final InitialisationIFoncResults.InfoExtraction info = lignesExtraites.get(1);
			assertNotNull(info);
			assertNull(info.idContribuable);
			assertNull(info.idCommunaute);
			assertNull(info.identificationRF);
			assertEquals(ImmeubleBeneficiaireRF.class, info.classAyantDroit);
			assertEquals(DroitProprieteImmeubleRF.class, info.classDroit);
			assertEquals("Achat", info.motifDebut);
			assertNull(info.motifFin);
			assertEquals((Long) ids.idImmeublePossede, info.infoImmeuble.idImmeuble);
			assertEquals("CHEGRID", info.infoImmeuble.egrid);
			assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
			assertEquals((Integer) 4, info.infoImmeuble.index1);
			assertEquals((Integer) 2, info.infoImmeuble.index2);
			assertEquals((Integer) 1, info.infoImmeuble.index3);
			assertEquals("Echallens", info.infoImmeuble.nomCommune);
			assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
			assertNotNull(info.part);
			assertEquals(1, info.part.getNumerateur());
			assertEquals(5, info.part.getDenominateur());
			assertEquals(GenrePropriete.FONDS_DOMINANT, info.regime);
			assertEquals("4678536545hjksdf", info.idRFAyantDroit);
			assertNull(info.noRFAyantDroit);
			assertEquals((Long) ids.idImmeubleBeneficiaire, info.idImmeubleBeneficiaire);
		}
	}

	@Test
	public void testImmeubleAvecDroitsHerites() throws Exception {

		final long noRfProprietaire = 5753865L;
		final RegDate dateHeritage1 = RegDate.get(2016, 8, 13);
		final RegDate dateHeritage2 = RegDate.get(2017, 2, 17);

		final class Ids {
			long proprietaire;
			long heritier1;
			long heritier2;
			long heritier3;
			long immeuble;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CommuneRF commune = addCommuneRF(42, MockCommune.Echallens.getNomOfficiel(), MockCommune.Echallens.getNoOFS());
			final BienFondsRF immeuble = addBienFondsRF("r385hgjbahkl", "CHEGRID", commune, 4514, 4, 2, 1);

			final PersonnePhysique proprio = addNonHabitant("Francis", "Rouge", date(1975, 4, 2), Sexe.MASCULIN);
			final PersonnePhysiqueRF proprioRF = addPersonnePhysiqueRF("6784t6gfsbnc", "Francis", "Rouge", date(1975, 4, 2));
			proprioRF.setNoRF(noRfProprietaire);
			addRapprochementRF(proprio, proprioRF, null, null, TypeRapprochementRF.AUTO);
			addDroitPersonnePhysiqueRF(null, date(2015, 5, 2), null, null, "Achat", null, "3458wgfs", "3458wgfr", new IdentifiantAffaireRF(213, "5823g"), new Fraction(1, 5), GenrePropriete.COPROPRIETE, proprioRF, immeuble, null);

			final PersonnePhysique heritier1 = addNonHabitant("Albertine", "Zorro", date(1979, 6, 1), Sexe.FEMININ);
			addHeritage(heritier1, proprio, dateHeritage1, null, true);

			final PersonnePhysique heritier2 = addNonHabitant("Eva", "Zorro", date(1980, 6, 1), Sexe.FEMININ);
			addHeritage(heritier2, proprio, dateHeritage2, null, false);

			final PersonnePhysique heritier3 = addNonHabitant("Nourra", "Zorro", date(1981, 6, 1), Sexe.FEMININ);
			addHeritage(heritier3, proprio, dateHeritage2, null, false);

			final Ids res = new Ids();
			res.proprietaire = proprio.getNumero();
			res.heritier1 = heritier1.getNumero();
			res.heritier2 = heritier2.getNumero();
			res.heritier3 = heritier3.getNumero();
			res.immeuble = immeuble.getId();
			return res;
		});

		// extraction au 01.01.2016 (le droit non-hérité de Francis)
		{
			final InitialisationIFoncResults results = processor.run(date(2016, 1, 1), 1, null, null);
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(1, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.proprietaire, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNotNull(info.identificationRF);
				assertEquals("Rouge", info.identificationRF.nom);
				assertEquals("Francis", info.identificationRF.prenom);
				assertNull(info.identificationRF.raisonSociale);
				assertEquals(date(1975, 4, 2), info.identificationRF.dateNaissance);
				assertEquals(PersonnePhysiqueRF.class, info.classAyantDroit);
				assertEquals(DroitProprietePersonnePhysiqueRF.class, info.classDroit);
				assertEquals("Achat", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.immeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertEquals("6784t6gfsbnc", info.idRFAyantDroit);
				assertEquals((Long) noRfProprietaire, info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
		}
		// extraction au 01.01.2017 (le droit hérité d'Albertine)
		{
			final InitialisationIFoncResults results = processor.run(date(2017, 1, 1), 1, null, null);
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(1, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.heritier1, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNull(info.identificationRF);
				assertNull(info.classAyantDroit);
				assertEquals(DroitVirtuelHeriteRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.immeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertNull(info.idRFAyantDroit);
				assertNull(info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
		}
		// extraction au 01.01.2018 (les droits hérités d'Albertine, Eva et Nourra)
		{
			final InitialisationIFoncResults results = processor.run(date(2018, 1, 1), 1, null, null);
			assertNotNull(results);
			assertEquals(1, results.getNbImmeublesInspectes());
			assertEquals(3, results.getLignesExtraites().size());
			assertEquals(0, results.getImmeublesIgnores().size());
			assertEquals(0, results.getErreurs().size());

			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(0);
				assertNotNull(info);
				assertEquals((Long) ids.heritier1, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNull(info.identificationRF);
				assertNull(info.classAyantDroit);
				assertEquals(DroitVirtuelHeriteRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.immeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertNull(info.idRFAyantDroit);
				assertNull(info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(1);
				assertNotNull(info);
				assertEquals((Long) ids.heritier2, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNull(info.identificationRF);
				assertNull(info.classAyantDroit);
				assertEquals(DroitVirtuelHeriteRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.immeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertNull(info.idRFAyantDroit);
				assertNull(info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
			{
				final InitialisationIFoncResults.InfoExtraction info = results.getLignesExtraites().get(2);
				assertNotNull(info);
				assertEquals((Long) ids.heritier3, info.idContribuable);
				assertNull(info.idCommunaute);
				assertNull(info.identificationRF);
				assertNull(info.classAyantDroit);
				assertEquals(DroitVirtuelHeriteRF.class, info.classDroit);
				assertEquals("Succession", info.motifDebut);
				assertNull(info.motifFin);
				assertEquals((Long) ids.immeuble, info.infoImmeuble.idImmeuble);
				assertEquals("CHEGRID", info.infoImmeuble.egrid);
				assertEquals((Integer) 4514, info.infoImmeuble.noParcelle);
				assertEquals((Integer) 4, info.infoImmeuble.index1);
				assertEquals((Integer) 2, info.infoImmeuble.index2);
				assertEquals((Integer) 1, info.infoImmeuble.index3);
				assertEquals("Echallens", info.infoImmeuble.nomCommune);
				assertEquals((Integer) MockCommune.Echallens.getNoOFS(), info.infoImmeuble.noOfsCommune);
				assertNotNull(info.part);
				assertEquals(1, info.part.getNumerateur());
				assertEquals(5, info.part.getDenominateur());
				assertEquals(GenrePropriete.COPROPRIETE, info.regime);
				assertNull(info.idRFAyantDroit);
				assertNull(info.noRFAyantDroit);
				assertNull(info.idImmeubleBeneficiaire);
			}
		}
	}

}
