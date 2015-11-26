package ch.vd.uniregctb.evenement.organisation.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRC;
import ch.vd.unireg.interfaces.organisation.mock.data.MockDonneesRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscal;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalInformationComplementaire;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

import static ch.vd.uniregctb.type.EmetteurEvenementOrganisation.FOSC;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.A_VERIFIER;
import static ch.vd.uniregctb.type.EtatEvenementOrganisation.EN_ERREUR;

/**
 * @author Raphaël Marmier, 2015-11-10
 */
public class ForcedEventProcessorTest extends AbstractEvenementOrganisationProcessorTest {

	private EvenementFiscalDAO evtFiscalDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtFiscalDAO = getBean(EvenementFiscalDAO.class, "evenementFiscalDAO");
	}

	protected boolean buildProcessorOnSetup() {
		return true;
	}

	@Test(timeout = 10000L)
	public void testForcerNeCreePasForEnvoieEvtFiscaux() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.RADIE,
						                                           StatusRegistreIDE.RADIE,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeStatusInscription(RegDate.get(2012, 1, 26), StatusInscriptionRC.RADIE);
				rc.changeStatusInscription(RegDate.get(2015, 7, 5), StatusInscriptionRC.PROVISOIRE);
				rc.changeDateRadiation(RegDate.get(2012, 1, 26), RegDate.get(2012, 1, 26));
				rc.changeDateRadiation(RegDate.get(2015, 7, 5), null);
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				addForPrincipal(entreprise, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		EvenementOrganisation evt = doInNewTransactionAndSession(new TransactionCallback<EvenementOrganisation>() {
			@Override
			public EvenementOrganisation doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event =
						createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, RegDate.get(2015, 7, 5), EN_ERREUR, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event);
			}
		});

		// Traitement synchrone de l'événement
		processor.forceEvenement(new EvenementOrganisationBasicInfo(evt, evt.getNoOrganisation()));

		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.FORCE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             // Si la réinscription devait être traitée et non forcée, on trouverait un for.
				                             Assert.assertEquals(0, entreprise.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             // Si la réinscription devait être traitée et non forcée, on trouverait deux événements de for en plus de
				                             // celui d'information.
				                             Assert.assertEquals(1, evtsFiscaux.size());

				                             final List<EvenementFiscal> evtsFiscauxTries = new ArrayList<>(evtsFiscaux);
				                             Collections.sort(evtsFiscauxTries, new Comparator<EvenementFiscal>() {
					                             @Override
					                             public int compare(EvenementFiscal o1, EvenementFiscal o2) {
						                             return Long.compare(o1.getId(), o2.getId());
					                             }
				                             });

				                             {
					                             final EvenementFiscal ef = evtsFiscauxTries.get(0);
					                             Assert.assertNotNull(ef);
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.class, ef.getClass());
					                             Assert.assertEquals(date(2015, 7, 5), ef.getDateValeur());

					                             final EvenementFiscalInformationComplementaire efrf = (EvenementFiscalInformationComplementaire) ef;
					                             Assert.assertEquals(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.APPEL_CREANCIERS_CONCORDAT, efrf.getType());
				                             }

				                             return null;
			                             }
		                             }
		);
	}

	@Test(timeout = 10000L)
	public void testForcerAVERIFIERNeRienFaire() throws Exception {

		// Mise en place service mock
		final Long noOrganisation = 101202100L;
		final Long noSite = noOrganisation + 1000000;

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				MockOrganisation organisation =
						MockOrganisationFactory.createOrganisation(noOrganisation, noSite, "Synergy SA", RegDate.get(2010, 6, 24), null, FormeLegale.N_0107_SOCIETE_A_RESPONSABILITE_LIMITE,
						                                           TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Lausanne.getNoOFS(), StatusRC.INSCRIT, StatusInscriptionRC.RADIE,
						                                           StatusRegistreIDE.RADIE,
						                                           TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, BigDecimal.valueOf(50000), "CHF");
				MockDonneesRC rc = (MockDonneesRC) organisation.getDonneesSites().get(0).getDonneesRC();
				rc.changeStatusInscription(RegDate.get(2012, 1, 26), StatusInscriptionRC.RADIE);
				rc.changeStatusInscription(RegDate.get(2015, 7, 5), StatusInscriptionRC.PROVISOIRE);
				rc.changeDateRadiation(RegDate.get(2012, 1, 26), RegDate.get(2012, 1, 26));
				rc.changeDateRadiation(RegDate.get(2015, 7, 5), null);
				MockDonneesRegistreIDE donneesRegistreIDE = (MockDonneesRegistreIDE) organisation.getDonneesSites().get(0).getDonneesRegistreIDE();
				donneesRegistreIDE.changeStatus(RegDate.get(2012, 1, 26), StatusRegistreIDE.RADIE);
				donneesRegistreIDE.changeStatus(RegDate.get(2015, 7, 5), StatusRegistreIDE.DEFINITIF);
				addOrganisation(organisation);
			}
		});

		// Création de l'entreprise

		final Entreprise entreprise = doInNewTransactionAndSession(new TransactionCallback<Entreprise>() {
			@Override
			public Entreprise doInTransaction(TransactionStatus transactionStatus) {
				final Entreprise entreprise = addEntrepriseConnueAuCivil(noOrganisation);
				addForPrincipal(entreprise, RegDate.get(2010, 6, 25), MotifFor.DEBUT_EXPLOITATION, RegDate.get(2012, 1, 1), MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.DOMICILE);
				return entreprise;
			}
		});

		// Création de l'événement
		final Long evtId = 12344321L;

		// Persistence événement
		final EvenementOrganisation evt = doInNewTransactionAndSession(new TransactionCallback<EvenementOrganisation>() {
			@Override
			public EvenementOrganisation doInTransaction(TransactionStatus transactionStatus) {
				final EvenementOrganisation event =
						createEvent(evtId, noOrganisation, TypeEvenementOrganisation.FOSC_APPEL_AUX_CREANCIERS_DANS_CONCORDAT, RegDate.get(2015, 7, 5), A_VERIFIER, FOSC, "rcent-ut");
				return hibernateTemplate.merge(event);
			}
		});

		processor.forceEvenement(new EvenementOrganisationBasicInfo(evt, evt.getNoOrganisation()));


		// Vérification du traitement de l'événement
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			                             @Override
			                             public Object doInTransaction(TransactionStatus status) {

				                             final EvenementOrganisation evt = evtOrganisationDAO.get(evtId);
				                             Assert.assertNotNull(evt);
				                             Assert.assertEquals(EtatEvenementOrganisation.FORCE, evt.getEtat());

				                             final Entreprise entreprise = tiersDAO.getEntrepriseByNumeroOrganisation(evt.getNoOrganisation());

				                             // Si la réinscription devait être traitée et non forcée, on trouverait un for.
				                             Assert.assertEquals(0, entreprise.getForsFiscauxValidAt(RegDate.get(2015, 7, 5)).size());

				                             // vérification des événements fiscaux
				                             final List<EvenementFiscal> evtsFiscaux = evtFiscalDAO.getAll();
				                             Assert.assertNotNull(evtsFiscaux);
				                             // Si la réinscription devait être traitée et non forcée, on trouverait deux événements de for en plus de
				                             // celui d'information.
				                             Assert.assertEquals(0, evtsFiscaux.size());

				                             return null;
			                             }
		                             }
		);
	}
}
