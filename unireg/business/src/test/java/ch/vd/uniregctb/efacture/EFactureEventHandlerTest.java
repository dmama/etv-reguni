package ch.vd.uniregctb.efacture;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class EFactureEventHandlerTest extends BusinessTest {

	private EFactureEventHandlerImpl handler;
	private EFactureServiceProxy eFactureService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		eFactureService = new EFactureServiceProxy();
		handler = new EFactureEventHandlerImpl();
		handler.setAdresseService(adresseService);
		handler.setTiersService(tiersService);
		handler.seteFactureService(eFactureService);
	}

	@Test
	public void testDemandeInscriptionAttenteSignature() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
	}

	@Test
	public void testDemandeInscriptionSourcier() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_CONTACT, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT.getDescription() + " Assujettissement incohérent avec la e-facture.", etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionDesinscritSuspendu() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addEtatDestinataire(ppId, DateHelper.getCurrentDate(), "Suspendu... pas gentil!", null, TypeEtatDestinataire.DESINSCRIT_SUSPENDU);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT_SUSPENDU, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_CONTACT, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT.getDescription() + " Assujettissement incohérent avec la e-facture.", etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionDejaInvalideDepuisEnvoi() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				final DemandeAvecHisto demande = addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
				addEtatDemande(demande, DateHelper.getCurrentDate(), null, "Désinscription immédiate demandée... Tralalère!", TypeEtatDemande.IGNOREE);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		// demande ignorée par le traitement -> rien n'a bougé!
		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.IGNOREE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals("Désinscription immédiate demandée... Tralalère!", etatDemande.getDescriptionRaison());     // même texte bidon que celui vu plus haut... (= même état)
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNoAvsDifferent() throws Exception {

		final String noAvsUnireg = "7564822568443";
		final String noAvsDemande = "7566721626798";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvsUnireg);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionMenage() throws Exception {

		final String noAvs1 = "7564822568443";
		final String noAvs2 = "7566721626798";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

				elle.setNumeroAssureSocial(noAvs1);
				lui.setNumeroAssureSocial(noAvs2);
				return mc.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvs1, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvs1);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
	}

	@Test
	public void testDemandeInscriptionNoAvsInconnu() throws Exception {

		final String noAvsUnireg = null;
		final String noAvsDemande = "7566721626798";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvsUnireg);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
	}

	@Test
	public void testDemandeInscriptionMenageAvecNavsConjointInconnu() throws Exception {

		final String noAvs1 = null;
		final String noAvs2 = "7566721626798";
		final String noAvsDemande = "7564854782886";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

				elle.setNumeroAssureSocial(noAvs1);
				lui.setNumeroAssureSocial(noAvs2);
				return mc.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
	}

	@Test
	public void testDemandeInscriptionMenageAvecNavsPrincipalInconnu() throws Exception {

		final String noAvs1 = null;
		final String noAvs2 = "7566721626798";
		final String noAvsDemande = "7564854782886";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

				lui.setNumeroAssureSocial(noAvs1);
				elle.setNumeroAssureSocial(noAvs2);
				return mc.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
	}

	@Test
	public void testDemandeInscriptionMenageAvecNavsDifferent() throws Exception {

		final String noAvs1 = "7562765456179";
		final String noAvs2 = "7566721626798";
		final String noAvsDemande = "7564854782886";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
				final MenageCommun mc = couple.getMenage();

				addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

				elle.setNumeroAssureSocial(noAvs1);
				lui.setNumeroAssureSocial(noAvs2);
				return mc.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNoAvsInvalide() throws Exception {

		final String noAvs = "7564822568441";       // normalement, le dernier chiffre devrait être un 3 ici
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_INVALIDE.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionDateAbsente() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = null;
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.DATE_DEMANDE_ABSENTE.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionAutreDemandeEnCours() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription("41", ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(2, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(1);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionContribuableInconnu() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final long ctbId = 10000025;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ctbId);
				addDemandeInscription(demandeId, ctbId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ctbId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ctbId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_CTB_INCOHERENT.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionAdresseCourrierInexistante() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
				addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				pp.setNumeroAssureSocial(noAvs);
				return pp.getNumero();
			}
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS);
			}
		});

		// traitement de la demande d'inscription
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs);
				handler.handle(demande);
				return null;
			}
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertNull(etatDemande.getDescriptionRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE.getDescription(), etatDemande.getChampLibre());
	}

}
