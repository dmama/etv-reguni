package ch.vd.uniregctb.tiers;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Map;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static junit.framework.Assert.assertNotNull;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
public class TiersVisuControllerTest extends WebTest {

	private final static String CONTROLLER_NAME = "tiersVisuController";

	private TiersVisuController controller;

	@Override
	public void onSetUp() throws Exception {

		super.onSetUp();

		controller = getBean(TiersVisuController.class, CONTROLLER_NAME);

		servicePM.setUp(new DefaultMockServicePM());

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				final MockIndividu individu1 = addIndividu(333908, RegDate.get(1974, 3, 22), "Cuendet", "Adrienne", true);
				final MockIndividu individu2 = addIndividu(333905, RegDate.get(1974, 3, 22), "Cuendet", "Biloute", true);
				final MockIndividu individu3 = addIndividu(674417, RegDate.get(1974, 3, 22), "Dardare", "Francois", true);
				final MockIndividu individu4 = addIndividu(327706, RegDate.get(1974, 3, 22), "Dardare", "Marcel", true);
				final MockIndividu individu5 = addIndividu(320073, RegDate.get(1952, 3, 21), "ERTEM", "Sabri", true);

				addAdresse(individu1, EnumTypeAdresse.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.COURRIER, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, EnumTypeAdresse.COURRIER, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

				addAdresse(individu1, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeBeaulieu, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu2, EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, RegDate.get(1980, 1, 1), null);
				addAdresse(individu3, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu4, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);
				addAdresse(individu5, EnumTypeAdresse.PRINCIPALE, null, MockLocalite.LeLieu, RegDate.get(1980, 1, 1), null);

			}
		});

	}

	@Test
	public void testShowForm() throws Exception {
		loadDatabase();
		//
		//Tiers Habitant 12300003
		//
		request.setMethod("GET");
		request.addParameter("id", "12300003");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Habitant 34807810
		//
		request.setMethod("GET");
		request.addParameter("id", "34807810");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Habitant 12300001
		//
		request.setMethod("GET");
		request.addParameter("id", "12300001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers NonHabitant 12600001
		//
		request.setMethod("GET");
		request.addParameter("id", "12600001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers MenageCommun 86006202
		//
		request.setMethod("GET");
		request.addParameter("id", "86006202");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers DebiteurPrestationImposable 12500001
		//
		request.setMethod("GET");
		request.addParameter("id", "12500001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers Entreprise 127001
		//
		request.setMethod("GET");
		request.addParameter("id", "127001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
		//
		//Tiers AutreCommunaute 2800001
		//
		request.setMethod("GET");
		request.addParameter("id", "2800001");
		mav = controller.handleRequest(request, response);
		model = mav.getModel();
		assertNotNull(model);
	}

	@Test
	public void testOnSubmit() throws Exception {
		loadDatabase();
		request.addParameter("id", "86006202");
		request.setMethod("POST");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		assertNotNull(model);
	}

	/**
	 * Le contenu de cette méthode a été généré automatiquement à partir du fichier DBUnit tiers-basic.xml avec la classe DbUnit2Java. Ceci pour des raisons
	 * de performance avec Oracle 11g (qui est très très très lent lorsqu'il s'agit de récupérer la méta-information du schéma, ce qui fait DBUnit)
	 */
	@SuppressWarnings({"UnusedAssignment", "unchecked"})
	private void loadDatabase() {
		PeriodeFiscale pf0 = new PeriodeFiscale();
		pf0.setId(2L);
		pf0.setAnnee(2002);
		pf0.setLogModifDate(new Timestamp(1199142000000L));
		pf0.setParametrePeriodeFiscale(new HashSet());
		pf0.setModelesDocument(new HashSet());
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		PeriodeFiscale pf1 = new PeriodeFiscale();
		pf1.setId(3L);
		pf1.setAnnee(2003);
		pf1.setLogModifDate(new Timestamp(1199142000000L));
		pf1.setParametrePeriodeFiscale(new HashSet());
		pf1.setModelesDocument(new HashSet());
		pf1 = (PeriodeFiscale) hibernateTemplate.merge(pf1);

		PeriodeFiscale pf2 = new PeriodeFiscale();
		pf2.setId(4L);
		pf2.setAnnee(2004);
		pf2.setLogModifDate(new Timestamp(1199142000000L));
		pf2.setParametrePeriodeFiscale(new HashSet());
		pf2.setModelesDocument(new HashSet());
		pf2 = (PeriodeFiscale) hibernateTemplate.merge(pf2);

		PeriodeFiscale pf3 = new PeriodeFiscale();
		pf3.setId(5L);
		pf3.setAnnee(2005);
		pf3.setLogModifDate(new Timestamp(1199142000000L));
		pf3.setParametrePeriodeFiscale(new HashSet());
		pf3.setModelesDocument(new HashSet());
		pf3 = (PeriodeFiscale) hibernateTemplate.merge(pf3);

		PeriodeFiscale pf4 = new PeriodeFiscale();
		pf4.setId(6L);
		pf4.setAnnee(2006);
		pf4.setLogModifDate(new Timestamp(1199142000000L));
		pf4.setParametrePeriodeFiscale(new HashSet());
		pf4.setModelesDocument(new HashSet());
		pf4 = (PeriodeFiscale) hibernateTemplate.merge(pf4);

		PeriodeFiscale pf5 = new PeriodeFiscale();
		pf5.setId(7L);
		pf5.setAnnee(2007);
		pf5.setLogModifDate(new Timestamp(1199142000000L));
		pf5.setParametrePeriodeFiscale(new HashSet());
		pf5.setModelesDocument(new HashSet());
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		PeriodeFiscale pf6 = new PeriodeFiscale();
		pf6.setId(8L);
		pf6.setAnnee(2008);
		pf6.setLogModifDate(new Timestamp(1199142000000L));
		pf6.setParametrePeriodeFiscale(new HashSet());
		pf6.setModelesDocument(new HashSet());
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		PeriodeFiscale pf7 = new PeriodeFiscale();
		pf7.setId(9L);
		pf7.setAnnee(2009);
		pf7.setLogModifDate(new Timestamp(1199142000000L));
		pf7.setParametrePeriodeFiscale(new HashSet());
		pf7.setModelesDocument(new HashSet());
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscale ppf0 = new ParametrePeriodeFiscale();
		ppf0.setId(1L);
		ppf0.setDateFinEnvoiMasseDI(RegDate.get(2003, 4, 30));
		ppf0.setLogModifDate(new Timestamp(1199142000000L));
		ppf0.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf0.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf0.addParametrePeriodeFiscale(ppf0);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf1 = new ParametrePeriodeFiscale();
		ppf1.setId(2L);
		ppf1.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf1.setLogModifDate(new Timestamp(1199142000000L));
		ppf1.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf1.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf1.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf0.addParametrePeriodeFiscale(ppf1);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf2 = new ParametrePeriodeFiscale();
		ppf2.setId(3L);
		ppf2.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf2.setLogModifDate(new Timestamp(1199142000000L));
		ppf2.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf2.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf2.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf0.addParametrePeriodeFiscale(ppf2);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf3 = new ParametrePeriodeFiscale();
		ppf3.setId(4L);
		ppf3.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf3.setLogModifDate(new Timestamp(1199142000000L));
		ppf3.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf3.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf3.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf0.addParametrePeriodeFiscale(ppf3);
		pf0 = (PeriodeFiscale) hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscale ppf4 = new ParametrePeriodeFiscale();
		ppf4.setId(5L);
		ppf4.setDateFinEnvoiMasseDI(RegDate.get(2004, 4, 30));
		ppf4.setLogModifDate(new Timestamp(1199142000000L));
		ppf4.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf4.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf4.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf1.addParametrePeriodeFiscale(ppf4);
		pf1 = (PeriodeFiscale) hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscale ppf5 = new ParametrePeriodeFiscale();
		ppf5.setId(6L);
		ppf5.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf5.setLogModifDate(new Timestamp(1199142000000L));
		ppf5.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf5.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf5.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf1.addParametrePeriodeFiscale(ppf5);
		pf1 = (PeriodeFiscale) hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscale ppf6 = new ParametrePeriodeFiscale();
		ppf6.setId(7L);
		ppf6.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf6.setLogModifDate(new Timestamp(1199142000000L));
		ppf6.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf6.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf6.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf1.addParametrePeriodeFiscale(ppf6);
		pf1 = (PeriodeFiscale) hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscale ppf7 = new ParametrePeriodeFiscale();
		ppf7.setId(8L);
		ppf7.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf7.setLogModifDate(new Timestamp(1199142000000L));
		ppf7.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf7.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf7.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf1.addParametrePeriodeFiscale(ppf7);
		pf1 = (PeriodeFiscale) hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscale ppf8 = new ParametrePeriodeFiscale();
		ppf8.setId(9L);
		ppf8.setDateFinEnvoiMasseDI(RegDate.get(2005, 4, 30));
		ppf8.setLogModifDate(new Timestamp(1199142000000L));
		ppf8.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf8.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf8.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf2.addParametrePeriodeFiscale(ppf8);
		pf2 = (PeriodeFiscale) hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscale ppf9 = new ParametrePeriodeFiscale();
		ppf9.setId(10L);
		ppf9.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf9.setLogModifDate(new Timestamp(1199142000000L));
		ppf9.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf9.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf9.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf2.addParametrePeriodeFiscale(ppf9);
		pf2 = (PeriodeFiscale) hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscale ppf10 = new ParametrePeriodeFiscale();
		ppf10.setId(11L);
		ppf10.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf10.setLogModifDate(new Timestamp(1199142000000L));
		ppf10.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf10.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf10.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf2.addParametrePeriodeFiscale(ppf10);
		pf2 = (PeriodeFiscale) hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscale ppf11 = new ParametrePeriodeFiscale();
		ppf11.setId(12L);
		ppf11.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf11.setLogModifDate(new Timestamp(1199142000000L));
		ppf11.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf11.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf11.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf2.addParametrePeriodeFiscale(ppf11);
		pf2 = (PeriodeFiscale) hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscale ppf12 = new ParametrePeriodeFiscale();
		ppf12.setId(13L);
		ppf12.setDateFinEnvoiMasseDI(RegDate.get(2006, 4, 30));
		ppf12.setLogModifDate(new Timestamp(1199142000000L));
		ppf12.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf12.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf12.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf3.addParametrePeriodeFiscale(ppf12);
		pf3 = (PeriodeFiscale) hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscale ppf13 = new ParametrePeriodeFiscale();
		ppf13.setId(14L);
		ppf13.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf13.setLogModifDate(new Timestamp(1199142000000L));
		ppf13.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf13.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf13.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf3.addParametrePeriodeFiscale(ppf13);
		pf3 = (PeriodeFiscale) hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscale ppf14 = new ParametrePeriodeFiscale();
		ppf14.setId(15L);
		ppf14.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf14.setLogModifDate(new Timestamp(1199142000000L));
		ppf14.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf14.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf14.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf3.addParametrePeriodeFiscale(ppf14);
		pf3 = (PeriodeFiscale) hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscale ppf15 = new ParametrePeriodeFiscale();
		ppf15.setId(16L);
		ppf15.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf15.setLogModifDate(new Timestamp(1199142000000L));
		ppf15.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf15.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf15.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf3.addParametrePeriodeFiscale(ppf15);
		pf3 = (PeriodeFiscale) hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscale ppf16 = new ParametrePeriodeFiscale();
		ppf16.setId(17L);
		ppf16.setDateFinEnvoiMasseDI(RegDate.get(2007, 4, 30));
		ppf16.setLogModifDate(new Timestamp(1199142000000L));
		ppf16.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf16.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf16.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf4.addParametrePeriodeFiscale(ppf16);
		pf4 = (PeriodeFiscale) hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscale ppf17 = new ParametrePeriodeFiscale();
		ppf17.setId(18L);
		ppf17.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf17.setLogModifDate(new Timestamp(1199142000000L));
		ppf17.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf17.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf17.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf4.addParametrePeriodeFiscale(ppf17);
		pf4 = (PeriodeFiscale) hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscale ppf18 = new ParametrePeriodeFiscale();
		ppf18.setId(19L);
		ppf18.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf18.setLogModifDate(new Timestamp(1199142000000L));
		ppf18.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf18.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf18.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf4.addParametrePeriodeFiscale(ppf18);
		pf4 = (PeriodeFiscale) hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscale ppf19 = new ParametrePeriodeFiscale();
		ppf19.setId(20L);
		ppf19.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf19.setLogModifDate(new Timestamp(1199142000000L));
		ppf19.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf19.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf19.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf4.addParametrePeriodeFiscale(ppf19);
		pf4 = (PeriodeFiscale) hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscale ppf20 = new ParametrePeriodeFiscale();
		ppf20.setId(21L);
		ppf20.setDateFinEnvoiMasseDI(RegDate.get(2008, 4, 30));
		ppf20.setLogModifDate(new Timestamp(1199142000000L));
		ppf20.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf20.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf20.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf5.addParametrePeriodeFiscale(ppf20);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscale ppf21 = new ParametrePeriodeFiscale();
		ppf21.setId(22L);
		ppf21.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf21.setLogModifDate(new Timestamp(1199142000000L));
		ppf21.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf21.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf21.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf5.addParametrePeriodeFiscale(ppf21);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscale ppf22 = new ParametrePeriodeFiscale();
		ppf22.setId(23L);
		ppf22.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf22.setLogModifDate(new Timestamp(1199142000000L));
		ppf22.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf22.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf22.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf5.addParametrePeriodeFiscale(ppf22);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscale ppf23 = new ParametrePeriodeFiscale();
		ppf23.setId(24L);
		ppf23.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf23.setLogModifDate(new Timestamp(1199142000000L));
		ppf23.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf23.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf23.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf5.addParametrePeriodeFiscale(ppf23);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscale ppf24 = new ParametrePeriodeFiscale();
		ppf24.setId(25L);
		ppf24.setDateFinEnvoiMasseDI(RegDate.get(2009, 4, 30));
		ppf24.setLogModifDate(new Timestamp(1199142000000L));
		ppf24.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf24.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf24.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf6.addParametrePeriodeFiscale(ppf24);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscale ppf25 = new ParametrePeriodeFiscale();
		ppf25.setId(26L);
		ppf25.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf25.setLogModifDate(new Timestamp(1199142000000L));
		ppf25.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf25.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf25.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf6.addParametrePeriodeFiscale(ppf25);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscale ppf26 = new ParametrePeriodeFiscale();
		ppf26.setId(27L);
		ppf26.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf26.setLogModifDate(new Timestamp(1199142000000L));
		ppf26.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf26.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf26.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf6.addParametrePeriodeFiscale(ppf26);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscale ppf27 = new ParametrePeriodeFiscale();
		ppf27.setId(28L);
		ppf27.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf27.setLogModifDate(new Timestamp(1199142000000L));
		ppf27.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf27.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf27.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf6.addParametrePeriodeFiscale(ppf27);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscale ppf28 = new ParametrePeriodeFiscale();
		ppf28.setId(29L);
		ppf28.setDateFinEnvoiMasseDI(RegDate.get(2010, 4, 30));
		ppf28.setLogModifDate(new Timestamp(1199142000000L));
		ppf28.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf28.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf28.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf7.addParametrePeriodeFiscale(ppf28);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscale ppf29 = new ParametrePeriodeFiscale();
		ppf29.setId(30L);
		ppf29.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf29.setLogModifDate(new Timestamp(1199142000000L));
		ppf29.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf29.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf29.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf7.addParametrePeriodeFiscale(ppf29);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscale ppf30 = new ParametrePeriodeFiscale();
		ppf30.setId(31L);
		ppf30.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf30.setLogModifDate(new Timestamp(1199142000000L));
		ppf30.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf30.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf30.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf7.addParametrePeriodeFiscale(ppf30);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscale ppf31 = new ParametrePeriodeFiscale();
		ppf31.setId(32L);
		ppf31.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf31.setLogModifDate(new Timestamp(1199142000000L));
		ppf31.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf31.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf31.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf7.addParametrePeriodeFiscale(ppf31);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleDocument md0 = new ModeleDocument();
		md0.setId(1L);
		md0.setLogModifDate(new Timestamp(1199142000000L));
		md0.setModelesFeuilleDocument(new HashSet());
		md0.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf5.addModeleDocument(md0);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ModeleDocument md1 = new ModeleDocument();
		md1.setId(101L);
		md1.setLogModifDate(new Timestamp(1199142000000L));
		md1.setModelesFeuilleDocument(new HashSet());
		md1.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf5.addModeleDocument(md1);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ModeleDocument md2 = new ModeleDocument();
		md2.setId(2L);
		md2.setLogModifDate(new Timestamp(1199142000000L));
		md2.setModelesFeuilleDocument(new HashSet());
		md2.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf5.addModeleDocument(md2);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ModeleDocument md3 = new ModeleDocument();
		md3.setId(3L);
		md3.setLogModifDate(new Timestamp(1199142000000L));
		md3.setModelesFeuilleDocument(new HashSet());
		md3.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf5.addModeleDocument(md3);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ModeleDocument md4 = new ModeleDocument();
		md4.setId(4L);
		md4.setLogModifDate(new Timestamp(1199142000000L));
		md4.setModelesFeuilleDocument(new HashSet());
		md4.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf5.addModeleDocument(md4);
		pf5 = (PeriodeFiscale) hibernateTemplate.merge(pf5);

		ModeleDocument md5 = new ModeleDocument();
		md5.setId(5L);
		md5.setLogModifDate(new Timestamp(1199142000000L));
		md5.setModelesFeuilleDocument(new HashSet());
		md5.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf6.addModeleDocument(md5);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ModeleDocument md6 = new ModeleDocument();
		md6.setId(105L);
		md6.setLogModifDate(new Timestamp(1199142000000L));
		md6.setModelesFeuilleDocument(new HashSet());
		md6.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf6.addModeleDocument(md6);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ModeleDocument md7 = new ModeleDocument();
		md7.setId(6L);
		md7.setLogModifDate(new Timestamp(1199142000000L));
		md7.setModelesFeuilleDocument(new HashSet());
		md7.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf6.addModeleDocument(md7);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ModeleDocument md8 = new ModeleDocument();
		md8.setId(7L);
		md8.setLogModifDate(new Timestamp(1199142000000L));
		md8.setModelesFeuilleDocument(new HashSet());
		md8.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf6.addModeleDocument(md8);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ModeleDocument md9 = new ModeleDocument();
		md9.setId(8L);
		md9.setLogModifDate(new Timestamp(1199142000000L));
		md9.setModelesFeuilleDocument(new HashSet());
		md9.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf6.addModeleDocument(md9);
		pf6 = (PeriodeFiscale) hibernateTemplate.merge(pf6);

		ModeleDocument md10 = new ModeleDocument();
		md10.setId(9L);
		md10.setLogModifDate(new Timestamp(1199142000000L));
		md10.setModelesFeuilleDocument(new HashSet());
		md10.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf7.addModeleDocument(md10);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleDocument md11 = new ModeleDocument();
		md11.setId(109L);
		md11.setLogModifDate(new Timestamp(1199142000000L));
		md11.setModelesFeuilleDocument(new HashSet());
		md11.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf7.addModeleDocument(md11);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleDocument md12 = new ModeleDocument();
		md12.setId(10L);
		md12.setLogModifDate(new Timestamp(1199142000000L));
		md12.setModelesFeuilleDocument(new HashSet());
		md12.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf7.addModeleDocument(md12);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleDocument md13 = new ModeleDocument();
		md13.setId(11L);
		md13.setLogModifDate(new Timestamp(1199142000000L));
		md13.setModelesFeuilleDocument(new HashSet());
		md13.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf7.addModeleDocument(md13);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleDocument md14 = new ModeleDocument();
		md14.setId(12L);
		md14.setLogModifDate(new Timestamp(1199142000000L));
		md14.setModelesFeuilleDocument(new HashSet());
		md14.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf7.addModeleDocument(md14);
		pf7 = (PeriodeFiscale) hibernateTemplate.merge(pf7);

		ModeleFeuilleDocument mfd0 = new ModeleFeuilleDocument();
		mfd0.setId(1L);
		mfd0.setIntituleFeuille("Déclaration d'impot standard");
		mfd0.setLogModifDate(new Timestamp(1199142000000L));
		mfd0.setNumeroFormulaire("210");
		md0.addModeleFeuilleDocument(mfd0);
		md0 = (ModeleDocument) hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd1 = new ModeleFeuilleDocument();
		mfd1.setId(2L);
		mfd1.setIntituleFeuille("Annexe 1");
		mfd1.setLogModifDate(new Timestamp(1199142000000L));
		mfd1.setNumeroFormulaire("220");
		md0.addModeleFeuilleDocument(mfd1);
		md0 = (ModeleDocument) hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd2 = new ModeleFeuilleDocument();
		mfd2.setId(3L);
		mfd2.setIntituleFeuille("Annexe 2 et 3");
		mfd2.setLogModifDate(new Timestamp(1199142000000L));
		mfd2.setNumeroFormulaire("230");
		md0.addModeleFeuilleDocument(mfd2);
		md0 = (ModeleDocument) hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd3 = new ModeleFeuilleDocument();
		mfd3.setId(4L);
		mfd3.setIntituleFeuille("Annexe 4 et 5");
		mfd3.setLogModifDate(new Timestamp(1199142000000L));
		mfd3.setNumeroFormulaire("240");
		md0.addModeleFeuilleDocument(mfd3);
		md0 = (ModeleDocument) hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd4 = new ModeleFeuilleDocument();
		mfd4.setId(5L);
		mfd4.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd4.setLogModifDate(new Timestamp(1199142000000L));
		mfd4.setNumeroFormulaire("250");
		md2.addModeleFeuilleDocument(mfd4);
		md2 = (ModeleDocument) hibernateTemplate.merge(md2);

		ModeleFeuilleDocument mfd5 = new ModeleFeuilleDocument();
		mfd5.setId(6L);
		mfd5.setIntituleFeuille("Déclaration d'impot standard");
		mfd5.setLogModifDate(new Timestamp(1199142000000L));
		mfd5.setNumeroFormulaire("210");
		md3.addModeleFeuilleDocument(mfd5);
		md3 = (ModeleDocument) hibernateTemplate.merge(md3);

		ModeleFeuilleDocument mfd6 = new ModeleFeuilleDocument();
		mfd6.setId(10L);
		mfd6.setIntituleFeuille("Annexe dépense");
		mfd6.setLogModifDate(new Timestamp(1199142000000L));
		mfd6.setNumeroFormulaire("270");
		md3.addModeleFeuilleDocument(mfd6);
		md3 = (ModeleDocument) hibernateTemplate.merge(md3);

		ModeleFeuilleDocument mfd7 = new ModeleFeuilleDocument();
		mfd7.setId(11L);
		mfd7.setIntituleFeuille("Déclaration d'impot HC");
		mfd7.setLogModifDate(new Timestamp(1199142000000L));
		mfd7.setNumeroFormulaire("200");
		md4.addModeleFeuilleDocument(mfd7);
		md4 = (ModeleDocument) hibernateTemplate.merge(md4);

		ModeleFeuilleDocument mfd8 = new ModeleFeuilleDocument();
		mfd8.setId(12L);
		mfd8.setIntituleFeuille("Déclaration d'impot standard");
		mfd8.setLogModifDate(new Timestamp(1199142000000L));
		mfd8.setNumeroFormulaire("210");
		md5.addModeleFeuilleDocument(mfd8);
		md5 = (ModeleDocument) hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd9 = new ModeleFeuilleDocument();
		mfd9.setId(13L);
		mfd9.setIntituleFeuille("Annexe 1");
		mfd9.setLogModifDate(new Timestamp(1199142000000L));
		mfd9.setNumeroFormulaire("220");
		md5.addModeleFeuilleDocument(mfd9);
		md5 = (ModeleDocument) hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd10 = new ModeleFeuilleDocument();
		mfd10.setId(14L);
		mfd10.setIntituleFeuille("Annexe 2 et 3");
		mfd10.setLogModifDate(new Timestamp(1199142000000L));
		mfd10.setNumeroFormulaire("230");
		md5.addModeleFeuilleDocument(mfd10);
		md5 = (ModeleDocument) hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd11 = new ModeleFeuilleDocument();
		mfd11.setId(15L);
		mfd11.setIntituleFeuille("Annexe 4 et 5");
		mfd11.setLogModifDate(new Timestamp(1199142000000L));
		mfd11.setNumeroFormulaire("240");
		md5.addModeleFeuilleDocument(mfd11);
		md5 = (ModeleDocument) hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd12 = new ModeleFeuilleDocument();
		mfd12.setId(16L);
		mfd12.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd12.setLogModifDate(new Timestamp(1199142000000L));
		mfd12.setNumeroFormulaire("250");
		md7.addModeleFeuilleDocument(mfd12);
		md7 = (ModeleDocument) hibernateTemplate.merge(md7);

		ModeleFeuilleDocument mfd13 = new ModeleFeuilleDocument();
		mfd13.setId(17L);
		mfd13.setIntituleFeuille("Déclaration d'impot standard");
		mfd13.setLogModifDate(new Timestamp(1199142000000L));
		mfd13.setNumeroFormulaire("210");
		md8.addModeleFeuilleDocument(mfd13);
		md8 = (ModeleDocument) hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd14 = new ModeleFeuilleDocument();
		mfd14.setId(18L);
		mfd14.setIntituleFeuille("Annexe 1");
		mfd14.setLogModifDate(new Timestamp(1199142000000L));
		mfd14.setNumeroFormulaire("220");
		md8.addModeleFeuilleDocument(mfd14);
		md8 = (ModeleDocument) hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd15 = new ModeleFeuilleDocument();
		mfd15.setId(19L);
		mfd15.setIntituleFeuille("Annexe 2 et 3");
		mfd15.setLogModifDate(new Timestamp(1199142000000L));
		mfd15.setNumeroFormulaire("230");
		md8.addModeleFeuilleDocument(mfd15);
		md8 = (ModeleDocument) hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd16 = new ModeleFeuilleDocument();
		mfd16.setId(20L);
		mfd16.setIntituleFeuille("Annexe 4 et 5");
		mfd16.setLogModifDate(new Timestamp(1199142000000L));
		mfd16.setNumeroFormulaire("240");
		md8.addModeleFeuilleDocument(mfd16);
		md8 = (ModeleDocument) hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd17 = new ModeleFeuilleDocument();
		mfd17.setId(21L);
		mfd17.setIntituleFeuille("Annexe dépense");
		mfd17.setLogModifDate(new Timestamp(1199142000000L));
		mfd17.setNumeroFormulaire("270");
		md8.addModeleFeuilleDocument(mfd17);
		md8 = (ModeleDocument) hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd18 = new ModeleFeuilleDocument();
		mfd18.setId(22L);
		mfd18.setIntituleFeuille("Déclaration d'impot HC");
		mfd18.setLogModifDate(new Timestamp(1199142000000L));
		mfd18.setNumeroFormulaire("200");
		md9.addModeleFeuilleDocument(mfd18);
		md9 = (ModeleDocument) hibernateTemplate.merge(md9);

		ModeleFeuilleDocument mfd19 = new ModeleFeuilleDocument();
		mfd19.setId(23L);
		mfd19.setIntituleFeuille("Déclaration d'impot standard");
		mfd19.setLogModifDate(new Timestamp(1199142000000L));
		mfd19.setNumeroFormulaire("210");
		md10.addModeleFeuilleDocument(mfd19);
		md10 = (ModeleDocument) hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd20 = new ModeleFeuilleDocument();
		mfd20.setId(24L);
		mfd20.setIntituleFeuille("Annexe 1");
		mfd20.setLogModifDate(new Timestamp(1199142000000L));
		mfd20.setNumeroFormulaire("220");
		md10.addModeleFeuilleDocument(mfd20);
		md10 = (ModeleDocument) hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd21 = new ModeleFeuilleDocument();
		mfd21.setId(25L);
		mfd21.setIntituleFeuille("Annexe 2 et 3");
		mfd21.setLogModifDate(new Timestamp(1199142000000L));
		mfd21.setNumeroFormulaire("230");
		md10.addModeleFeuilleDocument(mfd21);
		md10 = (ModeleDocument) hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd22 = new ModeleFeuilleDocument();
		mfd22.setId(26L);
		mfd22.setIntituleFeuille("Annexe 4 et 5");
		mfd22.setLogModifDate(new Timestamp(1199142000000L));
		mfd22.setNumeroFormulaire("240");
		md10.addModeleFeuilleDocument(mfd22);
		md10 = (ModeleDocument) hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd23 = new ModeleFeuilleDocument();
		mfd23.setId(27L);
		mfd23.setIntituleFeuille("Annexe 1-1");
		mfd23.setLogModifDate(new Timestamp(1199142000000L));
		mfd23.setNumeroFormulaire("310");
		md10.addModeleFeuilleDocument(mfd23);
		md10 = (ModeleDocument) hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd24 = new ModeleFeuilleDocument();
		mfd24.setId(28L);
		mfd24.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd24.setLogModifDate(new Timestamp(1199142000000L));
		mfd24.setNumeroFormulaire("250");
		md12.addModeleFeuilleDocument(mfd24);
		md12 = (ModeleDocument) hibernateTemplate.merge(md12);

		ModeleFeuilleDocument mfd25 = new ModeleFeuilleDocument();
		mfd25.setId(29L);
		mfd25.setIntituleFeuille("Annexe dépense");
		mfd25.setLogModifDate(new Timestamp(1199142000000L));
		mfd25.setNumeroFormulaire("270");
		md13.addModeleFeuilleDocument(mfd25);
		md13 = (ModeleDocument) hibernateTemplate.merge(md13);

		ModeleFeuilleDocument mfd26 = new ModeleFeuilleDocument();
		mfd26.setId(30L);
		mfd26.setIntituleFeuille("Déclaration d'impot HC");
		mfd26.setLogModifDate(new Timestamp(1199142000000L));
		mfd26.setNumeroFormulaire("200");
		md14.addModeleFeuilleDocument(mfd26);
		md14 = (ModeleDocument) hibernateTemplate.merge(md14);

		MenageCommun mc0 = new MenageCommun();
		mc0.setNumero(12600004L);
		mc0.setMouvementsDossier(new HashSet());
		mc0.setSituationsFamille(new HashSet());
		mc0.setDebiteurInactif(false);
		mc0.setLogCreationDate(new Timestamp(1199142000000L));
		mc0.setLogModifDate(new Timestamp(1199142000000L));
		mc0.setOfficeImpotId(10);
		mc0.setAdressesTiers(new HashSet());
		mc0.setDeclarations(new HashSet());
		mc0.setForsFiscaux(new HashSet());
		mc0.setRapportsObjet(new HashSet());
		mc0.setRapportsSujet(new HashSet());
		mc0 = (MenageCommun) hibernateTemplate.merge(mc0);

		PersonnePhysique pp0 = new PersonnePhysique();
		pp0.setNumero(12600003L);
		pp0.setBlocageRemboursementAutomatique(false);
		pp0.setMouvementsDossier(new HashSet());
		pp0.setSituationsFamille(new HashSet());
		pp0.setDebiteurInactif(false);
		pp0.setLogCreationDate(new Timestamp(1199142000000L));
		pp0.setLogModifDate(new Timestamp(1199142000000L));
		pp0.setDateNaissance(RegDate.get(1953, 12, 18));
		pp0.setNom("Mme");
		pp0.setNumeroOfsNationalite(8231);
		pp0.setPrenom("Mario");
		pp0.setSexe(Sexe.MASCULIN);
		pp0.setIdentificationsPersonnes(new HashSet());
		pp0.setOfficeImpotId(10);
		pp0.setHabitant(false);
		pp0.setAdressesTiers(new HashSet());
		pp0.setDeclarations(new HashSet());
		pp0.setDroitsAccesAppliques(new HashSet());
		pp0.setForsFiscaux(new HashSet());
		pp0.setRapportsObjet(new HashSet());
		pp0.setRapportsSujet(new HashSet());
		pp0 = (PersonnePhysique) hibernateTemplate.merge(pp0);

		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(12600009L);
		pp1.setBlocageRemboursementAutomatique(false);
		pp1.setMouvementsDossier(new HashSet());
		pp1.setSituationsFamille(new HashSet());
		pp1.setDebiteurInactif(false);
		pp1.setLogCreationDate(new Timestamp(1199142000000L));
		pp1.setLogModifDate(new Timestamp(1199142000000L));
		pp1.setDateNaissance(RegDate.get(1977, 2, 12));
		pp1.setNom("Tardy");
		pp1.setNumeroOfsNationalite(8201);
		pp1.setPrenom("Alain (HS)");
		pp1.setSexe(Sexe.MASCULIN);
		pp1.setIdentificationsPersonnes(new HashSet());
		pp1.setOfficeImpotId(10);
		pp1.setHabitant(false);
		pp1.setAdressesTiers(new HashSet());
		pp1.setDeclarations(new HashSet());
		pp1.setDroitsAccesAppliques(new HashSet());
		pp1.setForsFiscaux(new HashSet());
		pp1.setRapportsObjet(new HashSet());
		pp1.setRapportsSujet(new HashSet());
		pp1 = (PersonnePhysique) hibernateTemplate.merge(pp1);

		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(12600001L);
		pp2.setAdresseBicSwift("CCBPFRPPBDX");
		pp2.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp2.setBlocageRemboursementAutomatique(false);
		pp2.setComplementNom("Chopard");
		pp2.setMouvementsDossier(new HashSet());
		pp2.setSituationsFamille(new HashSet());
		pp2.setDebiteurInactif(false);
		pp2.setLogCreationDate(new Timestamp(1199142000000L));
		pp2.setLogModifDate(new Timestamp(1199142000000L));
		pp2.setDateNaissance(RegDate.get(1971, 1, 23));
		pp2.setNom("Pirez");
		pp2.setNumeroOfsNationalite(8212);
		pp2.setNumeroAssureSocial("7561234567897");
		pp2.setPrenom("Isidor (sourcier gris)");
		pp2.setSexe(Sexe.MASCULIN);
		pp2.setIdentificationsPersonnes(new HashSet());
		pp2.setNumeroCompteBancaire("CH9308440717427290198");
		pp2.setNumeroTelecopie("0219663629");
		pp2.setNumeroTelephonePortable("0219663999");
		pp2.setNumeroTelephonePrive("0219663623");
		pp2.setNumeroTelephoneProfessionnel("0219663625");
		pp2.setOfficeImpotId(10);
		pp2.setPersonneContact("MAURICE DUPONT");
		pp2.setHabitant(false);
		pp2.setAdressesTiers(new HashSet());
		pp2.setDeclarations(new HashSet());
		pp2.setDroitsAccesAppliques(new HashSet());
		pp2.setForsFiscaux(new HashSet());
		pp2.setRapportsObjet(new HashSet());
		pp2.setRapportsSujet(new HashSet());
		pp2.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);

		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(43308102L);
		pp3.setMouvementsDossier(new HashSet());
		pp3.setSituationsFamille(new HashSet());
		pp3.setDebiteurInactif(false);
		pp3.setLogCreationDate(new Timestamp(1199142000000L));
		pp3.setLogModifDate(new Timestamp(1199142000000L));
		pp3.setIdentificationsPersonnes(new HashSet());
		pp3.setNumeroIndividu(320073L);
		pp3.setOfficeImpotId(10);
		pp3.setHabitant(true);
		pp3.setAdressesTiers(new HashSet());
		pp3.setDeclarations(new HashSet());
		pp3.setDroitsAccesAppliques(new HashSet());
		pp3.setForsFiscaux(new HashSet());
		pp3.setRapportsObjet(new HashSet());
		pp3.setRapportsSujet(new HashSet());
		pp3 = (PersonnePhysique) hibernateTemplate.merge(pp3);

		PersonnePhysique pp4 = new PersonnePhysique();
		pp4.setNumero(43308103L);
		pp4.setMouvementsDossier(new HashSet());
		pp4.setSituationsFamille(new HashSet());
		pp4.setDebiteurInactif(false);
		pp4.setLogCreationDate(new Timestamp(1199142000000L));
		pp4.setLogModifDate(new Timestamp(1199142000000L));
		pp4.setIdentificationsPersonnes(new HashSet());
		pp4.setNumeroIndividu(325740L);
		pp4.setOfficeImpotId(10);
		pp4.setHabitant(true);
		pp4.setAdressesTiers(new HashSet());
		pp4.setDeclarations(new HashSet());
		pp4.setDroitsAccesAppliques(new HashSet());
		pp4.setForsFiscaux(new HashSet());
		pp4.setRapportsObjet(new HashSet());
		pp4.setRapportsSujet(new HashSet());
		pp4 = (PersonnePhysique) hibernateTemplate.merge(pp4);

		PersonnePhysique pp5 = new PersonnePhysique();
		pp5.setNumero(43308104L);
		pp5.setMouvementsDossier(new HashSet());
		pp5.setSituationsFamille(new HashSet());
		pp5.setDebiteurInactif(false);
		pp5.setLogCreationDate(new Timestamp(1199142000000L));
		pp5.setLogModifDate(new Timestamp(1199142000000L));
		pp5.setIdentificationsPersonnes(new HashSet());
		pp5.setNumeroIndividu(325631L);
		pp5.setOfficeImpotId(10);
		pp5.setHabitant(true);
		pp5.setAdressesTiers(new HashSet());
		pp5.setDeclarations(new HashSet());
		pp5.setDroitsAccesAppliques(new HashSet());
		pp5.setForsFiscaux(new HashSet());
		pp5.setRapportsObjet(new HashSet());
		pp5.setRapportsSujet(new HashSet());
		pp5 = (PersonnePhysique) hibernateTemplate.merge(pp5);

		DebiteurPrestationImposable dpi0 = new DebiteurPrestationImposable();
		dpi0.setNumero(1678432L);
		dpi0.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi0.setComplementNom("Café du Commerce");
		dpi0.setPeriodicites(new HashSet());
		dpi0.setDebiteurInactif(false);
		dpi0.setLogCreationDate(new Timestamp(1199142000000L));
		dpi0.setLogModifDate(new Timestamp(1199142000000L));
		dpi0.setModeCommunication(ModeCommunication.PAPIER);
		dpi0.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.setAdressesTiers(new HashSet());
		dpi0.setDeclarations(new HashSet());
		dpi0.setForsFiscaux(new HashSet());
		dpi0.setRapportsObjet(new HashSet());
		dpi0.setRapportsSujet(new HashSet());
		dpi0 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi0);

		DebiteurPrestationImposable dpi1 = new DebiteurPrestationImposable();
		dpi1.setNumero(1678439L);
		dpi1.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi1.setComplementNom("Café du Sayonara");
		dpi1.setPeriodicites(new HashSet());
		dpi1.setDebiteurInactif(false);
		dpi1.setNom1("Nom1");
		dpi1.setNom2("Nom2");
		dpi1.setLogCreationDate(new Timestamp(1199142000000L));
		dpi1.setLogModifDate(new Timestamp(1199142000000L));
		dpi1.setModeCommunication(ModeCommunication.PAPIER);
		dpi1.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);
		dpi1.setAdressesTiers(new HashSet());
		dpi1.setDeclarations(new HashSet());
		dpi1.setForsFiscaux(new HashSet());
		dpi1.setRapportsObjet(new HashSet());
		dpi1.setRapportsSujet(new HashSet());
		dpi1 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi1);

		PersonnePhysique pp6 = new PersonnePhysique();
		pp6.setNumero(12900001L);
		pp6.setBlocageRemboursementAutomatique(false);
		pp6.setMouvementsDossier(new HashSet());
		pp6.setSituationsFamille(new HashSet());
		pp6.setDebiteurInactif(false);
		pp6.setLogCreationDate(new Timestamp(1199142000000L));
		pp6.setLogModifDate(new Timestamp(1199142000000L));
		pp6.setDateNaissance(RegDate.get(1952, 1, 23));
		pp6.setNom("Lederet");
		pp6.setNumeroOfsNationalite(8100);
		pp6.setNumeroAssureSocial("7561234567897");
		pp6.setPrenom("Michel");
		pp6.setSexe(Sexe.MASCULIN);
		pp6.setIdentificationsPersonnes(new HashSet());
		pp6.setNumeroCompteBancaire("CH9308440717427290198");
		pp6.setNumeroTelephonePortable("0764537812");
		pp6.setNumeroTelephonePrive("032'897'45'32");
		pp6.setOfficeImpotId(10);
		pp6.setPersonneContact("");
		pp6.setHabitant(false);
		pp6.setAdressesTiers(new HashSet());
		pp6.setDeclarations(new HashSet());
		pp6.setDroitsAccesAppliques(new HashSet());
		pp6.setForsFiscaux(new HashSet());
		pp6.setRapportsObjet(new HashSet());
		pp6.setRapportsSujet(new HashSet());
		pp6.setTitulaireCompteBancaire("Lederet Michel");
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		PersonnePhysique pp7 = new PersonnePhysique();
		pp7.setNumero(12300003L);
		pp7.setMouvementsDossier(new HashSet());
		pp7.setSituationsFamille(new HashSet());
		pp7.setDebiteurInactif(false);
		pp7.setLogCreationDate(new Timestamp(1199142000000L));
		pp7.setLogModifDate(new Timestamp(1199142000000L));
		pp7.setIdentificationsPersonnes(new HashSet());
		pp7.setNumeroIndividu(327706L);
		pp7.setOfficeImpotId(10);
		pp7.setHabitant(true);
		pp7.setAdressesTiers(new HashSet());
		pp7.setDeclarations(new HashSet());
		pp7.setDroitsAccesAppliques(new HashSet());
		pp7.setForsFiscaux(new HashSet());
		pp7.setRapportsObjet(new HashSet());
		pp7.setRapportsSujet(new HashSet());
		pp7 = (PersonnePhysique) hibernateTemplate.merge(pp7);

		PersonnePhysique pp8 = new PersonnePhysique();
		pp8.setNumero(34807810L);
		pp8.setAdresseCourrierElectronique("pascaline@descloux.ch");
		pp8.setBlocageRemboursementAutomatique(false);
		pp8.setMouvementsDossier(new HashSet());
		pp8.setSituationsFamille(new HashSet());
		pp8.setDebiteurInactif(false);
		pp8.setLogCreationDate(new Timestamp(1199142000000L));
		pp8.setLogModifDate(new Timestamp(1199142000000L));
		pp8.setIdentificationsPersonnes(new HashSet());
		pp8.setNumeroCompteBancaire("CH9308440717427290198");
		pp8.setNumeroIndividu(674417L);
		pp8.setNumeroTelephonePortable("0792348732");
		pp8.setNumeroTelephonePrive("0213135489");
		pp8.setOfficeImpotId(10);
		pp8.setHabitant(true);
		pp8.setAdressesTiers(new HashSet());
		pp8.setDeclarations(new HashSet());
		pp8.setDroitsAccesAppliques(new HashSet());
		pp8.setForsFiscaux(new HashSet());
		pp8.setRapportsObjet(new HashSet());
		pp8.setRapportsSujet(new HashSet());
		pp8.setTitulaireCompteBancaire("Pascaline Descloux");
		pp8 = (PersonnePhysique) hibernateTemplate.merge(pp8);

		PersonnePhysique pp9 = new PersonnePhysique();
		pp9.setNumero(12300001L);
		pp9.setMouvementsDossier(new HashSet());
		pp9.setSituationsFamille(new HashSet());
		pp9.setDebiteurInactif(false);
		pp9.setLogCreationDate(new Timestamp(1199142000000L));
		pp9.setLogModifDate(new Timestamp(1199142000000L));
		pp9.setIdentificationsPersonnes(new HashSet());
		pp9.setNumeroIndividu(333905L);
		pp9.setOfficeImpotId(10);
		pp9.setHabitant(true);
		pp9.setAdressesTiers(new HashSet());
		pp9.setDeclarations(new HashSet());
		pp9.setDroitsAccesAppliques(new HashSet());
		pp9.setForsFiscaux(new HashSet());
		pp9.setRapportsObjet(new HashSet());
		pp9.setRapportsSujet(new HashSet());
		pp9 = (PersonnePhysique) hibernateTemplate.merge(pp9);

		PersonnePhysique pp10 = new PersonnePhysique();
		pp10.setNumero(12300002L);
		pp10.setMouvementsDossier(new HashSet());
		pp10.setSituationsFamille(new HashSet());
		pp10.setDebiteurInactif(false);
		pp10.setLogCreationDate(new Timestamp(1199142000000L));
		pp10.setLogModifDate(new Timestamp(1199142000000L));
		pp10.setIdentificationsPersonnes(new HashSet());
		pp10.setNumeroIndividu(333908L);
		pp10.setOfficeImpotId(10);
		pp10.setHabitant(true);
		pp10.setAdressesTiers(new HashSet());
		pp10.setDeclarations(new HashSet());
		pp10.setDroitsAccesAppliques(new HashSet());
		pp10.setForsFiscaux(new HashSet());
		pp10.setRapportsObjet(new HashSet());
		pp10.setRapportsSujet(new HashSet());
		pp10 = (PersonnePhysique) hibernateTemplate.merge(pp10);

		PersonnePhysique pp11 = new PersonnePhysique();
		pp11.setNumero(10246283L);
		pp11.setMouvementsDossier(new HashSet());
		pp11.setSituationsFamille(new HashSet());
		pp11.setDebiteurInactif(false);
		pp11.setLogCreationDate(new Timestamp(1199142000000L));
		pp11.setLogModifDate(new Timestamp(1199142000000L));
		pp11.setIdentificationsPersonnes(new HashSet());
		pp11.setNumeroIndividu(333911L);
		pp11.setOfficeImpotId(10);
		pp11.setHabitant(true);
		pp11.setAdressesTiers(new HashSet());
		pp11.setDeclarations(new HashSet());
		pp11.setDroitsAccesAppliques(new HashSet());
		pp11.setForsFiscaux(new HashSet());
		pp11.setRapportsObjet(new HashSet());
		pp11.setRapportsSujet(new HashSet());
		pp11 = (PersonnePhysique) hibernateTemplate.merge(pp11);

		MenageCommun mc1 = new MenageCommun();
		mc1.setNumero(86006202L);
		mc1.setAdresseBicSwift("CCBPFRPPBDX");
		mc1.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		mc1.setBlocageRemboursementAutomatique(false);
		mc1.setMouvementsDossier(new HashSet());
		mc1.setSituationsFamille(new HashSet());
		mc1.setDebiteurInactif(false);
		mc1.setLogCreationDate(new Timestamp(1199142000000L));
		mc1.setLogModifDate(new Timestamp(1199142000000L));
		mc1.setNumeroCompteBancaire("CH9308440717427290198");
		mc1.setNumeroTelecopie("0219663629");
		mc1.setNumeroTelephonePortable("0219663999");
		mc1.setNumeroTelephonePrive("0219663623");
		mc1.setNumeroTelephoneProfessionnel("0219663625");
		mc1.setOfficeImpotId(10);
		mc1.setPersonneContact("MAURICE DUPONT");
		mc1.setAdressesTiers(new HashSet());
		mc1.setDeclarations(new HashSet());
		mc1.setForsFiscaux(new HashSet());
		mc1.setRapportsObjet(new HashSet());
		mc1.setRapportsSujet(new HashSet());
		mc1.setTitulaireCompteBancaire("ERIC MONTAGNY");
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		Entreprise e0 = new Entreprise();
		e0.setNumero(127001L);
		e0.setMouvementsDossier(new HashSet());
		e0.setSituationsFamille(new HashSet());
		e0.setDebiteurInactif(false);
		e0.setLogCreationDate(new Timestamp(1199142000000L));
		e0.setLogModifDate(new Timestamp(1199142000000L));
		e0.setNumeroEntreprise(27769L);
		e0.setOfficeImpotId(10);
		e0.setAdressesTiers(new HashSet());
		e0.setDeclarations(new HashSet());
		e0.setForsFiscaux(new HashSet());
		e0.setRapportsObjet(new HashSet());
		e0.setRapportsSujet(new HashSet());
		e0 = (Entreprise) hibernateTemplate.merge(e0);

		PersonnePhysique pp12 = new PersonnePhysique();
		pp12.setNumero(12600008L);
		pp12.setAdresseBicSwift("CCBPFRPPBDX");
		pp12.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp12.setBlocageRemboursementAutomatique(false);
		pp12.setComplementNom("Chopard");
		pp12.setMouvementsDossier(new HashSet());
		pp12.setSituationsFamille(new HashSet());
		pp12.setDebiteurInactif(true);
		pp12.setLogCreationDate(new Timestamp(1199142000000L));
		pp12.setLogModifDate(new Timestamp(1199142000000L));
		pp12.setDateNaissance(RegDate.get(1970, 1, 23));
		pp12.setNom("The full i107");
		pp12.setNumeroOfsNationalite(8212);
		pp12.setNumeroAssureSocial("7561234567897");
		pp12.setPrenom("De la mort");
		pp12.setSexe(Sexe.MASCULIN);
		pp12.setIdentificationsPersonnes(new HashSet());
		pp12.setNumeroCompteBancaire("CH9308440717427290198");
		pp12.setNumeroTelecopie("0219663629");
		pp12.setNumeroTelephonePortable("0219663999");
		pp12.setNumeroTelephonePrive("0219663623");
		pp12.setNumeroTelephoneProfessionnel("0219663625");
		pp12.setOfficeImpotId(10);
		pp12.setPersonneContact("MAURICE DUPONT");
		pp12.setHabitant(false);
		pp12.setAdressesTiers(new HashSet());
		pp12.setDeclarations(new HashSet());
		pp12.setDroitsAccesAppliques(new HashSet());
		pp12.setForsFiscaux(new HashSet());
		pp12.setRapportsObjet(new HashSet());
		pp12.setRapportsSujet(new HashSet());
		pp12.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp12 = (PersonnePhysique) hibernateTemplate.merge(pp12);

		PersonnePhysique pp13 = new PersonnePhysique();
		pp13.setNumero(12600002L);
		pp13.setAdresseBicSwift("CCBPFRPPBDX");
		pp13.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp13.setBlocageRemboursementAutomatique(false);
		pp13.setComplementNom("Chopard");
		pp13.setMouvementsDossier(new HashSet());
		pp13.setSituationsFamille(new HashSet());
		pp13.setDebiteurInactif(false);
		pp13.setLogCreationDate(new Timestamp(1199142000000L));
		pp13.setLogModifDate(new Timestamp(1199142000000L));
		pp13.setDateNaissance(RegDate.get(1970, 1, 23));
		pp13.setNom("Martinez");
		pp13.setNumeroOfsNationalite(8212);
		pp13.setNumeroAssureSocial("7561234567897");
		pp13.setPrenom("Conchita");
		pp13.setSexe(Sexe.FEMININ);
		pp13.setIdentificationsPersonnes(new HashSet());
		pp13.setNumeroCompteBancaire("CH9308440717427290198");
		pp13.setNumeroTelecopie("0219663629");
		pp13.setNumeroTelephonePortable("0219663999");
		pp13.setNumeroTelephonePrive("0219663623");
		pp13.setNumeroTelephoneProfessionnel("0219663625");
		pp13.setOfficeImpotId(10);
		pp13.setPersonneContact("MAURICE DUPONT");
		pp13.setHabitant(false);
		pp13.setAdressesTiers(new HashSet());
		pp13.setDeclarations(new HashSet());
		pp13.setDroitsAccesAppliques(new HashSet());
		pp13.setForsFiscaux(new HashSet());
		pp13.setRapportsObjet(new HashSet());
		pp13.setRapportsSujet(new HashSet());
		pp13.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp13 = (PersonnePhysique) hibernateTemplate.merge(pp13);

		AutreCommunaute ac0 = new AutreCommunaute();
		ac0.setNumero(2800001L);
		ac0.setFormeJuridique(FormeJuridique.ASS);
		ac0.setNom("Communaute XYZ");
		ac0.setMouvementsDossier(new HashSet());
		ac0.setSituationsFamille(new HashSet());
		ac0.setDebiteurInactif(false);
		ac0.setLogCreationDate(new Timestamp(1199142000000L));
		ac0.setLogModifDate(new Timestamp(1199142000000L));
		ac0.setNumeroTelephonePortable("Chopard");
		ac0.setOfficeImpotId(10);
		ac0.setAdressesTiers(new HashSet());
		ac0.setDeclarations(new HashSet());
		ac0.setForsFiscaux(new HashSet());
		ac0.setRapportsObjet(new HashSet());
		ac0.setRapportsSujet(new HashSet());
		ac0 = (AutreCommunaute) hibernateTemplate.merge(ac0);

		CollectiviteAdministrative ca0 = new CollectiviteAdministrative();
		ca0.setNumero(2100001L);
		ca0.setMouvementsDossier(new HashSet());
		ca0.setSituationsFamille(new HashSet());
		ca0.setDebiteurInactif(false);
		ca0.setLogCreationDate(new Timestamp(1199142000000L));
		ca0.setLogModifDate(new Timestamp(1199142000000L));
		ca0.setNumeroCollectiviteAdministrative(1013);
		ca0.setAdressesTiers(new HashSet());
		ca0.setDeclarations(new HashSet());
		ca0.setForsFiscaux(new HashSet());
		ca0.setRapportsObjet(new HashSet());
		ca0.setRapportsSujet(new HashSet());
		ca0 = (CollectiviteAdministrative) hibernateTemplate.merge(ca0);

		CollectiviteAdministrative ca1 = new CollectiviteAdministrative();
		ca1.setNumero(2100002L);
		ca1.setMouvementsDossier(new HashSet());
		ca1.setSituationsFamille(new HashSet());
		ca1.setDebiteurInactif(false);
		ca1.setLogCreationDate(new Timestamp(1199142000000L));
		ca1.setLogModifDate(new Timestamp(1199142000000L));
		ca1.setNumeroCollectiviteAdministrative(10);
		ca1.setAdressesTiers(new HashSet());
		ca1.setDeclarations(new HashSet());
		ca1.setForsFiscaux(new HashSet());
		ca1.setRapportsObjet(new HashSet());
		ca1.setRapportsSujet(new HashSet());
		ca1 = (CollectiviteAdministrative) hibernateTemplate.merge(ca1);

		AdresseSuisse as0 = new AdresseSuisse();
		as0.setId(1L);
		as0.setDateDebut(RegDate.get(2002, 2, 12));
		as0.setLogModifDate(new Timestamp(1199142000000L));
		as0.setNumeroCasePostale(23);
		as0.setNumeroMaison("19");
		as0.setNumeroOrdrePoste(104);
		as0.setNumeroRue(83404);
		as0.setPermanente(false);
		as0.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
		as0.setUsage(TypeAdresseTiers.COURRIER);
		pp6.addAdresseTiers(as0);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		AdresseSuisse as1 = new AdresseSuisse();
		as1.setId(6L);
		as1.setDateDebut(RegDate.get(2008, 1, 29));
		as1.setLogModifDate(new Timestamp(1199142000000L));
		as1.setNumeroMaison("12");
		as1.setNumeroRue(32296);
		as1.setPermanente(false);
		as1.setUsage(TypeAdresseTiers.COURRIER);
		pp2.addAdresseTiers(as1);
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);

		AdresseSuisse as2 = new AdresseSuisse();
		as2.setId(7L);
		as2.setDateDebut(RegDate.get(2008, 4, 15));
		as2.setLogModifDate(new Timestamp(1199142000000L));
		as2.setNumeroMaison("12");
		as2.setNumeroOrdrePoste(104);
		as2.setNumeroRue(35365);
		as2.setPermanente(false);
		as2.setUsage(TypeAdresseTiers.COURRIER);
		pp13.addAdresseTiers(as2);
		pp13 = (PersonnePhysique) hibernateTemplate.merge(pp13);

		AdresseSuisse as3 = new AdresseSuisse();
		as3.setId(8L);
		as3.setDateDebut(RegDate.get(2008, 1, 29));
		as3.setLogModifDate(new Timestamp(1199142000000L));
		as3.setNumeroMaison("12");
		as3.setNumeroOrdrePoste(571);
		as3.setPermanente(false);
		as3.setRue("Rue des terreaux");
		as3.setUsage(TypeAdresseTiers.COURRIER);
		pp0.addAdresseTiers(as3);
		pp0 = (PersonnePhysique) hibernateTemplate.merge(pp0);

		AdresseSuisse as4 = new AdresseSuisse();
		as4.setId(9L);
		as4.setDateDebut(RegDate.get(2008, 1, 29));
		as4.setLogModifDate(new Timestamp(1199142000000L));
		as4.setNumeroMaison("12");
		as4.setNumeroOrdrePoste(571);
		as4.setPermanente(false);
		as4.setRue("Rue des terreaux");
		as4.setUsage(TypeAdresseTiers.COURRIER);
		mc0.addAdresseTiers(as4);
		mc0 = (MenageCommun) hibernateTemplate.merge(mc0);

		AdresseSuisse as5 = new AdresseSuisse();
		as5.setId(10L);
		as5.setDateDebut(RegDate.get(2006, 2, 21));
		as5.setLogModifDate(new Timestamp(1199142000000L));
		as5.setNumeroMaison("12");
		as5.setNumeroOrdrePoste(104);
		as5.setNumeroRue(35365);
		as5.setPermanente(false);
		as5.setUsage(TypeAdresseTiers.COURRIER);
		pp12.addAdresseTiers(as5);
		pp12 = (PersonnePhysique) hibernateTemplate.merge(pp12);

		AdresseSuisse as6 = new AdresseSuisse();
		as6.setId(11L);
		as6.setDateDebut(RegDate.get(2006, 7, 1));
		as6.setLogModifDate(new Timestamp(1199142000000L));
		as6.setNumeroMaison("12");
		as6.setNumeroRue(32296);
		as6.setPermanente(false);
		as6.setUsage(TypeAdresseTiers.COURRIER);
		pp1.addAdresseTiers(as6);
		pp1 = (PersonnePhysique) hibernateTemplate.merge(pp1);

		DeclarationImpotSource dis0 = new DeclarationImpotSource();
		dis0.setId(1L);
		dis0.setDateDebut(RegDate.get(2008, 1, 1));
		dis0.setDateFin(RegDate.get(2008, 3, 31));
		dis0.setDelais(new HashSet());
		dis0.setEtats(new HashSet());
		dis0.setLogCreationDate(new Timestamp(1199142000000L));
		dis0.setLogModifDate(new Timestamp(1199142000000L));
		dis0.setModeCommunication(ModeCommunication.PAPIER);
		dis0.setPeriode(pf6);
		dis0.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.addDeclaration(dis0);
		dpi0 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi0);

		DeclarationImpotSource dis1 = new DeclarationImpotSource();
		dis1.setId(5L);
		dis1.setDateDebut(RegDate.get(2008, 4, 1));
		dis1.setDateFin(RegDate.get(2008, 6, 30));
		dis1.setDelais(new HashSet());
		dis1.setEtats(new HashSet());
		dis1.setLogCreationDate(new Timestamp(1199142000000L));
		dis1.setLogModifDate(new Timestamp(1199142000000L));
		dis1.setModeCommunication(ModeCommunication.PAPIER);
		dis1.setPeriode(pf6);
		dis1.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.addDeclaration(dis1);
		dpi0 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi0);

		DeclarationImpotOrdinaire dio0 = new DeclarationImpotOrdinaire();
		dio0.setId(2L);
		dio0.setDateDebut(RegDate.get(2005, 1, 1));
		dio0.setDateFin(RegDate.get(2005, 12, 31));
		dio0.setDelais(new HashSet());
		dio0.setEtats(new HashSet());
		dio0.setLibre(false);
		dio0.setLogCreationDate(new Timestamp(1136070000000L));
		dio0.setLogModifDate(new Timestamp(1199142000000L));
		dio0.setModeleDocument(md0);
		dio0.setNumeroOfsForGestion(5652);
		dio0.setNumero(1);
		dio0.setPeriode(pf3);
		dio0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio0);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		DeclarationImpotOrdinaire dio1 = new DeclarationImpotOrdinaire();
		dio1.setId(3L);
		dio1.setDateDebut(RegDate.get(2006, 1, 1));
		dio1.setDateFin(RegDate.get(2006, 12, 31));
		dio1.setDelais(new HashSet());
		dio1.setEtats(new HashSet());
		dio1.setLibre(false);
		dio1.setLogCreationDate(new Timestamp(1167606000000L));
		dio1.setLogModifDate(new Timestamp(1199142000000L));
		dio1.setModeleDocument(md0);
		dio1.setNumeroOfsForGestion(5652);
		dio1.setNumero(1);
		dio1.setPeriode(pf4);
		dio1.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio1);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		DeclarationImpotOrdinaire dio2 = new DeclarationImpotOrdinaire();
		dio2.setId(4L);
		dio2.setDateDebut(RegDate.get(2007, 1, 1));
		dio2.setDateFin(RegDate.get(2007, 12, 31));
		dio2.setDelais(new HashSet());
		dio2.setEtats(new HashSet());
		dio2.setLibre(false);
		dio2.setLogCreationDate(new Timestamp(1199142000000L));
		dio2.setLogModifDate(new Timestamp(1199142000000L));
		dio2.setModeleDocument(md2);
		dio2.setNumeroOfsForGestion(5652);
		dio2.setNumero(1);
		dio2.setPeriode(pf5);
		dio2.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio2);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		DeclarationImpotSource dis2 = new DeclarationImpotSource();
		dis2.setId(15L);
		dis2.setDateDebut(RegDate.get(2008, 4, 1));
		dis2.setDateFin(RegDate.get(2008, 6, 30));
		dis2.setDelais(new HashSet());
		dis2.setEtats(new HashSet());
		dis2.setLogCreationDate(new Timestamp(1199142000000L));
		dis2.setLogModifDate(new Timestamp(1199142000000L));
		dis2.setModeCommunication(ModeCommunication.PAPIER);
		dis2.setPeriode(pf6);
		dis2.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi1.addDeclaration(dis2);
		dpi1 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi1);

		EtatDeclaration ed0 = new EtatDeclaration();
		ed0.setId(1L);
		ed0.setDateObtention(RegDate.get(2008, 3, 20));
		ed0.setLogModifDate(new Timestamp(1199142000000L));
		ed0.setEtat(TypeEtatDeclaration.EMISE);
		dis0.addEtatDeclaration(ed0);
		dis0 = (DeclarationImpotSource) hibernateTemplate.merge(dis0);

		EtatDeclaration ed1 = new EtatDeclaration();
		ed1.setId(8L);
		ed1.setDateObtention(RegDate.get(2008, 5, 15));
		ed1.setLogModifDate(new Timestamp(1199142000000L));
		ed1.setEtat(TypeEtatDeclaration.SOMMEE);
		dis0.addEtatDeclaration(ed1);
		dis0 = (DeclarationImpotSource) hibernateTemplate.merge(dis0);

		EtatDeclaration ed2 = new EtatDeclaration();
		ed2.setId(10L);
		ed2.setDateObtention(RegDate.get(2008, 5, 25));
		ed2.setLogModifDate(new Timestamp(1199142000000L));
		ed2.setEtat(TypeEtatDeclaration.RETOURNEE);
		dis0.addEtatDeclaration(ed2);
		dis0 = (DeclarationImpotSource) hibernateTemplate.merge(dis0);

		EtatDeclaration ed3 = new EtatDeclaration();
		ed3.setId(9L);
		ed3.setDateObtention(RegDate.get(2008, 6, 20));
		ed3.setLogModifDate(new Timestamp(1199142000000L));
		ed3.setEtat(TypeEtatDeclaration.EMISE);
		dis1.addEtatDeclaration(ed3);
		dis1 = (DeclarationImpotSource) hibernateTemplate.merge(dis1);

		EtatDeclaration ed4 = new EtatDeclaration();
		ed4.setId(2L);
		ed4.setDateObtention(RegDate.get(2006, 1, 15));
		ed4.setLogModifDate(new Timestamp(1199142000000L));
		ed4.setEtat(TypeEtatDeclaration.EMISE);
		dio0.addEtatDeclaration(ed4);
		dio0 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio0);

		EtatDeclaration ed5 = new EtatDeclaration();
		ed5.setId(3L);
		ed5.setDateObtention(RegDate.get(2006, 4, 13));
		ed5.setLogModifDate(new Timestamp(1199142000000L));
		ed5.setEtat(TypeEtatDeclaration.RETOURNEE);
		dio0.addEtatDeclaration(ed5);
		dio0 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio0);

		EtatDeclaration ed6 = new EtatDeclaration();
		ed6.setId(4L);
		ed6.setDateObtention(RegDate.get(2007, 1, 16));
		ed6.setLogModifDate(new Timestamp(1199142000000L));
		ed6.setEtat(TypeEtatDeclaration.EMISE);
		dio1.addEtatDeclaration(ed6);
		dio1 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio1);

		EtatDeclaration ed7 = new EtatDeclaration();
		ed7.setId(5L);
		ed7.setDateObtention(RegDate.get(2007, 9, 15));
		ed7.setLogModifDate(new Timestamp(1199142000000L));
		ed7.setEtat(TypeEtatDeclaration.SOMMEE);
		dio1.addEtatDeclaration(ed7);
		dio1 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio1);

		EtatDeclaration ed8 = new EtatDeclaration();
		ed8.setId(6L);
		ed8.setDateObtention(RegDate.get(2007, 11, 1));
		ed8.setLogModifDate(new Timestamp(1199142000000L));
		ed8.setEtat(TypeEtatDeclaration.ECHUE);
		dio1.addEtatDeclaration(ed8);
		dio1 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio1);

		EtatDeclaration ed9 = new EtatDeclaration();
		ed9.setId(7L);
		ed9.setDateObtention(RegDate.get(2007, 1, 15));
		ed9.setLogModifDate(new Timestamp(1199142000000L));
		ed9.setEtat(TypeEtatDeclaration.EMISE);
		dio2.addEtatDeclaration(ed9);
		dio2 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio2);

		EtatDeclaration ed10 = new EtatDeclaration();
		ed10.setId(18L);
		ed10.setDateObtention(RegDate.get(2008, 3, 20));
		ed10.setLogModifDate(new Timestamp(1199142000000L));
		ed10.setEtat(TypeEtatDeclaration.EMISE);
		dis2.addEtatDeclaration(ed10);
		dis2 = (DeclarationImpotSource) hibernateTemplate.merge(dis2);

		DelaiDeclaration dd0 = new DelaiDeclaration();
		dd0.setId(1L);
		dd0.setConfirmationEcrite(false);
		dd0.setDelaiAccordeAu(RegDate.get(2008, 4, 30));
		dd0.setLogModifDate(new Timestamp(1199142000000L));
		dis0.addDelaiDeclaration(dd0);
		dis0 = (DeclarationImpotSource) hibernateTemplate.merge(dis0);

		DelaiDeclaration dd1 = new DelaiDeclaration();
		dd1.setId(7L);
		dd1.setConfirmationEcrite(false);
		dd1.setDelaiAccordeAu(RegDate.get(2008, 7, 31));
		dd1.setLogModifDate(new Timestamp(1199142000000L));
		dis1.addDelaiDeclaration(dd1);
		dis1 = (DeclarationImpotSource) hibernateTemplate.merge(dis1);

		DelaiDeclaration dd2 = new DelaiDeclaration();
		dd2.setId(8L);
		dd2.setConfirmationEcrite(false);
		dd2.setDateDemande(RegDate.get(2008, 6, 25));
		dd2.setDateTraitement(RegDate.get(2008, 6, 25));
		dd2.setDelaiAccordeAu(RegDate.get(2008, 9, 30));
		dd2.setLogModifDate(new Timestamp(1199142000000L));
		dis1.addDelaiDeclaration(dd2);
		dis1 = (DeclarationImpotSource) hibernateTemplate.merge(dis1);

		DelaiDeclaration dd3 = new DelaiDeclaration();
		dd3.setId(2L);
		dd3.setConfirmationEcrite(false);
		dd3.setDelaiAccordeAu(RegDate.get(2006, 3, 15));
		dd3.setLogModifDate(new Timestamp(1199142000000L));
		dio0.addDelaiDeclaration(dd3);
		dio0 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio0);

		DelaiDeclaration dd4 = new DelaiDeclaration();
		dd4.setId(3L);
		dd4.setConfirmationEcrite(false);
		dd4.setDateDemande(RegDate.get(2006, 2, 20));
		dd4.setDateTraitement(RegDate.get(2006, 2, 20));
		dd4.setDelaiAccordeAu(RegDate.get(2006, 7, 31));
		dd4.setLogModifDate(new Timestamp(1199142000000L));
		dio0.addDelaiDeclaration(dd4);
		dio0 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio0);

		DelaiDeclaration dd5 = new DelaiDeclaration();
		dd5.setId(4L);
		dd5.setConfirmationEcrite(false);
		dd5.setDelaiAccordeAu(RegDate.get(2007, 3, 15));
		dd5.setLogModifDate(new Timestamp(1199142000000L));
		dio1.addDelaiDeclaration(dd5);
		dio1 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio1);

		DelaiDeclaration dd6 = new DelaiDeclaration();
		dd6.setId(5L);
		dd6.setConfirmationEcrite(false);
		dd6.setDelaiAccordeAu(RegDate.get(2008, 3, 15));
		dd6.setLogModifDate(new Timestamp(1199142000000L));
		dio2.addDelaiDeclaration(dd6);
		dio2 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio2);

		DelaiDeclaration dd7 = new DelaiDeclaration();
		dd7.setId(6L);
		dd7.setConfirmationEcrite(false);
		dd7.setDateDemande(RegDate.get(2008, 4, 12));
		dd7.setDateTraitement(RegDate.get(2008, 4, 12));
		dd7.setDelaiAccordeAu(RegDate.get(2008, 9, 15));
		dd7.setLogModifDate(new Timestamp(1199142000000L));
		dio2.addDelaiDeclaration(dd7);
		dio2 = (DeclarationImpotOrdinaire) hibernateTemplate.merge(dio2);

		AppartenanceMenage am0 = new AppartenanceMenage();
		am0.setId(5L);
		am0.setDateDebut(RegDate.get(1990, 7, 3));
		am0.setLogModifDate(new Timestamp(1199142000000L));
		am0.setObjetId(12600004L);
		am0.setSujetId(12600003L);
		am0 = (AppartenanceMenage) hibernateTemplate.merge(am0);
		pp0.addRapportSujet(am0);
		mc0.addRapportObjet(am0);

		AppartenanceMenage am1 = new AppartenanceMenage();
		am1.setId(1L);
		am1.setDateDebut(RegDate.get(1985, 2, 15));
		am1.setLogModifDate(new Timestamp(1199142000000L));
		am1.setObjetId(86006202L);
		am1.setSujetId(12300001L);
		am1 = (AppartenanceMenage) hibernateTemplate.merge(am1);
		pp9.addRapportSujet(am1);
		mc1.addRapportObjet(am1);

		AppartenanceMenage am2 = new AppartenanceMenage();
		am2.setId(2L);
		am2.setDateDebut(RegDate.get(1985, 2, 15));
		am2.setLogModifDate(new Timestamp(1199142000000L));
		am2.setObjetId(86006202L);
		am2.setSujetId(12300002L);
		am2 = (AppartenanceMenage) hibernateTemplate.merge(am2);
		pp10.addRapportSujet(am2);
		mc1.addRapportObjet(am2);

		RapportPrestationImposable rpi0 = new RapportPrestationImposable();
		rpi0.setId(3L);
		rpi0.setDateDebut(RegDate.get(2008, 1, 29));
		rpi0.setDateFin(RegDate.get(2008, 6, 25));
		rpi0.setLogModifDate(new Timestamp(1199142000000L));
		rpi0.setTauxActivite(100);
		rpi0.setObjetId(1678432L);
		rpi0.setSujetId(12600001L);
		rpi0.setTypeActivite(TypeActivite.PRINCIPALE);
		rpi0 = (RapportPrestationImposable) hibernateTemplate.merge(rpi0);
		pp2.addRapportSujet(rpi0);
		dpi0.addRapportObjet(rpi0);

		RapportPrestationImposable rpi1 = new RapportPrestationImposable();
		rpi1.setId(4L);
		rpi1.setDateDebut(RegDate.get(2008, 1, 29));
		rpi1.setLogModifDate(new Timestamp(1199142000000L));
		rpi1.setTauxActivite(100);
		rpi1.setObjetId(1678432L);
		rpi1.setSujetId(12600003L);
		rpi1.setTypeActivite(TypeActivite.PRINCIPALE);
		rpi1 = (RapportPrestationImposable) hibernateTemplate.merge(rpi1);
		pp0.addRapportSujet(rpi1);
		dpi0.addRapportObjet(rpi1);

		Tutelle t0 = new Tutelle();
		t0.setId(6L);
		t0.setDateDebut(RegDate.get(2006, 2, 23));
		t0.setLogModifDate(new Timestamp(1199142000000L));
		t0.setObjetId(12300002L);
		t0.setSujetId(34807810L);
		t0 = (Tutelle) hibernateTemplate.merge(t0);
		pp8.addRapportSujet(t0);
		pp10.addRapportObjet(t0);

		ContactImpotSource cis0 = new ContactImpotSource();
		cis0.setId(7L);
		cis0.setDateDebut(RegDate.get(2000, 1, 1));
		cis0.setLogModifDate(new Timestamp(1199142000000L));
		cis0.setObjetId(1678432L);
		cis0.setSujetId(43308102L);
		cis0 = (ContactImpotSource) hibernateTemplate.merge(cis0);
		pp3.addRapportSujet(cis0);
		dpi0.addRapportObjet(cis0);

		ForFiscalPrincipal ffp0 = new ForFiscalPrincipal();
		ffp0.setId(7L);
		ffp0.setDateDebut(RegDate.get(2002, 2, 12));
		ffp0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp0.setLogModifDate(new Timestamp(1199142000000L));
		ffp0.setModeImposition(ModeImposition.MIXTE_137_1);
		ffp0.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp0.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp0.setNumeroOfsAutoriteFiscale(6412);
		ffp0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp0);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		ForFiscalSecondaire ffs0 = new ForFiscalSecondaire();
		ffs0.setId(8L);
		ffs0.setDateFin(RegDate.get(2007, 12, 31));
		ffs0.setDateDebut(RegDate.get(2002, 2, 12));
		ffs0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs0.setLogModifDate(new Timestamp(1199142000000L));
		ffs0.setMotifFermeture(MotifFor.VENTE_IMMOBILIER);
		ffs0.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		ffs0.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs0.setNumeroOfsAutoriteFiscale(5407);
		ffs0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp6.addForFiscal(ffs0);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		ForFiscalSecondaire ffs1 = new ForFiscalSecondaire();
		ffs1.setId(80L);
		ffs1.setDateDebut(RegDate.get(2004, 7, 1));
		ffs1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs1.setLogModifDate(new Timestamp(1199142000000L));
		ffs1.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		ffs1.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs1.setNumeroOfsAutoriteFiscale(5890);
		ffs1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp6.addForFiscal(ffs1);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		ForFiscalPrincipal ffp1 = new ForFiscalPrincipal();
		ffp1.setId(107L);
		ffp1.setDateFin(RegDate.get(2002, 2, 11));
		ffp1.setDateDebut(RegDate.get(2001, 2, 12));
		ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp1.setLogModifDate(new Timestamp(1199142000000L));
		ffp1.setModeImposition(ModeImposition.SOURCE);
		ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp1.setNumeroOfsAutoriteFiscale(5591);
		ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp1);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		ForFiscalPrincipal ffp2 = new ForFiscalPrincipal();
		ffp2.setId(108L);
		ffp2.setDateFin(RegDate.get(2001, 2, 11));
		ffp2.setDateDebut(RegDate.get(2000, 2, 12));
		ffp2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp2.setLogModifDate(new Timestamp(1199142000000L));
		ffp2.setModeImposition(ModeImposition.SOURCE);
		ffp2.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		ffp2.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp2.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp2.setNumeroOfsAutoriteFiscale(6412);
		ffp2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp2);
		pp6 = (PersonnePhysique) hibernateTemplate.merge(pp6);

		ForFiscalPrincipal ffp3 = new ForFiscalPrincipal();
		ffp3.setId(1L);
		ffp3.setDateDebut(RegDate.get(2008, 1, 29));
		ffp3.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp3.setLogModifDate(new Timestamp(1199142000000L));
		ffp3.setModeImposition(ModeImposition.SOURCE);
		ffp3.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp3.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp3.setNumeroOfsAutoriteFiscale(5477);
		ffp3.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp2.addForFiscal(ffp3);
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);

		ForFiscalPrincipal ffp4 = new ForFiscalPrincipal();
		ffp4.setId(2L);
		ffp4.setDateFin(RegDate.get(1985, 2, 14));
		ffp4.setDateDebut(RegDate.get(1979, 2, 9));
		ffp4.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp4.setLogModifDate(new Timestamp(1199142000000L));
		ffp4.setModeImposition(ModeImposition.ORDINAIRE);
		ffp4.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp4.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp4.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp4.setNumeroOfsAutoriteFiscale(5652);
		ffp4.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp9.addForFiscal(ffp4);
		pp9 = (PersonnePhysique) hibernateTemplate.merge(pp9);

		ForFiscalPrincipal ffp5 = new ForFiscalPrincipal();
		ffp5.setId(6L);
		ffp5.setDateFin(RegDate.get(1985, 2, 14));
		ffp5.setDateDebut(RegDate.get(1978, 10, 20));
		ffp5.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp5.setLogModifDate(new Timestamp(1199142000000L));
		ffp5.setModeImposition(ModeImposition.ORDINAIRE);
		ffp5.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp5.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp5.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp5.setNumeroOfsAutoriteFiscale(5402);
		ffp5.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp10.addForFiscal(ffp5);
		pp10 = (PersonnePhysique) hibernateTemplate.merge(pp10);

		ForFiscalPrincipal ffp6 = new ForFiscalPrincipal();
		ffp6.setId(5L);
		ffp6.setDateDebut(RegDate.get(1985, 2, 15));
		ffp6.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp6.setLogModifDate(new Timestamp(1199142000000L));
		ffp6.setModeImposition(ModeImposition.ORDINAIRE);
		ffp6.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp6.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp6.setNumeroOfsAutoriteFiscale(5652);
		ffp6.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		mc1.addForFiscal(ffp6);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		ForFiscalPrincipal ffp7 = new ForFiscalPrincipal();
		ffp7.setId(4L);
		ffp7.setDateDebut(RegDate.get(1997, 6, 24));
		ffp7.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp7.setLogModifDate(new Timestamp(1199142000000L));
		ffp7.setModeImposition(ModeImposition.ORDINAIRE);
		ffp7.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp7.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp7.setNumeroOfsAutoriteFiscale(5586);
		ffp7.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp8.addForFiscal(ffp7);
		pp8 = (PersonnePhysique) hibernateTemplate.merge(pp8);

		ForFiscalPrincipal ffp8 = new ForFiscalPrincipal();
		ffp8.setId(9L);
		ffp8.setDateDebut(RegDate.get(2008, 1, 29));
		ffp8.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp8.setLogModifDate(new Timestamp(1199142000000L));
		ffp8.setModeImposition(ModeImposition.SOURCE);
		ffp8.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp8.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp8.setNumeroOfsAutoriteFiscale(5757);
		ffp8.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		mc0.addForFiscal(ffp8);
		mc0 = (MenageCommun) hibernateTemplate.merge(mc0);

		ForFiscalPrincipal ffp9 = new ForFiscalPrincipal();
		ffp9.setId(10L);
		ffp9.setDateDebut(RegDate.get(2008, 4, 15));
		ffp9.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp9.setLogModifDate(new Timestamp(1199142000000L));
		ffp9.setModeImposition(ModeImposition.SOURCE);
		ffp9.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp9.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp9.setNumeroOfsAutoriteFiscale(5402);
		ffp9.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp13.addForFiscal(ffp9);
		pp13 = (PersonnePhysique) hibernateTemplate.merge(pp13);

		ForDebiteurPrestationImposable fdpi0 = new ForDebiteurPrestationImposable();
		fdpi0.setId(11L);
		fdpi0.setDateFin(RegDate.get(2007, 12, 31));
		fdpi0.setDateDebut(RegDate.get(2007, 1, 1));
		fdpi0.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		fdpi0.setLogModifDate(new Timestamp(1199142000000L));
		fdpi0.setNumeroOfsAutoriteFiscale(5652);
		fdpi0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi0.addForFiscal(fdpi0);
		dpi0 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi0);

		ForDebiteurPrestationImposable fdpi1 = new ForDebiteurPrestationImposable();
		fdpi1.setId(12L);
		fdpi1.setDateDebut(RegDate.get(2008, 3, 23));
		fdpi1.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		fdpi1.setLogModifDate(new Timestamp(1199142000000L));
		fdpi1.setNumeroOfsAutoriteFiscale(5407);
		fdpi1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi0.addForFiscal(fdpi1);
		dpi0 = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi0);

		ForFiscalPrincipal ffp10 = new ForFiscalPrincipal();
		ffp10.setId(13L);
		ffp10.setDateDebut(RegDate.get(2006, 6, 5));
		ffp10.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp10.setLogModifDate(new Timestamp(1199142000000L));
		ffp10.setModeImposition(ModeImposition.SOURCE);
		ffp10.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp10.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp10.setNumeroOfsAutoriteFiscale(8201);
		ffp10.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		pp1.addForFiscal(ffp10);
		pp1 = (PersonnePhysique) hibernateTemplate.merge(pp1);

		ForFiscalSecondaire ffs2 = new ForFiscalSecondaire();
		ffs2.setId(14L);
		ffs2.setDateDebut(RegDate.get(2006, 6, 5));
		ffs2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs2.setLogModifDate(new Timestamp(1199142000000L));
		ffs2.setMotifOuverture(MotifFor.MAJORITE);
		ffs2.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs2.setNumeroOfsAutoriteFiscale(5407);
		ffs2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp1.addForFiscal(ffs2);
		pp1 = (PersonnePhysique) hibernateTemplate.merge(pp1);

		ForFiscalPrincipal ffp11 = new ForFiscalPrincipal();
		ffp11.setId(15L);
		ffp11.setDateFin(RegDate.get(1990, 7, 2));
		ffp11.setDateDebut(RegDate.get(1971, 12, 18));
		ffp11.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp11.setLogModifDate(new Timestamp(1199142000000L));
		ffp11.setModeImposition(ModeImposition.SOURCE);
		ffp11.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp11.setMotifOuverture(MotifFor.MAJORITE);
		ffp11.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp11.setNumeroOfsAutoriteFiscale(5591);
		ffp11.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp0.addForFiscal(ffp11);
		pp0 = (PersonnePhysique) hibernateTemplate.merge(pp0);

		SituationFamilleMenageCommun sfmc0 = new SituationFamilleMenageCommun();
		sfmc0.setId(10L);
		sfmc0.setDateDebut(RegDate.get(1990, 7, 3));
		sfmc0.setEtatCivil(EtatCivil.MARIE);
		sfmc0.setLogModifDate(new Timestamp(1199142000000L));
		sfmc0.setNombreEnfants(2);
		sfmc0.setTarifApplicable(TarifImpotSource.NORMAL);
		sfmc0.setContribuablePrincipalId(12600003L);
		mc0.addSituationFamille(sfmc0);
		mc0 = (MenageCommun) hibernateTemplate.merge(mc0);

		SituationFamillePersonnePhysique sfpp0 = new SituationFamillePersonnePhysique();
		sfpp0.setId(1L);
		sfpp0.setDateDebut(RegDate.get(1960, 10, 20));
		sfpp0.setDateFin(RegDate.get(1985, 2, 14));
		sfpp0.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp0.setLogModifDate(new Timestamp(1199142000000L));
		sfpp0.setNombreEnfants(0);
		pp9.addSituationFamille(sfpp0);
		pp9 = (PersonnePhysique) hibernateTemplate.merge(pp9);

		SituationFamillePersonnePhysique sfpp1 = new SituationFamillePersonnePhysique();
		sfpp1.setId(2L);
		sfpp1.setDateDebut(RegDate.get(1961, 2, 9));
		sfpp1.setDateFin(RegDate.get(1985, 2, 14));
		sfpp1.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp1.setLogModifDate(new Timestamp(1199142000000L));
		sfpp1.setNombreEnfants(0);
		pp10.addSituationFamille(sfpp1);
		pp10 = (PersonnePhysique) hibernateTemplate.merge(pp10);

		SituationFamillePersonnePhysique sfpp2 = new SituationFamillePersonnePhysique();
		sfpp2.setId(3L);
		sfpp2.setDateDebut(RegDate.get(2005, 8, 29));
		sfpp2.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp2.setLogModifDate(new Timestamp(1199142000000L));
		sfpp2.setNombreEnfants(0);
		pp7.addSituationFamille(sfpp2);
		pp7 = (PersonnePhysique) hibernateTemplate.merge(pp7);

		SituationFamillePersonnePhysique sfpp3 = new SituationFamillePersonnePhysique();
		sfpp3.setId(4L);
		sfpp3.setDateDebut(RegDate.get(1979, 6, 24));
		sfpp3.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp3.setLogModifDate(new Timestamp(1199142000000L));
		sfpp3.setNombreEnfants(0);
		pp8.addSituationFamille(sfpp3);
		pp8 = (PersonnePhysique) hibernateTemplate.merge(pp8);

		SituationFamilleMenageCommun sfmc1 = new SituationFamilleMenageCommun();
		sfmc1.setId(5L);
		sfmc1.setDateDebut(RegDate.get(1985, 2, 15));
		sfmc1.setDateFin(RegDate.get(1985, 6, 1));
		sfmc1.setEtatCivil(EtatCivil.MARIE);
		sfmc1.setLogModifDate(new Timestamp(1199142000000L));
		sfmc1.setNombreEnfants(0);
		sfmc1.setContribuablePrincipalId(12300002L);
		mc1.addSituationFamille(sfmc1);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		SituationFamilleMenageCommun sfmc2 = new SituationFamilleMenageCommun();
		sfmc2.setId(6L);
		sfmc2.setDateDebut(RegDate.get(1985, 6, 2));
		sfmc2.setEtatCivil(EtatCivil.MARIE);
		sfmc2.setLogModifDate(new Timestamp(1199142000000L));
		sfmc2.setNombreEnfants(1);
		sfmc2.setContribuablePrincipalId(12300002L);
		mc1.addSituationFamille(sfmc2);
		mc1 = (MenageCommun) hibernateTemplate.merge(mc1);

		SituationFamillePersonnePhysique sfpp4 = new SituationFamillePersonnePhysique();
		sfpp4.setId(9L);
		sfpp4.setDateDebut(RegDate.get(2008, 1, 29));
		sfpp4.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp4.setLogModifDate(new Timestamp(1199142000000L));
		sfpp4.setNombreEnfants(0);
		pp2.addSituationFamille(sfpp4);
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);

		IdentificationPersonne ip0 = new IdentificationPersonne();
		ip0.setId(1L);
		ip0.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
		ip0.setIdentifiant("15489652357");
		ip0.setLogModifDate(new Timestamp(1199142000000L));
		pp2.addIdentificationPersonne(ip0);
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);

		IdentificationPersonne ip1 = new IdentificationPersonne();
		ip1.setId(2L);
		ip1.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
		ip1.setIdentifiant("0784.7621/5");
		ip1.setLogModifDate(new Timestamp(1199142000000L));
		pp2.addIdentificationPersonne(ip1);
		pp2 = (PersonnePhysique) hibernateTemplate.merge(pp2);
	}
}
