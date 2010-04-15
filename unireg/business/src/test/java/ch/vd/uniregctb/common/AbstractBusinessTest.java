package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.*;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
import ch.vd.uniregctb.evenement.common.EnsembleTiersCouple;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.*;
import ch.vd.uniregctb.type.*;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * @author
 *
 */
// Surcharge des fichiers de config Spring. Il faut mettre les fichiers
// UT a la fin
@ContextConfiguration(locations = {
		TestingConstants.UNIREG_BUSINESS_UT_CACHE,
		TestingConstants.UNIREG_BUSINESS_INTERFACES,
		TestingConstants.UNIREG_BUSINESS_SERVICES,
		TestingConstants.UNIREG_BUSINESS_MDP,
		TestingConstants.UNIREG_BUSINESS_JOBS,
		TestingConstants.UNIREG_BUSINESS_EVT_CIVIL,
		TestingConstants.UNIREG_BUSINESS_UT_APIREG,
		TestingConstants.UNIREG_BUSINESS_UT_EDITIQUE,
		TestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
		TestingConstants.UNIREG_BUSINESS_UT_JMS,
		TestingConstants.UNIREG_BUSINESS_UT_SERVICES,
		TestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
		TestingConstants.UNIREG_BUSINESS_UT_CONFIG
})
public abstract class AbstractBusinessTest extends AbstractCoreDAOTest {

	// private static final Logger LOGGER = Logger.getLogger(AbstractBusinessTest.class);

	private boolean wantIndexation = false;
	protected HibernateTemplate hibernateTemplate;
	protected TiersService tiersService;

	protected abstract void indexData() throws Exception;

	protected abstract void removeIndexData() throws Exception;

	@Override
	protected void runOnSetUp() throws Exception {
		hibernateTemplate = getBean(HibernateTemplate.class, "hibernateTemplate");
		tiersService = getBean(TiersService.class, "tiersService");
		super.runOnSetUp();
	}

	@Override
	protected void truncateDatabase() throws Exception {
		super.truncateDatabase();

		removeIndexData();
	}

	@Override
	protected void loadDatabase(String filename) throws Exception {
		super.loadDatabase(filename);

		if (wantIndexation) {
			indexData();
		}
	}

	public void setWantIndexation(boolean wantIndexation) {
		this.wantIndexation = wantIndexation;
	}

	protected abstract class TestHibernateCallback implements HibernateCallback {
		public abstract Object testInHibernate(Session session) throws Exception;

		public final Object doInHibernate(Session session) throws HibernateException, SQLException {
			try {
				return testInHibernate(session);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected Object executeInSession(TestHibernateCallback action) {
		return hibernateTemplate.executeWithNativeSession(action);
	}

	protected Object executeInNewSession(TestHibernateCallback action) {
		return hibernateTemplate.executeWithNewSession(action);
	}

	protected Object doInNewTransactionAndSession(final TransactionCallback action) throws Exception {
		return executeInNewSession(new TestHibernateCallback() {
			@Override
			public Object testInHibernate(Session session) throws Exception {
				return doInNewTransaction(action);
			}
		});
	}

	protected void flushAndClearSession() {
		hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException, SQLException {
				session.flush();
				session.clear();
				return null;
			}
		});
	}

	protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, TypeAutoriteFiscale type, int noOfs,
			MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipal forPrincipal) {
		assertNotNull(forPrincipal);
		assertEquals(debut, forPrincipal.getDateDebut());
		assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
		assertNull(forPrincipal.getDateFin());
		assertNull(forPrincipal.getMotifFermeture());
		assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forPrincipal.getMotifRattachement());
		assertEquals(modeImposition, forPrincipal.getModeImposition());
	}

	protected static void assertForPrincipal(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture,
			TypeAutoriteFiscale type, int noOfs, MotifRattachement motif, ModeImposition modeImposition, ForFiscalPrincipal forPrincipal) {
		assertNotNull(forPrincipal);
		assertEquals(debut, forPrincipal.getDateDebut());
		assertEquals(motifOuverture, forPrincipal.getMotifOuverture());
		assertEquals(fin, forPrincipal.getDateFin());
		assertEquals(motifFermeture, forPrincipal.getMotifFermeture());
		assertEquals(type, forPrincipal.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forPrincipal.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forPrincipal.getMotifRattachement());
		assertEquals(modeImposition, forPrincipal.getModeImposition());
	}

	protected static void assertForSecondaire(RegDate debut, MotifFor motifOuverture, TypeAutoriteFiscale type, int noOfs,
			MotifRattachement motif, ForFiscalSecondaire forSecondaire) {
		assertNotNull(forSecondaire);
		assertEquals(debut, forSecondaire.getDateDebut());
		assertEquals(motifOuverture, forSecondaire.getMotifOuverture());
		assertNull(forSecondaire.getDateFin());
		assertNull(forSecondaire.getMotifFermeture());
		assertEquals(type, forSecondaire.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forSecondaire.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forSecondaire.getMotifRattachement());
	}

	protected static void assertForSecondaire(RegDate debut, MotifFor motifOuverture, RegDate fin, MotifFor motifFermeture,
			TypeAutoriteFiscale type, int noOfs, MotifRattachement motif, ForFiscalSecondaire forSecondaire) {
		assertNotNull(forSecondaire);
		assertEquals(debut, forSecondaire.getDateDebut());
		assertEquals(motifOuverture, forSecondaire.getMotifOuverture());
		assertEquals(fin, forSecondaire.getDateFin());
		assertEquals(motifFermeture, forSecondaire.getMotifFermeture());
		assertEquals(type, forSecondaire.getTypeAutoriteFiscale());
		assertEquals(Integer.valueOf(noOfs), forSecondaire.getNumeroOfsAutoriteFiscale());
		assertEquals(motif, forSecondaire.getMotifRattachement());
	}

	protected static void assertForDebiteur(RegDate debut, TypeAutoriteFiscale taf, int noOFS, ForDebiteurPrestationImposable forFiscal) {
		assertForDebiteur(debut, null, taf, noOFS, forFiscal);
	}

	protected static void assertForDebiteur(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, int noOFS, ForDebiteurPrestationImposable forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(noOFS, forFiscal.getNumeroOfsAutoriteFiscale().intValue());
	}

	protected static void assertForAutreImpot(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, GenreImpot genreImpot, ForFiscalAutreImpot forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(commune.getNoOFSEtendu(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
		Assert.assertEquals(genreImpot, forFiscal.getGenreImpot());
	}

	protected static void assertForAutreElementImposable(RegDate debut, RegDate fin, TypeAutoriteFiscale taf, MockCommune commune, MotifRattachement rattachement,
	                                            ForFiscalAutreElementImposable forFiscal) {
		Assert.assertEquals(debut, forFiscal.getDateDebut());
		Assert.assertEquals(fin, forFiscal.getDateFin());
		Assert.assertEquals(taf, forFiscal.getTypeAutoriteFiscale());
		Assert.assertEquals(commune.getNoOFSEtendu(), forFiscal.getNumeroOfsAutoriteFiscale().intValue());
		Assert.assertEquals(rattachement, forFiscal.getMotifRattachement());
	}


	/**
	 * Asserte qu'il y a une bien une (et une seule) déclaration d'impôt dans la collection passée en paramètre, et qu'elle possède bien les valeurs spécifiées.
	 *
	 * @param debut              la date de début de la déclaration
	 * @param fin                la date de fin de la déclaration
	 * @param etat               l'état courant de la déclaration
	 * @param typeContribuable   le type de contribuable de la déclaration
	 * @param typeDocument       le type de document de la déclaration
	 * @param idCollRetour       l'id de la collectivité administrative de l'adresse de retour de la déclaration
	 * @param delaiRetourImprime le délai de retour imprimé sur la déclaration
	 * @param declarations       la collection de déclarations à asserter.
	 */
	protected static void assertDI(RegDate debut, RegDate fin, TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                               TypeDocument typeDocument, int idCollRetour, RegDate dateRetourImprimee, List<Declaration> declarations) {
		assertNotNull(declarations);
		assertEquals(declarations.size(), 1);
		assertDI(debut, fin, etat, typeContribuable, typeDocument, idCollRetour, dateRetourImprimee, declarations.get(0));
	}

	/**
	 * Asserte que la déclaration d'impôt passée en paramètre possède bien les valeurs spécifiées.
	 *
	 * @param debut              la date de début de la déclaration
	 * @param fin                la date de fin de la déclaration
	 * @param etat               l'état courant de la déclaration
	 * @param typeContribuable   le type de contribuable de la déclaration
	 * @param typeDocument       le type de document de la déclaration
	 * @param idCollRetour       le numéro de la collectivité administrative (CEDI=1012/ACI=22) de l'adresse de retour de la déclaration
	 * @param delaiRetourImprime le délai de retour imprimé sur la déclaration
	 * @param declaration        la déclaration à asserter.
	 */
	protected static void assertDI(RegDate debut, RegDate fin, TypeEtatDeclaration etat, TypeContribuable typeContribuable,
	                               TypeDocument typeDocument, int idCollRetour, RegDate delaiRetourImprime, Declaration declaration) {
		assertNotNull(declaration);
		DeclarationImpotOrdinaire di = (DeclarationImpotOrdinaire) declaration;
		assertEquals(debut, di.getDateDebut());
		assertEquals(fin, di.getDateFin());
		final EtatDeclaration e = di.getDernierEtat();
		assertEquals(etat, (e == null ? null : e.getEtat()));
		assertEquals(typeContribuable, di.getTypeContribuable());
		assertEquals(typeDocument, di.getModeleDocument().getTypeDocument());
		final CollectiviteAdministrative coll = di.getRetourCollectiviteAdministrative();
		assertNotNull(coll);
		assertEquals(idCollRetour, coll.getNumeroCollectiviteAdministrative().intValue());
		assertEquals(delaiRetourImprime, di.getDelaiRetourImprime());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                  TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, TacheEnvoiDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDateDebut());
		assertEquals(dateFin, tache.getDateFin());
		assertEquals(typeCtb, tache.getTypeContribuable());
		assertEquals(typeDoc, tache.getTypeDocument());
		assertEquals(adresseRetour, tache.getAdresseRetour());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
			TacheAnnulationDeclarationImpot tache) {
		assertNotNull(tache);
		assertEquals(etat, tache.getEtat());
		assertEquals(dateEcheance, tache.getDateEcheance());
		assertEquals(dateDebut, tache.getDeclarationImpotOrdinaire().getDateDebut());
		assertEquals(dateFin, tache.getDeclarationImpotOrdinaire().getDateFin());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheNouveauDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheControleDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la tâche passée en paramètre possède bien les valeurs spécifiées.
	 */
	protected static void assertTache(TypeEtatTache etat, RegDate echeance, final TacheTransmissionDossier tache) {
		assertNotNull(tache);
		assertEquals(echeance, tache.getDateEcheance());
		assertEquals(etat, tache.getEtat());
	}

	/**
	 * Asserte que la collection passée en paramètre possède bien une seule tâche et que celle-ci possède bien les valeurs spécifiées.
	 */
	protected static void assertOneTache(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin,
	                                     TypeContribuable typeCtb, TypeDocument typeDoc, TypeAdresseRetour adresseRetour, List<TacheEnvoiDeclarationImpot> taches) {
		assertNotNull(taches);
		assertEquals(1, taches.size());
		final TacheEnvoiDeclarationImpot tache = taches.get(0);
		assertTache(etat, dateEcheance, dateDebut, dateFin, typeCtb, typeDoc, adresseRetour, tache);
	}

	protected static void assertSituation(RegDate debut, RegDate fin, int nombreEnfants, TarifImpotSource tarif,
			SituationFamilleMenageCommun situation) {
		assertNotNull(situation);
		assertEquals(debut, situation.getDateDebut());
		assertEquals(fin, situation.getDateFin());
		assertEquals(nombreEnfants, situation.getNombreEnfants());
		assertEquals(tarif, situation.getTarifApplicable());
	}

	/**
	 * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune) {
		TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFS(), type, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, MockCommune commune) {
		TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFS(), type,
				MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS,
				MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, MockPays pays) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(),
				TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS, TypeAutoriteFiscale type, MotifRattachement motif) {
		ForFiscalPrincipal f = new ForFiscalPrincipal();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f.setModeImposition(ModeImposition.ORDINAIRE);
		f = (ForFiscalPrincipal) tiersService.addAndSave(contribuable, f);
		return f;
	}

	/**
	 * Ajoute un for fiscal secondaire ouvert.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, Integer noOFS,
			MotifRattachement motif) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f = (ForFiscalSecondaire) tiersService.addAndSave(tiers, f);
		return f;
	}

	/**
	 * Ajoute un for fiscal secondaire fermé.
	 */
	protected ForFiscalSecondaire addForSecondaire(Contribuable tiers, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture,
			MotifFor motifFermeture, Integer noOFS, MotifRattachement motif) {
		ForFiscalSecondaire f = new ForFiscalSecondaire();
		f.setDateDebut(ouverture);
		f.setMotifOuverture(motifOuverture);
		f.setDateFin(fermeture);
		f.setMotifFermeture(motifFermeture);
		f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f.setMotifRattachement(motif);
		f = (ForFiscalSecondaire) tiersService.addAndSave(tiers, f);
		return f;
	}

	protected ForFiscalAutreImpot addForAutreImpot(Contribuable tiers, RegDate ouverture, RegDate fermeture, Integer noOFS,
			TypeAutoriteFiscale type, GenreImpot genre) {
		ForFiscalAutreImpot f = new ForFiscalAutreImpot();
		f.setDateDebut(ouverture);
		f.setDateFin(fermeture);
		f.setGenreImpot(genre);
		f.setTypeAutoriteFiscale(type);
		f.setNumeroOfsAutoriteFiscale(noOFS);
		f = (ForFiscalAutreImpot) tiersService.addAndSave(tiers, f);
		return f;
	}

	protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate debut, RegDate fin, MockCommune commune) {
		ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
		f.setDateDebut(debut);
		f.setDateFin(fin);
		f.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
		f = (ForDebiteurPrestationImposable) tiersService.addAndSave(dpi, f);
		return f;
	}

	protected AdresseSuisse addAdresseSuisse(Tiers tiers, TypeAdresseTiers usage, RegDate debut, RegDate fin, MockRue rue) {
		AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setNumeroRue(rue.getNoRue());
		adresse.setNumeroOrdrePoste(rue.getLocalite().getNPA());
		adresse = (AdresseSuisse) tiersService.addAndSave(tiers, adresse);
		return adresse;
	}

	protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, RegDate fin, Pays pays) {
		AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setNumeroOfsPays(pays.getNoOFS());
		adresse = (AdresseEtrangere) tiersService.addAndSave(tiers, adresse);
		return adresse;
	}

	/**
	 * Ajoute une période fiscale dans la base de données (avec les délais usuels)
	 */
	protected PeriodeFiscale addPeriodeFiscale(int annee) {
		PeriodeFiscale periode = new PeriodeFiscale();
		periode.setAnnee(annee);
		periode.setAllPeriodeFiscaleParametres(date(annee + 1, 1, 31), date(annee + 1, 3, 31), date(annee + 1, 6, 30));
		return (PeriodeFiscale) hibernateTemplate.merge(periode);
	}

	/**
	 * Ajoute un nouveau type de document dans la base de données
	 */
	protected ModeleDocument addModeleDocument(TypeDocument type, PeriodeFiscale periode) {
		assertNotNull(type);
		assertNotNull(periode);
		ModeleDocument doc = new ModeleDocument();
		doc.setTypeDocument(type);
		doc.setModelesFeuilleDocument(new HashSet<ModeleFeuilleDocument>());
		doc = (ModeleDocument) hibernateTemplate.merge(doc);
		periode.addModeleDocument(doc);
		return doc;
	}

	/**
	 * Ajoute un nouveau modele de feuille dans la base de données
	 */
	protected ModeleFeuilleDocument addModeleFeuilleDocument(String intitule, String numero, ModeleDocument modeleDoc) {
		assertNotNull(intitule);
		assertNotNull(numero);
		ModeleFeuilleDocument feuille = new ModeleFeuilleDocument();
		feuille.setNumeroFormulaire(numero);
		feuille.setIntituleFeuille(intitule);
		feuille.setModeleDocument(modeleDoc);
		feuille = (ModeleFeuilleDocument) hibernateTemplate.merge(feuille);
		return feuille;
	}

	protected CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative oid) {
		CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(oid.getNoColAdm());
		ca = (CollectiviteAdministrative) hibernateTemplate.merge(ca);
		hibernateTemplate.flush();
		return ca;
	}

	/**
	 * Ajoute une tâche d'envoi de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheEnvoiDeclarationImpot addTacheEnvoiDI(TypeEtatTache etat, RegDate dateEcheance, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable, TypeDocument typeDocument,
	                                                     Contribuable contribuable, Qualification qualification, CollectiviteAdministrative colAdm) {

		TacheEnvoiDeclarationImpot tache = new TacheEnvoiDeclarationImpot(etat, dateEcheance, contribuable, dateDebut, dateFin,
				typeContribuable, typeDocument, qualification, TypeAdresseRetour.CEDI, colAdm);

		tache = (TacheEnvoiDeclarationImpot) hibernateTemplate.merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheAnnulationDeclarationImpot addTacheAnnulDI(TypeEtatTache etat, RegDate dateEcheance, DeclarationImpotOrdinaire declaration, Contribuable contribuable,
	                                                          CollectiviteAdministrative colAdm) {

		TacheAnnulationDeclarationImpot tache = new TacheAnnulationDeclarationImpot(etat, dateEcheance, contribuable, declaration, colAdm);
		tache = (TacheAnnulationDeclarationImpot) hibernateTemplate.merge(tache);
		return tache;
	}

	/**
	 * Ajoute une tâche d'annulation de déclaration d'impôt avec les paramètres spécifiés.
	 */
	protected TacheControleDossier addTacheControleDossier(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, CollectiviteAdministrative colAdm) {

		TacheControleDossier tache = new TacheControleDossier(etat, dateEcheance, contribuable, colAdm);
		tache = (TacheControleDossier) hibernateTemplate.merge(tache);
		return tache;
	}

	protected PersonnePhysique addHabitant(long noIndividu) {
		PersonnePhysique hab = new PersonnePhysique(true);
		hab.setNumeroIndividu(noIndividu);
		return (PersonnePhysique) hibernateTemplate.merge(hab);
	}

	/**
	 * Crée et ajoute dans la base de données un non-habitant minimal.
	 */
	protected PersonnePhysique addNonHabitant(String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		PersonnePhysique nh = new PersonnePhysique(false);
		nh.setPrenom(prenom);
		nh.setNom(nom);
		nh.setDateNaissance(dateNaissance);
		nh.setSexe(sexe);
		return (PersonnePhysique) hibernateTemplate.merge(nh);
	}

	protected DebiteurPrestationImposable addDebiteur() {
		DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi = (DebiteurPrestationImposable) hibernateTemplate.merge(dpi);
		return dpi;
	}

	protected DeclarationImpotSource addLR(DebiteurPrestationImposable debiteur, RegDate debut, RegDate fin, PeriodeFiscale periode) {
		DeclarationImpotSource lr = new DeclarationImpotSource();
		lr.setDateDebut(debut);
		lr.setDateFin(fin);
		lr.setPeriode(periode);
		lr.setModeCommunication(ModeCommunication.PAPIER);
		lr.setPeriodicite(debiteur.getPeriodiciteDecompte());
		lr.setTiers(debiteur);
		lr = (DeclarationImpotSource) hibernateTemplate.merge(lr);
		debiteur.addDeclaration(lr);
		return lr;
	}

	protected DebiteurPrestationImposable addDebiteur(CategorieImpotSource categorie, PeriodiciteDecompte periodicite) {
		DebiteurPrestationImposable debiteur = addDebiteur();
		debiteur.setCategorieImpotSource(categorie);
		debiteur.setPeriodiciteDecompte(periodicite);
		return debiteur;
	}

	protected ForDebiteurPrestationImposable addForDebiteur(DebiteurPrestationImposable dpi, RegDate dateDebut, RegDate dateFin, TypeAutoriteFiscale typeAutorite, MockCommune commune) {
		ForDebiteurPrestationImposable f = new ForDebiteurPrestationImposable();
		f.setTypeAutoriteFiscale(typeAutorite);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFS());
		f.setDateDebut(dateDebut);
		f.setDateFin(dateFin);
		return (ForDebiteurPrestationImposable)tiersService.addAndSave(dpi, f);
	}

	protected ForFiscalAutreElementImposable addForAutreElementImposable(Contribuable ctb, RegDate dateDebut, RegDate dateFin, MockCommune commune, TypeAutoriteFiscale taf,
	                                                                     MotifRattachement rattachement) {
		ForFiscalAutreElementImposable f = new ForFiscalAutreElementImposable();
		f.setDateDebut(dateDebut);
		f.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
		f.setDateFin(dateFin);
		if (dateFin != null) {
			f.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
		}
		f.setTypeAutoriteFiscale(taf);
		f.setNumeroOfsAutoriteFiscale(commune.getNoOFSEtendu());
		f.setMotifRattachement(rattachement);
		return (ForFiscalAutreElementImposable) tiersService.addAndSave(ctb, f);
	}

	/**
	 * Crée et ajoute dans la base de données un menage-commun.
	 */
	protected EnsembleTiersCouple createEnsembleTiersCouple(PersonnePhysique principal, PersonnePhysique conjoint, RegDate dateMariage) {

		MenageCommun menage = (MenageCommun) hibernateTemplate.merge(new MenageCommun());
		principal = (PersonnePhysique) hibernateTemplate.merge(principal);
		if (conjoint != null) {
			conjoint = (PersonnePhysique) hibernateTemplate.merge(conjoint);
		}

		RapportEntreTiers rapport = new AppartenanceMenage();
		rapport.setDateDebut(dateMariage);
		rapport.setObjet(menage);
		rapport.setSujet(principal);
		rapport = (RapportEntreTiers) hibernateTemplate.merge(rapport);

		menage.addRapportObjet(rapport);
		principal.addRapportSujet(rapport);

		if (conjoint != null) {
			rapport = new AppartenanceMenage();
			rapport.setDateDebut(dateMariage);
			rapport.setObjet(menage);
			rapport.setSujet(conjoint);
			rapport = (RapportEntreTiers) hibernateTemplate.merge(rapport);

			menage.addRapportObjet(rapport);
			conjoint.addRapportSujet(rapport);
		}

		EnsembleTiersCouple ensemble = new EnsembleTiersCouple();
		ensemble.setMenage(menage);
		ensemble.setPrincipal(principal);
		ensemble.setConjoint(conjoint);

		return ensemble;
	}

	/**
	 * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
	 */
	protected DeclarationImpotOrdinaire addDeclarationImpot(Contribuable tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
			TypeContribuable typeC, ModeleDocument modele) {

		DeclarationImpotOrdinaire d = new DeclarationImpotOrdinaire();
		d.setPeriode(periode);
		d.setDateDebut(debut);
		d.setDateFin(fin);
		d.setTypeContribuable(typeC);
		d.setModeleDocument(modele);

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		assertNotNull(cedi);
		d.setRetourCollectiviteAdministrative(cedi);

		int numero = 0;
		final int annee = periode.getAnnee().intValue();
		Set<Declaration> decls = tiers.getDeclarations();
		if (decls != null) {
			for (Declaration dd : decls) {
				if (!dd.isAnnule() && dd.getPeriode().getAnnee() == annee) {
					++numero;
				}
			}
		}
		d.setNumero(numero + 1);

		d.setTiers(tiers);
		d = (DeclarationImpotOrdinaire) hibernateTemplate.merge(d);

		tiers.addDeclaration(d);
		return d;
	}

	protected void addEtatDeclaration(Declaration declaration, RegDate dateObtention, TypeEtatDeclaration type) {
		EtatDeclaration etat = new EtatDeclaration();
		etat.setDateObtention(dateObtention);
		etat.setEtat(type);
		declaration.addEtat(etat);
		hibernateTemplate.merge(declaration);
	}

	protected void addDelaiDeclaration(Declaration declaration, RegDate dateTraitement, RegDate delaiAccordeAu) {
		DelaiDeclaration delai = new DelaiDeclaration();
		delai.setDateTraitement(dateTraitement);
		delai.setDateDemande(dateTraitement);
		delai.setDelaiAccordeAu(delaiAccordeAu);
		declaration.addDelai(delai);
		hibernateTemplate.merge(declaration);
	}

	/**
	 * Ajoute un droit d'accès (autorisation ou interdiction) entre un opérateur et un tiers.
	 */
	protected DroitAcces addDroitAcces(long noIndOperateur, PersonnePhysique pp, TypeDroitAcces type, Niveau niveau, RegDate debut,
			RegDate fin) {

		DroitAcces da = new DroitAcces();
		da.setDateDebut(debut);
		da.setDateFin(fin);
		da.setNoIndividuOperateur(noIndOperateur);
		da.setType(type);
		da.setNiveau(niveau);
		da.setTiers(pp);

		da = (DroitAcces) hibernateTemplate.merge(da);
		return da;
	}

	/**
	 * Crée et ajoute dans la base un événement civil unitaire.
	 */
	protected EvenementCivilUnitaire addEvCivUnit(long id, RegDate dateEvenement, MockCommune communeAnnonce, long noIndividu,
			TypeEvenementCivil type) {
		EvenementCivilUnitaire e = new EvenementCivilUnitaire();
		e.setId(id);
		e.setDateEvenement(dateEvenement);
		e.setNumeroOfsCommuneAnnonce(communeAnnonce.getNoOFSEtendu());
		e.setEtat(EtatEvenementCivil.A_TRAITER);
		e.setNumeroIndividu(noIndividu);
		e.setType(type);

		e = (EvenementCivilUnitaire) hibernateTemplate.merge(e);
		return e;
	}

	protected SituationFamille addSituation(PersonnePhysique pp, RegDate debut, RegDate fin, Integer nombreEnfants) {
		SituationFamille situation = new SituationFamille();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		return tiersService.addAndSave(pp, situation);
	}

	protected SituationFamilleMenageCommun addSituation(MenageCommun menage, RegDate debut, RegDate fin, int nombreEnfants,
			TarifImpotSource tarif) {
		SituationFamilleMenageCommun situation = new SituationFamilleMenageCommun();
		situation.setDateDebut(debut);
		situation.setDateFin(fin);
		situation.setNombreEnfants(nombreEnfants);
		situation.setTarifApplicable(tarif);
		return (SituationFamilleMenageCommun) tiersService.addAndSave(menage, situation);
	}

	/**
	 * Raccourci pour créer une RegDate.
	 */
	protected static RegDate date(int year, int month, int day) {
		return RegDate.get(year, month, day);
	}
}
