package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.Fuse;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.EvenementReqDesDAO;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.ModeInscription;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TransactionImmobiliere;
import ch.vd.uniregctb.reqdes.TypeInscription;
import ch.vd.uniregctb.reqdes.TypeRole;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.dao.RemarqueDAO;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;

public class EvenementReqDesProcessorTest extends BusinessTest {

	private EvenementReqDesProcessorImpl processor;
	private UniteTraitementDAO uniteTraitementDAO;
	private EvenementReqDesDAO evenementReqDesDAO;
	private RemarqueDAO remarqueDAO;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		uniteTraitementDAO = getBean(UniteTraitementDAO.class, "reqdesUniteTraitementDAO");
		evenementReqDesDAO = getBean(EvenementReqDesDAO.class, "reqdesEvenementDAO");
		remarqueDAO = getBean(RemarqueDAO.class, "remarqueDAO");

		processor = new EvenementReqDesProcessorImpl();
		processor.setHibernateTemplate(hibernateTemplate);
		processor.setInfraService(serviceInfra);
		processor.setTransactionManager(transactionManager);
		processor.setUniteTraitementDAO(uniteTraitementDAO);
		processor.setTiersService(tiersService);
		processor.setAdresseService(getBean(AdresseService.class, "adresseService"));
		processor.setAssujettissementService(getBean(AssujettissementService.class, "assujettissementService"));
		processor.afterPropertiesSet();
	}

	@Override
	public void onTearDown() throws Exception {
		if (processor != null) {
			processor.destroy();
			processor = null;
		}
		super.onTearDown();
	}

	/**
	 * Méthode de traitement synchrone d'une unité de traitement indiquée par son identifiant
	 * @param id identifiant de l'unité de traitement à traiter
	 * @throws InterruptedException si le thread est interrompu
	 */
	protected void traiteUniteTraitement(final long id) throws InterruptedException {

		// le fusible saute dès le traitement terminé
		final Fuse done = new Fuse();
		final EvenementReqDesProcessor.ListenerHandle handle = processor.registerListener(new EvenementReqDesProcessor.Listener() {
			@Override
			public void onUniteTraite(long idUniteTraitement) {
				if (idUniteTraitement == id) {
					done.blow();
				}
			}

			@Override
			public void onStop() {
				done.blow();
			}
		});

		try {
			processor.postUniteTraitement(id);

			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (done) {
				// tant que le traitement n'est pas terminé, on attend...
				while (!done.isBlown()) {
					done.wait();
				}
			}
		}
		finally {
			processor.unregisterListener(handle);
		}
	}

	protected EvenementReqDes addEvenementReqDes(InformationsActeur notaire, @Nullable InformationsActeur operateur, RegDate dateActe, String noMinute) {
		final EvenementReqDes evt = new EvenementReqDes();
		evt.setNotaire(notaire);
		evt.setOperateur(operateur);
		evt.setDateActe(dateActe);
		evt.setNumeroMinute(noMinute);
		evt.setXml("<tubidu/>");
		return evenementReqDesDAO.save(evt);
	}

	protected UniteTraitement addUniteTraitement(EvenementReqDes evt, EtatTraitement etat, @Nullable Date dateTraitement) {
		final UniteTraitement ut = new UniteTraitement();
		ut.setEvenement(evt);
		ut.setDateTraitement(dateTraitement);
		ut.setEtat(etat);
		return uniteTraitementDAO.save(ut);
	}

	protected PartiePrenante addPartiePrenante(UniteTraitement ut, String nom, String prenoms) {
		final PartiePrenante pp = new PartiePrenante();
		pp.setUniteTraitement(ut);
		pp.setNom(nom);
		pp.setPrenoms(prenoms);
		return hibernateTemplate.merge(pp);
	}

	protected TransactionImmobiliere addTransactionImmobiliere(EvenementReqDes evt, String description, ModeInscription modeInscription, TypeInscription typeInscription, int noOfsCommune) {
		final TransactionImmobiliere ti = new TransactionImmobiliere();
		ti.setEvenementReqDes(evt);
		ti.setDescription(description);
		ti.setModeInscription(modeInscription);
		ti.setTypeInscription(typeInscription);
		ti.setOfsCommune(noOfsCommune);
		return hibernateTemplate.merge(ti);
	}

	protected RolePartiePrenante addRole(PartiePrenante pp, TransactionImmobiliere transactionImmobiliere, TypeRole typeRole) {
		final RolePartiePrenante role = new RolePartiePrenante();
		role.setTransaction(transactionImmobiliere);
		role.setRole(typeRole);
		addRole(pp, role);
		return role;
	}

	protected void addRole(PartiePrenante pp, RolePartiePrenante role) {
		if (pp.getRoles() == null) {
			pp.setRoles(new HashSet<RolePartiePrenante>());
		}
		pp.getRoles().add(role);
	}

	@Test(timeout = 10000)
	public void testDateFuture() throws Exception {

		// mise en place d'une unité de traitement bidon dont l'événement a une date à demain
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, RegDate.get().getOneDayAfter(), "56754K");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final ErreurTraitement erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("La date de l'acte est dans le futur.", erreur.getMessage());
			}
		});
	}

	@Test(timeout = 10000)
	public void testControlesPreliminaires() throws Exception {

		final RegDate dateActe = RegDate.get();

		final class Ids {
			long utResidentVaudois;
			long utResidentCommuneInconnue;
		}

		// mise en place d'une unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
				final TransactionImmobiliere t1 = addTransactionImmobiliere(evt, "Bonne commune", ModeInscription.INSCRIPTION, TypeInscription.CHARGE_FONCIERE, MockCommune.Lausanne.getNoOFS());
				final TransactionImmobiliere t2 = addTransactionImmobiliere(evt, "Commune inconnue", ModeInscription.MODIFICATION, TypeInscription.ANNOTATION, 9999);
				final TransactionImmobiliere t3 = addTransactionImmobiliere(evt, "Commune faîtiere", ModeInscription.RADIATION, TypeInscription.PROPRIETE, MockCommune.LeChenit.getNoOFS());

				final UniteTraitement ut1 = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut1, "Résident", "Vaudois");
				pp1.setOfsCommune(MockCommune.Echallens.getNoOFS());
				pp1.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp1.setNomConjoint("Conjoint");
				addRole(pp1, t1, TypeRole.ACQUEREUR);

				final UniteTraitement ut2 = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp2 = addPartiePrenante(ut2, "Résident", "Commune inconnue");
				pp2.setOfsCommune(9999);
				pp2.setEtatCivil(EtatCivil.MARIE);
				pp2.setNomConjoint("Conjoint marié");
				addRole(pp2, t1, TypeRole.ALIENATEUR);
				addRole(pp2, t2, TypeRole.AUTRE);
				addRole(pp2, t3, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.utResidentVaudois = ut1.getId();
				ids.utResidentCommuneInconnue = ut2.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utResidentVaudois);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement autreUT = uniteTraitementDAO.get(ids.utResidentCommuneInconnue);
				Assert.assertNotNull(autreUT);
				Assert.assertEquals(EtatTraitement.A_TRAITER, autreUT.getEtat());
				Assert.assertNull(autreUT.getDateTraitement());
				Assert.assertEquals(0, autreUT.getErreurs().size());

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utResidentVaudois);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(2, erreurs.size());

				final List<ErreurTraitement> sorted = new ArrayList<>(erreurs);
				Collections.sort(sorted, new Comparator<ErreurTraitement>() {
					@Override
					public int compare(ErreurTraitement o1, ErreurTraitement o2) {
						return o1.getMessage().compareTo(o2.getMessage());
					}
				});

				{
					final ErreurTraitement erreur = sorted.get(0);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("Incohérence entre l'état civil (Célibataire) et la présence d'un conjoint.", erreur.getMessage());
				}
				{
					final ErreurTraitement erreur = sorted.get(1);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("La commune de résidence (Echallens/5518) est vaudoise.", erreur.getMessage());
				}
			}
		});

		// traitement de l'unité suivante
		traiteUniteTraitement(ids.utResidentCommuneInconnue);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utResidentCommuneInconnue);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(2, erreurs.size());

				final List<ErreurTraitement> sorted = new ArrayList<>(erreurs);
				Collections.sort(sorted, new Comparator<ErreurTraitement>() {
					@Override
					public int compare(ErreurTraitement o1, ErreurTraitement o2) {
						return o1.getMessage().compareTo(o2.getMessage());
					}
				});

				{
					final ErreurTraitement erreur = sorted.get(0);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("Commune 9999 inconnue au " + RegDateHelper.dateToDisplayString(dateActe) + ".", erreur.getMessage());
				}
				{
					final ErreurTraitement erreur = sorted.get(1);
					Assert.assertNotNull(erreur);
					Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
					Assert.assertEquals("La commune 'Le Chenit' (5872) est une commune faîtière de fractions.", erreur.getMessage());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testControlesPreliminairesSurLiensMatrimoniaux() throws Exception {

		final RegDate dateActe = RegDate.get().getOneDayBefore();

		// mise en place d'un lien matrimonial unidirectionel
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "Petit", "Bonhomme");
				final PartiePrenante pp2 = addPartiePrenante(ut, "Grand", "Garçon");
				pp1.setConjointPartiePrenante(pp2);
				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification des erreurs
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final ErreurTraitement erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("Liens matrimoniaux incohérents entres les parties prenantes.", erreur.getMessage());
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationContribuableCelibataire() throws Exception {

		final RegDate dateNaissance = date(1976, 4, 23);
		final RegDate today = RegDate.get();
		final RegDate dateActe = today.addDays(-5);

		// mise en place d'une création de contribuable
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), null, dateActe, "3783");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Incaunu Jean Albert");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.MASCULIN);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);

				pp.setDateEtatCivil(dateNaissance);
				pp.setDateNaissance(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");

				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification de l'existence de ce nouveau contribuable
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());

				final List<Tiers> allTiers = tiersDAO.getAll();
				PersonnePhysique found = null;
				for (Tiers tiers : allTiers) {
					if (tiers instanceof PersonnePhysique) {
						found = (PersonnePhysique) tiers;
						break;
					}
				}

				Assert.assertNotNull(found);
				Assert.assertEquals("moinot-reqdes", found.getLogCreationUser());
				Assert.assertEquals("moinot-reqdes", found.getLogModifUser());
				Assert.assertEquals("O'Batayon", found.getNom());
				Assert.assertEquals("Incaunu", found.getPrenomUsuel());
				Assert.assertEquals("Incaunu Jean Albert", found.getTousPrenoms());
				Assert.assertEquals("Delaplanche", found.getNomMere());
				Assert.assertEquals("Martine Sophie Mafalda", found.getPrenomsMere());
				Assert.assertEquals("O'Batayon", found.getNomPere());
				Assert.assertEquals("Iain François Robert", found.getPrenomsPere());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), found.getNumeroOfsNationalite());
				Assert.assertEquals(Sexe.MASCULIN, found.getSexe());
				Assert.assertEquals(dateNaissance, found.getDateNaissance());
				Assert.assertNull(found.getDateDeces());
				Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, found.getCategorieEtranger());
				Assert.assertNull(found.getDateDebutValiditeAutorisation());

				final Set<AdresseTiers> adresses = found.getAdressesTiers();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());

				final AdresseTiers adresse = adresses.iterator().next();
				Assert.assertNotNull(adresse);
				Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

				final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
				Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
				Assert.assertEquals("77415 Meulin", adresseEtrangere.getNumeroPostalLocalite());
				Assert.assertEquals("Rue de la porte en bois", adresseEtrangere.getRue());
				Assert.assertEquals("13b", adresseEtrangere.getNumeroMaison());
				Assert.assertNull(adresseEtrangere.getTexteCasePostale());
				Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

				final List<Remarque> remarques = remarqueDAO.getRemarques(found.getNumero());
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());

				final Remarque remarque = remarques.get(0);
				Assert.assertNotNull(remarque);
				Assert.assertEquals("Contribuable créé le " + RegDateHelper.dateToDisplayString(today) + " par l'acte notarial du " + RegDateHelper.dateToDisplayString(dateActe)
						                    + " par le notaire Tranquille Petiboulot (moinot) et enregistré par lui-même.", remarque.getTexte());
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCelibataireNonAssujetti() throws Exception {

		final RegDate dateNaissance = date(1976, 4, 23);
		final RegDate today = RegDate.get();
		final RegDate dateActe = today.addDays(-5);

		// mise en place d'une création de contribuable
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pphysique = addNonHabitant("Inconnu", "Aubataillon", date(dateNaissance.year()), Sexe.FEMININ);
				pphysique.setNomMere("Delaplanche");
				pphysique.setPrenomsMere("Martine");

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), null, dateActe, "3783");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Incaunu Jean Albert");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.MASCULIN);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp.setNumeroContribuable(pphysique.getNumero());

				pp.setDateEtatCivil(dateNaissance);
				pp.setDateNaissance(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");

				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification de l'existence de ce nouveau contribuable
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				PersonnePhysique found = null;
				for (Tiers tiers : allTiers) {
					if (tiers instanceof PersonnePhysique) {
						found = (PersonnePhysique) tiers;
						break;
					}
				}

				Assert.assertNotNull(found);
				Assert.assertEquals(getDefaultOperateurName(), found.getLogCreationUser());
				Assert.assertEquals("moinot-reqdes", found.getLogModifUser());
				Assert.assertEquals("O'Batayon", found.getNom());
				Assert.assertEquals("Incaunu", found.getPrenomUsuel());
				Assert.assertEquals("Incaunu Jean Albert", found.getTousPrenoms());
				Assert.assertEquals("Delaplanche", found.getNomMere());
				Assert.assertEquals("Martine Sophie Mafalda", found.getPrenomsMere());
				Assert.assertEquals("O'Batayon", found.getNomPere());
				Assert.assertEquals("Iain François Robert", found.getPrenomsPere());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), found.getNumeroOfsNationalite());
				Assert.assertEquals(Sexe.MASCULIN, found.getSexe());
				Assert.assertEquals(dateNaissance, found.getDateNaissance());
				Assert.assertNull(found.getDateDeces());
				Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, found.getCategorieEtranger());
				Assert.assertNull(found.getDateDebutValiditeAutorisation());

				final Set<AdresseTiers> adresses = found.getAdressesTiers();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());

				final AdresseTiers adresse = adresses.iterator().next();
				Assert.assertNotNull(adresse);
				Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

				final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
				Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
				Assert.assertEquals("77415 Meulin", adresseEtrangere.getNumeroPostalLocalite());
				Assert.assertEquals("Rue de la porte en bois", adresseEtrangere.getRue());
				Assert.assertEquals("13b", adresseEtrangere.getNumeroMaison());
				Assert.assertNull(adresseEtrangere.getTexteCasePostale());
				Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

				final List<Remarque> remarques = remarqueDAO.getRemarques(found.getNumero());
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());

				final Remarque remarque = remarques.get(0);
				Assert.assertNotNull(remarque);

				final StringBuilder b = new StringBuilder();
				b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
				b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
				b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par lui-même.");
				b.append("\n- Nom : \"Aubataillon\" -> \"O'Batayon\"");
				b.append("\n- Prénoms : vide -> \"Incaunu Jean Albert\"");
				b.append("\n- Prénom usuel : \"Inconnu\" -> \"Incaunu\"");
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.name());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Prénoms de la mère : \"Martine\" -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNoOFS());
				b.append("\n- Sexe : ").append(Sexe.FEMININ.name()).append(" -> ").append(Sexe.MASCULIN.name());
				final String expected = b.toString();

				Assert.assertEquals(expected, remarque.getTexte());
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCelibataireAssujettiSource() throws Exception {

		final RegDate dateNaissance = date(1976, 4, 23);
		final RegDate today = RegDate.get();
		final RegDate dateActe = today.addDays(-5);

		// mise en place d'une création de contribuable
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pphysique = addNonHabitant("Inconnu", "Aubataillon", date(dateNaissance.year()), Sexe.MASCULIN);
				addForPrincipal(pphysique, dateNaissance.addYears(18), MotifFor.MAJORITE, null, null, MockPays.France, ModeImposition.SOURCE);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), null, dateActe, "3783");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Incaunu Jean Albert");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.MASCULIN);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp.setNumeroContribuable(pphysique.getNumero());

				pp.setDateEtatCivil(dateNaissance);
				pp.setDateNaissance(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");

				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification de l'existence de ce nouveau contribuable
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				PersonnePhysique found = null;
				for (Tiers tiers : allTiers) {
					if (tiers instanceof PersonnePhysique) {
						found = (PersonnePhysique) tiers;
						break;
					}
				}

				Assert.assertNotNull(found);
				Assert.assertEquals(getDefaultOperateurName(), found.getLogCreationUser());
				Assert.assertEquals("moinot-reqdes", found.getLogModifUser());
				Assert.assertEquals("O'Batayon", found.getNom());
				Assert.assertEquals("Incaunu", found.getPrenomUsuel());
				Assert.assertEquals("Incaunu Jean Albert", found.getTousPrenoms());
				Assert.assertEquals("Delaplanche", found.getNomMere());
				Assert.assertEquals("Martine Sophie Mafalda", found.getPrenomsMere());
				Assert.assertEquals("O'Batayon", found.getNomPere());
				Assert.assertEquals("Iain François Robert", found.getPrenomsPere());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), found.getNumeroOfsNationalite());
				Assert.assertEquals(Sexe.MASCULIN, found.getSexe());
				Assert.assertEquals(dateNaissance, found.getDateNaissance());
				Assert.assertNull(found.getDateDeces());
				Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, found.getCategorieEtranger());
				Assert.assertNull(found.getDateDebutValiditeAutorisation());

				final Set<AdresseTiers> adresses = found.getAdressesTiers();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(1, adresses.size());

				final AdresseTiers adresse = adresses.iterator().next();
				Assert.assertNotNull(adresse);
				Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

				final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
				Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
				Assert.assertEquals("77415 Meulin", adresseEtrangere.getNumeroPostalLocalite());
				Assert.assertEquals("Rue de la porte en bois", adresseEtrangere.getRue());
				Assert.assertEquals("13b", adresseEtrangere.getNumeroMaison());
				Assert.assertNull(adresseEtrangere.getTexteCasePostale());
				Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

				final List<Remarque> remarques = remarqueDAO.getRemarques(found.getNumero());
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());

				final Remarque remarque = remarques.get(0);
				Assert.assertNotNull(remarque);

				final StringBuilder b = new StringBuilder();
				b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
				b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
				b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par lui-même.");
				b.append("\n- Nom : \"Aubataillon\" -> \"O'Batayon\"");
				b.append("\n- Prénoms : vide -> \"Incaunu Jean Albert\"");
				b.append("\n- Prénom usuel : \"Inconnu\" -> \"Incaunu\"");
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.name());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
				b.append("\n- Prénoms de la mère : vide -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNoOFS());
				final String expected = b.toString();

				Assert.assertEquals(expected, remarque.getTexte());
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCelibataireAssujettiRole() throws Exception {

		final RegDate dateNaissance = date(1976, 4, 23);
		final RegDate today = RegDate.get();
		final RegDate dateAchatPrecedent = date(2005, 3, 12);
		final RegDate dateActe = today.addDays(-5);

		// mise en place d'une création de contribuable
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique pphysique = addNonHabitant("Inconnu", "Aubataillon", date(dateNaissance.year()), Sexe.MASCULIN);
				addForPrincipal(pphysique, dateAchatPrecedent, MotifFor.ACHAT_IMMOBILIER, MockPays.France);
				addForSecondaire(pphysique, dateAchatPrecedent, MotifFor.ACHAT_IMMOBILIER, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), null, dateActe, "3783");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Incaunu Jean Albert");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.MASCULIN);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp.setNumeroContribuable(pphysique.getNumero());

				pp.setDateEtatCivil(dateNaissance);
				pp.setDateNaissance(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");

				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification de l'existence de ce nouveau contribuable
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.A_VERIFIER, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.WARNING, erreur.getType());
				Assert.assertEquals("Adresse non modifiée sur un contribuable assujetti, uniquement reprise dans les remarques du tiers.", erreur.getMessage());

				final List<Tiers> allTiers = tiersDAO.getAll();
				PersonnePhysique found = null;
				for (Tiers tiers : allTiers) {
					if (tiers instanceof PersonnePhysique) {
						found = (PersonnePhysique) tiers;
						break;
					}
				}

				Assert.assertNotNull(found);
				Assert.assertEquals(getDefaultOperateurName(), found.getLogCreationUser());
				Assert.assertEquals("moinot-reqdes", found.getLogModifUser());
				Assert.assertEquals("O'Batayon", found.getNom());
				Assert.assertEquals("Incaunu", found.getPrenomUsuel());
				Assert.assertEquals("Incaunu Jean Albert", found.getTousPrenoms());
				Assert.assertEquals("Delaplanche", found.getNomMere());
				Assert.assertEquals("Martine Sophie Mafalda", found.getPrenomsMere());
				Assert.assertEquals("O'Batayon", found.getNomPere());
				Assert.assertEquals("Iain François Robert", found.getPrenomsPere());
				Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), found.getNumeroOfsNationalite());
				Assert.assertEquals(Sexe.MASCULIN, found.getSexe());
				Assert.assertEquals(dateNaissance, found.getDateNaissance());
				Assert.assertNull(found.getDateDeces());
				Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, found.getCategorieEtranger());
				Assert.assertNull(found.getDateDebutValiditeAutorisation());

				final Set<AdresseTiers> adresses = found.getAdressesTiers();
				Assert.assertNotNull(adresses);
				Assert.assertEquals(0, adresses.size());

				final List<Remarque> remarques = remarqueDAO.getRemarques(found.getNumero());
				Assert.assertNotNull(remarques);
				Assert.assertEquals(1, remarques.size());

				final Remarque remarque = remarques.get(0);
				Assert.assertNotNull(remarque);

				final StringBuilder b = new StringBuilder();
				b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
				b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
				b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par lui-même.");
				b.append("\n- Nom : \"Aubataillon\" -> \"O'Batayon\"");
				b.append("\n- Prénoms : vide -> \"Incaunu Jean Albert\"");
				b.append("\n- Prénom usuel : \"Inconnu\" -> \"Incaunu\"");
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.name());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
				b.append("\n- Prénoms de la mère : vide -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNoOFS());
				b.append("\n- Adresse transmise non enregistrée : Rue de la porte en bois 13b / 77415 Meulin / France");
				final String expected = b.toString();

				Assert.assertEquals(expected, remarque.getTexte());
			}
		});
	}
}
