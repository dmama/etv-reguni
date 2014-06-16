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
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
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

		final RegDate dateActe = RegDate.get().getOneDayAfter();

		// mise en place d'une unité de traitement bidon dont l'événement a une date à demain
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
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
				Assert.assertEquals(String.format("La date de l'acte (%s) est dans le futur.", RegDateHelper.dateToDisplayString(dateActe)), erreur.getMessage());
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
	public void testControlesPreliminairesSurLiensMatrimoniauxUnidirectionel() throws Exception {

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
	public void testControlesPreliminairesSurLiensMatrimoniauxMarieAvecSoiMeme() throws Exception {

		final RegDate dateActe = RegDate.get().getOneDayBefore();

		// mise en place d'un lien matrimonial sur soi-même
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Francis"), null, dateActe, "56754K");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "Petit", "Bonhomme");
				pp1.setConjointPartiePrenante(pp1);
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
				Assert.assertEquals("Partie prenante mariée/pacsée avec elle-même !", erreur.getMessage());
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
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.name());
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
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.name());
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
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.name());
				final String expected = b.toString();

				Assert.assertEquals(expected, remarque.getTexte());
			}
		});
	}
	
	@Test(timeout = 10000)
	public void testCreationCompleteCouplePartiesPrenantes() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateMariage = date(1994, 5, 7);
		final RegDate dateActe = date(2014, 3, 12); 
		
		//  mise en place de l'unité de traitement
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), new InformationsActeur("petit", "Tabasco", "Albertine"), dateActe, "4845151");

				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "O'Batayon", "Marie Constantine");
				pp1.setNomMere("Delaplanche");
				pp1.setPrenomsMere("Martine Sophie Mafalda");
				pp1.setNomPere("O'Batayon");
				pp1.setPrenomsPere("Iain François Robert");
				pp1.setOfsPays(MockPays.France.getNoOFS());
				pp1.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp1.setRue("Rue de la porte en bois");
				pp1.setNumeroMaison("13b");
				pp1.setSexe(Sexe.FEMININ);
				pp1.setSourceCivile(false);
				pp1.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp1.setLocalite("Meulin");
				pp1.setNumeroPostal("77415");

				pp1.setDateEtatCivil(dateMariage);
				pp1.setDateNaissance(date(1965, 9, 27));
				pp1.setEtatCivil(EtatCivil.MARIE);
				
				final PartiePrenante pp2 = addPartiePrenante(ut, "Bataillard", "Arthur");
				pp2.setNomMere("Bataillard");
				pp2.setPrenomsMere("Françoise Alicia");
				pp2.setNomPere("Bataillard");
				pp2.setPrenomsPere("Francis Patrice");
				pp2.setOfsPays(MockPays.France.getNoOFS());
				pp2.setOfsPaysNationalite(MockPays.Allemagne.getNoOFS());
				pp2.setRue("Rue de la porte en bois");
				pp2.setNumeroMaison("13b");
				pp2.setSexe(Sexe.MASCULIN);
				pp2.setSourceCivile(false);
				pp2.setCategorieEtranger(CategorieEtranger._01_SAISONNIER_A);
				pp2.setLocalite("Meulin-Tatard");
				pp2.setNumeroPostal("77415");

				pp2.setDateEtatCivil(dateMariage);
				pp2.setDateNaissance(date(1965, 9, 28));
				pp2.setEtatCivil(EtatCivil.MARIE);

				// lien de conjoint
				pp1.setConjointPartiePrenante(pp2);
				pp2.setConjointPartiePrenante(pp1);

				// rôles
				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp1, ti, TypeRole.AUTRE);
				addRole(pp2, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(3, allTiers.size());        // 2 PP + 1 MC

				final List<Tiers> allSortedTiers = new ArrayList<>(allTiers);
				Collections.sort(allSortedTiers, new Comparator<Tiers>() {
					@Override
					public int compare(Tiers o1, Tiers o2) {
						int result = o1.getNatureTiers().compareTo(o2.getNatureTiers());
						if (result == 0) {
							// les seuls qui ont la même nature sont les deux personnes physiques
							final PersonnePhysique pp1 = (PersonnePhysique) o1;
							final PersonnePhysique pp2 = (PersonnePhysique) o2;
							result = pp1.getNom().compareTo(pp2.getNom());
						}
						return result;
					}
				});

				// et finalement, on obtient :
				// 1. Arthur Bataillard
				// 2. Marie O'Batayon
				// 3. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Bataillard", pp.getNom());
					Assert.assertEquals("Arthur", pp.getTousPrenoms());
					Assert.assertEquals("Arthur", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Bataillard", pp.getNomMere());
					Assert.assertEquals("Françoise Alicia", pp.getPrenomsMere());
					Assert.assertEquals("Bataillard", pp.getNomPere());
					Assert.assertEquals("Francis Patrice", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 28), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._01_SAISONNIER_A, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("77415 Meulin-Tatard", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en bois", adresseEtrangere.getRue());
					Assert.assertEquals("13b", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("O'Batayon", pp.getNom());
					Assert.assertEquals("Marie Constantine", pp.getTousPrenoms());
					Assert.assertEquals("Marie", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Delaplanche", pp.getNomMere());
					Assert.assertEquals("Martine Sophie Mafalda", pp.getPrenomsMere());
					Assert.assertEquals("O'Batayon", pp.getNomPere());
					Assert.assertEquals("Iain François Robert", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 27), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
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

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);

					Assert.assertEquals("petit-reqdes", mc.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", mc.getLogModifUser());

					final Set<AdresseTiers> adresses = mc.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(mc.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
					Assert.assertNotNull(couple);
					Assert.assertSame(allSortedTiers.get(0), couple.getPrincipal());
					Assert.assertSame(allSortedTiers.get(1), couple.getConjoint());

					// situation de famille
					final Set<SituationFamille> situations = mc.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(0, situations.size());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationCompleteCoupleAvecUnePartiePrenanteCivile() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateMariage = date(1994, 5, 7);
		final RegDate dateActe = date(2014, 3, 12);

		//  mise en place de l'unité de traitement
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), new InformationsActeur("petit", "Tabasco", "Albertine"), dateActe, "4845151");

				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "O'Batayon", "Marie Constantine");
				pp1.setNomMere("Delaplanche");
				pp1.setPrenomsMere("Martine Sophie Mafalda");
				pp1.setNomPere("O'Batayon");
				pp1.setPrenomsPere("Iain François Robert");
				pp1.setOfsPays(MockPays.France.getNoOFS());
				pp1.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp1.setRue("Rue de la porte en bois");
				pp1.setNumeroMaison("13b");
				pp1.setSexe(Sexe.FEMININ);
				pp1.setSourceCivile(false);
				pp1.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp1.setLocalite("Meulin");
				pp1.setNumeroPostal("77415");
				pp1.setSourceCivile(true);

				pp1.setDateEtatCivil(dateMariage);
				pp1.setDateNaissance(date(1965, 9, 27));
				pp1.setEtatCivil(EtatCivil.MARIE);

				final PartiePrenante pp2 = addPartiePrenante(ut, "Bataillard", "Arthur");
				pp2.setNomMere("Bataillard");
				pp2.setPrenomsMere("Françoise Alicia");
				pp2.setNomPere("Bataillard");
				pp2.setPrenomsPere("Francis Patrice");
				pp2.setOfsPays(MockPays.France.getNoOFS());
				pp2.setOfsPaysNationalite(MockPays.Allemagne.getNoOFS());
				pp2.setRue("Rue de la porte en bois");
				pp2.setNumeroMaison("13b");
				pp2.setSexe(Sexe.MASCULIN);
				pp2.setSourceCivile(false);
				pp2.setCategorieEtranger(CategorieEtranger._01_SAISONNIER_A);
				pp2.setLocalite("Meulin-Tatard");
				pp2.setNumeroPostal("77415");

				pp2.setDateEtatCivil(dateMariage);
				pp2.setDateNaissance(date(1965, 9, 28));
				pp2.setEtatCivil(EtatCivil.MARIE);

				// lien de conjoint
				pp1.setConjointPartiePrenante(pp2);
				pp2.setConjointPartiePrenante(pp1);

				// rôles
				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp1, ti, TypeRole.AUTRE);
				addRole(pp2, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(3, allTiers.size());        // 2 PP + 1 MC

				final List<Tiers> allSortedTiers = new ArrayList<>(allTiers);
				Collections.sort(allSortedTiers, new Comparator<Tiers>() {
					@Override
					public int compare(Tiers o1, Tiers o2) {
						int result = o1.getNatureTiers().compareTo(o2.getNatureTiers());
						if (result == 0) {
							// les seuls qui ont la même nature sont les deux personnes physiques
							final PersonnePhysique pp1 = (PersonnePhysique) o1;
							final PersonnePhysique pp2 = (PersonnePhysique) o2;
							result = pp1.getNom().compareTo(pp2.getNom());
						}
						return result;
					}
				});

				// et finalement, on obtient :
				// 1. Arthur Bataillard
				// 2. Marie O'Batayon
				// 3. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Bataillard", pp.getNom());
					Assert.assertEquals("Arthur", pp.getTousPrenoms());
					Assert.assertEquals("Arthur", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Bataillard", pp.getNomMere());
					Assert.assertEquals("Françoise Alicia", pp.getPrenomsMere());
					Assert.assertEquals("Bataillard", pp.getNomPere());
					Assert.assertEquals("Francis Patrice", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 28), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._01_SAISONNIER_A, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("77415 Meulin-Tatard", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en bois", adresseEtrangere.getRue());
					Assert.assertEquals("13b", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("O'Batayon", pp.getNom());
					Assert.assertEquals("Marie Constantine", pp.getTousPrenoms());
					Assert.assertEquals("Marie", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Delaplanche", pp.getNomMere());
					Assert.assertEquals("Martine Sophie Mafalda", pp.getPrenomsMere());
					Assert.assertEquals("O'Batayon", pp.getNomPere());
					Assert.assertEquals("Iain François Robert", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 27), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					// sur les sources civiles, l'adresse n'est pas recopiée (elle est a priori vaudoise)
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);

					Assert.assertEquals("petit-reqdes", mc.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", mc.getLogModifUser());

					final Set<AdresseTiers> adresses = mc.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(mc.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
					Assert.assertNotNull(couple);
					Assert.assertSame(allSortedTiers.get(0), couple.getPrincipal());
					Assert.assertSame(allSortedTiers.get(1), couple.getConjoint());

					// situation de famille
					final Set<SituationFamille> situations = mc.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(0, situations.size());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationCompleteCoupleAvecConjointJusteNomme() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateMariage = date(1994, 5, 7);
		final RegDate dateActe = date(2014, 3, 12);

		//  mise en place de l'unité de traitement
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), new InformationsActeur("petit", "Tabasco", "Albertine"), dateActe, "4845151");

				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Marie Constantine");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.FEMININ);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");
				pp.setNomConjoint("Bataillard");
				pp.setPrenomConjoint("Alfred Jean Marie");

				pp.setDateEtatCivil(dateMariage);
				pp.setDateNaissance(date(1965, 9, 27));
				pp.setEtatCivil(EtatCivil.MARIE);

				// rôles
				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(3, allTiers.size());        // 2 PP + 1 MC

				final List<Tiers> allSortedTiers = new ArrayList<>(allTiers);
				Collections.sort(allSortedTiers, new Comparator<Tiers>() {
					@Override
					public int compare(Tiers o1, Tiers o2) {
						int result = o1.getNatureTiers().compareTo(o2.getNatureTiers());
						if (result == 0) {
							// les seuls qui ont la même nature sont les deux personnes physiques
							final PersonnePhysique pp1 = (PersonnePhysique) o1;
							final PersonnePhysique pp2 = (PersonnePhysique) o2;
							result = pp1.getNom().compareTo(pp2.getNom());
						}
						return result;
					}
				});

				// et finalement, on obtient :
				// 1. Alfred Bataillard
				// 2. Marie O'Batayon
				// 3. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Bataillard", pp.getNom());
					Assert.assertEquals("Alfred Jean Marie", pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertNull(pp.getNomMere());
					Assert.assertNull(pp.getPrenomsMere());
					Assert.assertNull(pp.getNomPere());
					Assert.assertNull(pp.getPrenomsPere());
					Assert.assertNull(pp.getNumeroOfsNationalite());
					Assert.assertNull(pp.getSexe());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertNull(pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("O'Batayon", pp.getNom());
					Assert.assertEquals("Marie Constantine", pp.getTousPrenoms());
					Assert.assertEquals("Marie", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Delaplanche", pp.getNomMere());
					Assert.assertEquals("Martine Sophie Mafalda", pp.getPrenomsMere());
					Assert.assertEquals("O'Batayon", pp.getNomPere());
					Assert.assertEquals("Iain François Robert", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 27), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
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

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);

					Assert.assertEquals("petit-reqdes", mc.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", mc.getLogModifUser());

					final Set<AdresseTiers> adresses = mc.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(mc.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
					Assert.assertNotNull(couple);
					Assert.assertSame(allSortedTiers.get(0), couple.getPrincipal());
					Assert.assertSame(allSortedTiers.get(1), couple.getConjoint());

					// situation de famille
					final Set<SituationFamille> situations = mc.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(0, situations.size());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationCompleteCoupleMarieSeul() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateMariage = date(1994, 5, 7);
		final RegDate dateActe = date(2014, 3, 12);

		//  mise en place de l'unité de traitement
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("moinot", "Petiboulot", "Tranquille"), new InformationsActeur("petit", "Tabasco", "Albertine"), dateActe, "4845151");

				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "O'Batayon", "Marie Constantine");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Martine Sophie Mafalda");
				pp.setNomPere("O'Batayon");
				pp.setPrenomsPere("Iain François Robert");
				pp.setOfsPays(MockPays.France.getNoOFS());
				pp.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				pp.setRue("Rue de la porte en bois");
				pp.setNumeroMaison("13b");
				pp.setSexe(Sexe.FEMININ);
				pp.setSourceCivile(false);
				pp.setCategorieEtranger(CategorieEtranger._06_FRONTALIER_G);
				pp.setLocalite("Meulin");
				pp.setNumeroPostal("77415");

				pp.setDateEtatCivil(dateMariage);
				pp.setDateNaissance(date(1965, 9, 27));
				pp.setEtatCivil(EtatCivil.MARIE);

				// rôles
				final TransactionImmobiliere ti = addTransactionImmobiliere(evt, "Truc bidon", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Echallens.getNoOFS());
				addRole(pp, ti, TypeRole.AUTRE);

				return ut.getId();
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(id);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(2, allTiers.size());        // 1 PP + 1 MC

				final List<Tiers> allSortedTiers = new ArrayList<>(allTiers);
				Collections.sort(allSortedTiers, new Comparator<Tiers>() {
					@Override
					public int compare(Tiers o1, Tiers o2) {
						return o1.getNatureTiers().compareTo(o2.getNatureTiers());
					}
				});

				// et finalement, on obtient :
				// 1. Marie O'Batayon
				// 2. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("O'Batayon", pp.getNom());
					Assert.assertEquals("Marie Constantine", pp.getTousPrenoms());
					Assert.assertEquals("Marie", pp.getPrenomUsuel());

					Assert.assertEquals("petit-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Delaplanche", pp.getNomMere());
					Assert.assertEquals("Martine Sophie Mafalda", pp.getPrenomsMere());
					Assert.assertEquals("O'Batayon", pp.getNomPere());
					Assert.assertEquals("Iain François Robert", pp.getPrenomsPere());
					Assert.assertEquals((Integer) MockPays.RoyaumeUni.getNoOFS(), pp.getNumeroOfsNationalite());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(date(1965, 9, 27), pp.getDateNaissance());
					Assert.assertNull(pp.getDateDeces());
					Assert.assertEquals(CategorieEtranger._06_FRONTALIER_G, pp.getCategorieEtranger());
					Assert.assertNull(pp.getDateDebutValiditeAutorisation());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
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

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());
					final AppartenanceMenage am = (AppartenanceMenage) rapports.iterator().next();
					Assert.assertNotNull(am);
					Assert.assertFalse(am.isAnnule());
					Assert.assertEquals(dateMariage, am.getDateDebut());
					Assert.assertNull(am.getDateFin());
					Assert.assertEquals(allSortedTiers.get(1).getNumero(), am.getObjetId());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.MARIE, situ.getEtatCivil());
					Assert.assertEquals(dateMariage, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(1);
					Assert.assertNotNull(mc);

					Assert.assertEquals("petit-reqdes", mc.getLogCreationUser());
					Assert.assertEquals("petit-reqdes", mc.getLogModifUser());

					final Set<AdresseTiers> adresses = mc.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					final List<Remarque> remarques = remarqueDAO.getRemarques(mc.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Tranquille Petiboulot (moinot) et enregistré par Albertine Tabasco (petit).");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// couple
					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
					Assert.assertNotNull(couple);
					Assert.assertSame(allSortedTiers.get(0), couple.getPrincipal());
					Assert.assertNull(couple.getConjoint());

					// situation de famille
					final Set<SituationFamille> situations = mc.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(0, situations.size());
				}
			}
		});
	}
}
