package ch.vd.uniregctb.dao.jdbc;

import java.io.File;

import org.apache.log4j.xml.DOMConfigurator;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdressePM;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.ModeleFeuilleDocument;
import ch.vd.uniregctb.declaration.ParametrePeriodeFiscale;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.tiers.AnnuleEtRemplace;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.ConseilLegal;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.Curatelle;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreElementImposable;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tutelle;

public class JdbcDaoGenerator {

	public static void main(String[] args) throws Exception {

		final String corePath = getCorePath();
		initLog4J(corePath);

		final String jdbcDaoPath = corePath + "/src/main/java/ch/vd/uniregctb/dao/jdbc";

		JdbcHibernateEntityDaoGenerator tg = new JdbcHibernateEntityDaoGenerator(PersonnePhysique.class, AutreCommunaute.class, Entreprise.class, MenageCommun.class,
				CollectiviteAdministrative.class, Etablissement.class, DebiteurPrestationImposable.class);
		tg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcTiersDaoImpl.java.template", jdbcDaoPath + "/JdbcTiersDaoImpl.java");

		JdbcHibernateEntityDaoGenerator atg = new JdbcHibernateEntityDaoGenerator(AdresseSuisse.class, AdresseEtrangere.class, AdresseCivile.class, AdressePM.class, AdresseAutreTiers.class);
		atg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcAdresseTiersDaoImpl.java.template", jdbcDaoPath + "/JdbcAdresseTiersDaoImpl.java");

		JdbcHibernateEntityDaoGenerator dg = new JdbcHibernateEntityDaoGenerator(DeclarationImpotOrdinaire.class, DeclarationImpotSource.class);
		dg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcDeclarationDaoImpl.java.template", jdbcDaoPath + "/JdbcDeclarationDaoImpl.java");

		JdbcHibernateEntityDaoGenerator edg = new JdbcHibernateEntityDaoGenerator(EtatDeclaration.class);
		edg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcEtatDeclarationDaoImpl.java.template", jdbcDaoPath + "/JdbcEtatDeclarationDaoImpl.java");

		JdbcHibernateEntityDaoGenerator ffg = new JdbcHibernateEntityDaoGenerator(ForFiscalPrincipal.class, ForFiscalSecondaire.class,
				ForFiscalAutreElementImposable.class, ForFiscalAutreImpot.class, ForDebiteurPrestationImposable.class);
		ffg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcForFiscalDaoImpl.java.template", jdbcDaoPath + "/JdbcForFiscalDaoImpl.java");

		JdbcHibernateEntityDaoGenerator retg = new JdbcHibernateEntityDaoGenerator(Tutelle.class, Curatelle.class, ConseilLegal.class, AnnuleEtRemplace.class, AppartenanceMenage.class,
				RapportPrestationImposable.class, RepresentationConventionnelle.class, ContactImpotSource.class);
		retg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcRapportEntreTiersDaoImpl.java.template", jdbcDaoPath + "/JdbcRapportEntreTiersDaoImpl.java");

		JdbcHibernateEntityDaoGenerator sfg = new JdbcHibernateEntityDaoGenerator(SituationFamillePersonnePhysique.class, SituationFamilleMenageCommun.class);
		sfg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcSituationFamilleDaoImpl.java.template", jdbcDaoPath + "/JdbcSituationFamilleDaoImpl.java");

		JdbcHibernateEntityDaoGenerator ipg = new JdbcHibernateEntityDaoGenerator(IdentificationPersonne.class);
		ipg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcIdentificationPersonneDaoImpl.java.template", jdbcDaoPath + "/JdbcIdentificationPersonneDaoImpl.java");

		JdbcHibernateEntityDaoGenerator pg = new JdbcHibernateEntityDaoGenerator(Periodicite.class);
		pg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcPeriodiciteDaoImpl.java.template", jdbcDaoPath + "/JdbcPeriodiciteDaoImpl.java");

		JdbcHibernateEntityDaoGenerator pfg = new JdbcHibernateEntityDaoGenerator(PeriodeFiscale.class);
		pfg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcPeriodeFiscaleDaoImpl.java.template", jdbcDaoPath + "/JdbcPeriodeFiscaleDaoImpl.java");

		JdbcHibernateEntityDaoGenerator ppfg = new JdbcHibernateEntityDaoGenerator(ParametrePeriodeFiscale.class);
		ppfg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcParametrePeriodeFiscaleDaoImpl.java.template", jdbcDaoPath + "/JdbcParametrePeriodeFiscaleDaoImpl.java");

		JdbcHibernateEntityDaoGenerator mdg = new JdbcHibernateEntityDaoGenerator(ModeleDocument.class);
		mdg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcModeleDocumentDaoImpl.java.template", jdbcDaoPath + "/JdbcModeleDocumentDaoImpl.java");

		JdbcHibernateEntityDaoGenerator mfdg = new JdbcHibernateEntityDaoGenerator(ModeleFeuilleDocument.class);
		mfdg.generate("classpath:ch/vd/uniregctb/dao/jdbc/JdbcModeleFeuilleDocumentDaoImpl.java.template", jdbcDaoPath + "/JdbcModeleFeuilleDocumentDaoImpl.java");
	}

	private static void initLog4J(String corePath) {
		final String filepath = corePath + "/src/test/resources/ddlgen/log4j.xml";
		Assert.isTrue(new File(filepath).exists(), "Pas de fichier Log4j");
		DOMConfigurator.configure(filepath);
	}

	private static String getCorePath() {
		String corePath = System.getProperty("user.dir");
		if (!corePath.endsWith("core")) {
			corePath += "/core";
		}
		return corePath;
	}
}
