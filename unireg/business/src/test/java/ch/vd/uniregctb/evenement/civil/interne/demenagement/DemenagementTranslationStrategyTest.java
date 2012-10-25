package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.evenement.civil.interne.AbstractEvenementCivilInterneTest;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.Arrivee;
import ch.vd.uniregctb.evenement.civil.interne.arrivee.ArriveePrincipale;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * [UNIREG-3379] Traitement des événements civils en cas de fusion de communes, voir la spécification SCU-TraiterAutomatiquementEvenementsCivils.doc.
 */
@SuppressWarnings({"JavaDoc"})
public class DemenagementTranslationStrategyTest extends AbstractEvenementCivilInterneTest {

	private DemenagementTranslationStrategy strategy = new DemenagementTranslationStrategy();

	/**
	 * Vérifie qu'un événement de déménagement est bien traduit en arrivée lorsqu'un habitant déménagement entre deux communes fusionnées au civil mais pas au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementEntreCommunesFusionneesAuCivilMaisPasAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1);

		// Crée un individu qui déménage entre deux communes fusionnées pendant la période où elles sont fusionnées au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Hutter", "Marcel", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentCheminDeCreuxBechet, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentSentierDesVinches, null, dateDemenagement, null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		final ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(date(1990, 1, 1));
		f.setMotifOuverture(MotifFor.MAJORITE);
		f.setDateFin(null);
		f.setMotifFermeture(null);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.Villette.getNoOFSEtendu());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		// Simule l'arrivée du déménagement de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne d'arrivée,
		// parce que - du point de vue fiscal - les communes n'ont pas encore fusionné.
		final EvenementCivilInterne interne = strategy.create(externe, context, options);
		assertNotNull(interne);
		assertInstanceOf(Arrivee.class, interne);

		final ArriveePrincipale arrivee = (ArriveePrincipale) interne;
		assertEquals(MockCommune.Villette, arrivee.getAncienneCommune());
		assertEquals(MockCommune.Grandvaux, arrivee.getNouvelleCommune());

		assertEquals("Traité comme une arrivée car les communes Villette et Grandvaux ne sont pas encore fusionnées du point-de-vue fiscal.", externe.getCommentaireTraitement());
	}

	/**
	 * Vérifie qu'un événement de déménagement reste traduit en déménagement lorsqu'un habitant déménagement à l'intérieur d'une commune fusionnée au civil mais pas au fiscal.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementDansUneCommuneFusionneeAuCivilMaisPasAuFiscal() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1);

		// Crée un individu qui déménage à l'intérieur d'une commune fusionnée pendant la période où elle est fusionnée au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Hutter", "Marcel", true);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentCheminDeCreuxBechet, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockBatiment.Villette.BatimentRouteDeLausanne, null, dateDemenagement, null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		final ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(date(1990, 1, 1));
		f.setMotifOuverture(MotifFor.MAJORITE);
		f.setDateFin(null);
		f.setMotifFermeture(null);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(MockCommune.Villette.getNoOFSEtendu());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		// Simule l'arrivée du déménagement de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.BourgEnLavaux.getNoOFSEtendu(), null);

		// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne de déménagement, parce que
		// même si - du point de vue fiscal - les communes n'ont pas encore fusionné, la commune de départ et celle d'arrivée est la même.
		final EvenementCivilInterne interne = strategy.create(externe, context, options);
		assertNotNull(interne);
		assertInstanceOf(Demenagement.class, interne);

		final Demenagement demenagement = (Demenagement) interne;
		assertEquals(MockCommune.Villette, demenagement.getNouvelleCommunePrincipale());
	}


	/**
	 * Vérifie qu'un événement de déménagement secondaire est bien detecté par la strategy.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDemenagementSecondaire() throws Exception {

		final Long noInd = 1234L;
		final RegDate dateDemenagement = date(2010, 9, 1);

		// Crée un individu qui déménage à l'intérieur d'une commune fusionnée pendant la période où elle est fusionnée au civil, mais pas encore au fiscal
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1970, 1, 1), "Hutter", "Marcel", true);
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1990, 1, 1), dateDemenagement.getOneDayBefore());
				addAdresse(ind, TypeAdresseCivil.SECONDAIRE, MockBatiment.Cully.BatimentPlaceDuTemple, null, dateDemenagement, null);
			}
		});

		final PersonnePhysique pp = addHabitant(noInd);
		final ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(date(1990, 1, 1));
		f.setMotifOuverture(MotifFor.MAJORITE);
		f.setDateFin(null);
		f.setMotifFermeture(null);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		f.setNumeroOfsAutoriteFiscale(MockCommune.Zurich.getNoOFSEtendu());
		f.setMotifRattachement(MotifRattachement.DOMICILE);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		final ForFiscalSecondaire fs = new ForFiscalSecondaire();
		fs.setDateDebut(date(1990, 1, 1));
		fs.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		fs.setDateFin(null);
		fs.setMotifFermeture(null);
		fs.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		fs.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		fs.setNumeroOfsAutoriteFiscale(MockCommune.Cully.getNoOFSEtendu());
		fs.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);

		// Simule l'arrivée du déménagement de la part de la commune fusionnée
		final EvenementCivilRegPP externe = new EvenementCivilRegPP(0L, TypeEvenementCivil.DEMENAGEMENT_DANS_COMMUNE, EtatEvenementCivil.A_TRAITER, dateDemenagement, noInd, null,
				MockCommune.Cully.getNoOFSEtendu(), null);

		// L'événement fiscal externe de déménagement doit être traduit en un événement fiscal interne de déménagement, parce que
		// même si - du point de vue fiscal - les communes n'ont pas encore fusionné, la commune de départ et celle d'arrivée est la même.
		final EvenementCivilInterne interne = strategy.create(externe, context, options);
		assertNotNull(interne);
		assertInstanceOf(DemenagementSecondaire.class, interne);

	}
}
