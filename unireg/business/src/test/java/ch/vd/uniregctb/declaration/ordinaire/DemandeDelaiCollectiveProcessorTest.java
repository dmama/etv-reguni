package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationHelper;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscaleDAO;
import ch.vd.uniregctb.declaration.ordinaire.DemandeDelaiCollectiveResults.ErreurType;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DemandeDelaiCollectiveProcessorTest extends BusinessTest {

	private DemandeDelaiCollectiveProcessor processor;
	private final RegDate dateTraitement = RegDate.get();
	private AdresseService adresseService;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();
		adresseService = getBean(AdresseService.class, "adresseService");
		final PeriodeFiscaleDAO periodeDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");

		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new DemandeDelaiCollectiveProcessor(periodeDAO, hibernateTemplate, transactionManager, tiersService, adresseService);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAccorderDelaiTiersSansDeclaration() throws Exception {

		final RegDate dateDelai = RegDate.get(2010, 9, 1);

		final int annee = 2009;
		final PeriodeFiscale periode = addPeriodeFiscale(annee);
		final ModeleDocument modeleDocument = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);

		final List<Long> ids = Collections.emptyList();
		final Contribuable mrKong = addNonHabitant("King", "Kong", date(1965, 4, 13), Sexe.MASCULIN);

		{
			// TEST : un tiers sans déclaration pour 2009.
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.CONTRIBUABLE_SANS_DI, rapport.errors.get(0).raison);
		}

		final Declaration d = addDeclarationImpot(mrKong, periode, RegDate.get(2009, 1, 1), RegDate.get(2009, 12, 31),
				TypeContribuable.HORS_CANTON, modeleDocument);
		d.setDelais(new HashSet<DelaiDeclaration>());
		assertNull(d.getDelaiAccordeAu());
		final EtatDeclaration etatEmis = newEtatDeclaration(TypeEtatDeclaration.EMISE);
		etatEmis.setDateObtention(date(2010,1,7));
		d.addEtat(etatEmis);

		{
			// TEST : On lui ajoute 1 declaration pour 2009 à l'état émise :
			// - La déclaration n'a pas de délai
			// - La déclaration n'est pas retournée
			// - La déclaration n'est pas annulée
			// - On souhaite accordé un délai au 01.09.2010
			//
			// Resultats attendus :
			// - Le délai est d'abord null
			// - une fois le délai accordé, le délai est au au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai), rapport);
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai antérieur (au 31.08.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010

			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 8, 31)), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelais().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter le même délai (au 01.09.2010)
			// Resultat attendu :
			// - le délai ne doit pas etre ajouté
			// - le délai est toujours au 01.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(dateDelai), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(1, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(1, d.getDelais().size());
			assertEquals(dateDelai, d.getDelaiAccordeAu());
		}

		{
			// TEST : On essaye de rajouter un délai posterieur (au 02.09.2010)
			// Resultat attendu :
			// - le délai doit etre ajouté
			// - le délai est maintenant au 02.09.2010
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 9, 2)), rapport);
			assertEquals(1, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(0, rapport.errors.size());
			assertEquals(2, d.getDelais().size());
			assertEquals(RegDate.get(2010, 9, 2), d.getDelaiAccordeAu());
		}


		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			final EtatDeclaration etatSomme = newEtatDeclaration(TypeEtatDeclaration.SOMMEE);
			etatSomme.setDateObtention(date(2010,7,18));
			d.addEtat(etatSomme);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_SOMMEE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			final EtatDeclaration etatEchu = newEtatDeclaration(TypeEtatDeclaration.ECHUE);
			etatEchu.setDateObtention(date(2010,8,17));
			d.addEtat(etatEchu);
			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_ECHUE, rapport.errors.get(0).raison);
		}

		{
			// TEST : La déclaration passe à l'état reçu :
			// Resultat attendu :
			// - aucun accord de délai ne doit passer
			final EtatDeclaration etatRetourne = newEtatDeclaration(TypeEtatDeclaration.RETOURNEE);
			etatRetourne.setDateObtention(date(2010,10,1));
			d.addEtat(etatRetourne);
		}

		{


			final DemandeDelaiCollectiveResults rapport = new DemandeDelaiCollectiveResults(2009, dateDelai, ids, dateTraitement, tiersService, adresseService);
			processor.accorderDelaiDeclaration(mrKong, 2009, newDelaiDeclaration(date(2010, 12, 4)), rapport);
			assertEquals(0, rapport.traites.size());
			assertEquals(0, rapport.ignores.size());
			assertEquals(1, rapport.errors.size());
			assertEquals(ErreurType.DI_RETOURNEE, rapport.errors.get(0).raison);
		}
	}

	private DelaiDeclaration newDelaiDeclaration(RegDate delaiAccordeAu) {
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDelaiAccordeAu(delaiAccordeAu);
		return delai;
	}

	private EtatDeclaration newEtatDeclaration(TypeEtatDeclaration typeEtat) {
		EtatDeclaration etat = EtatDeclarationHelper.getInstanceOfEtatDeclaration(typeEtat);
		return etat;
	}
}
