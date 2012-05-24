package ch.vd.uniregctb.editique;


import noNamespace.InfoDocumentDocument1;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;

public class EditiqueHelperTest extends BusinessTest {
	private static final Logger LOGGER = Logger.getLogger(EditiqueHelperTest.class);

	private TiersService tiersService;
	private EditiqueHelper editiqueHelper;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		editiqueHelper = getBean(EditiqueHelper.class, "editiqueHelper");
	}

	@Test
	public void testRemplitAffranchissement() throws Exception {

		final long numeroIndJerome = 2;
		final long numeroIndJacques = 3;
		final long numeroIndGeorges = 4;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu indJerome = addIndividu(numeroIndJerome, date(1960, 1, 1), "Cognac", "Jerome", true);
				addAdresse(indJerome, TypeAdresseCivil.PRINCIPALE, MockBatiment.Cully.BatimentChDesColombaires, null, date(1998, 1, 1), null);

				MockIndividu indJacques = addIndividu(numeroIndJacques, date(1960, 1, 1), "Cognac", "Jacques", true);
				addAdresse(indJacques, TypeAdresseCivil.COURRIER, "Chemin du palais","15b",null, CasePostale.parse("7507"),"Paris", MockPays.France, date(2012,1,1), null);


				MockIndividu indGeorges = addIndividu(numeroIndGeorges, date(1960, 1, 1), "Cognac", "Georges", true);
				addAdresse(indGeorges, TypeAdresseCivil.COURRIER, "Android Street","3H4",null, CasePostale.parse("9654"),"San Francisco", MockPays.EtatsUnis, date(2012,1,1), null);

			}
		});

		class Ids {
			Long jerome;
			Long jacques;
			Long georges;

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
				return null;
			}
		});

		doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				Tiers jerome = tiersService.getTiers(ids.jerome);
				Tiers jacques = tiersService.getTiers(ids.jacques);
				Tiers georges = tiersService.getTiers(ids.georges);

				InfoDocumentDocument1.InfoDocument infoDocumentJerome = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentJerome, jerome);
				assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_SUISSE, infoDocumentJerome.getAffranchissement().getZone());

				InfoDocumentDocument1.InfoDocument infoDocumentJacques = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentJacques, jacques);
				assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_EUROPE, infoDocumentJacques.getAffranchissement().getZone());

				InfoDocumentDocument1.InfoDocument infoDocumentGeorges = InfoDocumentDocument1.Factory.newInstance().addNewInfoDocument();
				editiqueHelper.remplitAffranchissement(infoDocumentGeorges, georges);
				assertEquals(EditiqueHelper.ZONE_AFFRANCHISSEMENT_RESTE_MONDE, infoDocumentGeorges.getAffranchissement().getZone());
				return null;
			}
		});

	}

}
