package ch.vd.unireg.editique;


import noNamespace.InfoDocumentDocument1;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LegacyEditiqueHelperTest extends BusinessTest {

	private TiersService tiersService;
	private LegacyEditiqueHelper editiqueHelper;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		editiqueHelper = getBean(LegacyEditiqueHelper.class, "legacyEditiqueHelper");
	}

	@Test
	public void testRemplitAffranchissement() throws Exception {

		final long numeroIndJerome = 2;
		final long numeroIndJacques = 3;
		final long numeroIndGeorges = 4;
		final long numeroIndTheotime = 5;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indJerome = addIndividu(numeroIndJerome, date(1960, 1, 1), "Cognac", "Jerome", true);
				addAdresse(indJerome, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, null, date(1998, 1, 1), null);

				MockIndividu indJacques = addIndividu(numeroIndJacques, date(1960, 1, 1), "Cognac", "Jacques", true);
				addAdresse(indJacques, TypeAdresseCivil.COURRIER, "Chemin du palais","15b",null, CasePostale.parse("7507"),"Paris", MockPays.France, date(2012,1,1), null);


				MockIndividu indGeorges = addIndividu(numeroIndGeorges, date(1960, 1, 1), "Cognac", "Georges", true);
				addAdresse(indGeorges, TypeAdresseCivil.COURRIER, "Android Street","3H4",null, CasePostale.parse("9654"),"San Francisco", MockPays.EtatsUnis, date(2012,1,1), null);

				MockIndividu indTheotime = addIndividu(numeroIndTheotime, date(1960, 1, 1), "Cognac", "Theotime", true);
				addAdresse(indTheotime, TypeAdresseCivil.COURRIER, "Chemin du village","7b12",null, CasePostale.parse("32254"),"Villeneuve", MockPays.PaysInconnu, date(2012,1,1), null);


			}
		});

		class Ids {
			Long jerome;
			Long jacques;
			Long georges;
			Long theotime;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique jeromeCtb = addHabitant(numeroIndJerome);
				ids.jerome = jeromeCtb.getId();

				final PersonnePhysique jacquesCtb = addHabitant(numeroIndJacques);
				ids.jacques = jacquesCtb.getId();

				final PersonnePhysique georgesCtb = addHabitant(numeroIndGeorges);
				ids.georges = georgesCtb.getId();

				final PersonnePhysique theotimeCtb = addHabitant(numeroIndTheotime);
				ids.theotime = theotimeCtb.getId();
				return null;


			}
		});

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				Tiers jerome = tiersService.getTiers(ids.jerome);
				Tiers jacques = tiersService.getTiers(ids.jacques);
				Tiers georges = tiersService.getTiers(ids.georges);
				Tiers theotime = tiersService.getTiers(ids.theotime);

				InfoDocumentDocument1.InfoDocument infoDocumentJerome = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentJerome, jerome);
				assertEquals(ZoneAffranchissementEditique.SUISSE.getCode(), infoDocumentJerome.getAffranchissement().getZone());

				InfoDocumentDocument1.InfoDocument infoDocumentJacques = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentJacques, jacques);
				assertEquals(ZoneAffranchissementEditique.EUROPE.getCode(), infoDocumentJacques.getAffranchissement().getZone());

				InfoDocumentDocument1.InfoDocument infoDocumentGeorges = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentGeorges, georges);
				assertEquals(ZoneAffranchissementEditique.RESTE_MONDE.getCode(), infoDocumentGeorges.getAffranchissement().getZone());

				InfoDocumentDocument1.InfoDocument infoDocumentTheotime = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentTheotime, theotime);
				assertEquals(ZoneAffranchissementEditique.INCONNU.getCode(), infoDocumentTheotime.getAffranchissement().getZone());

				return null;
			}
		});
	}

	/**
	 * SIFISC-6860
	 */
	@Test
	public void testForGestionPourHorsCantonQuiAcheteEtVendSonImmeubleLaMemeAnnee() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
			}
		});

		final int pf = 2011;
		final RegDate dateAchatImmeuble = date(pf, 1, 24);
		final RegDate dateVenteImmeuble = date(pf, 10, 5);

		// création fiscale d'un non-habitant HC propriétaire d'un immeuble acheté et revendu la même année
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albertine", "Zweisteinen", date(1956, 3, 12), Sexe.FEMININ);
				addForPrincipal(pp, dateAchatImmeuble, MotifFor.ACHAT_IMMOBILIER, MockCommune.Bale);
				addForSecondaire(pp, dateAchatImmeuble, MotifFor.ACHAT_IMMOBILIER, dateVenteImmeuble, MotifFor.VENTE_IMMOBILIER, MockCommune.Cossonay.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode = addPeriodeFiscale(pf);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
				addDeclarationImpot(pp, periode, date(pf, 1, 1), date(pf, 12, 31), TypeContribuable.HORS_CANTON, md);
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(dateVenteImmeuble);
				assertNotNull(di);
				try {
					final String commune = editiqueHelper.getCommune(di);
					assertEquals(MockCommune.Cossonay.getNomCourt(), commune);
				}
				catch (EditiqueException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});
	}
}
