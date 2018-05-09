package ch.vd.unireg.declaration.ordinaire.pp;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ListeNoteTest extends BusinessTest {

	private ListeNoteProcessor processor;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();


		// création du processeur à la main de manière à pouvoir appeler les méthodes protégées
		processor = new ListeNoteProcessor(null, null, null, null, null);


	}


	// 1 for secondaire avec une date de fin dans la période fiscale
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireSeulNonRecouvert() {
		// Un tiers avec un for ouvert à droite
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse(processor.isForSecondaireRecouvert(paul, for1));


	}

	//2 for secondaires finissant le meme jour
	// 1 for secondaire finissant en fin d'année
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireMultiplesNonRecouvert() {

		// Un tiers avec un for ouvert à droite
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2007, 3, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);

		ForFiscalSecondaire for3 = addForSecondaire(paul, date(2009, 7, 20), MotifFor.ACHAT_IMMOBILIER, date(2009, 12, 20),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse(processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));
		assertFalse(processor.isForSecondaireRecouvert(paul,for3));


	}

	//1 for secondaire fermé le meme jour que l'ouverture d'un nouveau for secondaire
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireRecouvertSurUnJour() {

		// Un tiers avec un for ouvert à droite
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2009, 3, 25), MotifFor.ACHAT_IMMOBILIER, date(2009, 7, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);


		assertTrue( processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));



	}

	//1 for secondaire recouvert par un for secondaire ouvert
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireRecouvert() {

		// Un tiers avec un for ouvert à droite
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2008, 12, 25), MotifFor.ACHAT_IMMOBILIER, null,
				null, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);


		assertTrue( processor.isForSecondaireRecouvert(paul, for1));
	



	}

	//1 for secondaire non recouvert avec un for secondaire ouvert le jours d'après
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireNonRecouvertJoursApres() {

		// Un tiers avec un for ouvert à droite
		PersonnePhysique paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay, MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2009, 3, 26), MotifFor.ACHAT_IMMOBILIER,date(2009, 10, 30),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne, MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse( processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));



	}
}
