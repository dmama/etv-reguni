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

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
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
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
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
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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

		// mise en place d'une modification de contribuable
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
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.getDisplayName());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Prénoms de la mère : \"Martine\" -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
				b.append("\n- Sexe : ").append(Sexe.FEMININ.getDisplayName()).append(" -> ").append(Sexe.MASCULIN.getDisplayName());
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.format());
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

		// mise en place d'une modification de contribuable
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
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.getDisplayName());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
				b.append("\n- Prénoms de la mère : vide -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.format());
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

		// mise en place d'une modification de contribuable
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
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
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

				checkNoAdresse(found);

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
				b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._06_FRONTALIER_G.getDisplayName());
				b.append("\n- Date de naissance : ").append(dateNaissance.year()).append(" -> ").append(RegDateHelper.dateToDisplayString(dateNaissance));
				b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
				b.append("\n- Prénoms de la mère : vide -> \"Martine Sophie Mafalda\"");
				b.append("\n- Nom du père : vide -> \"O'Batayon\"");
				b.append("\n- Prénoms du père : vide -> \"Iain François Robert\"");
				b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
				b.append("\n- Adresse transmise non enregistrée : Rue de la porte en bois 13b / 77415 Meulin / France");
				b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateNaissance)).append(" : vide -> ").append(EtatCivil.CELIBATAIRE.format());
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

					checkNoAdresse(mc);

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
					checkNoSituationFamille(mc);
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
					checkNoAdresse(pp);

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

					checkNoAdresse(mc);

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
					checkNoSituationFamille(mc);
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

					checkNoAdresse(pp);

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

					checkNoAdresse(mc);

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
					checkNoSituationFamille(mc);
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

					checkNoAdresse(mc);

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
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testContribuableConnuEnCoupleEtIndiqueCelibataireDansActe() throws Exception {

		final RegDate dateNaissance = date(1976, 4, 23);
		final RegDate today = RegDate.get();
		final RegDate dateActe = today.addDays(-5);

		final class Ids {
			long utId;
			long ppId;
		}

		// mise en place d'une modification de contribuable
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique pphysique = addNonHabitant("Inconnu", "Aubataillon", date(dateNaissance.year()), Sexe.FEMININ);
				pphysique.setNomMere("Delaplanche");
				pphysique.setPrenomsMere("Martine");
				addEnsembleTiersCouple(pphysique, null, dateActe.addYears(-1), null);

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

				final Ids ids = new Ids();
				ids.ppId = pphysique.getId();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du traitement (erreur !)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());

				final Set<ErreurTraitement> erreurs = ut.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());

				final ErreurTraitement erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals(String.format("La personne physique %s est connue en couple après le %s mais est indiquée comme Célibataire dans l'acte.",
				                                  FormatNumeroHelper.numeroCTBToDisplay(ids.ppId), RegDateHelper.dateToDisplayString(dateNaissance)),
				                    erreur.getMessage());

				// et on vérifie que le contribuable n'a pas été mis à jour
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.ppId);
				Assert.assertNotNull(pp);
				Assert.assertEquals("Aubataillon", pp.getNom());
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationCouplePartiesPrenantesCompletementConnu() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateNaissanceElle = date(1979, 7, 22);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Pascaline", "Dumoulin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), null, dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final PartiePrenante ppElle = addPartiePrenante(ut, "Dumoulin", "Marie-Pascaline");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Paradis");
				ppElle.setPrenomsMere("Corrine");
				ppElle.setNomPere("Paradis");
				ppElle.setPrenomsPere("Albert");
				ppElle.setOfsPaysNationalite(MockPays.France.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppElle.setDateNaissance(dateNaissanceElle);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setEtatCivil(EtatCivil.MARIE);

				ppElle.setRue("Rue de la porte en fer");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.France.getNoOFS());
				ppElle.setLocalite("Paris");
				ppElle.setNumeroPostal("75016");

				// lien entre les parties prenantes
				ppLui.setConjointPartiePrenante(ppElle);
				ppElle.setConjointPartiePrenante(ppLui);

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Alfred Henri", pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Alfred Henri\"");
					b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._03_ETABLI_C.getDisplayName());
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateMariage)).append(" : vide -> ").append(EtatCivil.MARIE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Marie-Pascaline", pp.getTousPrenoms());
					Assert.assertEquals("Marie-Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Marie-Pascaline\"");
					b.append("\n- Prénom usuel : \"Pascaline\" -> \"Marie-Pascaline\"");
					b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._03_ETABLI_C.getDisplayName());
					b.append("\n- Date de naissance : vide -> ").append(RegDateHelper.dateToDisplayString(dateNaissanceElle));
					b.append("\n- Nom de la mère : vide -> \"Paradis\"");
					b.append("\n- Prénoms de la mère : vide -> \"Corrine\"");
					b.append("\n- Nom du père : vide -> \"Paradis\"");
					b.append("\n- Prénoms du père : vide -> \"Albert\"");
					b.append("\n- Nationalité : vide -> ").append(MockPays.France.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateMariage)).append(" : vide -> ").append(EtatCivil.MARIE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}
				{
					final MenageCommun pp = (MenageCommun) tiersDAO.get(ids.idMenage);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());

					checkNoAdresse(pp);

					checkNoRemarque(pp);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationCouplePartiesPrenantesDeuxConnusMariesSeuls() throws Exception {

		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateNaissanceElle = date(1979, 7, 22);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idElle;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Pascaline", "Dumoulin", null, Sexe.FEMININ);

				// deux mariés seuls connus et différents
				addEnsembleTiersCouple(lui, null, dateMariage, null);
				addEnsembleTiersCouple(elle, null, dateMariage, null);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), null, dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final PartiePrenante ppElle = addPartiePrenante(ut, "Dumoulin", "Marie-Pascaline");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Paradis");
				ppElle.setPrenomsMere("Corrine");
				ppElle.setNomPere("Paradis");
				ppElle.setPrenomsPere("Albert");
				ppElle.setOfsPaysNationalite(MockPays.France.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppElle.setDateNaissance(dateNaissanceElle);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setEtatCivil(EtatCivil.MARIE);

				ppElle.setRue("Rue de la porte en fer");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.France.getNoOFS());
				ppElle.setLocalite("Paris");
				ppElle.setNumeroPostal("75016");

				// lien entre les parties prenantes
				ppLui.setConjointPartiePrenante(ppElle);
				ppElle.setConjointPartiePrenante(ppLui);

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Les tiers %s et %s ne forment pas un couple au %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idLui),
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle),
				                                             RegDateHelper.dateToDisplayString(dateActe));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
    			}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationCouplePartiesPrenantesUnConnuMarieSeul() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateNaissanceElle = date(1979, 7, 22);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idMenage;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), null, dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final PartiePrenante ppElle = addPartiePrenante(ut, "Dumoulin", "Marie-Pascaline");
				ppElle.setNomMere("Paradis");
				ppElle.setPrenomsMere("Corrine");
				ppElle.setNomPere("Paradis");
				ppElle.setPrenomsPere("Albert");
				ppElle.setOfsPaysNationalite(MockPays.France.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppElle.setDateNaissance(dateNaissanceElle);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setEtatCivil(EtatCivil.MARIE);

				ppElle.setRue("Rue de la porte en fer");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.France.getNoOFS());
				ppElle.setLocalite("Paris");
				ppElle.setNumeroPostal("75016");

				// lien entre les parties prenantes
				ppLui.setConjointPartiePrenante(ppElle);
				ppElle.setConjointPartiePrenante(ppLui);

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Alfred Henri", pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Alfred Henri\"");
					b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._03_ETABLI_C.getDisplayName());
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateMariage)).append(" : vide -> ").append(EtatCivil.MARIE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}

				final long idElle;
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", mc.getLogModifUser());     // modifié par l'introduction du deuxième conjoint

					checkNoAdresse(mc);

					checkNoRemarque(mc);

					final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateMariage);
					Assert.assertNotNull(couple);
					Assert.assertNotNull(couple.getPrincipal());
					Assert.assertNotNull(couple.getConjoint());
					Assert.assertEquals((Long) ids.idLui, couple.getPrincipal().getNumero());
					idElle = couple.getConjoint().getNumero();
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals("nOtAir-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Marie-Pascaline", pp.getTousPrenoms());
					Assert.assertEquals("Marie-Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("nOtAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par lui-même.");
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationPartiePrenanteMarieeSeuleAvecConjointConnuAuFiscal() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Pascaline", "Dumoulin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), new InformationsActeur("seCretAir", "Planchet", "Adeline"), dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("seCretAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Alfred Henri", pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("seCretAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("seCretAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par Adeline Planchet (seCretAir).");
					b.append("\n- Prénoms : vide -> \"Alfred Henri\"");
					b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._03_ETABLI_C.getDisplayName());
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateMariage)).append(" : vide -> ").append(EtatCivil.MARIE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
				{
					final MenageCommun pp = (MenageCommun) tiersDAO.get(ids.idMenage);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationPartiePrenanteAvecNomConjointEtConjointConnuAuFiscalMemeNom() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);
		final String noAvs = "7567098402251";

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Pascaline", "Dumoulin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), new InformationsActeur("seCretAir", "Planchet", "Adeline"), dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);
				ppLui.setAvs(noAvs);
				ppLui.setNomConjoint("Dumoulin");
				ppLui.setPrenomConjoint("Pascaline");

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("seCretAir-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Alfred Henri", pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertEquals(AdresseEtrangere.class, adresse.getClass());

					final AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;
					Assert.assertEquals((Integer) MockPays.France.getNoOFS(), adresseEtrangere.getNumeroOfsPays());
					Assert.assertEquals("75016 Paris", adresseEtrangere.getNumeroPostalLocalite());
					Assert.assertEquals("Rue de la porte en fer", adresseEtrangere.getRue());
					Assert.assertEquals("42", adresseEtrangere.getNumeroMaison());
					Assert.assertNull(adresseEtrangere.getTexteCasePostale());
					Assert.assertNull(adresseEtrangere.getNumeroCasePostale());

					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("seCretAir-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("seCretAir-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Francis Bollomey (nOtAir) et enregistré par Adeline Planchet (seCretAir).");
					b.append("\n- Prénoms : vide -> \"Alfred Henri\"");
					b.append("\n- Catégorie d'étranger : vide -> ").append(CategorieEtranger._03_ETABLI_C.getDisplayName());
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- NAVS13 : vide -> ").append(FormatNumeroHelper.formatNumAVS(noAvs));
					b.append("\n- Nationalité : vide -> ").append(MockPays.RoyaumeUni.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateMariage)).append(" : vide -> ").append(EtatCivil.MARIE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
				{
					final MenageCommun pp = (MenageCommun) tiersDAO.get(ids.idMenage);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}

				// il faut de plus vérifier que l'on a bien 3 tiers seulement en base (= ceux vus plus haut) sans création d'un autre avec les données de l'acte
				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertEquals(3, allTiers.size());
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationPartiePrenanteAvecNomConjointEtConjointConnuAuFiscalNomDifferent() throws Exception {

		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idElle;
			long idMenage;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Pascaline", "Dumoulin", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), new InformationsActeur("seCretAir", "Planchet", "Adeline"), dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);
				ppLui.setNomConjoint("Dumoulin");
				ppLui.setPrenomConjoint("Pascaline Marie");

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = mc.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Le conjoint du tiers %s (%s) n'a pas le même nom que celui qui est annoncé dans l'acte.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idLui),
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Alfred", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idElle);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals("Pascaline", pp.getPrenomUsuel());
					Assert.assertEquals("Dumoulin", pp.getNom());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
				{
					final MenageCommun pp = (MenageCommun) tiersDAO.get(ids.idMenage);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationPartiePrenanteMarieeSeuleEtContribuableCelibataireFiscal() throws Exception {

		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), new InformationsActeur("seCretAir", "Planchet", "Adeline"), dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("Le tiers " + FormatNumeroHelper.numeroCTBToDisplay(ids.idLui) + " n'est pas en couple au " + RegDateHelper.dateToDisplayString(dateActe) + ".", erreur.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
				Assert.assertNotNull(pp);
				Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
				Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
				Assert.assertNull(pp.getTousPrenoms());
				Assert.assertEquals("Alfred", pp.getPrenomUsuel());
				Assert.assertEquals("Dumoulin", pp.getNom());

				checkNoAdresse(pp);
				checkNoRemarque(pp);
				checkNoSituationFamille(pp);
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationPartiePrenanteAvecNomConjointEtContribuableCelibataireFiscal() throws Exception {

		final RegDate dateNaissanceLui = date(1979, 7, 31);
		final RegDate dateMariage = date(2005, 5, 12);
		final RegDate dateActe = date(2014, 6, 2);

		final class Ids {
			long idLui;
			long idut;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Alfred", "Dumoulin", dateNaissanceLui, Sexe.MASCULIN);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("nOtAir", "Bollomey", "Francis"), new InformationsActeur("seCretAir", "Planchet", "Adeline"), dateActe, "784156");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppLui = addPartiePrenante(ut, "Dumoulin", "Alfred Henri");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setNomMere("Delaplanche");
				ppLui.setPrenomsMere("Sophie Mafalda");
				ppLui.setNomPere("Dumoulin");
				ppLui.setPrenomsPere("François Robert");
				ppLui.setOfsPaysNationalite(MockPays.RoyaumeUni.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);
				ppLui.setDateNaissance(dateNaissanceLui);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);
				ppLui.setNomConjoint("Dumoulin");
				ppLui.setPrenomConjoint("Anne-Lise");

				ppLui.setRue("Rue de la porte en fer");
				ppLui.setNumeroMaison("42");
				ppLui.setOfsPays(MockPays.France.getNoOFS());
				ppLui.setLocalite("Paris");
				ppLui.setNumeroPostal("75016");

				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idut = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.idut);

		// vérification des résultats
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.idut);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());
				Assert.assertEquals("Le tiers " + FormatNumeroHelper.numeroCTBToDisplay(ids.idLui) + " n'est pas connu en couple au " + RegDateHelper.dateToDisplayString(dateMariage) + ".", erreur.getMessage());

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.idLui);
				Assert.assertNotNull(pp);
				Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
				Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
				Assert.assertNull(pp.getTousPrenoms());
				Assert.assertEquals("Alfred", pp.getPrenomUsuel());
				Assert.assertEquals("Dumoulin", pp.getNom());

				checkNoAdresse(pp);
				checkNoRemarque(pp);
				checkNoSituationFamille(pp);

				// pas de nouveau tiers
				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(1, allTiers.size());
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationContribuableSepare() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		// création de l'unité de traitement
		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.FEMININ);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateMariage);
				pp.setDateSeparation(dateSeparation);
				pp.setEtatCivil(EtatCivil.MARIE);
				pp.setAvs(noAvs);
				pp.setNomConjoint("Cordoba");
				pp.setPrenomConjoint("Marcel");

				pp.setRue("Eisentürstrasse");
				pp.setNumeroMaison("42");
				pp.setOfsPays(MockPays.Suisse.getNoOFS());
				pp.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				pp.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				pp.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				pp.setOfsCommune(MockCommune.Zurich.getNoOFS());

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
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(0, ut.getErreurs().size());

				// 3 contribuables créés
				// - madame Valentine Cordoba
				// - monsieur Marcel Cordoba (données minimales)
				// - couple Cordoba fermé à la veille de la date de séparation
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Marcel, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("zainotaire-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Marcel", pp.getPrenomUsuel());
					Assert.assertEquals("Marcel", pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertNull(pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("zainotaire-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertEquals("Valentine Catherine Julie", pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(noAvs, pp.getNumeroAssureSocial());

					// adresses
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

					final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
					Assert.assertEquals(MockLocalite.Zurich.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals("42", adresseSuisse.getNumeroMaison());
					Assert.assertEquals("Eisentürstrasse", adresseSuisse.getRue());
					Assert.assertNull(adresseSuisse.getNumeroRue());

					// remarque
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals("zainotaire-reqdes", mc.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", mc.getLogModifUser());

					// adresses
					checkNoAdresse(mc);

					// remarque
					final List<Remarque> remarques = remarqueDAO.getRemarques(mc.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableMarieSeulSepare() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long ppid;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				addEnsembleTiersCouple(elle, null, dateMariage, dateSeparation.getOneDayBefore());

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				pp.setNumeroContribuable(elle.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.FEMININ);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateMariage);
				pp.setDateSeparation(dateSeparation);
				pp.setEtatCivil(EtatCivil.MARIE);
				pp.setAvs(noAvs);
				pp.setNomConjoint("Cordoba");
				pp.setPrenomConjoint("Marcel");

				pp.setRue("Eisentürstrasse");
				pp.setNumeroMaison("42");
				pp.setOfsPays(MockPays.Suisse.getNoOFS());
				pp.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				pp.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				pp.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				pp.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final Ids ids = new Ids();
				ids.ppid = elle.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(0, ut.getErreurs().size());

				// 3 contribuables en base
				// - madame Valentine Cordoba (modifiée)
				// - monsieur Marcel Cordoba (créé avec les données minimales)
				// - couple Cordoba fermé à la veille de la date de séparation (laissé en l'état)
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Marcel, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("zainotaire-reqdes", pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Marcel", pp.getPrenomUsuel());
					Assert.assertEquals("Marcel", pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertNull(pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(0, adresses.size());

					// remarques
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertEquals("Valentine Catherine Julie", pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(noAvs, pp.getNumeroAssureSocial());

					// adresses
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

					final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
					Assert.assertEquals(MockLocalite.Zurich.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals("42", adresseSuisse.getNumeroMaison());
					Assert.assertEquals("Eisentürstrasse", adresseSuisse.getRue());
					Assert.assertNull(adresseSuisse.getNumeroRue());

					// remarque
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Valentine Catherine Julie\"");
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- NAVS13 : vide -> ").append(FormatNumeroHelper.formatNumAVS(noAvs));
					b.append("\n- Nationalité : vide -> ").append(MockPays.Suisse.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateSeparation)).append(" : vide -> ").append(EtatCivil.SEPARE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", mc.getLogModifUser());     // modifié par l'ajout du conjoint

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableSepareCompletementConnuFiscalement() throws Exception {

		final RegDate today = RegDate.get();
		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long ppid;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Philippe", "Cordoba", null, Sexe.MASCULIN);
				addEnsembleTiersCouple(elle, lui, dateMariage, dateSeparation.getOneDayBefore());

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppElle = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Delaplanche");
				ppElle.setPrenomsMere("Sophie Mafalda");
				ppElle.setNomPere("Dumoulin");
				ppElle.setPrenomsPere("François Robert");
				ppElle.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setDateNaissance(dateNaissance);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setDateSeparation(dateSeparation);
				ppElle.setEtatCivil(EtatCivil.MARIE);
				ppElle.setAvs(noAvs);

				ppElle.setRue("Eisentürstrasse");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.Suisse.getNoOFS());
				ppElle.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppElle.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppElle.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppElle.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final PartiePrenante ppLui = addPartiePrenante(ut, "Cordoba", "Philippe");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setDateSeparation(dateSeparation);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Eisentürstrasse");
				ppLui.setNumeroMaison("421");
				ppLui.setOfsPays(MockPays.Suisse.getNoOFS());
				ppLui.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppLui.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppLui.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppLui.setOfsCommune(MockCommune.Zurich.getNoOFS());

				// liens
				ppElle.setConjointPartiePrenante(ppLui);
				ppLui.setConjointPartiePrenante(ppElle);

				final Ids ids = new Ids();
				ids.ppid = elle.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(0, ut.getErreurs().size());

				// 3 contribuables en base
				// - madame Valentine Cordoba (modifiée)
				// - monsieur Marcel Cordoba (modifié)
				// - couple Cordoba fermé à la veille de la date de séparation (laissé en l'état)
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Philippe, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Philippe", pp.getPrenomUsuel());
					Assert.assertEquals("Philippe", pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

					final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
					Assert.assertEquals(MockLocalite.Zurich.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals("421", adresseSuisse.getNumeroMaison());
					Assert.assertEquals("Eisentürstrasse", adresseSuisse.getRue());
					Assert.assertNull(adresseSuisse.getNumeroRue());

					// remarques
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Philippe\"");
					b.append("\n- Nationalité : vide -> ").append(MockPays.Suisse.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateSeparation)).append(" : vide -> ").append(EtatCivil.SEPARE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertEquals("Valentine Catherine Julie", pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertEquals(noAvs, pp.getNumeroAssureSocial());

					// adresses
					final Set<AdresseTiers> adresses = pp.getAdressesTiers();
					Assert.assertNotNull(adresses);
					Assert.assertEquals(1, adresses.size());

					final AdresseTiers adresse = adresses.iterator().next();
					Assert.assertNotNull(adresse);
					Assert.assertFalse(adresse.isAnnule());
					Assert.assertEquals(AdresseSuisse.class, adresse.getClass());

					final AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;
					Assert.assertEquals(MockLocalite.Zurich.getNoOrdre(), adresseSuisse.getNumeroOrdrePoste());
					Assert.assertEquals("42", adresseSuisse.getNumeroMaison());
					Assert.assertEquals("Eisentürstrasse", adresseSuisse.getRue());
					Assert.assertNull(adresseSuisse.getNumeroRue());

					// remarque
					final List<Remarque> remarques = remarqueDAO.getRemarques(pp.getNumero());
					Assert.assertNotNull(remarques);
					Assert.assertEquals(1, remarques.size());

					final Remarque remarque = remarques.get(0);
					Assert.assertNotNull(remarque);
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogCreationUser());
					Assert.assertEquals("zainotaire-reqdes", remarque.getLogModifUser());

					final StringBuilder b = new StringBuilder();
					b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(today));
					b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(dateActe));
					b.append(" par le notaire Alexandra Duchmol (zainotaire) et enregistré par lui-même.");
					b.append("\n- Prénoms : vide -> \"Valentine Catherine Julie\"");
					b.append("\n- Nom de la mère : vide -> \"Delaplanche\"");
					b.append("\n- Prénoms de la mère : vide -> \"Sophie Mafalda\"");
					b.append("\n- Nom du père : vide -> \"Dumoulin\"");
					b.append("\n- Prénoms du père : vide -> \"François Robert\"");
					b.append("\n- NAVS13 : vide -> ").append(FormatNumeroHelper.formatNumAVS(noAvs));
					b.append("\n- Nationalité : vide -> ").append(MockPays.Suisse.getNomCourt());
					b.append("\n- Etat civil au ").append(RegDateHelper.dateToDisplayString(dateSeparation)).append(" : vide -> ").append(EtatCivil.SEPARE.format());
					Assert.assertEquals(b.toString(), remarque.getTexte());

					// situation de famille
					final Set<SituationFamille> situations = pp.getSituationsFamille();
					Assert.assertNotNull(situations);
					Assert.assertEquals(1, situations.size());
					final SituationFamille situ = situations.iterator().next();
					Assert.assertNotNull(situ);
					Assert.assertEquals(SituationFamillePersonnePhysique.class, situ.getClass());
					Assert.assertEquals(EtatCivil.SEPARE, situ.getEtatCivil());
					Assert.assertEquals(dateSeparation, situ.getDateDebut());
					Assert.assertNull(situ.getDateFin());

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogModifUser());     // pas modifié du tout

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCoupleMarieMaisFiscalementSepare() throws Exception {

		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long idElle;
			long idLui;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Philippe", "Cordoba", null, Sexe.MASCULIN);
				addEnsembleTiersCouple(elle, lui, dateMariage, dateSeparation.getOneDayBefore());

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppElle = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Delaplanche");
				ppElle.setPrenomsMere("Sophie Mafalda");
				ppElle.setNomPere("Dumoulin");
				ppElle.setPrenomsPere("François Robert");
				ppElle.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setDateNaissance(dateNaissance);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setEtatCivil(EtatCivil.MARIE);
				ppElle.setAvs(noAvs);

				ppElle.setRue("Eisentürstrasse");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.Suisse.getNoOFS());
				ppElle.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppElle.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppElle.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppElle.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final PartiePrenante ppLui = addPartiePrenante(ut, "Cordoba", "Philippe");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Eisentürstrasse");
				ppLui.setNumeroMaison("421");
				ppLui.setOfsPays(MockPays.Suisse.getNoOFS());
				ppLui.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppLui.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppLui.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppLui.setOfsCommune(MockCommune.Zurich.getNoOFS());

				// liens
				ppElle.setConjointPartiePrenante(ppLui);
				ppLui.setConjointPartiePrenante(ppElle);

				final Ids ids = new Ids();
				ids.idElle = elle.getNumero();
				ids.idLui = lui.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Les tiers %s et %s ne forment pas un couple au %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle),
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idLui),
				                                             RegDateHelper.dateToDisplayString(dateActe));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				// 3 contribuables en base (aucun n'est modifié)
				// - madame Valentine Cordoba
				// - monsieur Marcel Cordoba
				// - couple Cordoba fermé à la veille de la date de séparation
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Philippe, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Philippe", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogModifUser());     // pas modifié du tout

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCoupleMarieSeulMaisFiscalementSepare() throws Exception {

		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long idElle;
			long idLui;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Philippe", "Cordoba", null, Sexe.MASCULIN);
				addEnsembleTiersCouple(elle, lui, dateMariage, dateSeparation.getOneDayBefore());

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppElle = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Delaplanche");
				ppElle.setPrenomsMere("Sophie Mafalda");
				ppElle.setNomPere("Dumoulin");
				ppElle.setPrenomsPere("François Robert");
				ppElle.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setDateNaissance(dateNaissance);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setEtatCivil(EtatCivil.MARIE);
				ppElle.setAvs(noAvs);

				ppElle.setRue("Eisentürstrasse");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.Suisse.getNoOFS());
				ppElle.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppElle.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppElle.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppElle.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final Ids ids = new Ids();
				ids.idElle = elle.getNumero();
				ids.idLui = lui.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Le tiers %s n'est pas en couple au %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle),
				                                             RegDateHelper.dateToDisplayString(dateActe));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				// 3 contribuables en base (aucun n'est modifié)
				// - madame Valentine Cordoba
				// - monsieur Marcel Cordoba
				// - couple Cordoba fermé à la veille de la date de séparation
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Philippe, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Philippe", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertEquals(dateSeparation.getOneDayBefore(), rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogModifUser());     // pas modifié du tout

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableCoupleSepareMaisFiscalementMarie() throws Exception {

		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long idElle;
			long idLui;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Philippe", "Cordoba", null, Sexe.MASCULIN);
				addEnsembleTiersCouple(elle, lui, dateMariage, null);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppElle = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Delaplanche");
				ppElle.setPrenomsMere("Sophie Mafalda");
				ppElle.setNomPere("Dumoulin");
				ppElle.setPrenomsPere("François Robert");
				ppElle.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setDateNaissance(dateNaissance);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setDateSeparation(dateSeparation);
				ppElle.setEtatCivil(EtatCivil.MARIE);
				ppElle.setAvs(noAvs);

				ppElle.setRue("Eisentürstrasse");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.Suisse.getNoOFS());
				ppElle.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppElle.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppElle.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppElle.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final PartiePrenante ppLui = addPartiePrenante(ut, "Cordoba", "Philippe");
				ppLui.setNumeroContribuable(lui.getNumero());
				ppLui.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppLui.setSexe(Sexe.MASCULIN);
				ppLui.setDateSeparation(dateSeparation);
				ppLui.setDateEtatCivil(dateMariage);
				ppLui.setEtatCivil(EtatCivil.MARIE);

				ppLui.setRue("Eisentürstrasse");
				ppLui.setNumeroMaison("421");
				ppLui.setOfsPays(MockPays.Suisse.getNoOFS());
				ppLui.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppLui.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppLui.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppLui.setOfsCommune(MockCommune.Zurich.getNoOFS());

				// liens
				ppElle.setConjointPartiePrenante(ppLui);
				ppLui.setConjointPartiePrenante(ppElle);

				final Ids ids = new Ids();
				ids.idElle = elle.getNumero();
				ids.idLui = lui.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Les tiers %s et %s ne sont pas séparés fiscalement au %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle),
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idLui),
				                                             RegDateHelper.dateToDisplayString(dateSeparation));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				// 3 contribuables en base (aucun n'est modifié)
				// - madame Valentine Cordoba
				// - monsieur Marcel Cordoba
				// - couple Cordoba fermé à la veille de la date de séparation
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Philippe, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Philippe", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertNull(rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertNull(rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogModifUser());     // pas modifié du tout

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testModificationContribuableSepareSeulMaisFiscalementMarie() throws Exception {

		final RegDate dateNaissance = date(1973, 2, 12);
		final RegDate dateMariage = date(2005, 7, 12);
		final RegDate dateSeparation = date(2010, 9, 28);
		final RegDate dateActe = date(2014, 6, 10);
		final String noAvs = "7561912776368";

		final class Ids {
			long idElle;
			long idLui;
			long utId;
		}

		// création de l'unité de traitement
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique elle = addNonHabitant("Valentine", "Cordoba", dateNaissance, Sexe.FEMININ);
				final PersonnePhysique lui = addNonHabitant("Philippe", "Cordoba", null, Sexe.MASCULIN);
				addEnsembleTiersCouple(elle, lui, dateMariage, null);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("zainotaire", "Duchmol", "Alexandra"), null, dateActe, "4879846498");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante ppElle = addPartiePrenante(ut, "Cordoba",  "Valentine Catherine Julie");
				ppElle.setNumeroContribuable(elle.getNumero());
				ppElle.setNomMere("Delaplanche");
				ppElle.setPrenomsMere("Sophie Mafalda");
				ppElle.setNomPere("Dumoulin");
				ppElle.setPrenomsPere("François Robert");
				ppElle.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				ppElle.setSexe(Sexe.FEMININ);
				ppElle.setDateNaissance(dateNaissance);
				ppElle.setDateEtatCivil(dateMariage);
				ppElle.setDateSeparation(dateSeparation);
				ppElle.setEtatCivil(EtatCivil.MARIE);
				ppElle.setAvs(noAvs);

				ppElle.setRue("Eisentürstrasse");
				ppElle.setNumeroMaison("42");
				ppElle.setOfsPays(MockPays.Suisse.getNoOFS());
				ppElle.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				ppElle.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				ppElle.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				ppElle.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final Ids ids = new Ids();
				ids.idElle = elle.getNumero();
				ids.idLui = lui.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traitement de l'unité
		traiteUniteTraitement(ids.utId);

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.EN_ERREUR, ut.getEtat());
				Assert.assertNotNull(ut.getDateTraitement());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.ERROR, erreur.getType());

				final String expectedMessage = String.format("Le tiers %s est en couple au %s.",
				                                             FormatNumeroHelper.numeroCTBToDisplay(ids.idElle),
				                                             RegDateHelper.dateToDisplayString(dateSeparation));
				Assert.assertEquals(expectedMessage, erreur.getMessage());

				// 3 contribuables en base (aucun n'est modifié)
				// - madame Valentine Cordoba
				// - monsieur Marcel Cordoba
				// - couple Cordoba fermé à la veille de la date de séparation
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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et l'ordre est finalement 1. Philippe, 2. Valentine, 3 le couple
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Philippe", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertNull(pp.getDateNaissance());
					Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					// adresses
					checkNoAdresse(pp);

					// remarques
					checkNoRemarque(pp);

					// situation de famille
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertNull(rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), pp.getLogModifUser());
					Assert.assertEquals("Cordoba", pp.getNom());
					Assert.assertEquals("Valentine", pp.getPrenomUsuel());
					Assert.assertNull(pp.getTousPrenoms());
					Assert.assertEquals(dateNaissance, pp.getDateNaissance());
					Assert.assertEquals(Sexe.FEMININ, pp.getSexe());
					Assert.assertNull(pp.getNumeroAssureSocial());

					checkNoAdresse(pp);
					checkNoRemarque(pp);
					checkNoSituationFamille(pp);

					// couple
					final Set<RapportEntreTiers> rapports = pp.getRapportsSujet();
					Assert.assertNotNull(rapports);
					Assert.assertEquals(1, rapports.size());

					final RapportEntreTiers rapport = rapports.iterator().next();
					Assert.assertNotNull(rapport);
					Assert.assertFalse(rapport.isAnnule());
					Assert.assertEquals(dateMariage, rapport.getDateDebut());
					Assert.assertNull(rapport.getDateFin());
					Assert.assertEquals(allSortedTiers.get(2).getNumero(), rapport.getObjetId());
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogCreationUser());
					Assert.assertEquals(getDefaultOperateurName(), mc.getLogModifUser());     // pas modifié du tout

					// adresses
					checkNoAdresse(mc);

					// remarque
					checkNoRemarque(mc);

					// situation de famille
					checkNoSituationFamille(mc);
				}
			}
		});
	}

	private static void checkNoSituationFamille(Contribuable ctb) {
		final Set<SituationFamille> situations = ctb.getSituationsFamille();
		Assert.assertNotNull(situations);
		Assert.assertEquals(0, situations.size());
	}

	private static void checkNoAdresse(Contribuable ctb) {
		final Set<AdresseTiers> adresses = ctb.getAdressesTiers();
		Assert.assertNotNull(adresses);
		Assert.assertEquals(0, adresses.size());
	}

	private void checkNoRemarque(Contribuable ctb) {
		final List<Remarque> remarques = remarqueDAO.getRemarques(ctb.getNumero());
		Assert.assertNotNull(remarques);
		Assert.assertEquals(0, remarques.size());
	}

	@Test(timeout = 10000)
	public void testCreationForNouvelAcquereur() throws Exception {

		final RegDate dateNaissance = date(1985, 10, 20);
		final RegDate dateActe = date(2014, 6, 9);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");

				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);

				pp.setRue("Eisentürstrasse");
				pp.setNumeroMaison("42");
				pp.setOfsPays(MockPays.Suisse.getNoOFS());
				pp.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				pp.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				pp.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				pp.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Servitude Lausanne", ModeInscription.INSCRIPTION, TypeInscription.SERVITUDE, MockCommune.Lausanne.getNoOFS());
				final TransactionImmobiliere ti2 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				final TransactionImmobiliere ti3 = addTransactionImmobiliere(evt, "Propriété Vevey", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Vevey.getNoOFS());

				addRole(pp, ti1, TypeRole.ACQUEREUR);
				addRole(pp, ti2, TypeRole.ACQUEREUR);
				addRole(pp, ti3, TypeRole.ALIENATEUR);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(1, allTiers.size());

				final Tiers tiers = allTiers.get(0);
				Assert.assertEquals(PersonnePhysique.class, tiers.getClass());

				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(2, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new Comparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						return Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
					}
				});

				// d'abord le secondaire, ensuite le principal
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testChangementForPrincipalHorsSuisseVersHorsCantonNouvelAcquereur() throws Exception {

		final RegDate dateNaissance = date(1985, 10, 20);
		final RegDate dateActe = date(2014, 6, 9);

		final class Ids {
			long ppId;
			long utId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ctb = addNonHabitant("Alain", "Zigotto", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(ctb, date(2000, 5, 13), MotifFor.INDETERMINE, MockPays.Allemagne);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");
				pp.setNumeroContribuable(ctb.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);

				pp.setRue("Eisentürstrasse");
				pp.setNumeroMaison("42");
				pp.setOfsPays(MockPays.Suisse.getNoOFS());
				pp.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				pp.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				pp.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				pp.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp, ti1, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.ppId = ctb.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traiter l'unité
		traiteUniteTraitement(ids.utId);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(1, allTiers.size());

				final Tiers tiers = allTiers.get(0);
				Assert.assertEquals(PersonnePhysique.class, tiers.getClass());
				Assert.assertEquals((Long) ids.ppId, tiers.getNumero());

				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(3, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
						if (comparison == 0) {
							comparison = super.compare(o1, o2);
						}
						return comparison;
					}
				});

				// d'abord le secondaire, ensuite les principaux
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(date(2000, 5, 13), ff.getDateDebut());
					Assert.assertEquals(dateActe.getOneDayBefore(), ff.getDateFin());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
				{
					final ForFiscal ff = sortedFors.get(2);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testChangementForPrincipalHorsCantonVersHorsCantonNouvelAcquereur() throws Exception {

		final RegDate dateNaissance = date(1985, 10, 20);
		final RegDate dateActe = date(2014, 6, 9);

		final class Ids {
			long ppId;
			long utId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ctb = addNonHabitant("Alain", "Zigotto", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(ctb, date(2000, 5, 13), MotifFor.INDETERMINE, MockCommune.Geneve);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");
				pp.setNumeroContribuable(ctb.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);

				pp.setRue("Eisentürstrasse");
				pp.setNumeroMaison("42");
				pp.setOfsPays(MockPays.Suisse.getNoOFS());
				pp.setLocalite(MockLocalite.Zurich.getNomCompletMinuscule());
				pp.setNumeroPostal(Integer.toString(MockLocalite.Zurich.getNPA()));
				pp.setNumeroPostalComplementaire(MockLocalite.Zurich.getComplementNPA());
				pp.setOfsCommune(MockCommune.Zurich.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp, ti1, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.ppId = ctb.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traiter l'unité
		traiteUniteTraitement(ids.utId);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(1, allTiers.size());

				final Tiers tiers = allTiers.get(0);
				Assert.assertEquals(PersonnePhysique.class, tiers.getClass());
				Assert.assertEquals((Long) ids.ppId, tiers.getNumero());

				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(3, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
						if (comparison == 0) {
							comparison = super.compare(o1, o2);
						}
						return comparison;
					}
				});

				// d'abord le secondaire, ensuite les principaux
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(date(2000, 5, 13), ff.getDateDebut());
					Assert.assertEquals(dateActe.getOneDayBefore(), ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
				{
					final ForFiscal ff = sortedFors.get(2);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Zurich.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.DEMENAGEMENT_VD, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testChangementForPrincipalHorsCantonVersHorsSuisseNouvelAcquereur() throws Exception {

		final RegDate dateNaissance = date(1985, 10, 20);
		final RegDate dateActe = date(2014, 6, 9);

		final class Ids {
			long ppId;
			long utId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ctb = addNonHabitant("Alain", "Zigotto", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(ctb, date(2000, 5, 13), MotifFor.INDETERMINE, MockCommune.Geneve);

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");
				pp.setNumeroContribuable(ctb.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateNaissance);
				pp.setEtatCivil(EtatCivil.CELIBATAIRE);

				pp.setRue("Nizzaallee");
				pp.setNumeroMaison("7");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp.setLocalite("Aachen");
				pp.setNumeroPostal("52064");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp, ti1, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.ppId = ctb.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traiter l'unité
		traiteUniteTraitement(ids.utId);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(1, allTiers.size());

				final Tiers tiers = allTiers.get(0);
				Assert.assertEquals(PersonnePhysique.class, tiers.getClass());
				Assert.assertEquals((Long) ids.ppId, tiers.getNumero());

				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(3, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
						if (comparison == 0) {
							comparison = super.compare(o1, o2);
						}
						return comparison;
					}
				});

				// d'abord le secondaire, ensuite les principaux
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(date(2000, 5, 13), ff.getDateDebut());
					Assert.assertEquals(dateActe.getOneDayBefore(), ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
				{
					final ForFiscal ff = sortedFors.get(2);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testForSecondaireDejaPresentSurCoupleAcquereurMarieSeul() throws Exception {

		final RegDate dateNaissance = date(1975, 10, 20);
		final RegDate dateMariage = date(1998, 4, 25);
		final RegDate dateAchatPrecedent = date(2000, 6, 12);
		final RegDate dateActe = date(2014, 6, 9);

		final class Ids {
			long ctbId;
			long utId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ctb = addNonHabitant("Alain", "Zigotto", dateNaissance, Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(ctb, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateAchatPrecedent, MotifFor.INDETERMINE, MockCommune.Geneve);
				addForSecondaire(mc, dateAchatPrecedent, MotifFor.ACHAT_IMMOBILIER, MockCommune.Morges.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);          // on ne devrait pas créer de nouveau for secondaire

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");
				pp.setNumeroContribuable(ctb.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateMariage);
				pp.setEtatCivil(EtatCivil.MARIE);

				pp.setRue("Nizzaallee");
				pp.setNumeroMaison("7");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp.setLocalite("Aachen");
				pp.setNumeroPostal("52064");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp, ti1, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.ctbId = mc.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traiter l'unité
		traiteUniteTraitement(ids.utId);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.WARNING, erreur.getType());
				Assert.assertEquals("Adresse non modifiée sur un contribuable assujetti, uniquement reprise dans les remarques du tiers.", erreur.getMessage());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(2, allTiers.size());

				final MenageCommun tiers = (MenageCommun) tiersDAO.get(ids.ctbId);
				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(3, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
						if (comparison == 0) {
							comparison = super.compare(o1, o2);
						}
						return comparison;
					}
				});

				// d'abord le secondaire, ensuite les principaux
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateAchatPrecedent, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateAchatPrecedent, ff.getDateDebut());
					Assert.assertEquals(dateActe.getOneDayBefore(), ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
				{
					final ForFiscal ff = sortedFors.get(2);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testForSecondaireDejaPresentAutreMotifRattachementSurCoupleAcquereurMarieSeul() throws Exception {

		final RegDate dateNaissance = date(1975, 10, 20);
		final RegDate dateMariage = date(1998, 4, 25);
		final RegDate dateDebutExploitation = date(2000, 5, 13);
		final RegDate dateActe = date(2014, 6, 9);

		final class Ids {
			long ctbId;
			long utId;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ctb = addNonHabitant("Alain", "Zigotto", dateNaissance, Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(ctb, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateDebutExploitation, MotifFor.INDETERMINE, MockCommune.Geneve);
				addForSecondaire(mc, dateDebutExploitation, MotifFor.DEBUT_EXPLOITATION, MockCommune.Morges.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);          // on devrait créer un nouveau for secondaire car le motif de rattachement est différent !

				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp = addPartiePrenante(ut, "Zigotto", "Alain");
				pp.setNumeroContribuable(ctb.getNumero());
				pp.setNomMere("Delaplanche");
				pp.setPrenomsMere("Sophie Mafalda");
				pp.setNomPere("Dumoulin");
				pp.setPrenomsPere("François Robert");
				pp.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp.setSexe(Sexe.MASCULIN);
				pp.setDateNaissance(dateNaissance);
				pp.setDateEtatCivil(dateMariage);
				pp.setEtatCivil(EtatCivil.MARIE);

				pp.setRue("Nizzaallee");
				pp.setNumeroMaison("7");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp.setLocalite("Aachen");
				pp.setNumeroPostal("52064");
				pp.setOfsPays(MockPays.Allemagne.getNoOFS());

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp, ti1, TypeRole.ACQUEREUR);

				final Ids ids = new Ids();
				ids.ctbId = mc.getNumero();
				ids.utId = ut.getId();
				return ids;
			}
		});

		// traiter l'unité
		traiteUniteTraitement(ids.utId);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(ids.utId);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(1, ut.getErreurs().size());

				final ErreurTraitement erreur = ut.getErreurs().iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(ErreurTraitement.TypeErreur.WARNING, erreur.getType());
				Assert.assertEquals("Adresse non modifiée sur un contribuable assujetti, uniquement reprise dans les remarques du tiers.", erreur.getMessage());

				final List<Tiers> allTiers = tiersDAO.getAll();
				Assert.assertNotNull(allTiers);
				Assert.assertEquals(2, allTiers.size());

				final MenageCommun tiers = (MenageCommun) tiersDAO.get(ids.ctbId);
				final Set<ForFiscal> ffs = tiers.getForsFiscaux();
				Assert.assertNotNull(ffs);
				Assert.assertEquals(4, ffs.size());

				final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
				Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
					@Override
					public int compare(ForFiscal o1, ForFiscal o2) {
						int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
						if (comparison == 0) {
							comparison = super.compare(o1, o2);
						}
						return comparison;
					}
				});

				// d'abord les secondaires, ensuite les principaux
				{
					final ForFiscal ff = sortedFors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateDebutExploitation, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.DEBUT_EXPLOITATION, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.ACTIVITE_INDEPENDANTE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(1);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
					Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
					Assert.assertNull(ffsec.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
				}
				{
					final ForFiscal ff = sortedFors.get(2);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(date(2000, 5, 13), ff.getDateDebut());
					Assert.assertEquals(dateActe.getOneDayBefore(), ff.getDateFin());
					Assert.assertEquals((Integer) MockCommune.Geneve.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.INDETERMINE, ffp.getMotifOuverture());
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
				{
					final ForFiscal ff = sortedFors.get(3);
					Assert.assertNotNull(ff);
					Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
					Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
					Assert.assertEquals(dateActe, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
					Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

					final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
					Assert.assertEquals(MotifFor.DEPART_HS, ffp.getMotifOuverture());
					Assert.assertNull(ffp.getMotifFermeture());
					Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
					Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationPlusieursForsSecondairesSurCoupleDepuisDifferentesPartiesPrenantes() throws Exception {

		final RegDate dateNaissance = date(1975, 10, 20);
		final RegDate dateMariage = date(1998, 4, 25);
		final RegDate dateActe = date(2014, 6, 9);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "Zigotto", "Alain");

				pp1.setNomMere("Delaplanche");
				pp1.setPrenomsMere("Sophie Mafalda");
				pp1.setNomPere("Dumoulin");
				pp1.setPrenomsPere("François Robert");
				pp1.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp1.setSexe(Sexe.MASCULIN);
				pp1.setDateNaissance(dateNaissance);
				pp1.setDateEtatCivil(dateMariage);
				pp1.setEtatCivil(EtatCivil.MARIE);

				pp1.setRue("Nizzaallee");
				pp1.setNumeroMaison("7");
				pp1.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp1.setLocalite("Aachen");
				pp1.setNumeroPostal("52064");
				pp1.setOfsPays(MockPays.Allemagne.getNoOFS());

				final PartiePrenante pp2 = addPartiePrenante(ut, "Zigotto", "Francine");

				pp2.setNomMere("Martin");
				pp2.setPrenomsMere("Carabine");
				pp2.setNomPere("Zanallo");
				pp2.setPrenomsPere("Sergio");
				pp2.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp2.setSexe(Sexe.FEMININ);
				pp2.setDateNaissance(null);
				pp2.setDateEtatCivil(dateMariage);
				pp2.setEtatCivil(EtatCivil.MARIE);

				pp2.setRue("Nizzaallee");
				pp2.setNumeroMaison("7");
				pp2.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp2.setLocalite("Aachen");
				pp2.setNumeroPostal("52064");
				pp2.setOfsPays(MockPays.Allemagne.getNoOFS());

				pp1.setConjointPartiePrenante(pp2);
				pp2.setConjointPartiePrenante(pp1);

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				final TransactionImmobiliere ti2 = addTransactionImmobiliere(evt, "Propriété Echallens", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Echallens.getNoOFS());
				addRole(pp1, ti1, TypeRole.ACQUEREUR);
				addRole(pp2, ti2, TypeRole.ACQUEREUR);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et finalement, on obtient :
				// 1. Alain Zigotto
				// 2. Francine Zigotto
				// 3. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Zigotto", pp.getNom());
					Assert.assertEquals("Alain", pp.getPrenomUsuel());
					Assert.assertEquals("Alain", pp.getTousPrenoms());
					Assert.assertEquals(0, pp.getForsFiscaux().size());   // pas de fors fiscaux
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Zigotto", pp.getNom());
					Assert.assertEquals("Francine", pp.getPrenomUsuel());
					Assert.assertEquals("Francine", pp.getTousPrenoms());
					Assert.assertEquals(0, pp.getForsFiscaux().size());   // pas de fors fiscaux
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					final Set<ForFiscal> ffs = mc.getForsFiscaux();
					Assert.assertNotNull(ffs);
					Assert.assertEquals(3, ffs.size());

					final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
					Collections.sort(sortedFors, new Comparator<ForFiscal>() {
						@Override
						public int compare(ForFiscal o1, ForFiscal o2) {
							int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
							if (comparison == 0) {
								comparison = o1.getNumeroOfsAutoriteFiscale() - o2.getNumeroOfsAutoriteFiscale();
							}
							return comparison;
						}
					});

					// d'abord les secondaires (par ordre de numéro OFS de commune), ensuite le principal
					{
						final ForFiscal ff = sortedFors.get(0);
						Assert.assertNotNull(ff);
						Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
						Assert.assertEquals(dateActe, ff.getDateDebut());
						Assert.assertNull(ff.getDateFin());
						Assert.assertEquals((Integer) MockCommune.Echallens.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
						Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

						final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
						Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
						Assert.assertNull(ffsec.getMotifFermeture());
						Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
					}
					{
						final ForFiscal ff = sortedFors.get(1);
						Assert.assertNotNull(ff);
						Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
						Assert.assertEquals(dateActe, ff.getDateDebut());
						Assert.assertNull(ff.getDateFin());
						Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
						Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

						final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
						Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
						Assert.assertNull(ffsec.getMotifFermeture());
						Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
					}
					{
						final ForFiscal ff = sortedFors.get(2);
						Assert.assertNotNull(ff);
						Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
						Assert.assertEquals(dateActe, ff.getDateDebut());
						Assert.assertNull(ff.getDateFin());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
						Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

						final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
						Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffp.getMotifOuverture());
						Assert.assertNull(ffp.getMotifFermeture());
						Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
						Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
					}
				}
			}
		});
	}

	@Test(timeout = 10000)
	public void testCreationMemeForSecondaireSurCoupleDepuisDifferentesPartiesPrenantes() throws Exception {

		final RegDate dateNaissance = date(1975, 10, 20);
		final RegDate dateMariage = date(1998, 4, 25);
		final RegDate dateActe = date(2014, 6, 9);

		final long id = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementReqDes evt = addEvenementReqDes(new InformationsActeur("tabou", "Taboumata", "Oli"), null, dateActe, "541154651");
				final UniteTraitement ut = addUniteTraitement(evt, EtatTraitement.A_TRAITER, null);
				final PartiePrenante pp1 = addPartiePrenante(ut, "Zigotto", "Alain");

				pp1.setNomMere("Delaplanche");
				pp1.setPrenomsMere("Sophie Mafalda");
				pp1.setNomPere("Dumoulin");
				pp1.setPrenomsPere("François Robert");
				pp1.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp1.setSexe(Sexe.MASCULIN);
				pp1.setDateNaissance(dateNaissance);
				pp1.setDateEtatCivil(dateMariage);
				pp1.setEtatCivil(EtatCivil.MARIE);

				pp1.setRue("Nizzaallee");
				pp1.setNumeroMaison("7");
				pp1.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp1.setLocalite("Aachen");
				pp1.setNumeroPostal("52064");
				pp1.setOfsPays(MockPays.Allemagne.getNoOFS());

				final PartiePrenante pp2 = addPartiePrenante(ut, "Zigotto", "Francine");

				pp2.setNomMere("Martin");
				pp2.setPrenomsMere("Carabine");
				pp2.setNomPere("Zanallo");
				pp2.setPrenomsPere("Sergio");
				pp2.setOfsPaysNationalite(MockPays.Suisse.getNoOFS());
				pp2.setSexe(Sexe.FEMININ);
				pp2.setDateNaissance(null);
				pp2.setDateEtatCivil(dateMariage);
				pp2.setEtatCivil(EtatCivil.MARIE);

				pp2.setRue("Nizzaallee");
				pp2.setNumeroMaison("7");
				pp2.setOfsPays(MockPays.Allemagne.getNoOFS());
				pp2.setLocalite("Aachen");
				pp2.setNumeroPostal("52064");
				pp2.setOfsPays(MockPays.Allemagne.getNoOFS());

				pp1.setConjointPartiePrenante(pp2);
				pp2.setConjointPartiePrenante(pp1);

				final TransactionImmobiliere ti1 = addTransactionImmobiliere(evt, "Propriété Morges", ModeInscription.INSCRIPTION, TypeInscription.PROPRIETE, MockCommune.Morges.getNoOFS());
				addRole(pp1, ti1, TypeRole.ACQUEREUR);
				addRole(pp2, ti1, TypeRole.ACQUEREUR);

				return ut.getId();
			}
		});

		// traiter l'unité
		traiteUniteTraitement(id);

		// vérification des fors
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = uniteTraitementDAO.get(id);
				Assert.assertNotNull(ut);
				Assert.assertEquals(EtatTraitement.TRAITE, ut.getEtat());
				Assert.assertEquals(0, ut.getErreurs().size());

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
							result = pp1.getPrenomUsuel().compareTo(pp2.getPrenomUsuel());
						}
						return result;
					}
				});

				// et finalement, on obtient :
				// 1. Alain Zigotto
				// 2. Francine Zigotto
				// 3. le ménage commun
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(0);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Zigotto", pp.getNom());
					Assert.assertEquals("Alain", pp.getPrenomUsuel());
					Assert.assertEquals("Alain", pp.getTousPrenoms());
					Assert.assertEquals(0, pp.getForsFiscaux().size());   // pas de fors fiscaux
				}
				{
					final PersonnePhysique pp = (PersonnePhysique) allSortedTiers.get(1);
					Assert.assertNotNull(pp);
					Assert.assertEquals("Zigotto", pp.getNom());
					Assert.assertEquals("Francine", pp.getPrenomUsuel());
					Assert.assertEquals("Francine", pp.getTousPrenoms());
					Assert.assertEquals(0, pp.getForsFiscaux().size());   // pas de fors fiscaux
				}
				{
					final MenageCommun mc = (MenageCommun) allSortedTiers.get(2);
					Assert.assertNotNull(mc);
					final Set<ForFiscal> ffs = mc.getForsFiscaux();
					Assert.assertNotNull(ffs);
					Assert.assertEquals(2, ffs.size());

					final List<ForFiscal> sortedFors = new ArrayList<>(ffs);
					Collections.sort(sortedFors, new DateRangeComparator<ForFiscal>() {
						@Override
						public int compare(ForFiscal o1, ForFiscal o2) {
							int comparison = Boolean.compare(o1.isPrincipal(), o2.isPrincipal());
							if (comparison == 0) {
								comparison = super.compare(o1, o2);
							}
							return comparison;
						}
					});

					// d'abord le secondaire, ensuite le principal
					{
						final ForFiscal ff = sortedFors.get(0);
						Assert.assertNotNull(ff);
						Assert.assertEquals(ForFiscalSecondaire.class, ff.getClass());
						Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ff.getTypeAutoriteFiscale());
						Assert.assertEquals(dateActe, ff.getDateDebut());
						Assert.assertNull(ff.getDateFin());
						Assert.assertEquals((Integer) MockCommune.Morges.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
						Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

						final ForFiscalSecondaire ffsec = (ForFiscalSecondaire) ff;
						Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffsec.getMotifOuverture());
						Assert.assertNull(ffsec.getMotifFermeture());
						Assert.assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffsec.getMotifRattachement());
					}
					{
						final ForFiscal ff = sortedFors.get(1);
						Assert.assertNotNull(ff);
						Assert.assertEquals(ForFiscalPrincipal.class, ff.getClass());
						Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ff.getTypeAutoriteFiscale());
						Assert.assertEquals(dateActe, ff.getDateDebut());
						Assert.assertNull(ff.getDateFin());
						Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ff.getNumeroOfsAutoriteFiscale());
						Assert.assertEquals(GenreImpot.REVENU_FORTUNE, ff.getGenreImpot());

						final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff;
						Assert.assertEquals(MotifFor.ACHAT_IMMOBILIER, ffp.getMotifOuverture());
						Assert.assertNull(ffp.getMotifFermeture());
						Assert.assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
						Assert.assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition());
					}
				}
			}
		});
	}
}
