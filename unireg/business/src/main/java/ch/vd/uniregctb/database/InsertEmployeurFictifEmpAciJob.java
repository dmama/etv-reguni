package ch.vd.uniregctb.database;

import java.util.Map;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.scheduler.JobDefinition;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;

/**
 * Job qui insère l'employeur fictif pour les tests de validation d'EmpACI.
 */
public class InsertEmployeurFictifEmpAciJob extends JobDefinition {

	public static final String NAME = "InsertEmployeurFictifEmpAciJob";
	private static final String CATEGORIE = "Database";

	private static final long ID_EMPLOYEUR_FICTIF = 1999999;
	private static final long ID_PP_FICTIF = 10820998;

	private TiersDAO tiersDAO;
	private PlatformTransactionManager transactionManager;

	public InsertEmployeurFictifEmpAciJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		final StatusManager status = getStatusManager();

		if (!isTesting()) {
			throw new IllegalArgumentException("Ce batch ne doit pas tourner en dehors des environnements de test.");
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus transactionStatus) {
				status.setMessage("Vérification de la base...");
				assertNonPreexistance();
				status.setMessage("Insertion de l'employeur fictif...");
				insertEmployeurFictif();
				return null;
			}
		});

		final String message = "L'employeur fictif n°" + ID_EMPLOYEUR_FICTIF + " pour EmpACI a bien été inséré dans la base de données.";
		status.setMessage(message);
		Audit.success(message);
	}

	private void insertEmployeurFictif() {
		final PersonnePhysique pp = insertPersonnePhysiqueFictive();
		final DebiteurPrestationImposable dpi = insertDebiteurFictif();
		ContactImpotSource cis = new ContactImpotSource(RegDate.get(2011, 1, 5), null, pp, dpi);
		tiersDAO.save(cis);
	}

	private DebiteurPrestationImposable insertDebiteurFictif() {

		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		dpi.setNumero(ID_EMPLOYEUR_FICTIF);
		dpi.setBlocageRemboursementAutomatique(true);
		dpi.setNumeroTelephonePrive("60415");
		dpi.setPersonneContact("c/o M. Sandro Berney");
		dpi.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi.setModeCommunication(ModeCommunication.ELECTRONIQUE);
		dpi.setNom1("Sandro KinKin");

		final Periodicite periodicite = new Periodicite();
		periodicite.setDateDebut(RegDate.get(2011, 1, 1));
		periodicite.setPeriodeDecompte(PeriodeDecompte.S1);
		periodicite.setPeriodiciteDecompte(PeriodiciteDecompte.SEMESTRIEL);
		dpi.addPeriodicite(periodicite);
		return (DebiteurPrestationImposable) tiersDAO.save(dpi);
	}

	private PersonnePhysique insertPersonnePhysiqueFictive() {

		final PersonnePhysique pp = new PersonnePhysique(false);
		pp.setNumero(ID_PP_FICTIF);
		pp.setNom("Employeur fictif validation V4");
		pp.setNumeroOfsNationalite(8228);
		pp.setSexe(Sexe.MASCULIN);

		final AdresseSuisse adresse = new AdresseSuisse();
		adresse.setDateDebut(RegDate.get(2010, 1, 1));
		adresse.setUsage(TypeAdresseTiers.COURRIER);
		adresse.setNumeroMaison("1");
		adresse.setRue("Avenue de Recordon");
		adresse.setNumeroOrdrePoste(162);
		adresse.setNumeroRue(192971);
		pp.addAdresseTiers(adresse);

		return (PersonnePhysique) tiersDAO.save(pp);
	}

	private void assertNonPreexistance() {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(ID_EMPLOYEUR_FICTIF, false);
		if (dpi != null) {
			throw new RuntimeException("L'employeur fictif n°" + ID_EMPLOYEUR_FICTIF + " existe déjà.");
		}

		final Contribuable ctb = (Contribuable) tiersDAO.get(ID_PP_FICTIF, false);
		if (ctb != null) {
			throw new RuntimeException("Le contribuable fictif n°" + ID_PP_FICTIF + " existe déjà.");
		}
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
