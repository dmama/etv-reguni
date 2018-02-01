package ch.vd.uniregctb.registrefoncier.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalRapprochementTiersRF;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.MockRapprochementManuelTiersRFService;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RapprochementRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.dao.RapprochementRFDAO;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeGenerationEtatEntreprise;
import ch.vd.uniregctb.type.TypeRapprochementRF;

public class RapprochementTiersRFProcessorTest extends BusinessTest {

	private RapprochementTiersRFProcessor processor;
	private RapprochementRFDAO rapprochementDAO;
	private CollectingRapprochementManuelTiersRFService rapprochementManuelTiersRFService;
	private EvenementFiscalDAO evenementFiscalDAO;

	private static class CollectingRapprochementManuelTiersRFService extends MockRapprochementManuelTiersRFService {

		public final List<Long> tiersRFCollectes = new LinkedList<>();
		public final List<Pair<Long, Long>> marquagesCollectes = new LinkedList<>();

		@Override
		public void genererDemandeIdentificationManuelle(TiersRF tiersRF) {
			super.genererDemandeIdentificationManuelle(tiersRF);
			tiersRFCollectes.add(tiersRF.getId());
		}

		@Override
		public void marquerDemandesIdentificationManuelleEventuelles(TiersRF tiersRF, Tiers tiersUnireg) {
			super.marquerDemandesIdentificationManuelleEventuelles(tiersRF, tiersUnireg);
			marquagesCollectes.add(Pair.of(tiersRF.getId(), tiersUnireg.getId()));
		}
	}

