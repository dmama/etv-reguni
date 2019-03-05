package ch.vd.unireg.declaration.ordinaire.common;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.mandataire.DemandeDelaisMandataireDAO;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AjouterDelaiPourMandataireProcessorTest extends BusinessTest {

	private AjouterDelaiPourMandataireProcessor processor;
	private AdresseService adresseService;
	private DemandeDelaisMandataireDAO demandeDelaisMandataireDAO;
	private DeclarationImpotService declarationImpotService;
	private final RegDate dateTraitement = RegDate.get();

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		demandeDelaisMandataireDAO = getBean(DemandeDelaisMandataireDAO.class, "demandeDelaisMandataireDAO");
		declarationImpotService = getBean(DeclarationImpotService.class, "diService");
		final PeriodeFiscaleDAO periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor =
				new AjouterDelaiPourMandataireProcessor(periodeFiscaleDAO, hibernateTemplate, transactionManager, tiersService, adresseService, demandeDelaisMandataireDAO, declarationImpotService);
		addPeriodeFiscale(2018);

	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAccorderDelaiSurCtbSansDI() throws Exception {

		final RegDate dateDelai = RegDate.get(2019, 9, 30);

		final int annee = 2018;
		final long idKong = doInNewTransaction(status -> {

			final PersonnePhysique k = addNonHabitant("King", "Kong", date(1965, 4, 13), Sexe.MASCULIN);
			return k.getNumero();
		});

		final PeriodeFiscale periode = doInNewTransaction(status -> {
			final PeriodeFiscale p = addPeriodeFiscale(annee);

			return p;
		});


		final ModeleDocument modeleDocument = doInNewTransaction(status -> {
			final ModeleDocument m = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

			return m;
		});

		{


			// TEST : un tiers sans déclaration pour 2018.
			PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);
			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));

			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.CONTRIBUABLE_SANS_DI, rapport.errors.get(0).raison);
		}
	}


	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAccorderDelaiSurCtbAvecDi() throws Exception {


		final RegDate dateDelai = RegDate.get().addMonths(6);

		final int annee = 2018;
		final long idKong = doInNewTransaction(status -> {

			final PersonnePhysique k = addNonHabitant("King", "Kong", date(1965, 4, 13), Sexe.MASCULIN);
			return k.getNumero();
		});

		final PeriodeFiscale periode = doInNewTransaction(status -> {
			final PeriodeFiscale p = addPeriodeFiscale(annee);

			return p;
		});


		final ModeleDocument modeleDocument = doInNewTransaction(status -> {
			final ModeleDocument m = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

			return m;
		});

		final Declaration d = doInNewTransaction(status -> {
			PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idKong);
			final Declaration declaration = addDeclarationImpot(pp, periode, RegDate.get(2018, 1, 1), RegDate.get(2018, 12, 31), TypeContribuable.HORS_CANTON, modeleDocument);
			declaration.setDelais(new HashSet<>());
			assertNull(declaration.getDelaiAccordeAu());
			addEtatDeclarationEmise(declaration, date(2019, 1, 7));

			return declaration;
		});



			// TEST : On lui ajoute 1 declaration pour 2018 à l'état émise :
			// - La déclaration n'a pas de délai
			// - La déclaration n'est pas retournée
			// - La déclaration n'est pas annulée
			// - On souhaite accordé un délai au 30.09.2019
			//
			// Resultats attendus :
			// - Le délai est d'abord null
			// - une fois le délai accordé, le délai est au au 30.09.2019

			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);

				InfosDelaisMandataire infoDemande =
						new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
								date(2019, 2, 19));

				final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
				processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai, dateTraitement, rapport);
				assertEquals(1, rapport.traites.size());
				assertEquals(0, rapport.ignores.size());
				assertEquals(0, rapport.errors.size());
				final DeclarationImpotOrdinairePP declaration = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
				assertEquals(dateDelai, declaration.getDelaiAccordeAu());

				return null;
			});
		// TEST : On essaye de rajouter un délai antérieur (au 31.08.2019)
		// Resultat attendu :
		// - le délai ne doit pas etre ajouté
		// - le délai est toujours au 30.09.2019
		{
			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);
						InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));
			final RegDate delaiAnterieur = dateDelai.addMonths(-1);

			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, delaiAnterieur, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DELAI_DATE_DELAI_INVALIDE, rapport.errors.get(0).raison);
				Assert.assertEquals("Un délai plus lointain existe déjà.", rapport.errors.get(0).details);

				final DeclarationImpotOrdinairePP declaration = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
			assertEquals(1, declaration.getDelaisDeclaration().size());
			assertEquals(dateDelai, declaration.getDelaiAccordeAu());
				return null;
			});
		}

		// TEST : On essaye de rajouter le même délai (au 30.09.2019)
		// Resultat attendu :
		// - le délai ne doit pas etre ajouté
		// - le délai est toujours au 30.09.2019
		{

			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);

			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));

			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DELAI_DEJA_EXISTANT, rapport.errors.get(0).raison);
				final DeclarationImpotOrdinairePP declaration = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
				assertEquals(1, declaration.getDelaisDeclaration().size());
				assertEquals(dateDelai, declaration.getDelaiAccordeAu());

				return null;
			});
		}



		// TEST : On essaye de rajouter un délai plus vieux que la date du jour
		// Resultat attendu :
		// - le délai n'est pas ajouté

		{

			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);
			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));
			final RegDate delaiDansPasse =RegDate.get().addDays(-10);

			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, delaiDansPasse, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DELAI_DATE_DELAI_INVALIDE, rapport.errors.get(0).raison);
				Assert.assertEquals("Un nouveau délai ne peut pas être demandé dans le passé de la date du jour.", rapport.errors.get(0).details);
				final DeclarationImpotOrdinairePP declaration = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
				assertEquals(1, declaration.getDelaisDeclaration().size());
				assertEquals(dateDelai, declaration.getDelaiAccordeAu());

				return null;
			});
		}



		// TEST : La déclaration passe à l'état reçu :
		// Resultat attendu :
		// - aucun accord de délai ne doit passer
		{
		doInNewTransaction(status -> {
			PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);
			final DeclarationImpotOrdinairePP declaration = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
			addEtatDeclarationRetournee(declaration, date(2019, 3, 1));

			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));

			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai.addMonths(1), dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DECL_MAUVAIS_ETAT, rapport.errors.get(0).raison);
			Assert.assertEquals("La déclaration n'est pas dans l'état 'EMIS'.", rapport.errors.get(0).details);
			final DeclarationImpotOrdinairePP dpp = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
			assertEquals(1, dpp.getDelaisDeclaration().size());
			assertEquals(dateDelai, dpp.getDelaiAccordeAu());

			return null;
		});
		}


		// TEST : La date d'otention renseignée est dans le futur:
		// Resultat attendu :
		// - aucun accord de délai ne doit passer
		{

			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);
			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							RegDate.get().addMonths(3));

			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DELAI_DATE_OBTENTION_INVALIDE, rapport.errors.get(0).raison);
				final DeclarationImpotOrdinairePP dpp = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
				assertEquals(1, dpp.getDelaisDeclaration().size());
				assertEquals(dateDelai, dpp.getDelaiAccordeAu());

				return null;
			});
		}


		// TEST : La declaration est annulée:
		// Resultat attendu :
		// - aucun accord de délai ne doit passer
		{

			doInNewTransaction(status -> {
				PersonnePhysique mrKong = (PersonnePhysique) tiersDAO.get(idKong);

			final DeclarationImpotOrdinairePP declaration2018 = mrKong.getDeclarationActiveAt(date(2018, 12, 31));
			declarationImpotService.annulationDI(mrKong, declaration2018, null, RegDate.get());


			final AjouterDelaiPourMandataireResults rapport = new AjouterDelaiPourMandataireResults(dateDelai, null, RegDate.get(), tiersService, adresseService);
			InfosDelaisMandataire infoDemande =
					new InfosDelaisMandataire(idKong, 2018, InfosDelaisMandataire.StatutDemandeType.ACCEPTE, "CHE232016940", "Sur Mesure, Gestion & Conseils", "M4QINF-BH7KPU",
							date(2019, 2, 19));

			processor.ajouterDelaiDeclarationPourMandataire(mrKong, infoDemande, dateDelai, dateTraitement, rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			Assert.assertEquals(AjouterDelaiPourMandataireResults.ErreurType.DECL_ANNULEE, rapport.errors.get(0).raison);
				return null;
			});
			}


	}

}
