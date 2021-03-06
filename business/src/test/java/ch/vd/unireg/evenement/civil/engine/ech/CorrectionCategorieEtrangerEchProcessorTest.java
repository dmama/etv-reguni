package ch.vd.unireg.evenement.civil.engine.ech;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.civil.mock.MockPermis;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.type.TypePermis;

public class CorrectionCategorieEtrangerEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {
	
	@Test(timeout = 10000L)
	public void testPermisC() throws Exception {

		final long noIndividu = 3564572L;
		final RegDate dateArrivee = date(2000, 12, 23);
		final RegDate dateObtentionPermis = date(2011, 6, 3);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1964, 3, 12);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Suzuki", "Tsetsuko", false);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(ind, MockPays.Japon, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, null, null, MockCommune.Cossonay, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// changement de permis au civil
		doModificationIndividu(noIndividu, individu -> {
			final MockPermis permis = new MockPermis();
			permis.setDateDebutValidite(dateObtentionPermis);
			permis.setTypePermis(TypePermis.ETABLISSEMENT);
			individu.setPermis(permis);
		});

		// création de l'événement civil d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(535643L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateObtentionPermis);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals((Long) ppId, pp.getNumero());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateObtentionPermis.getOneDayAfter(), ffp.getDateDebut());
			Assert.assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testPermisNonC() throws Exception {

		final long noIndividu = 45267823L;
		final RegDate dateArrivee = date(2000, 12, 23);
		final RegDate dateObtentionPermis = date(2011, 6, 3);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1964, 3, 12);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Suzuki", "Tsetsuko", false);
				addPermis(ind, TypePermis.COURTE_DUREE, dateArrivee, null, false);
				addNationalite(ind, MockPays.Japon, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, null, null, MockCommune.Cossonay, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			return pp.getNumero();
		});

		// changement de permis au civil
		doModificationIndividu(noIndividu, individu -> {
			final MockPermis permis = new MockPermis();
			permis.setDateDebutValidite(dateObtentionPermis);
			permis.setTypePermis(TypePermis.SEJOUR);
			individu.setPermis(permis);
		});

		// création de l'événement civil d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(5426738L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateObtentionPermis);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			Assert.assertEquals((Long) ppId, pp.getNumero());

			final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
			Assert.assertNotNull(ffp);
			Assert.assertEquals(dateArrivee, ffp.getDateDebut());
			Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
			return null;
		});
	}

	@Test(timeout = 10000L)
	public void testPermisCAvecDecisionAci() throws Exception {

		final long noIndividu = 3564572L;
		final RegDate dateArrivee = date(2000, 12, 23);
		final RegDate dateObtentionPermis = date(2011, 6, 3);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final RegDate dateNaissance = date(1964, 3, 12);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Suzuki", "Tsetsuko", false);
				addPermis(ind, TypePermis.SEJOUR, dateArrivee, null, false);
				addNationalite(ind, MockPays.Japon, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.CheminDeRiondmorcel, null, dateArrivee, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, null, null, MockCommune.Cossonay, MotifRattachement.DOMICILE, ModeImposition.SOURCE);
			addDecisionAci(pp, dateArrivee.addYears(2), null, MockCommune.Aigle.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
			return pp.getNumero();
		});

		// changement de permis au civil
		doModificationIndividu(noIndividu, individu -> {
			final MockPermis permis = new MockPermis();
			permis.setDateDebutValidite(dateObtentionPermis);
			permis.setTypePermis(TypePermis.ETABLISSEMENT);
			individu.setPermis(permis);
		});

		// création de l'événement civil d'obtention de permis
		final long evtId = doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = new EvenementCivilEch();
			evt.setId(535643L);
			evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
			evt.setDateEvenement(dateObtentionPermis);
			evt.setEtat(EtatEvenementCivil.A_TRAITER);
			evt.setNumeroIndividu(noIndividu);
			evt.setType(TypeEvenementCivilEch.CORR_CATEGORIE_ETRANGER);
			return hibernateTemplate.merge(evt).getId();
		});

		// traitement de l'événement
		traiterEvenements(noIndividu);

		// vérification du traitement
		doInNewTransactionAndSession(status -> {
			final EvenementCivilEch evt = evtCivilDAO.get(evtId);
			Assert.assertNotNull(evt);
			Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());
			final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
			Assert.assertNotNull(pp);
			final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
			Assert.assertNotNull(erreurs);
			Assert.assertEquals(1, erreurs.size());
			final EvenementCivilEchErreur erreur = erreurs.iterator().next();
			String message = String.format("Le contribuable trouvé (%s) est sous l'influence d'une décision ACI",
			                               FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero()));
			Assert.assertEquals(message, erreur.getMessage());
			return null;
		});
	}

}
