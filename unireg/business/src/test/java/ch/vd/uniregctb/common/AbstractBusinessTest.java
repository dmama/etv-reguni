package ch.vd.uniregctb.common;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.*;
import ch.vd.uniregctb.evenement.EvenementCivilUnitaire;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionCallback;

import java.sql.SQLException;
import java.util.Set;

import static junit.framework.Assert.*;

/**
 * @author
 *
 */
// Surcharge des fichiers de config Spring. Il faut mettre les fichiers
// UT a la fin
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CACHE,
		BusinessTestingConstants.UNIREG_BUSINESS_INTERFACES,
		BusinessTestingConstants.UNIREG_BUSINESS_SERVICES,
		BusinessTestingConstants.UNIREG_BUSINESS_JOBS,
		BusinessTestingConstants.UNIREG_BUSINESS_EVT_CIVIL,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_EDITIQUE,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_JMS,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_SERVICES,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
		BusinessTestingConstants.UNIREG_BUSINESS_UT_CONFIG
})
public abstract class AbstractBusinessTest extends AbstractCoreDAOTest {

	// private static final Logger LOGGER = Logger.getLogger(AbstractBusinessTest.class);

	private boolean wantIndexation = false;
	protected TiersService tiersService;

	protected abstract void indexData() throws Exception;

	protected abstract void removeIndexData() throws Exception;

	@Override
	protected void runOnSetUp() throws Exception {
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
	 * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune) {
		TypeAutoriteFiscale type = commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC;
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune.getNoOFS(), type, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal ouvert sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockCommune commune, MotifRattachement motifRattachement) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, commune, motifRattachement);
	}

	/**
	 * Ajoute un for principal fermé sur une commune Suisse (rattachement = DOMICILE) sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockCommune commune) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal fermé sur une commune Suisse sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement) {
		final TypeAutoriteFiscale type = (commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC);
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, commune.getNoOFS(), type, motifRattachement);
	}

	/**
	 * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays) {
		return addForPrincipal(contribuable, ouverture, motifOuverture, pays, MotifRattachement.DOMICILE);
	}

	/**
	 * Ajoute un for principal ouvert à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, MockPays pays, MotifRattachement motifRattachement) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, null, null, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, motifRattachement);
	}

	/**
	 * Ajoute un for principal fermé à l'étranger sur le contribuable spécifié.
	 */
	protected ForFiscalPrincipal addForPrincipal(Contribuable contribuable, RegDate ouverture, MotifFor motifOuverture, RegDate fermeture, MotifFor motifFermeture, MockPays pays) {
		assertFalse("Il faut spécifier la commune pour les fors en Suisse", "CH".equals(pays.getSigleOFS()));
		return addForPrincipal(contribuable, ouverture, motifOuverture, fermeture, motifFermeture, pays.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE);
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

	protected AdresseEtrangere addAdresseEtrangere(Tiers tiers, TypeAdresseTiers usage, RegDate debut, RegDate fin, String numeroPostalEtLocalite, Pays pays) {
		AdresseEtrangere adresse = new AdresseEtrangere();
		adresse.setDateDebut(debut);
		adresse.setDateFin(fin);
		adresse.setUsage(usage);
		adresse.setNumeroPostalLocalite(numeroPostalEtLocalite);
		adresse.setNumeroOfsPays(pays.getNoOFS());
		adresse = (AdresseEtrangere) tiersService.addAndSave(tiers, adresse);
		return adresse;
	}

	protected CollectiviteAdministrative addCollAdm(MockCollectiviteAdministrative oid) {
		CollectiviteAdministrative ca = new CollectiviteAdministrative();
		ca.setNumeroCollectiviteAdministrative(oid.getNoColAdm());
		ca = (CollectiviteAdministrative) hibernateTemplate.merge(ca);
		hibernateTemplate.flush();
		return ca;
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
	 * Ajoute une déclaration d'impôt ordinaire sur le contribuable spécifié.
	 */
	protected DeclarationImpotOrdinaire addDeclarationImpot(Contribuable tiers, PeriodeFiscale periode, RegDate debut, RegDate fin,
	                                                        TypeContribuable typeC, ModeleDocument modele) {

		final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noCEDI);
		assertNotNull(cedi);

		return addDeclarationImpot(tiers, periode, debut, fin, cedi, typeC, modele);
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
}
