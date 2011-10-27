package ch.vd.uniregctb.declaration.ordinaire;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

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
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse(processor.isForSecondaireRecouvert(paul, for1));


	}

	//2 for secondaires finissant le meme jour
	// 1 for secondaire finissant en fin d'année
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireMultiplesNonRecouvert() {

		// Un tiers avec un for ouvert à droite
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2007, 3, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

		ForFiscalSecondaire for3 = addForSecondaire(paul, date(2009, 7, 20), MotifFor.ACHAT_IMMOBILIER, date(2009, 12, 20),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse(processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));
		assertFalse(processor.isForSecondaireRecouvert(paul,for3));


	}

	//1 for secondaire fermé le meme jour que l'ouverture d'un nouveau for secondaire
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireRecouvertSurUnJour() {

		// Un tiers avec un for ouvert à droite
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2009, 3, 25), MotifFor.ACHAT_IMMOBILIER, date(2009, 7, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);


		assertTrue( processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));



	}

	//1 for secondaire recouvert par un for secondaire ouvert
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireRecouvert() {

		// Un tiers avec un for ouvert à droite
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2008, 12, 25), MotifFor.ACHAT_IMMOBILIER, null,
				null, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);


		assertTrue( processor.isForSecondaireRecouvert(paul, for1));
	



	}

	//1 for secondaire non recouvert avec un for secondaire ouvert le jours d'après
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsForSecondaireNonRecouvertJoursApres() {

		// Un tiers avec un for ouvert à droite
		Contribuable paul = addNonHabitant("Paul", "Duchêne", date(1965, 4, 13), Sexe.MASCULIN);
		addForPrincipal(paul, date(2006, 4, 13), MotifFor.DEPART_HC, MockCommune.Geneve);
		 ForFiscalSecondaire for1 = addForSecondaire(paul, date(2008, 7, 13), MotifFor.ACHAT_IMMOBILIER, date(2009, 3, 25),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
		ForFiscalSecondaire for2 = addForSecondaire(paul, date(2009, 3, 26), MotifFor.ACHAT_IMMOBILIER,date(2009, 10, 30),
				MotifFor.VENTE_IMMOBILIER, MockCommune.Aubonne.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);


		assertFalse( processor.isForSecondaireRecouvert(paul, for1));
		assertFalse(processor.isForSecondaireRecouvert(paul,for2));



	}
}
