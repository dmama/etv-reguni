package ch.vd.uniregctb.tiers;

import java.sql.Timestamp;
import java.util.HashSet;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackException;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.type.CategorieIdentifiant;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class TiersBasic {

	/**
	 * Charge la base de données avec les mêmes données que celles définies dans le fichier tiers-basic.xml.
	 *
	 * @param hibernateTemplate  l'hibernate template
	 * @param transactionManager le transaction manager
	 * @throws Exception en cas de problème
	 */
	public static void loadDatabase(final HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager) throws Exception {

		doInNewTransaction(new TransactionCallback<Object>() {
			@SuppressWarnings({"unchecked", "UnusedAssignment"})
			@Override
			public Object doInTransaction(TransactionStatus status) {

				PersonnePhysique pp0 = new PersonnePhysique();
				pp0.setNumero(10006789L);
				pp0.setMouvementsDossier(new HashSet());
				pp0.setSituationsFamille(new HashSet());
				pp0.setDebiteurInactif(false);
				pp0.setLogModifDate(new Timestamp(1199142000000L));
				pp0.setIdentificationsPersonnes(new HashSet());
				pp0.setNumeroIndividu(282315L);
				pp0.setHabitant(true);
				pp0.setAdressesTiers(new HashSet());
				pp0.setDeclarations(new HashSet());
				pp0.setDroitsAccesAppliques(new HashSet());
				pp0.setForsFiscaux(new HashSet());
				pp0.setRapportsObjet(new HashSet());
				pp0.setRapportsSujet(new HashSet());
				pp0 = hibernateTemplate.merge(pp0);

				DebiteurPrestationImposable dpi0 = new DebiteurPrestationImposable();
				dpi0.setNumero(1001234L);
				dpi0.setCategorieImpotSource(CategorieImpotSource.ADMINISTRATEURS);
				dpi0.setPeriodicites(new HashSet());
				dpi0.setDebiteurInactif(false);
				dpi0.setLogModifDate(new Timestamp(1199142000000L));
				dpi0.setModeCommunication(ModeCommunication.SITE_WEB);
				dpi0.setPeriodiciteDecompteAvantMigration(PeriodiciteDecompte.MENSUEL);
				dpi0.setAdressesTiers(new HashSet());
				dpi0.setDeclarations(new HashSet());
				dpi0.setForsFiscaux(new HashSet());
				dpi0.setRapportsObjet(new HashSet());
				dpi0.setRapportsSujet(new HashSet());
				dpi0 = hibernateTemplate.merge(dpi0);

				Entreprise e0 = new Entreprise();
				e0.setNumero(9876L);
				e0.setMouvementsDossier(new HashSet());
				e0.setSituationsFamille(new HashSet());
				e0.setDebiteurInactif(false);
				e0.setLogModifDate(new Timestamp(1199142000000L));
				e0.setAdressesTiers(new HashSet());
				e0.setDeclarations(new HashSet());
				e0.setForsFiscaux(new HashSet());
				e0.setRapportsObjet(new HashSet());
				e0.setRapportsSujet(new HashSet());
				e0 = hibernateTemplate.merge(e0);

				PersonnePhysique pp1 = new PersonnePhysique();
				pp1.setNumero(10007890L);
				pp1.setMouvementsDossier(new HashSet());
				pp1.setSituationsFamille(new HashSet());
				pp1.setDebiteurInactif(false);
				pp1.setLogModifDate(new Timestamp(1199142000000L));
				pp1.setIdentificationsPersonnes(new HashSet());
				pp1.setNumeroIndividu(333528L);
				pp1.setHabitant(true);
				pp1.setAdressesTiers(new HashSet());
				pp1.setDeclarations(new HashSet());
				pp1.setDroitsAccesAppliques(new HashSet());
				pp1.setForsFiscaux(new HashSet());
				pp1.setRapportsObjet(new HashSet());
				pp1.setRapportsSujet(new HashSet());
				pp1 = hibernateTemplate.merge(pp1);

				MenageCommun mc0 = new MenageCommun();
				mc0.setNumero(10008901L);
				mc0.setMouvementsDossier(new HashSet());
				mc0.setSituationsFamille(new HashSet());
				mc0.setDebiteurInactif(false);
				mc0.setLogModifDate(new Timestamp(1199142000000L));
				mc0.setAdressesTiers(new HashSet());
				mc0.setDeclarations(new HashSet());
				mc0.setForsFiscaux(new HashSet());
				mc0.setRapportsObjet(new HashSet());
				mc0.setRapportsSujet(new HashSet());
				mc0 = hibernateTemplate.merge(mc0);

				PersonnePhysique pp2 = new PersonnePhysique();
				pp2.setNumero(10001111L);
				pp2.setMouvementsDossier(new HashSet());
				pp2.setSituationsFamille(new HashSet());
				pp2.setDebiteurInactif(false);
				pp2.setLogModifDate(new Timestamp(1199142000000L));
				pp2.setDateNaissance(RegDate.get(1970, 1, 23));
				pp2.setNom("Conchita");
				pp2.setNumeroOfsNationalite(8212);
				pp2.setNumeroAssureSocial("1245100071000");
				pp2.setPrenom("Andrea");
				pp2.setSexe(Sexe.FEMININ);
				pp2.setIdentificationsPersonnes(new HashSet());
				pp2.setNumeroIndividu(10001111L);
				pp2.setHabitant(false);
				pp2.setAdressesTiers(new HashSet());
				pp2.setDeclarations(new HashSet());
				pp2.setDroitsAccesAppliques(new HashSet());
				pp2.setForsFiscaux(new HashSet());
				pp2.setRapportsObjet(new HashSet());
				pp2.setRapportsSujet(new HashSet());
				pp2 = hibernateTemplate.merge(pp2);

				AutreCommunaute ac0 = new AutreCommunaute();
				ac0.setNumero(2002222L);
				ac0.setFormeJuridique(FormeJuridique.ASS);
				ac0.setNom("Communaute XYZ");
				ac0.setMouvementsDossier(new HashSet());
				ac0.setSituationsFamille(new HashSet());
				ac0.setDebiteurInactif(false);
				ac0.setLogModifDate(new Timestamp(1199142000000L));
				ac0.setAdressesTiers(new HashSet());
				ac0.setDeclarations(new HashSet());
				ac0.setForsFiscaux(new HashSet());
				ac0.setRapportsObjet(new HashSet());
				ac0.setRapportsSujet(new HashSet());
				ac0 = hibernateTemplate.merge(ac0);

				PersonnePhysique pp3 = new PersonnePhysique();
				pp3.setNumero(10000010L);
				pp3.setMouvementsDossier(new HashSet());
				pp3.setSituationsFamille(new HashSet());
				pp3.setDebiteurInactif(false);
				pp3.setLogModifDate(new Timestamp(1199142000000L));
				pp3.setIdentificationsPersonnes(new HashSet());
				pp3.setNumeroIndividu(333526L);
				pp3.setHabitant(true);
				pp3.setAdressesTiers(new HashSet());
				pp3.setDeclarations(new HashSet());
				pp3.setDroitsAccesAppliques(new HashSet());
				pp3.setForsFiscaux(new HashSet());
				pp3.setRapportsObjet(new HashSet());
				pp3.setRapportsSujet(new HashSet());
				pp3 = hibernateTemplate.merge(pp3);

				PersonnePhysique pp4 = new PersonnePhysique();
				pp4.setNumero(10000001L);
				pp4.setMouvementsDossier(new HashSet());
				pp4.setSituationsFamille(new HashSet());
				pp4.setDebiteurInactif(false);
				pp4.setLogModifDate(new Timestamp(1199142000000L));
				pp4.setIdentificationsPersonnes(new HashSet());
				pp4.setNumeroIndividu(333529L);
				pp4.setHabitant(true);
				pp4.setAdressesTiers(new HashSet());
				pp4.setDeclarations(new HashSet());
				pp4.setDroitsAccesAppliques(new HashSet());
				pp4.setForsFiscaux(new HashSet());
				pp4.setRapportsObjet(new HashSet());
				pp4.setRapportsSujet(new HashSet());
				pp4 = hibernateTemplate.merge(pp4);

				PersonnePhysique pp5 = new PersonnePhysique();
				pp5.setNumero(10000002L);
				pp5.setMouvementsDossier(new HashSet());
				pp5.setSituationsFamille(new HashSet());
				pp5.setDebiteurInactif(false);
				pp5.setLogModifDate(new Timestamp(1199142000000L));
				pp5.setIdentificationsPersonnes(new HashSet());
				pp5.setNumeroIndividu(333527L);
				pp5.setHabitant(true);
				pp5.setAdressesTiers(new HashSet());
				pp5.setDeclarations(new HashSet());
				pp5.setDroitsAccesAppliques(new HashSet());
				pp5.setForsFiscaux(new HashSet());
				pp5.setRapportsObjet(new HashSet());
				pp5.setRapportsSujet(new HashSet());
				pp5 = hibernateTemplate.merge(pp5);

				PersonnePhysique pp6 = new PersonnePhysique();
				pp6.setNumero(10000004L);
				pp6.setMouvementsDossier(new HashSet());
				pp6.setSituationsFamille(new HashSet());
				pp6.setDebiteurInactif(false);
				pp6.setLogModifDate(new Timestamp(1199142000000L));
				pp6.setIdentificationsPersonnes(new HashSet());
				pp6.setNumeroIndividu(333525L);
				pp6.setHabitant(true);
				pp6.setAdressesTiers(new HashSet());
				pp6.setDeclarations(new HashSet());
				pp6.setDroitsAccesAppliques(new HashSet());
				pp6.setForsFiscaux(new HashSet());
				pp6.setRapportsObjet(new HashSet());
				pp6.setRapportsSujet(new HashSet());
				pp6 = hibernateTemplate.merge(pp6);

				PersonnePhysique pp7 = new PersonnePhysique();
				pp7.setNumero(10000005L);
				pp7.setMouvementsDossier(new HashSet());
				pp7.setSituationsFamille(new HashSet());
				pp7.setDebiteurInactif(false);
				pp7.setLogModifDate(new Timestamp(1199142000000L));
				pp7.setIdentificationsPersonnes(new HashSet());
				pp7.setNumeroIndividu(333524L);
				pp7.setHabitant(true);
				pp7.setAdressesTiers(new HashSet());
				pp7.setDeclarations(new HashSet());
				pp7.setDroitsAccesAppliques(new HashSet());
				pp7.setForsFiscaux(new HashSet());
				pp7.setRapportsObjet(new HashSet());
				pp7.setRapportsSujet(new HashSet());
				pp7 = hibernateTemplate.merge(pp7);

				PersonnePhysique pp8 = new PersonnePhysique();
				pp8.setNumero(10000005L);
				pp8.setMouvementsDossier(new HashSet());
				pp8.setSituationsFamille(new HashSet());
				pp8.setDebiteurInactif(false);
				pp8.setLogModifDate(new Timestamp(1199142000000L));
				pp8.setIdentificationsPersonnes(new HashSet());
				pp8.setNumeroIndividu(333524L);
				pp8.setHabitant(true);
				pp8.setAdressesTiers(new HashSet());
				pp8.setDeclarations(new HashSet());
				pp8.setDroitsAccesAppliques(new HashSet());
				pp8.setForsFiscaux(new HashSet());
				pp8.setRapportsObjet(new HashSet());
				pp8.setRapportsSujet(new HashSet());
				pp8.setLogCreationDate(DateHelper.getDate(2010, 11, 20));
				pp8 = hibernateTemplate.merge(pp8);

				AdresseSuisse as0 = new AdresseSuisse();
				as0.setDateDebut(RegDate.get(2006, 2, 23));
				as0.setLogModifDate(new Timestamp(1199142000000L));
				as0.setNumeroMaison("12");
				as0.setNumeroOrdrePoste(104);
				as0.setPermanente(false);
				as0.setUsage(TypeAdresseTiers.COURRIER);
				pp0.addAdresseTiers(as0);
				pp0 = hibernateTemplate.merge(pp0);

				AdresseSuisse as1 = new AdresseSuisse();
				as1.setDateDebut(RegDate.get(2005, 2, 23));
				as1.setDateFin(RegDate.get(2006, 2, 22));
				as1.setLogModifDate(new Timestamp(1199142000000L));
				as1.setNumeroMaison("9");
				as1.setNumeroOrdrePoste(104);
				as1.setPermanente(false);
				as1.setUsage(TypeAdresseTiers.COURRIER);
				pp0.addAdresseTiers(as1);
				pp0 = hibernateTemplate.merge(pp0);

				AdresseSuisse as2 = new AdresseSuisse();
				as2.setDateDebut(RegDate.get(2006, 2, 23));
				as2.setLogModifDate(new Timestamp(1199142000000L));
				as2.setNumeroMaison("56");
				as2.setNumeroOrdrePoste(104);
				as2.setPermanente(false);
				as2.setUsage(TypeAdresseTiers.COURRIER);
				pp2.addAdresseTiers(as2);
				pp2 = hibernateTemplate.merge(pp2);

				AdresseSuisse as3 = new AdresseSuisse();
				as3.setDateDebut(RegDate.get(2006, 2, 23));
				as3.setLogModifDate(new Timestamp(1199142000000L));
				as3.setNumeroMaison("8");
				as3.setNumeroOrdrePoste(104);
				as3.setPermanente(false);
				as3.setUsage(TypeAdresseTiers.COURRIER);
				dpi0.addAdresseTiers(as3);
				dpi0 = hibernateTemplate.merge(dpi0);

				AdresseSuisse as4 = new AdresseSuisse();
				as4.setDateDebut(RegDate.get(2006, 2, 23));
				as4.setLogModifDate(new Timestamp(1199142000000L));
				as4.setNumeroMaison("8");
				as4.setNumeroOrdrePoste(104);
				as4.setPermanente(false);
				as4.setUsage(TypeAdresseTiers.COURRIER);
				e0.addAdresseTiers(as4);
				e0 = hibernateTemplate.merge(e0);

				AppartenanceMenage am0 = new AppartenanceMenage();
				am0.setLogModifDate(new Timestamp(1199142000000L));
				am0.setDateDebut(RegDate.get(2006, 9, 1));
				am0.setObjetId(10008901L);
				am0.setSujetId(10006789L);
				am0 = hibernateTemplate.merge(am0);
				pp0.addRapportSujet(am0);
				mc0.addRapportObjet(am0);

				AppartenanceMenage am1 = new AppartenanceMenage();
				am1.setLogModifDate(new Timestamp(1199142000000L));
				am1.setDateDebut(RegDate.get(2006, 9, 1));
				am1.setObjetId(10008901L);
				am1.setSujetId(10007890L);
				am1 = hibernateTemplate.merge(am1);
				pp1.addRapportSujet(am1);
				mc0.addRapportObjet(am1);

				RapportPrestationImposable rpi0 = new RapportPrestationImposable();
				rpi0.setLogModifDate(new Timestamp(1199142000000L));
				rpi0.setDateDebut(RegDate.get(2000, 1, 1));
				rpi0.setObjetId(1001234L);
				rpi0.setSujetId(10001111L);
				rpi0 = hibernateTemplate.merge(rpi0);
				pp2.addRapportSujet(rpi0);
				dpi0.addRapportObjet(rpi0);

				ContactImpotSource cis0 = new ContactImpotSource();
				cis0.setLogModifDate(new Timestamp(1199142000000L));
				cis0.setDateDebut(RegDate.get(2000, 1, 1));
				cis0.setObjetId(1001234L);
				cis0.setSujetId(10006789L);
				cis0 = hibernateTemplate.merge(cis0);
				pp0.addRapportSujet(cis0);
				dpi0.addRapportObjet(cis0);

				ForFiscalPrincipal ffp0 = new ForFiscalPrincipal();
				ffp0.setDateFin(RegDate.get(2006, 8, 31));
				ffp0.setDateDebut(RegDate.get(2006, 3, 1));
				ffp0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				ffp0.setLogModifDate(new Timestamp(1199142000000L));
				ffp0.setModeImposition(ModeImposition.ORDINAIRE);
				ffp0.setMotifRattachement(MotifRattachement.DOMICILE);
				ffp0.setNumeroOfsAutoriteFiscale(6200);
				ffp0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
				ffp0.setLogCreationDate(DateHelper.getDate(2010, 11, 20));
				pp0.addForFiscal(ffp0);
				pp0 = hibernateTemplate.merge(pp0);

				ForFiscalSecondaire ffs0 = new ForFiscalSecondaire();
				ffs0.setDateFin(RegDate.get(2006, 8, 31));
				ffs0.setDateDebut(RegDate.get(2006, 5, 1));
				ffs0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				ffs0.setLogModifDate(new Timestamp(1199142000000L));
				ffs0.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
				ffs0.setMotifOuverture(MotifFor.DEBUT_EXPLOITATION);
				ffs0.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
				ffs0.setNumeroOfsAutoriteFiscale(8212);
				ffs0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				pp0.addForFiscal(ffs0);
				pp0 = hibernateTemplate.merge(pp0);

				IdentificationPersonne ip0 = new IdentificationPersonne();
				ip0.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
				ip0.setIdentifiant("15489652357");
				ip0.setLogModifDate(new Timestamp(1199142000000L));
				pp2.addIdentificationPersonne(ip0);
				pp2 = hibernateTemplate.merge(pp2);

				IdentificationPersonne ip1 = new IdentificationPersonne();
				ip1.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
				ip1.setIdentifiant("99999999");
				ip1.setLogModifDate(new Timestamp(1199142000000L));
				pp2.addIdentificationPersonne(ip1);
				pp2 = hibernateTemplate.merge(pp2);

				return null;
			}
		}, transactionManager);
	}

	private static <T> T doInNewTransaction(TransactionCallback<T> action, PlatformTransactionManager transactionManager) throws Exception {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
		try {
			return template.execute(action);
		}
		catch (TxCallbackException e) {
			throw (Exception) e.getCause();
		}
	}

}