	public RapprochementTiersRFProcessorTest() {
		setWantIndexationTiers(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		rapprochementDAO = getBean(RapprochementRFDAO.class, "rapprochementRFDAO");
		final IdentificationContribuableService identificationService = getBean(IdentificationContribuableService.class, "identCtbService");
		rapprochementManuelTiersRFService = new CollectingRapprochementManuelTiersRFService();
		evenementFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
		final EvenementFiscalService evenementFiscalService = getBean(EvenementFiscalService.class, "evenementFiscalService");

		processor = new RapprochementTiersRFProcessor(transactionManager, tiersService, adresseService, rapprochementDAO, hibernateTemplate,
		                                              identificationService, rapprochementManuelTiersRFService, evenementFiscalService);
	}

	/**
	 * Rapprochement qui fonctionne sur une personne physique
	 */
	@Test
	public void testRapprochementPersonnePhysique() throws Exception {

		final class Ids {
			long pp;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final PersonnePhysique pp = addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// on crée, pour être sûr, une personne morale avec le même nom et date d'inscription au RC
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// le tiers RF
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Alphonse", "Baudet", dateNaissance, "monidrf", 43723L, null);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(0, results.getNbNonIdentifications());
		Assert.assertEquals(1, results.getNbIdentifications());

		final RapprochementTiersRFResults.NouveauRapprochement rapprochement = results.getNouveauxRapprochements().get(0);
		Assert.assertNotNull(rapprochement);
		Assert.assertEquals(ids.pp, rapprochement.idContribuable);
		Assert.assertEquals(ids.idTiersRF, rapprochement.idTiersRF);
		Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.type);

		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.singletonList(Pair.of(ids.idTiersRF, ids.pp)), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp);
			Assert.assertNotNull(pp);

			final Set<RapprochementRF> rapprochementsRF = pp.getRapprochementsRF();
			Assert.assertNotNull(rapprochementsRF);
			Assert.assertEquals(1, rapprochementsRF.size());

			final RapprochementRF rapprochementRF = rapprochementsRF.iterator().next();
			Assert.assertNotNull(rapprochementRF);
			Assert.assertFalse(rapprochementRF.isAnnule());
			Assert.assertEquals((Long) ids.idTiersRF, rapprochementRF.getTiersRF().getId());
			Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochementRF.getTypeRapprochement());
			Assert.assertNull(rapprochementRF.getDateDebut());
			Assert.assertNull(rapprochementRF.getDateFin());

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.singletonList(rapprochementRF), tousRapprochements);
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.pp), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}

	/**
	 * Rapprochement qui fonctionne sur une personne morale
	 */
	@Test
	public void testRapprochementPersonneMorale() throws Exception {

		final class Ids {
			long pm;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// une personne physique avec les mêmes données
			addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// le tiers RF
			final PersonneMoraleRF tiersRF = addPersonneMoraleRF("Alphonse Baudet sàrl", "CH550744154", "monidrf", 43723L, null);

			final Ids ids1 = new Ids();
			ids1.pm = entreprise.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(0, results.getNbNonIdentifications());
		Assert.assertEquals(1, results.getNbIdentifications());

		final RapprochementTiersRFResults.NouveauRapprochement rapprochement = results.getNouveauxRapprochements().get(0);
		Assert.assertNotNull(rapprochement);
		Assert.assertEquals(ids.pm, rapprochement.idContribuable);
		Assert.assertEquals(ids.idTiersRF, rapprochement.idTiersRF);
		Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.type);

		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.singletonList(Pair.of(ids.idTiersRF, ids.pm)), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise pm = (Entreprise) tiersDAO.get(ids.pm);
			Assert.assertNotNull(pm);

			final Set<RapprochementRF> rapprochementsRF = pm.getRapprochementsRF();
			Assert.assertNotNull(rapprochementsRF);
			Assert.assertEquals(1, rapprochementsRF.size());

			final RapprochementRF rapprochementRF = rapprochementsRF.iterator().next();
			Assert.assertNotNull(rapprochementRF);
			Assert.assertFalse(rapprochementRF.isAnnule());
			Assert.assertEquals((Long) ids.idTiersRF, rapprochementRF.getTiersRF().getId());
			Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochementRF.getTypeRapprochement());
			Assert.assertNull(rapprochementRF.getDateDebut());
			Assert.assertNull(rapprochementRF.getDateFin());

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.singletonList(rapprochementRF), tousRapprochements);
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.pm), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}

	/**
	 * Rapprochement qui fonctionne sur une collectivité publique
	 * (en fait, pour le moment, cela ne fonctionne jamais)
	 */
	@Test
	public void testRapprochementCollectivitePublique() throws Exception {

		final class Ids {
			long pp;
			long pm;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// une personne physique avec les mêmes données
			final PersonnePhysique pp = addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// le tiers RF
			final CollectivitePubliqueRF tiersRF = addCollectivitePubliqueRF("Alphonse Baudet", "monidrf", 43723L, null);

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.pm = entreprise.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(1, results.getNbNonIdentifications());
		Assert.assertEquals(0, results.getNbIdentifications());

		final RapprochementTiersRFResults.NonIdentification nonIdentification = results.getNonIdentifications().get(0);
		Assert.assertNotNull(nonIdentification);
		Assert.assertEquals(ids.idTiersRF, nonIdentification.idTiersRF);
		Assert.assertEquals(Collections.emptyList(), nonIdentification.candidats);

		Assert.assertEquals(Collections.singletonList(ids.idTiersRF), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise pm = (Entreprise) tiersDAO.get(ids.pm);
			Assert.assertNotNull(pm);
			{
				final Set<RapprochementRF> rapprochementsRF = pm.getRapprochementsRF();
				Assert.assertNotNull(rapprochementsRF);
				Assert.assertEquals(0, rapprochementsRF.size());
			}

			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp);
			Assert.assertNotNull(pp);
			{
				final Set<RapprochementRF> rapprochementsRF = pp.getRapprochementsRF();
				Assert.assertNotNull(rapprochementsRF);
				Assert.assertEquals(0, rapprochementsRF.size());
			}

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.emptyList(), tousRapprochements);
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * Rapprochement qui fonctionne sur une personne physique quand on trouve plusieurs candidats chez nous
	 * dont l'un a le numéro de contribuable fourni par le RF
	 */
	@Test
	public void testRapprochementPersonnePhysiquePlusieursCandidatsDontLeBon() throws Exception {

		final class Ids {
			long pp;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final PersonnePhysique pp = addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// le presque jumeau !
			addNonHabitant("Alphonse André", "Baudet Madus", dateNaissance, Sexe.MASCULIN);

			// on crée, pour être sûr, une personne morale avec le même nom et date d'inscription au RC
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// le tiers RF
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Alphonse", "Baudet", dateNaissance, "monidrf", 43723L, pp.getNumero());       // le RF nous aide à la résolution du cas

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(0, results.getNbNonIdentifications());
		Assert.assertEquals(1, results.getNbIdentifications());

		final RapprochementTiersRFResults.NouveauRapprochement rapprochement = results.getNouveauxRapprochements().get(0);
		Assert.assertNotNull(rapprochement);
		Assert.assertEquals(ids.pp, rapprochement.idContribuable);
		Assert.assertEquals(ids.idTiersRF, rapprochement.idTiersRF);
		Assert.assertEquals(TypeRapprochementRF.AUTO_MULTIPLE, rapprochement.type);

		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.singletonList(Pair.of(ids.idTiersRF, ids.pp)), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp);
			Assert.assertNotNull(pp);

			final Set<RapprochementRF> rapprochementsRF = pp.getRapprochementsRF();
			Assert.assertNotNull(rapprochementsRF);
			Assert.assertEquals(1, rapprochementsRF.size());

			final RapprochementRF rapprochementRF = rapprochementsRF.iterator().next();
			Assert.assertNotNull(rapprochementRF);
			Assert.assertFalse(rapprochementRF.isAnnule());
			Assert.assertEquals((Long) ids.idTiersRF, rapprochementRF.getTiersRF().getId());
			Assert.assertEquals(TypeRapprochementRF.AUTO_MULTIPLE, rapprochementRF.getTypeRapprochement());
			Assert.assertNull(rapprochementRF.getDateDebut());
			Assert.assertNull(rapprochementRF.getDateFin());

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.singletonList(rapprochementRF), tousRapprochements);
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.pp), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}

	/**
	 * Rapprochement sur une personne physique alors que nous avons plusieurs candidats
	 * et que le RF ne nous fournit pas de numéro de contribuable pour nous aider
	 */
	@Test
	public void testRapprochementPersonnePhysiquePlusieursCandidatsSansAide() throws Exception {

		final class Ids {
			long pp1;
			long pp2;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final PersonnePhysique pp1 = addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// le presque jumeau !
			final PersonnePhysique pp2 = addNonHabitant("Alphonse André", "Baudet Madus", dateNaissance, Sexe.MASCULIN);

			// on crée, pour être sûr, une personne morale avec le même nom et date d'inscription au RC
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// le tiers RF
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Alphonse", "Baudet", dateNaissance, "monidrf", 43723L, null);       // le RF ne nous donne aucun numéro de contribuable

			final Ids ids1 = new Ids();
			ids1.pp1 = pp1.getNumero();
			ids1.pp2 = pp2.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(1, results.getNbNonIdentifications());
		Assert.assertEquals(0, results.getNbIdentifications());

		final RapprochementTiersRFResults.NonIdentification nonIdentification = results.getNonIdentifications().get(0);
		Assert.assertNotNull(nonIdentification);
		Assert.assertEquals(ids.idTiersRF, nonIdentification.idTiersRF);
		Assert.assertEquals(Arrays.asList(ids.pp1, ids.pp2), nonIdentification.candidats.stream().sorted().collect(Collectors.toList()));

		Assert.assertEquals(Collections.singletonList(ids.idTiersRF), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.emptyList(), tousRapprochements);
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * Rapprochement sur une personne physique alors que nous avons plusieurs candidats
	 * et que le RF nous fournit un numéro de contribuable qui ne fait pas partie de la liste des candidats
	 */
	@Test
	public void testRapprochementPersonnePhysiquePlusieursCandidatsAvecAideFausse() throws Exception {

		final class Ids {
			long pp1;
			long pp2;
			long idTiersRF;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final RegDate dateNaissance = date(1985, 3, 1);
			final PersonnePhysique pp1 = addNonHabitant("Alphonse", "Baudet", dateNaissance, Sexe.MASCULIN);

			// le presque jumeau !
			final PersonnePhysique pp2 = addNonHabitant("Alphonse André", "Baudet Madus", dateNaissance, Sexe.MASCULIN);

			// on crée, pour être sûr, une personne morale avec le même nom et date d'inscription au RC
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addFormeJuridique(entreprise, dateNaissance, null, FormeJuridiqueEntreprise.SARL);
			addRaisonSociale(entreprise, dateNaissance, null, "Alphonse Baudet sàrl");
			addEtatEntreprise(entreprise, dateNaissance, TypeEtatEntreprise.INSCRITE_RC, TypeGenerationEtatEntreprise.AUTOMATIQUE);

			// le tiers RF
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Alphonse", "Baudet", dateNaissance, "monidrf", 43723L, entreprise.getNumero());       // le RF nous donne un numéro qui n'est pas le bon

			final Ids ids1 = new Ids();
			ids1.pp1 = pp1.getNumero();
			ids1.pp2 = pp2.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation du tiers
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(0, results.getNbErreurs());
		Assert.assertEquals(1, results.getNbNonIdentifications());
		Assert.assertEquals(0, results.getNbIdentifications());

		final RapprochementTiersRFResults.NonIdentification nonIdentification = results.getNonIdentifications().get(0);
		Assert.assertNotNull(nonIdentification);
		Assert.assertEquals(ids.idTiersRF, nonIdentification.idTiersRF);
		Assert.assertEquals(Arrays.asList(ids.pp1, ids.pp2), nonIdentification.candidats.stream().sorted().collect(Collectors.toList()));

		Assert.assertEquals(Collections.singletonList(ids.idTiersRF), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.emptyList(), tousRapprochements);
			return null;
		});

		// postcondition : aucun événement fiscal n'a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(0, events.size());
			return null;
		});
	}

	/**
	 * Cas du tiers RF déjà rapproché à un contribuable unireg mais sur une période qui ne comprend pas
	 * la date du jour : il est donc candidat à un nouveau rapprochement qui, s'il est positif, ne doit
	 * bien-sûr pas être valide sur la période de l'ancien rapprochement...
	 */
	@Test
	public void testRapprochementComplementaire() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate finAnneeDerniere = date(today.year() - 1, 12, 31);

		final class Ids {
			long pp1;
			long pp2;
			long idTiersRF;
		}

		// mise en place préliminaire
		final Ids ids = doInNewTransactionAndSession(status -> {
			// l'ancien
			final PersonnePhysique pp1 = addNonHabitant("Alfred", "Jacquart", null, Sexe.MASCULIN);

			// le nouveau candidat
			final RegDate dateNaissance = date(1964, 7, 23);
			final PersonnePhysique pp2 = addNonHabitant("Alfredo", "Jacquouille", dateNaissance, Sexe.MASCULIN);

			// le tiers RF
			final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Alfredo", "Jacquouille", dateNaissance, "547835673zg", 3224L, pp1.getNumero());

			// il existe déjà un rapprochement entre pp1 l'ancien et le tiers RF (automatique puis réduit manuellement - dédoublonage)
			addRapprochementRF(null, null, TypeRapprochementRF.AUTO_MULTIPLE, pp1, tiersRF, true);
			addRapprochementRF(null, finAnneeDerniere, TypeRapprochementRF.MANUEL, pp1, tiersRF, false);

			final Ids ids1 = new Ids();
			ids1.pp1 = pp1.getNumero();
			ids1.pp2 = pp2.getNumero();
			ids1.idTiersRF = tiersRF.getId();
			return ids1;
		});

		// attente de la fin de l'indexation
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(Collections.emptyList(), results.getErreurs());
		Assert.assertEquals(0, results.getNbNonIdentifications());
		Assert.assertEquals(1, results.getNbIdentifications());

		final RapprochementTiersRFResults.NouveauRapprochement rapprochement = results.getNouveauxRapprochements().get(0);
		Assert.assertNotNull(rapprochement);
		Assert.assertEquals(ids.pp2, rapprochement.idContribuable);
		Assert.assertEquals(ids.idTiersRF, rapprochement.idTiersRF);
		Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.type);

		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.singletonList(Pair.of(ids.idTiersRF, ids.pp2)), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.pp2);
			Assert.assertNotNull(pp);

			final Set<RapprochementRF> rapprochementsRF = pp.getRapprochementsRF();
			Assert.assertNotNull(rapprochementsRF);
			Assert.assertEquals(1, rapprochementsRF.size());

			final RapprochementRF rapprochementRF = rapprochementsRF.iterator().next();
			Assert.assertNotNull(rapprochementRF);
			Assert.assertFalse(rapprochementRF.isAnnule());
			Assert.assertEquals((Long) ids.idTiersRF, rapprochementRF.getTiersRF().getId());
			Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochementRF.getTypeRapprochement());
			Assert.assertEquals(finAnneeDerniere.getOneDayAfter(), rapprochementRF.getDateDebut());
			Assert.assertNull(rapprochementRF.getDateFin());

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(3, tousRapprochements.size());      // les deux préparés dans l'initialisation, plus le nouveau
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertEquals(finAnneeDerniere.getOneDayAfter(), event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.pp2), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.idTiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}

	@Test
	public void testRapprochementEntrepriseParNumeroRC() throws Exception {

		final String numeroRC = "CH-550.1.051.910-3";
		final RegDate dateDebut = date(2009, 1, 1);
		final long noCantonalEntreprise = 423784356372L;
		final long noCantonalEtablissementPrincipal = 7964324789623L;

		final class Ids {
			long pm;
			long tiersRF;
		}

		// mise en place civile
		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = addOrganisation(noCantonalEntreprise);
				final MockSiteOrganisation sitePrincipal = addSite(org, noCantonalEtablissementPrincipal, dateDebut, null, null, null);
				sitePrincipal.changeNumeroRC(dateDebut, numeroRC);
				sitePrincipal.changeTypeDeSite(dateDebut, TypeDeSite.ETABLISSEMENT_PRINCIPAL);
				sitePrincipal.changeDomicile(dateDebut, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS());
				sitePrincipal.changeFormeLegale(dateDebut, FormeLegale.N_0106_SOCIETE_ANONYME);
				sitePrincipal.changeNom(dateDebut, "Machin bidule truc SA");
			}
		});

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseConnueAuCivil(noCantonalEntreprise);

			// on en crée une autre avec le même nom pour être certain que ce n'est pas le nom qui est déterminant
			final Entreprise leure = addEntrepriseInconnueAuCivil();
			addRaisonSociale(leure, dateDebut, null, "Machin bidule chose SA");
			addFormeJuridique(leure, dateDebut, null, FormeJuridiqueEntreprise.SA);

			final TiersRF tiersRF = addPersonneMoraleRF("Machin bidule SA", numeroRC, "548354837lJDFSGZ", 235643L, null);
			final Ids ids1 = new Ids();
			ids1.pm = entreprise.getNumero();
			ids1.tiersRF = tiersRF.getId();
			return ids1;
		});

		// indexation...
		globalTiersIndexer.sync();

		// lancement du rapprochement
		final RapprochementTiersRFResults results = processor.run(1, null);
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.getNbDossiersInspectes());
		Assert.assertEquals(Collections.emptyList(), results.getErreurs());
		Assert.assertEquals(0, results.getNbNonIdentifications());
		Assert.assertEquals(1, results.getNbIdentifications());

		final RapprochementTiersRFResults.NouveauRapprochement rapprochement = results.getNouveauxRapprochements().get(0);
		Assert.assertNotNull(rapprochement);
		Assert.assertEquals(ids.pm, rapprochement.idContribuable);
		Assert.assertEquals(ids.tiersRF, rapprochement.idTiersRF);
		Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochement.type);

		Assert.assertEquals(Collections.emptyList(), rapprochementManuelTiersRFService.tiersRFCollectes);
		Assert.assertEquals(Collections.singletonList(Pair.of(ids.tiersRF, ids.pm)), rapprochementManuelTiersRFService.marquagesCollectes);

		// vérification en base
		doInNewTransactionAndSession(status -> {
			final Entreprise pm = (Entreprise) tiersDAO.get(ids.pm);
			Assert.assertNotNull(pm);

			final Set<RapprochementRF> rapprochementsRF = pm.getRapprochementsRF();
			Assert.assertNotNull(rapprochementsRF);
			Assert.assertEquals(1, rapprochementsRF.size());

			final RapprochementRF rapprochementRF = rapprochementsRF.iterator().next();
			Assert.assertNotNull(rapprochementRF);
			Assert.assertFalse(rapprochementRF.isAnnule());
			Assert.assertEquals((Long) ids.tiersRF, rapprochementRF.getTiersRF().getId());
			Assert.assertEquals(TypeRapprochementRF.AUTO, rapprochementRF.getTypeRapprochement());
			Assert.assertNull(rapprochementRF.getDateDebut());
			Assert.assertNull(rapprochementRF.getDateFin());

			final List<RapprochementRF> tousRapprochements = rapprochementDAO.getAll();
			Assert.assertEquals(Collections.singletonList(rapprochementRF), tousRapprochements);
			return null;
		});

		// postcondition : l'événement fiscal correspondant a été envoyé
		doInNewTransaction(status -> {
			final List<EvenementFiscal> events = evenementFiscalDAO.getAll();
			Assert.assertEquals(1, events.size());

			final EvenementFiscalRapprochementTiersRF event0 = (EvenementFiscalRapprochementTiersRF) events.get(0);
			Assert.assertEquals(EvenementFiscalRapprochementTiersRF.TypeEvenementFiscalRapprochement.OUVERTURE, event0.getType());
			Assert.assertNull(event0.getDateValeur());
			Assert.assertEquals(Long.valueOf(ids.pm), event0.getRapprochement().getContribuable().getId());
			Assert.assertEquals(Long.valueOf(ids.tiersRF), event0.getRapprochement().getTiersRF().getId());
			return null;
		});
	}
}
