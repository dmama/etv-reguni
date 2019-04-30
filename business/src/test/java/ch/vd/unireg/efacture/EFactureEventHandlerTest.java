package ch.vd.unireg.efacture;

import java.math.BigInteger;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.EtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseTiers;

public class EFactureEventHandlerTest extends BusinessTest {

	private EFactureEventHandlerImpl handler;
	private EFactureServiceProxy eFactureService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final AdresseService adresseService = getBean(AdresseService.class, "adresseService");
		eFactureService = new EFactureServiceProxy();
		handler = new EFactureEventHandlerImpl();
		handler.setAdresseService(adresseService);
		handler.setTiersService(tiersService);
		handler.seteFactureService(eFactureService);
	}

	protected static BigInteger getNewNumeroAdherent() {
		final BigInteger min = BigInteger.valueOf(10000000000000000L);      // 17 chiffres!
		final BigInteger max = BigInteger.valueOf(99999999999999999L);
		final BigInteger len = max.subtract(min).add(BigInteger.ONE);
		final byte[] arrayMuster = len.toByteArray();

		final Random rnd = new Random();
		final byte[] newArray = new byte[arrayMuster.length];
		rnd.nextBytes(newArray);
		return min.add(new BigInteger(newArray).abs().mod(len));
	}

	/**
	 * Méthode à appeler dans ce test pour la gestion des transactions (gestion des "transactions" dans les actions efacture asynchrones demandées)
	 * @param callback le code à exécuter dans la transaction
	 * @param <T> le type de retour
	 * @return la valeur retournée par le callback
	 */
	private <T> T doInEFactureAwareTransaction(final TransactionCallback<T> callback) throws Exception {
		boolean committed = false;
		try {
			final T res = doInNewTransactionAndSession(callback);
			committed = true;
			return res;
		}
		finally {
			if (committed) {
				eFactureService.commit();
			}
			else {
				eFactureService.rollback();
			}
		}
	}

	@Test
	public void testDemandeInscriptionAttenteSignature() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addEtatDestinataire(ppId, DateHelper.getCurrentDate(), "Suspendu... pas gentil!", null, TypeEtatDestinataire.DESINSCRIT_SUSPENDU, null, null);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
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
	public void testDemandeInscriptionNonInscritSuspendu() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addEtatDestinataire(ppId, DateHelper.getCurrentDate(), "Suspendu... pas gentil!", null, TypeEtatDestinataire.NON_INSCRIT_SUSPENDU, null, null);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT_SUSPENDU, histo.getDernierEtat().getType());
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
	public void testDemandeInscriptionInscritSuspendu() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addEtatDestinataire(ppId, DateHelper.getCurrentDate(), "Suspendu... pas gentil!", null, TypeEtatDestinataire.INSCRIT_SUSPENDU, "albert@dufoin.ch", getNewNumeroAdherent());
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.DESINSCRIT_SUSPENDU, histo.getDernierEtat().getType());        // désinscription automatique
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				final DemandeAvecHisto demande = addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
				addEtatDemande(demande, DateHelper.getCurrentDate(), null, "Désinscription immédiate demandée... Tralalère!", TypeEtatDemande.IGNOREE);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvsUnireg);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionMenage() throws Exception {

		final String noAvs1 = "7564822568443";
		final String noAvs2 = "7566721626798";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

			elle.setNumeroAssureSocial(noAvs1);
			lui.setNumeroAssureSocial(noAvs2);
			return mc.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvs1, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvs1, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvsUnireg);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvsDemande, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

			elle.setNumeroAssureSocial(noAvs1);
			lui.setNumeroAssureSocial(noAvs2);
			return mc.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

			lui.setNumeroAssureSocial(noAvs1);
			elle.setNumeroAssureSocial(noAvs2);
			return mc.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
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
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Françoise", "Dufoin", null, Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2010, 1, 1), null);
			final MenageCommun mc = couple.getMenage();

			addForPrincipal(mc, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(lui, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);

			elle.setNumeroAssureSocial(noAvs1);
			lui.setNumeroAssureSocial(noAvs2);
			return mc.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(mcId);
				addDemandeInscription(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, mcId, email, dateDemande, typeDemande, noAvsDemande, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(mcId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNoAvsInvalide() throws Exception {

		final String noAvs = "7564822568441";       // normalement, le dernier chiffre devrait être un 3 ici
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_INVALIDE.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionDateAbsente() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = null;
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.DATE_DEMANDE_ABSENTE.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionAutreDemandeEnCours() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addAdresseSuisse(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 13), null, MockRue.Lausanne.CheminPrazBerthoud);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription("41", ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, noAdherent);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(2, histo.getHistoriqueDemandes().size());

		// la nouvelle demande doit avoir été acceptée
		{
			final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(1);
			Assert.assertEquals(demandeId, demande.getIdDemande());

			final EtatDemande etatDemande = demande.getDernierEtat();
			Assert.assertNotNull(etatDemande);
			Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
			Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
			Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
		}

		// l'ancienne demande, quant à elle, doit être annulée
		{
			final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
			final EtatDemande etatDemande = demande.getDernierEtat();
			Assert.assertNotNull(etatDemande);
			Assert.assertEquals(TypeEtatDemande.ANNULEE, etatDemande.getType());
			Assert.assertEquals("Traitement d'une nouvelle demande d'inscription.", etatDemande.getDescriptionRaison());
		}
	}

	@Test
	public void testDemandeInscriptionContribuableInconnu() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final long ctbId = 10000025;
		final BigInteger noAdherent = getNewNumeroAdherent();

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
				addDemandeInscription(demandeId, ctbId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ctbId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ctbId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_CTB_INCOHERENT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionAdresseCourrierInexistante() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNumeroSecuriteSocialePourContribuableVaudois() throws Exception {

		final String noAvs = "7564822568443";
		final String noSecu = "54843678543654";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noSecu, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noSecu, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_SECU_SANS_FOR_PRINCIPAL_HS.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNumeroSecuriteSocialePourContribuableResidentHorsCanton() throws Exception {

		final String noAvs = "7564822568443";
		final String noSecu = "54843678543654";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Bale);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noSecu, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noSecu, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_SECU_SANS_FOR_PRINCIPAL_HS.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNumeroSecuriteSocialePourContribuableResidentHorsSuisse() throws Exception {

		final String noAvs = "7564822568443";
		final String noSecu = "54843678543654";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.INDETERMINE, MockPays.France);
			pp.setNumeroAssureSocial(noAvs);

			addAdresseEtrangere(pp, TypeAdresseTiers.COURRIER, date(2010, 3, 12), null, "35 rue des alouettes", "63000 Clermont-Ferrand", MockPays.France);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, noSecu, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, noSecu, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.VALIDATION_EN_COURS_EN_ATTENTE_SIGNATURE, etatDemande.getType());
		Assert.assertEquals((Integer) TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(), etatDemande.getCodeRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionNumeroAvsOuSecuSocialeAbsent() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final String email = "albert@dufoin.ch";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, email, dateDemande, typeDemande, null, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, email, dateDemande, typeDemande, null, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.NUMERO_AVS_OU_SECURITE_SOCIALE_ABSENT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

	@Test
	public void testDemandeInscriptionAdresseElectroniqueAbsente() throws Exception {

		final String noAvs = "7564822568443";
		final String demandeId = "42";
		final RegDate dateDemande = RegDate.get();
		final Demande.Action typeDemande = Demande.Action.INSCRIPTION;
		final BigInteger noAdherent = getNewNumeroAdherent();

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Dufoin", null, Sexe.MASCULIN);
			addForPrincipal(pp, date(2010, 3, 12), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			pp.setNumeroAssureSocial(noAvs);
			return pp.getNumero();
		});

		// mise en place...
		eFactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ppId);
				addDemandeInscription(demandeId, ppId, null, dateDemande, typeDemande, noAvs, TypeEtatDemande.VALIDATION_EN_COURS, noAdherent);
			}
		});

		// traitement de la demande d'inscription
		doInEFactureAwareTransaction(status -> {
			final Demande demande = new Demande(demandeId, ppId, null, dateDemande, typeDemande, noAvs, noAdherent);
			handler.handle(demande);
			return null;
		});

		// vérification état final e-facture
		final DestinataireAvecHisto histo = eFactureService.getDestinataireAvecSonHistorique(ppId);
		Assert.assertNotNull(histo);
		Assert.assertEquals(TypeEtatDestinataire.NON_INSCRIT, histo.getDernierEtat().getType());
		Assert.assertEquals(1, histo.getHistoriqueDemandes().size());

		final DemandeAvecHisto demande = histo.getHistoriqueDemandes().get(0);
		Assert.assertEquals(demandeId, demande.getIdDemande());

		final EtatDemande etatDemande = demande.getDernierEtat();
		Assert.assertNotNull(etatDemande);
		Assert.assertEquals(TypeEtatDemande.REFUSEE, etatDemande.getType());
		Assert.assertNull(etatDemande.getCodeRaison());
		Assert.assertEquals(EFactureEventHandlerImpl.TypeRefusDemande.EMAIL_ABSENT.getDescription(), etatDemande.getDescriptionRaison());
		Assert.assertNull(etatDemande.getChampLibre());
	}

}
