package ch.vd.unireg.common;

import java.sql.Timestamp;
import java.util.HashSet;

import org.junit.Ignore;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.adresse.AdresseSuisse;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEchue;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.EtatDeclarationSommee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.ModeleFeuilleDocument;
import ch.vd.unireg.declaration.ParametrePeriodeFiscalePP;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.AppartenanceMenage;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.ContactImpotSource;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForDebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.IdentificationPersonne;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.SituationFamilleMenageCommun;
import ch.vd.unireg.tiers.SituationFamillePersonnePhysique;
import ch.vd.unireg.tiers.Tutelle;
import ch.vd.unireg.type.CategorieIdentifiant;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.ModeleFeuille;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TarifImpotSource;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;

@Ignore
@SuppressWarnings({"JavaDoc"})
public class TestData {

	/**
	 * Le contenu de cette méthode a été généré automatiquement à partir du fichier DBUnit tiers-basic.xml avec la classe DbUnit2Java. Ceci pour des raisons de performance avec Oracle 11g (qui est très
	 * très très lent lorsqu'il s'agit de récupérer la méta-information du schéma, ce qui fait DBUnit)
	 */
	@SuppressWarnings({"UnusedAssignment", "unchecked"})
	public static void loadTiersBasic(HibernateTemplate hibernateTemplate, boolean withCollectivitesAdministratives) {
		PeriodeFiscale pf0 = new PeriodeFiscale();
		pf0.setAnnee(2002);
		pf0.setLogModifDate(new Timestamp(1199142000000L));
		pf0.setParametrePeriodeFiscale(new HashSet());
		pf0.setModelesDocument(new HashSet());
		pf0 = hibernateTemplate.merge(pf0);

		PeriodeFiscale pf1 = new PeriodeFiscale();
		pf1.setAnnee(2003);
		pf1.setLogModifDate(new Timestamp(1199142000000L));
		pf1.setParametrePeriodeFiscale(new HashSet());
		pf1.setModelesDocument(new HashSet());
		pf1 = hibernateTemplate.merge(pf1);

		PeriodeFiscale pf2 = new PeriodeFiscale();
		pf2.setAnnee(2004);
		pf2.setLogModifDate(new Timestamp(1199142000000L));
		pf2.setParametrePeriodeFiscale(new HashSet());
		pf2.setModelesDocument(new HashSet());
		pf2 = hibernateTemplate.merge(pf2);

		PeriodeFiscale pf3 = new PeriodeFiscale();
		pf3.setAnnee(2005);
		pf3.setLogModifDate(new Timestamp(1199142000000L));
		pf3.setParametrePeriodeFiscale(new HashSet());
		pf3.setModelesDocument(new HashSet());
		pf3 = hibernateTemplate.merge(pf3);

		PeriodeFiscale pf4 = new PeriodeFiscale();
		pf4.setAnnee(2006);
		pf4.setLogModifDate(new Timestamp(1199142000000L));
		pf4.setParametrePeriodeFiscale(new HashSet());
		pf4.setModelesDocument(new HashSet());
		pf4 = hibernateTemplate.merge(pf4);

		PeriodeFiscale pf5 = new PeriodeFiscale();
		pf5.setAnnee(2007);
		pf5.setLogModifDate(new Timestamp(1199142000000L));
		pf5.setParametrePeriodeFiscale(new HashSet());
		pf5.setModelesDocument(new HashSet());
		pf5 = hibernateTemplate.merge(pf5);

		PeriodeFiscale pf6 = new PeriodeFiscale();
		pf6.setAnnee(2008);
		pf6.setLogModifDate(new Timestamp(1199142000000L));
		pf6.setParametrePeriodeFiscale(new HashSet());
		pf6.setModelesDocument(new HashSet());
		pf6 = hibernateTemplate.merge(pf6);

		PeriodeFiscale pf7 = new PeriodeFiscale();
		pf7.setAnnee(2009);
		pf7.setLogModifDate(new Timestamp(1199142000000L));
		pf7.setParametrePeriodeFiscale(new HashSet());
		pf7.setModelesDocument(new HashSet());
		pf7 = hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscalePP ppf0 = new ParametrePeriodeFiscalePP();
		ppf0.setDateFinEnvoiMasseDI(RegDate.get(2003, 4, 30));
		ppf0.setLogModifDate(new Timestamp(1199142000000L));
		ppf0.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf0.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf0.addParametrePeriodeFiscale(ppf0);
		pf0 = hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscalePP ppf1 = new ParametrePeriodeFiscalePP();
		ppf1.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf1.setLogModifDate(new Timestamp(1199142000000L));
		ppf1.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf1.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf1.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf0.addParametrePeriodeFiscale(ppf1);
		pf0 = hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscalePP ppf2 = new ParametrePeriodeFiscalePP();
		ppf2.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf2.setLogModifDate(new Timestamp(1199142000000L));
		ppf2.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf2.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf2.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf0.addParametrePeriodeFiscale(ppf2);
		pf0 = hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscalePP ppf3 = new ParametrePeriodeFiscalePP();
		ppf3.setDateFinEnvoiMasseDI(RegDate.get(2003, 6, 30));
		ppf3.setLogModifDate(new Timestamp(1199142000000L));
		ppf3.setTermeGeneralSommationEffectif(RegDate.get(2003, 3, 31));
		ppf3.setTermeGeneralSommationReglementaire(RegDate.get(2003, 1, 31));
		ppf3.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf0.addParametrePeriodeFiscale(ppf3);
		pf0 = hibernateTemplate.merge(pf0);

		ParametrePeriodeFiscalePP ppf4 = new ParametrePeriodeFiscalePP();
		ppf4.setDateFinEnvoiMasseDI(RegDate.get(2004, 4, 30));
		ppf4.setLogModifDate(new Timestamp(1199142000000L));
		ppf4.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf4.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf4.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf1.addParametrePeriodeFiscale(ppf4);
		pf1 = hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscalePP ppf5 = new ParametrePeriodeFiscalePP();
		ppf5.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf5.setLogModifDate(new Timestamp(1199142000000L));
		ppf5.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf5.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf5.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf1.addParametrePeriodeFiscale(ppf5);
		pf1 = hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscalePP ppf6 = new ParametrePeriodeFiscalePP();
		ppf6.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf6.setLogModifDate(new Timestamp(1199142000000L));
		ppf6.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf6.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf6.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf1.addParametrePeriodeFiscale(ppf6);
		pf1 = hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscalePP ppf7 = new ParametrePeriodeFiscalePP();
		ppf7.setDateFinEnvoiMasseDI(RegDate.get(2004, 6, 30));
		ppf7.setLogModifDate(new Timestamp(1199142000000L));
		ppf7.setTermeGeneralSommationEffectif(RegDate.get(2004, 3, 31));
		ppf7.setTermeGeneralSommationReglementaire(RegDate.get(2004, 1, 31));
		ppf7.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf1.addParametrePeriodeFiscale(ppf7);
		pf1 = hibernateTemplate.merge(pf1);

		ParametrePeriodeFiscalePP ppf8 = new ParametrePeriodeFiscalePP();
		ppf8.setDateFinEnvoiMasseDI(RegDate.get(2005, 4, 30));
		ppf8.setLogModifDate(new Timestamp(1199142000000L));
		ppf8.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf8.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf8.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf2.addParametrePeriodeFiscale(ppf8);
		pf2 = hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscalePP ppf9 = new ParametrePeriodeFiscalePP();
		ppf9.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf9.setLogModifDate(new Timestamp(1199142000000L));
		ppf9.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf9.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf9.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf2.addParametrePeriodeFiscale(ppf9);
		pf2 = hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscalePP ppf10 = new ParametrePeriodeFiscalePP();
		ppf10.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf10.setLogModifDate(new Timestamp(1199142000000L));
		ppf10.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf10.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf10.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf2.addParametrePeriodeFiscale(ppf10);
		pf2 = hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscalePP ppf11 = new ParametrePeriodeFiscalePP();
		ppf11.setDateFinEnvoiMasseDI(RegDate.get(2005, 6, 30));
		ppf11.setLogModifDate(new Timestamp(1199142000000L));
		ppf11.setTermeGeneralSommationEffectif(RegDate.get(2005, 3, 31));
		ppf11.setTermeGeneralSommationReglementaire(RegDate.get(2005, 1, 31));
		ppf11.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf2.addParametrePeriodeFiscale(ppf11);
		pf2 = hibernateTemplate.merge(pf2);

		ParametrePeriodeFiscalePP ppf12 = new ParametrePeriodeFiscalePP();
		ppf12.setDateFinEnvoiMasseDI(RegDate.get(2006, 4, 30));
		ppf12.setLogModifDate(new Timestamp(1199142000000L));
		ppf12.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf12.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf12.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf3.addParametrePeriodeFiscale(ppf12);
		pf3 = hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscalePP ppf13 = new ParametrePeriodeFiscalePP();
		ppf13.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf13.setLogModifDate(new Timestamp(1199142000000L));
		ppf13.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf13.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf13.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf3.addParametrePeriodeFiscale(ppf13);
		pf3 = hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscalePP ppf14 = new ParametrePeriodeFiscalePP();
		ppf14.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf14.setLogModifDate(new Timestamp(1199142000000L));
		ppf14.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf14.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf14.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf3.addParametrePeriodeFiscale(ppf14);
		pf3 = hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscalePP ppf15 = new ParametrePeriodeFiscalePP();
		ppf15.setDateFinEnvoiMasseDI(RegDate.get(2006, 6, 30));
		ppf15.setLogModifDate(new Timestamp(1199142000000L));
		ppf15.setTermeGeneralSommationEffectif(RegDate.get(2006, 3, 31));
		ppf15.setTermeGeneralSommationReglementaire(RegDate.get(2006, 1, 31));
		ppf15.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf3.addParametrePeriodeFiscale(ppf15);
		pf3 = hibernateTemplate.merge(pf3);

		ParametrePeriodeFiscalePP ppf16 = new ParametrePeriodeFiscalePP();
		ppf16.setDateFinEnvoiMasseDI(RegDate.get(2007, 4, 30));
		ppf16.setLogModifDate(new Timestamp(1199142000000L));
		ppf16.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf16.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf16.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf4.addParametrePeriodeFiscale(ppf16);
		pf4 = hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscalePP ppf17 = new ParametrePeriodeFiscalePP();
		ppf17.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf17.setLogModifDate(new Timestamp(1199142000000L));
		ppf17.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf17.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf17.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf4.addParametrePeriodeFiscale(ppf17);
		pf4 = hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscalePP ppf18 = new ParametrePeriodeFiscalePP();
		ppf18.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf18.setLogModifDate(new Timestamp(1199142000000L));
		ppf18.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf18.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf18.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf4.addParametrePeriodeFiscale(ppf18);
		pf4 = hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscalePP ppf19 = new ParametrePeriodeFiscalePP();
		ppf19.setDateFinEnvoiMasseDI(RegDate.get(2007, 6, 30));
		ppf19.setLogModifDate(new Timestamp(1199142000000L));
		ppf19.setTermeGeneralSommationEffectif(RegDate.get(2007, 3, 31));
		ppf19.setTermeGeneralSommationReglementaire(RegDate.get(2007, 1, 31));
		ppf19.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf4.addParametrePeriodeFiscale(ppf19);
		pf4 = hibernateTemplate.merge(pf4);

		ParametrePeriodeFiscalePP ppf20 = new ParametrePeriodeFiscalePP();
		ppf20.setDateFinEnvoiMasseDI(RegDate.get(2008, 4, 30));
		ppf20.setLogModifDate(new Timestamp(1199142000000L));
		ppf20.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf20.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf20.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf5.addParametrePeriodeFiscale(ppf20);
		pf5 = hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscalePP ppf21 = new ParametrePeriodeFiscalePP();
		ppf21.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf21.setLogModifDate(new Timestamp(1199142000000L));
		ppf21.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf21.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf21.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf5.addParametrePeriodeFiscale(ppf21);
		pf5 = hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscalePP ppf22 = new ParametrePeriodeFiscalePP();
		ppf22.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf22.setLogModifDate(new Timestamp(1199142000000L));
		ppf22.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf22.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf22.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf5.addParametrePeriodeFiscale(ppf22);
		pf5 = hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscalePP ppf23 = new ParametrePeriodeFiscalePP();
		ppf23.setDateFinEnvoiMasseDI(RegDate.get(2008, 6, 30));
		ppf23.setLogModifDate(new Timestamp(1199142000000L));
		ppf23.setTermeGeneralSommationEffectif(RegDate.get(2008, 3, 31));
		ppf23.setTermeGeneralSommationReglementaire(RegDate.get(2008, 1, 31));
		ppf23.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf5.addParametrePeriodeFiscale(ppf23);
		pf5 = hibernateTemplate.merge(pf5);

		ParametrePeriodeFiscalePP ppf24 = new ParametrePeriodeFiscalePP();
		ppf24.setDateFinEnvoiMasseDI(RegDate.get(2009, 4, 30));
		ppf24.setLogModifDate(new Timestamp(1199142000000L));
		ppf24.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf24.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf24.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf6.addParametrePeriodeFiscale(ppf24);
		pf6 = hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscalePP ppf25 = new ParametrePeriodeFiscalePP();
		ppf25.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf25.setLogModifDate(new Timestamp(1199142000000L));
		ppf25.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf25.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf25.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf6.addParametrePeriodeFiscale(ppf25);
		pf6 = hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscalePP ppf26 = new ParametrePeriodeFiscalePP();
		ppf26.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf26.setLogModifDate(new Timestamp(1199142000000L));
		ppf26.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf26.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf26.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf6.addParametrePeriodeFiscale(ppf26);
		pf6 = hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscalePP ppf27 = new ParametrePeriodeFiscalePP();
		ppf27.setDateFinEnvoiMasseDI(RegDate.get(2009, 6, 30));
		ppf27.setLogModifDate(new Timestamp(1199142000000L));
		ppf27.setTermeGeneralSommationEffectif(RegDate.get(2009, 3, 31));
		ppf27.setTermeGeneralSommationReglementaire(RegDate.get(2009, 1, 31));
		ppf27.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf6.addParametrePeriodeFiscale(ppf27);
		pf6 = hibernateTemplate.merge(pf6);

		ParametrePeriodeFiscalePP ppf28 = new ParametrePeriodeFiscalePP();
		ppf28.setDateFinEnvoiMasseDI(RegDate.get(2010, 4, 30));
		ppf28.setLogModifDate(new Timestamp(1199142000000L));
		ppf28.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf28.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf28.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		pf7.addParametrePeriodeFiscale(ppf28);
		pf7 = hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscalePP ppf29 = new ParametrePeriodeFiscalePP();
		ppf29.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf29.setLogModifDate(new Timestamp(1199142000000L));
		ppf29.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf29.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf29.setTypeContribuable(TypeContribuable.VAUDOIS_DEPENSE);
		pf7.addParametrePeriodeFiscale(ppf29);
		pf7 = hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscalePP ppf30 = new ParametrePeriodeFiscalePP();
		ppf30.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf30.setLogModifDate(new Timestamp(1199142000000L));
		ppf30.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf30.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf30.setTypeContribuable(TypeContribuable.HORS_CANTON);
		pf7.addParametrePeriodeFiscale(ppf30);
		pf7 = hibernateTemplate.merge(pf7);

		ParametrePeriodeFiscalePP ppf31 = new ParametrePeriodeFiscalePP();
		ppf31.setDateFinEnvoiMasseDI(RegDate.get(2010, 6, 30));
		ppf31.setLogModifDate(new Timestamp(1199142000000L));
		ppf31.setTermeGeneralSommationEffectif(RegDate.get(2010, 3, 31));
		ppf31.setTermeGeneralSommationReglementaire(RegDate.get(2010, 1, 31));
		ppf31.setTypeContribuable(TypeContribuable.HORS_SUISSE);
		pf7.addParametrePeriodeFiscale(ppf31);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleDocument md0 = new ModeleDocument();
		md0.setLogModifDate(new Timestamp(1199142000000L));
		md0.setModelesFeuilleDocument(new HashSet());
		md0.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf5.addModeleDocument(md0);
		pf5 = hibernateTemplate.merge(pf5);

		ModeleDocument md1 = new ModeleDocument();
		md1.setLogModifDate(new Timestamp(1199142000000L));
		md1.setModelesFeuilleDocument(new HashSet());
		md1.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf5.addModeleDocument(md1);
		pf5 = hibernateTemplate.merge(pf5);

		ModeleDocument md2 = new ModeleDocument();
		md2.setLogModifDate(new Timestamp(1199142000000L));
		md2.setModelesFeuilleDocument(new HashSet());
		md2.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf5.addModeleDocument(md2);
		pf5 = hibernateTemplate.merge(pf5);

		ModeleDocument md3 = new ModeleDocument();
		md3.setLogModifDate(new Timestamp(1199142000000L));
		md3.setModelesFeuilleDocument(new HashSet());
		md3.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf5.addModeleDocument(md3);
		pf5 = hibernateTemplate.merge(pf5);

		ModeleDocument md4 = new ModeleDocument();
		md4.setLogModifDate(new Timestamp(1199142000000L));
		md4.setModelesFeuilleDocument(new HashSet());
		md4.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf5.addModeleDocument(md4);
		pf5 = hibernateTemplate.merge(pf5);

		ModeleDocument md5 = new ModeleDocument();
		md5.setLogModifDate(new Timestamp(1199142000000L));
		md5.setModelesFeuilleDocument(new HashSet());
		md5.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf6.addModeleDocument(md5);
		pf6 = hibernateTemplate.merge(pf6);

		ModeleDocument md6 = new ModeleDocument();
		md6.setLogModifDate(new Timestamp(1199142000000L));
		md6.setModelesFeuilleDocument(new HashSet());
		md6.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf6.addModeleDocument(md6);
		pf6 = hibernateTemplate.merge(pf6);

		ModeleDocument md7 = new ModeleDocument();
		md7.setLogModifDate(new Timestamp(1199142000000L));
		md7.setModelesFeuilleDocument(new HashSet());
		md7.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf6.addModeleDocument(md7);
		pf6 = hibernateTemplate.merge(pf6);

		ModeleDocument md8 = new ModeleDocument();
		md8.setLogModifDate(new Timestamp(1199142000000L));
		md8.setModelesFeuilleDocument(new HashSet());
		md8.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf6.addModeleDocument(md8);
		pf6 = hibernateTemplate.merge(pf6);

		ModeleDocument md9 = new ModeleDocument();
		md9.setLogModifDate(new Timestamp(1199142000000L));
		md9.setModelesFeuilleDocument(new HashSet());
		md9.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf6.addModeleDocument(md9);
		pf6 = hibernateTemplate.merge(pf6);

		ModeleDocument md10 = new ModeleDocument();
		md10.setLogModifDate(new Timestamp(1199142000000L));
		md10.setModelesFeuilleDocument(new HashSet());
		md10.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH);
		pf7.addModeleDocument(md10);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleDocument md11 = new ModeleDocument();
		md11.setLogModifDate(new Timestamp(1199142000000L));
		md11.setModelesFeuilleDocument(new HashSet());
		md11.setTypeDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_LOCAL);
		pf7.addModeleDocument(md11);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleDocument md12 = new ModeleDocument();
		md12.setLogModifDate(new Timestamp(1199142000000L));
		md12.setModelesFeuilleDocument(new HashSet());
		md12.setTypeDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX);
		pf7.addModeleDocument(md12);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleDocument md13 = new ModeleDocument();
		md13.setLogModifDate(new Timestamp(1199142000000L));
		md13.setModelesFeuilleDocument(new HashSet());
		md13.setTypeDocument(TypeDocument.DECLARATION_IMPOT_DEPENSE);
		pf7.addModeleDocument(md13);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleDocument md14 = new ModeleDocument();
		md14.setLogModifDate(new Timestamp(1199142000000L));
		md14.setModelesFeuilleDocument(new HashSet());
		md14.setTypeDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE);
		pf7.addModeleDocument(md14);
		pf7 = hibernateTemplate.merge(pf7);

		ModeleFeuilleDocument mfd0 = new ModeleFeuilleDocument();
		mfd0.setIntituleFeuille("Déclaration d'impot standard");
		mfd0.setLogModifDate(new Timestamp(1199142000000L));
		mfd0.setNoCADEV(ModeleFeuille.ANNEXE_210.getNoCADEV());
		mfd0.setNoFormulaireACI(ModeleFeuille.ANNEXE_210.getNoFormulaireACI());
		mfd0.setPrincipal(ModeleFeuille.ANNEXE_210.isPrincipal());
		md0.addModeleFeuilleDocument(mfd0);
		md0 = hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd1 = new ModeleFeuilleDocument();
		mfd1.setIntituleFeuille("Annexe 1");
		mfd1.setLogModifDate(new Timestamp(1199142000000L));
		mfd1.setNoCADEV(ModeleFeuille.ANNEXE_220.getNoCADEV());
		mfd1.setNoFormulaireACI(ModeleFeuille.ANNEXE_220.getNoFormulaireACI());
		mfd1.setPrincipal(ModeleFeuille.ANNEXE_220.isPrincipal());
		md0.addModeleFeuilleDocument(mfd1);
		md0 = hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd2 = new ModeleFeuilleDocument();
		mfd2.setIntituleFeuille("Annexe 2 et 3");
		mfd2.setLogModifDate(new Timestamp(1199142000000L));
		mfd2.setNoCADEV(ModeleFeuille.ANNEXE_230.getNoCADEV());
		mfd2.setNoFormulaireACI(ModeleFeuille.ANNEXE_230.getNoFormulaireACI());
		mfd2.setPrincipal(ModeleFeuille.ANNEXE_230.isPrincipal());
		md0.addModeleFeuilleDocument(mfd2);
		md0 = hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd3 = new ModeleFeuilleDocument();
		mfd3.setIntituleFeuille("Annexe 4 et 5");
		mfd3.setLogModifDate(new Timestamp(1199142000000L));
		mfd3.setNoCADEV(ModeleFeuille.ANNEXE_240.getNoCADEV());
		mfd3.setNoFormulaireACI(ModeleFeuille.ANNEXE_240.getNoFormulaireACI());
		mfd3.setPrincipal(ModeleFeuille.ANNEXE_240.isPrincipal());
		md0.addModeleFeuilleDocument(mfd3);
		md0 = hibernateTemplate.merge(md0);

		ModeleFeuilleDocument mfd4 = new ModeleFeuilleDocument();
		mfd4.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd4.setLogModifDate(new Timestamp(1199142000000L));
		mfd4.setNoCADEV(ModeleFeuille.ANNEXE_250.getNoCADEV());
		mfd4.setNoFormulaireACI(ModeleFeuille.ANNEXE_250.getNoFormulaireACI());
		mfd4.setPrincipal(ModeleFeuille.ANNEXE_250.isPrincipal());
		md2.addModeleFeuilleDocument(mfd4);
		md2 = hibernateTemplate.merge(md2);

		ModeleFeuilleDocument mfd5 = new ModeleFeuilleDocument();
		mfd5.setIntituleFeuille("Déclaration d'impot standard");
		mfd5.setLogModifDate(new Timestamp(1199142000000L));
		mfd5.setNoCADEV(ModeleFeuille.ANNEXE_210.getNoCADEV());
		mfd5.setNoFormulaireACI(ModeleFeuille.ANNEXE_210.getNoFormulaireACI());
		mfd5.setPrincipal(ModeleFeuille.ANNEXE_210.isPrincipal());
		md3.addModeleFeuilleDocument(mfd5);
		md3 = hibernateTemplate.merge(md3);

		ModeleFeuilleDocument mfd6 = new ModeleFeuilleDocument();
		mfd6.setIntituleFeuille("Annexe dépense");
		mfd6.setLogModifDate(new Timestamp(1199142000000L));
		mfd6.setNoCADEV(ModeleFeuille.ANNEXE_270.getNoCADEV());
		mfd6.setNoFormulaireACI(ModeleFeuille.ANNEXE_270.getNoFormulaireACI());
		mfd6.setPrincipal(ModeleFeuille.ANNEXE_270.isPrincipal());
		md3.addModeleFeuilleDocument(mfd6);
		md3 = hibernateTemplate.merge(md3);

		ModeleFeuilleDocument mfd7 = new ModeleFeuilleDocument();
		mfd7.setIntituleFeuille("Déclaration d'impot HC");
		mfd7.setLogModifDate(new Timestamp(1199142000000L));
		mfd7.setNoCADEV(ModeleFeuille.ANNEXE_200.getNoCADEV());
		mfd7.setNoFormulaireACI(ModeleFeuille.ANNEXE_200.getNoFormulaireACI());
		mfd7.setPrincipal(ModeleFeuille.ANNEXE_200.isPrincipal());
		md4.addModeleFeuilleDocument(mfd7);
		md4 = hibernateTemplate.merge(md4);

		ModeleFeuilleDocument mfd8 = new ModeleFeuilleDocument();
		mfd8.setIntituleFeuille("Déclaration d'impot standard");
		mfd8.setLogModifDate(new Timestamp(1199142000000L));
		mfd8.setNoCADEV(ModeleFeuille.ANNEXE_210.getNoCADEV());
		mfd8.setNoFormulaireACI(ModeleFeuille.ANNEXE_210.getNoFormulaireACI());
		mfd8.setPrincipal(ModeleFeuille.ANNEXE_210.isPrincipal());
		md5.addModeleFeuilleDocument(mfd8);
		md5 = hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd9 = new ModeleFeuilleDocument();
		mfd9.setIntituleFeuille("Annexe 1");
		mfd9.setLogModifDate(new Timestamp(1199142000000L));
		mfd9.setNoCADEV(ModeleFeuille.ANNEXE_220.getNoCADEV());
		mfd9.setNoFormulaireACI(ModeleFeuille.ANNEXE_220.getNoFormulaireACI());
		mfd9.setPrincipal(ModeleFeuille.ANNEXE_220.isPrincipal());
		md5.addModeleFeuilleDocument(mfd9);
		md5 = hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd10 = new ModeleFeuilleDocument();
		mfd10.setIntituleFeuille("Annexe 2 et 3");
		mfd10.setLogModifDate(new Timestamp(1199142000000L));
		mfd10.setNoCADEV(ModeleFeuille.ANNEXE_230.getNoCADEV());
		mfd10.setNoFormulaireACI(ModeleFeuille.ANNEXE_230.getNoFormulaireACI());
		mfd10.setPrincipal(ModeleFeuille.ANNEXE_230.isPrincipal());
		md5.addModeleFeuilleDocument(mfd10);
		md5 = hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd11 = new ModeleFeuilleDocument();
		mfd11.setIntituleFeuille("Annexe 4 et 5");
		mfd11.setLogModifDate(new Timestamp(1199142000000L));
		mfd11.setNoCADEV(ModeleFeuille.ANNEXE_240.getNoCADEV());
		mfd11.setNoFormulaireACI(ModeleFeuille.ANNEXE_240.getNoFormulaireACI());
		mfd11.setPrincipal(ModeleFeuille.ANNEXE_240.isPrincipal());
		md5.addModeleFeuilleDocument(mfd11);
		md5 = hibernateTemplate.merge(md5);

		ModeleFeuilleDocument mfd12 = new ModeleFeuilleDocument();
		mfd12.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd12.setLogModifDate(new Timestamp(1199142000000L));
		mfd12.setNoCADEV(ModeleFeuille.ANNEXE_250.getNoCADEV());
		mfd12.setNoFormulaireACI(ModeleFeuille.ANNEXE_250.getNoFormulaireACI());
		mfd12.setPrincipal(ModeleFeuille.ANNEXE_250.isPrincipal());
		md7.addModeleFeuilleDocument(mfd12);
		md7 = hibernateTemplate.merge(md7);

		ModeleFeuilleDocument mfd13 = new ModeleFeuilleDocument();
		mfd13.setIntituleFeuille("Déclaration d'impot standard");
		mfd13.setLogModifDate(new Timestamp(1199142000000L));
		mfd13.setNoCADEV(ModeleFeuille.ANNEXE_210.getNoCADEV());
		mfd13.setNoFormulaireACI(ModeleFeuille.ANNEXE_210.getNoFormulaireACI());
		mfd13.setPrincipal(ModeleFeuille.ANNEXE_210.isPrincipal());
		md8.addModeleFeuilleDocument(mfd13);
		md8 = hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd14 = new ModeleFeuilleDocument();
		mfd14.setIntituleFeuille("Annexe 1");
		mfd14.setLogModifDate(new Timestamp(1199142000000L));
		mfd14.setNoCADEV(ModeleFeuille.ANNEXE_220.getNoCADEV());
		mfd14.setNoFormulaireACI(ModeleFeuille.ANNEXE_220.getNoFormulaireACI());
		mfd14.setPrincipal(ModeleFeuille.ANNEXE_220.isPrincipal());
		md8.addModeleFeuilleDocument(mfd14);
		md8 = hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd15 = new ModeleFeuilleDocument();
		mfd15.setIntituleFeuille("Annexe 2 et 3");
		mfd15.setLogModifDate(new Timestamp(1199142000000L));
		mfd15.setNoCADEV(ModeleFeuille.ANNEXE_230.getNoCADEV());
		mfd15.setNoFormulaireACI(ModeleFeuille.ANNEXE_230.getNoFormulaireACI());
		mfd15.setPrincipal(ModeleFeuille.ANNEXE_230.isPrincipal());
		md8.addModeleFeuilleDocument(mfd15);
		md8 = hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd16 = new ModeleFeuilleDocument();
		mfd16.setIntituleFeuille("Annexe 4 et 5");
		mfd16.setLogModifDate(new Timestamp(1199142000000L));
		mfd16.setNoCADEV(ModeleFeuille.ANNEXE_240.getNoCADEV());
		mfd16.setNoFormulaireACI(ModeleFeuille.ANNEXE_240.getNoFormulaireACI());
		mfd16.setPrincipal(ModeleFeuille.ANNEXE_240.isPrincipal());
		md8.addModeleFeuilleDocument(mfd16);
		md8 = hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd17 = new ModeleFeuilleDocument();
		mfd17.setIntituleFeuille("Annexe dépense");
		mfd17.setLogModifDate(new Timestamp(1199142000000L));
		mfd17.setNoCADEV(ModeleFeuille.ANNEXE_270.getNoCADEV());
		mfd17.setNoFormulaireACI(ModeleFeuille.ANNEXE_270.getNoFormulaireACI());
		mfd17.setPrincipal(ModeleFeuille.ANNEXE_270.isPrincipal());
		md8.addModeleFeuilleDocument(mfd17);
		md8 = hibernateTemplate.merge(md8);

		ModeleFeuilleDocument mfd18 = new ModeleFeuilleDocument();
		mfd18.setIntituleFeuille("Déclaration d'impot HC");
		mfd18.setLogModifDate(new Timestamp(1199142000000L));
		mfd18.setNoCADEV(ModeleFeuille.ANNEXE_200.getNoCADEV());
		mfd18.setNoFormulaireACI(ModeleFeuille.ANNEXE_200.getNoFormulaireACI());
		mfd18.setPrincipal(ModeleFeuille.ANNEXE_200.isPrincipal());
		md9.addModeleFeuilleDocument(mfd18);
		md9 = hibernateTemplate.merge(md9);

		ModeleFeuilleDocument mfd19 = new ModeleFeuilleDocument();
		mfd19.setIntituleFeuille("Déclaration d'impot standard");
		mfd19.setLogModifDate(new Timestamp(1199142000000L));
		mfd19.setNoCADEV(ModeleFeuille.ANNEXE_210.getNoCADEV());
		mfd19.setNoFormulaireACI(ModeleFeuille.ANNEXE_210.getNoFormulaireACI());
		mfd19.setPrincipal(ModeleFeuille.ANNEXE_210.isPrincipal());
		md10.addModeleFeuilleDocument(mfd19);
		md10 = hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd20 = new ModeleFeuilleDocument();
		mfd20.setIntituleFeuille("Annexe 1");
		mfd20.setLogModifDate(new Timestamp(1199142000000L));
		mfd20.setNoCADEV(ModeleFeuille.ANNEXE_220.getNoCADEV());
		mfd20.setNoFormulaireACI(ModeleFeuille.ANNEXE_220.getNoFormulaireACI());
		mfd20.setPrincipal(ModeleFeuille.ANNEXE_220.isPrincipal());
		md10.addModeleFeuilleDocument(mfd20);
		md10 = hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd21 = new ModeleFeuilleDocument();
		mfd21.setIntituleFeuille("Annexe 2 et 3");
		mfd21.setLogModifDate(new Timestamp(1199142000000L));
		mfd21.setNoCADEV(ModeleFeuille.ANNEXE_230.getNoCADEV());
		mfd21.setNoFormulaireACI(ModeleFeuille.ANNEXE_230.getNoFormulaireACI());
		mfd21.setPrincipal(ModeleFeuille.ANNEXE_230.isPrincipal());
		md10.addModeleFeuilleDocument(mfd21);
		md10 = hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd22 = new ModeleFeuilleDocument();
		mfd22.setIntituleFeuille("Annexe 4 et 5");
		mfd22.setLogModifDate(new Timestamp(1199142000000L));
		mfd22.setNoCADEV(ModeleFeuille.ANNEXE_240.getNoCADEV());
		mfd22.setNoFormulaireACI(ModeleFeuille.ANNEXE_240.getNoFormulaireACI());
		mfd22.setPrincipal(ModeleFeuille.ANNEXE_240.isPrincipal());
		md10.addModeleFeuilleDocument(mfd22);
		md10 = hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd23 = new ModeleFeuilleDocument();
		mfd23.setIntituleFeuille("Annexe 1-1");
		mfd23.setLogModifDate(new Timestamp(1199142000000L));
		mfd23.setNoCADEV(ModeleFeuille.ANNEXE_310.getNoCADEV());
		mfd23.setNoFormulaireACI(ModeleFeuille.ANNEXE_310.getNoFormulaireACI());
		mfd23.setPrincipal(ModeleFeuille.ANNEXE_310.isPrincipal());
		md10.addModeleFeuilleDocument(mfd23);
		md10 = hibernateTemplate.merge(md10);

		ModeleFeuilleDocument mfd24 = new ModeleFeuilleDocument();
		mfd24.setIntituleFeuille("Déclaration d'impot vaud tax");
		mfd24.setLogModifDate(new Timestamp(1199142000000L));
		mfd24.setNoCADEV(ModeleFeuille.ANNEXE_250.getNoCADEV());
		mfd24.setNoFormulaireACI(ModeleFeuille.ANNEXE_250.getNoFormulaireACI());
		mfd24.setPrincipal(ModeleFeuille.ANNEXE_250.isPrincipal());
		md12.addModeleFeuilleDocument(mfd24);
		md12 = hibernateTemplate.merge(md12);

		ModeleFeuilleDocument mfd25 = new ModeleFeuilleDocument();
		mfd25.setIntituleFeuille("Annexe dépense");
		mfd25.setLogModifDate(new Timestamp(1199142000000L));
		mfd25.setNoCADEV(ModeleFeuille.ANNEXE_270.getNoCADEV());
		mfd25.setNoFormulaireACI(ModeleFeuille.ANNEXE_270.getNoFormulaireACI());
		mfd25.setPrincipal(ModeleFeuille.ANNEXE_270.isPrincipal());
		md13.addModeleFeuilleDocument(mfd25);
		md13 = hibernateTemplate.merge(md13);

		ModeleFeuilleDocument mfd26 = new ModeleFeuilleDocument();
		mfd26.setIntituleFeuille("Déclaration d'impot HC");
		mfd26.setLogModifDate(new Timestamp(1199142000000L));
		mfd26.setNoCADEV(ModeleFeuille.ANNEXE_200.getNoCADEV());
		mfd26.setNoFormulaireACI(ModeleFeuille.ANNEXE_200.getNoFormulaireACI());
		mfd26.setPrincipal(ModeleFeuille.ANNEXE_200.isPrincipal());
		md14.addModeleFeuilleDocument(mfd26);
		md14 = hibernateTemplate.merge(md14);

		MenageCommun mc0 = new MenageCommun();
		mc0.setNumero(12600004L);
		mc0.setMouvementsDossier(new HashSet());
		mc0.setSituationsFamille(new HashSet());
		mc0.setDebiteurInactif(false);
		mc0.setLogCreationDate(new Timestamp(1199142000000L));
		mc0.setLogModifDate(new Timestamp(1199142000000L));
		mc0.setOfficeImpotId(10);
		mc0.setAdressesTiers(new HashSet());
		mc0.setDocumentsFiscaux(new HashSet());
		mc0.setForsFiscaux(new HashSet());
		mc0.setRapportsObjet(new HashSet());
		mc0.setRapportsSujet(new HashSet());
		mc0 = hibernateTemplate.merge(mc0);

		PersonnePhysique pp0 = new PersonnePhysique();
		pp0.setNumero(12600003L);
		pp0.setBlocageRemboursementAutomatique(false);
		pp0.setMouvementsDossier(new HashSet());
		pp0.setSituationsFamille(new HashSet());
		pp0.setDebiteurInactif(false);
		pp0.setLogCreationDate(new Timestamp(1199142000000L));
		pp0.setLogModifDate(new Timestamp(1199142000000L));
		pp0.setDateNaissance(RegDate.get(1953, 12, 18));
		pp0.setNom("Mme");
		pp0.setNumeroOfsNationalite(8231);
		pp0.setPrenomUsuel("Mario");
		pp0.setSexe(Sexe.MASCULIN);
		pp0.setIdentificationsPersonnes(new HashSet());
		pp0.setOfficeImpotId(10);
		pp0.setHabitant(false);
		pp0.setAdressesTiers(new HashSet());
		pp0.setDocumentsFiscaux(new HashSet());
		pp0.setDroitsAccesAppliques(new HashSet());
		pp0.setForsFiscaux(new HashSet());
		pp0.setRapportsObjet(new HashSet());
		pp0.setRapportsSujet(new HashSet());
		pp0 = hibernateTemplate.merge(pp0);

		PersonnePhysique pp1 = new PersonnePhysique();
		pp1.setNumero(12600009L);
		pp1.setBlocageRemboursementAutomatique(false);
		pp1.setMouvementsDossier(new HashSet());
		pp1.setSituationsFamille(new HashSet());
		pp1.setDebiteurInactif(false);
		pp1.setLogCreationDate(new Timestamp(1199142000000L));
		pp1.setLogModifDate(new Timestamp(1199142000000L));
		pp1.setDateNaissance(RegDate.get(1977, 2, 12));
		pp1.setNom("Tardy");
		pp1.setNumeroOfsNationalite(8201);
		pp1.setPrenomUsuel("Alain (HS)");
		pp1.setSexe(Sexe.MASCULIN);
		pp1.setIdentificationsPersonnes(new HashSet());
		pp1.setOfficeImpotId(10);
		pp1.setHabitant(false);
		pp1.setAdressesTiers(new HashSet());
		pp1.setDocumentsFiscaux(new HashSet());
		pp1.setDroitsAccesAppliques(new HashSet());
		pp1.setForsFiscaux(new HashSet());
		pp1.setRapportsObjet(new HashSet());
		pp1.setRapportsSujet(new HashSet());
		pp1 = hibernateTemplate.merge(pp1);

		PersonnePhysique pp2 = new PersonnePhysique();
		pp2.setNumero(12600001L);
		pp2.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp2.setBlocageRemboursementAutomatique(false);
		pp2.setComplementNom("Chopard");
		pp2.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", "CCBPFRPPBDX"));
		pp2.setMouvementsDossier(new HashSet());
		pp2.setSituationsFamille(new HashSet());
		pp2.setDebiteurInactif(false);
		pp2.setLogCreationDate(new Timestamp(1199142000000L));
		pp2.setLogModifDate(new Timestamp(1199142000000L));
		pp2.setDateNaissance(RegDate.get(1971, 1, 23));
		pp2.setNom("Pirez");
		pp2.setNumeroOfsNationalite(8212);
		pp2.setNumeroAssureSocial("7561234567897");
		pp2.setPrenomUsuel("Isidor (sourcier gris)");
		pp2.setSexe(Sexe.MASCULIN);
		pp2.setIdentificationsPersonnes(new HashSet());
		pp2.setNumeroTelecopie("0219663629");
		pp2.setNumeroTelephonePortable("0219663999");
		pp2.setNumeroTelephonePrive("0219663623");
		pp2.setNumeroTelephoneProfessionnel("0219663625");
		pp2.setOfficeImpotId(10);
		pp2.setPersonneContact("MAURICE DUPONT");
		pp2.setHabitant(false);
		pp2.setAdressesTiers(new HashSet());
		pp2.setDocumentsFiscaux(new HashSet());
		pp2.setDroitsAccesAppliques(new HashSet());
		pp2.setForsFiscaux(new HashSet());
		pp2.setRapportsObjet(new HashSet());
		pp2.setRapportsSujet(new HashSet());
		pp2.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp2 = hibernateTemplate.merge(pp2);

		PersonnePhysique pp3 = new PersonnePhysique();
		pp3.setNumero(43308102L);
		pp3.setMouvementsDossier(new HashSet());
		pp3.setSituationsFamille(new HashSet());
		pp3.setDebiteurInactif(false);
		pp3.setLogCreationDate(new Timestamp(1199142000000L));
		pp3.setLogModifDate(new Timestamp(1199142000000L));
		pp3.setIdentificationsPersonnes(new HashSet());
		pp3.setNumeroIndividu(320073L);
		pp3.setOfficeImpotId(10);
		pp3.setHabitant(true);
		pp3.setAdressesTiers(new HashSet());
		pp3.setDocumentsFiscaux(new HashSet());
		pp3.setDroitsAccesAppliques(new HashSet());
		pp3.setForsFiscaux(new HashSet());
		pp3.setRapportsObjet(new HashSet());
		pp3.setRapportsSujet(new HashSet());
		pp3 = hibernateTemplate.merge(pp3);

		PersonnePhysique pp4 = new PersonnePhysique();
		pp4.setNumero(43308103L);
		pp4.setMouvementsDossier(new HashSet());
		pp4.setSituationsFamille(new HashSet());
		pp4.setDebiteurInactif(false);
		pp4.setLogCreationDate(new Timestamp(1199142000000L));
		pp4.setLogModifDate(new Timestamp(1199142000000L));
		pp4.setIdentificationsPersonnes(new HashSet());
		pp4.setNumeroIndividu(325740L);
		pp4.setOfficeImpotId(10);
		pp4.setHabitant(true);
		pp4.setAdressesTiers(new HashSet());
		pp4.setDocumentsFiscaux(new HashSet());
		pp4.setDroitsAccesAppliques(new HashSet());
		pp4.setForsFiscaux(new HashSet());
		pp4.setRapportsObjet(new HashSet());
		pp4.setRapportsSujet(new HashSet());
		pp4 = hibernateTemplate.merge(pp4);

		PersonnePhysique pp5 = new PersonnePhysique();
		pp5.setNumero(43308104L);
		pp5.setMouvementsDossier(new HashSet());
		pp5.setSituationsFamille(new HashSet());
		pp5.setDebiteurInactif(false);
		pp5.setLogCreationDate(new Timestamp(1199142000000L));
		pp5.setLogModifDate(new Timestamp(1199142000000L));
		pp5.setIdentificationsPersonnes(new HashSet());
		pp5.setNumeroIndividu(325631L);
		pp5.setOfficeImpotId(10);
		pp5.setHabitant(true);
		pp5.setAdressesTiers(new HashSet());
		pp5.setDocumentsFiscaux(new HashSet());
		pp5.setDroitsAccesAppliques(new HashSet());
		pp5.setForsFiscaux(new HashSet());
		pp5.setRapportsObjet(new HashSet());
		pp5.setRapportsSujet(new HashSet());
		pp5 = hibernateTemplate.merge(pp5);

		DebiteurPrestationImposable dpi0 = new DebiteurPrestationImposable();
		dpi0.setNumero(1678432L);
		dpi0.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi0.setComplementNom("Café du Commerce");
		dpi0.setPeriodicites(new HashSet());
		dpi0.setDebiteurInactif(false);
		dpi0.setLogCreationDate(new Timestamp(1199142000000L));
		dpi0.setLogModifDate(new Timestamp(1199142000000L));
		dpi0.setModeCommunication(ModeCommunication.PAPIER);
		dpi0.setAdressesTiers(new HashSet());
		dpi0.setDocumentsFiscaux(new HashSet());
		dpi0.setForsFiscaux(new HashSet());
		dpi0.setRapportsObjet(new HashSet());
		dpi0.setRapportsSujet(new HashSet());
		dpi0 = hibernateTemplate.merge(dpi0);

		final Periodicite per0 = new Periodicite();
		per0.setId(1L);
		per0.setDateDebut(RegDate.get(2007, 1, 1));
		per0.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.addPeriodicite(per0);

		DebiteurPrestationImposable dpi1 = new DebiteurPrestationImposable();
		dpi1.setNumero(1678439L);
		dpi1.setCategorieImpotSource(CategorieImpotSource.REGULIERS);
		dpi1.setComplementNom("Café du Sayonara");
		dpi1.setPeriodicites(new HashSet());
		dpi1.setDebiteurInactif(false);
		dpi1.setNom1("Nom1");
		dpi1.setNom2("Nom2");
		dpi1.setLogCreationDate(new Timestamp(1199142000000L));
		dpi1.setLogModifDate(new Timestamp(1199142000000L));
		dpi1.setModeCommunication(ModeCommunication.PAPIER);
		dpi1.setAdressesTiers(new HashSet());
		dpi1.setDocumentsFiscaux(new HashSet());
		dpi1.setForsFiscaux(new HashSet());
		dpi1.setRapportsObjet(new HashSet());
		dpi1.setRapportsSujet(new HashSet());

		final Periodicite per1 = new Periodicite();
		per1.setId(2L);
		per1.setDateDebut(RegDate.get(2008, 1, 1));
		per1.setPeriodiciteDecompte(PeriodiciteDecompte.TRIMESTRIEL);
		dpi1.addPeriodicite(per1);

		ForDebiteurPrestationImposable forDebiteur1 = new ForDebiteurPrestationImposable();
		forDebiteur1.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		forDebiteur1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		forDebiteur1.setNumeroOfsAutoriteFiscale(MockCommune.Lausanne.getNoOFS());
		forDebiteur1.setDateDebut(RegDate.get(2008, 1, 1));
		forDebiteur1.setMotifOuverture(MotifFor.DEBUT_PRESTATION_IS);
		dpi1.addForFiscal(forDebiteur1);

		dpi1 = hibernateTemplate.merge(dpi1);

		PersonnePhysique pp6 = new PersonnePhysique();
		pp6.setNumero(12900001L);
		pp6.setBlocageRemboursementAutomatique(false);
		pp6.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));
		pp6.setMouvementsDossier(new HashSet());
		pp6.setSituationsFamille(new HashSet());
		pp6.setDebiteurInactif(false);
		pp6.setLogCreationDate(new Timestamp(1199142000000L));
		pp6.setLogModifDate(new Timestamp(1199142000000L));
		pp6.setDateNaissance(RegDate.get(1952, 1, 23));
		pp6.setNom("Lederet");
		pp6.setNumeroOfsNationalite(8100);
		pp6.setNumeroAssureSocial("7561234567897");
		pp6.setPrenomUsuel("Michel");
		pp6.setSexe(Sexe.MASCULIN);
		pp6.setIdentificationsPersonnes(new HashSet());
		pp6.setNumeroTelephonePortable("0764537812");
		pp6.setNumeroTelephonePrive("032'897'45'32");
		pp6.setOfficeImpotId(10);
		pp6.setPersonneContact("");
		pp6.setHabitant(false);
		pp6.setAdressesTiers(new HashSet());
		pp6.setDocumentsFiscaux(new HashSet());
		pp6.setDroitsAccesAppliques(new HashSet());
		pp6.setForsFiscaux(new HashSet());
		pp6.setRapportsObjet(new HashSet());
		pp6.setRapportsSujet(new HashSet());
		pp6.setTitulaireCompteBancaire("Lederet Michel");
		pp6 = hibernateTemplate.merge(pp6);

		PersonnePhysique pp7 = new PersonnePhysique();
		pp7.setNumero(12300003L);
		pp7.setMouvementsDossier(new HashSet());
		pp7.setSituationsFamille(new HashSet());
		pp7.setDebiteurInactif(false);
		pp7.setLogCreationDate(new Timestamp(1199142000000L));
		pp7.setLogModifDate(new Timestamp(1199142000000L));
		pp7.setIdentificationsPersonnes(new HashSet());
		pp7.setNumeroIndividu(327706L);
		pp7.setOfficeImpotId(10);
		pp7.setHabitant(true);
		pp7.setAdressesTiers(new HashSet());
		pp7.setDocumentsFiscaux(new HashSet());
		pp7.setDroitsAccesAppliques(new HashSet());
		pp7.setForsFiscaux(new HashSet());
		pp7.setRapportsObjet(new HashSet());
		pp7.setRapportsSujet(new HashSet());
		pp7 = hibernateTemplate.merge(pp7);

		PersonnePhysique pp8 = new PersonnePhysique();
		pp8.setNumero(34807810L);
		pp8.setAdresseCourrierElectronique("pascaline@descloux.ch");
		pp8.setBlocageRemboursementAutomatique(false);
		pp8.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));
		pp8.setMouvementsDossier(new HashSet());
		pp8.setSituationsFamille(new HashSet());
		pp8.setDebiteurInactif(false);
		pp8.setLogCreationDate(new Timestamp(1199142000000L));
		pp8.setLogModifDate(new Timestamp(1199142000000L));
		pp8.setIdentificationsPersonnes(new HashSet());
		pp8.setNumeroIndividu(674417L);
		pp8.setNumeroTelephonePortable("0792348732");
		pp8.setNumeroTelephonePrive("0213135489");
		pp8.setOfficeImpotId(10);
		pp8.setHabitant(true);
		pp8.setAdressesTiers(new HashSet());
		pp8.setDocumentsFiscaux(new HashSet());
		pp8.setDroitsAccesAppliques(new HashSet());
		pp8.setForsFiscaux(new HashSet());
		pp8.setRapportsObjet(new HashSet());
		pp8.setRapportsSujet(new HashSet());
		pp8.setTitulaireCompteBancaire("Pascaline Descloux");
		pp8 = hibernateTemplate.merge(pp8);

		PersonnePhysique pp9 = new PersonnePhysique();
		pp9.setNumero(12300001L);
		pp9.setMouvementsDossier(new HashSet());
		pp9.setSituationsFamille(new HashSet());
		pp9.setDebiteurInactif(false);
		pp9.setLogCreationDate(new Timestamp(1199142000000L));
		pp9.setLogModifDate(new Timestamp(1199142000000L));
		pp9.setIdentificationsPersonnes(new HashSet());
		pp9.setNumeroIndividu(333905L);
		pp9.setOfficeImpotId(10);
		pp9.setHabitant(true);
		pp9.setAdressesTiers(new HashSet());
		pp9.setDocumentsFiscaux(new HashSet());
		pp9.setDroitsAccesAppliques(new HashSet());
		pp9.setForsFiscaux(new HashSet());
		pp9.setRapportsObjet(new HashSet());
		pp9.setRapportsSujet(new HashSet());
		pp9 = hibernateTemplate.merge(pp9);

		PersonnePhysique pp10 = new PersonnePhysique();
		pp10.setNumero(12300002L);
		pp10.setMouvementsDossier(new HashSet());
		pp10.setSituationsFamille(new HashSet());
		pp10.setDebiteurInactif(false);
		pp10.setLogCreationDate(new Timestamp(1199142000000L));
		pp10.setLogModifDate(new Timestamp(1199142000000L));
		pp10.setIdentificationsPersonnes(new HashSet());
		pp10.setNumeroIndividu(333908L);
		pp10.setOfficeImpotId(10);
		pp10.setHabitant(true);
		pp10.setAdressesTiers(new HashSet());
		pp10.setDocumentsFiscaux(new HashSet());
		pp10.setDroitsAccesAppliques(new HashSet());
		pp10.setForsFiscaux(new HashSet());
		pp10.setRapportsObjet(new HashSet());
		pp10.setRapportsSujet(new HashSet());
		pp10 = hibernateTemplate.merge(pp10);

		PersonnePhysique pp11 = new PersonnePhysique();
		pp11.setNumero(10246283L);
		pp11.setMouvementsDossier(new HashSet());
		pp11.setSituationsFamille(new HashSet());
		pp11.setDebiteurInactif(false);
		pp11.setLogCreationDate(new Timestamp(1199142000000L));
		pp11.setLogModifDate(new Timestamp(1199142000000L));
		pp11.setIdentificationsPersonnes(new HashSet());
		pp11.setNumeroIndividu(333911L);
		pp11.setOfficeImpotId(10);
		pp11.setHabitant(true);
		pp11.setAdressesTiers(new HashSet());
		pp11.setDocumentsFiscaux(new HashSet());
		pp11.setDroitsAccesAppliques(new HashSet());
		pp11.setForsFiscaux(new HashSet());
		pp11.setRapportsObjet(new HashSet());
		pp11.setRapportsSujet(new HashSet());
		pp11 = hibernateTemplate.merge(pp11);

		MenageCommun mc1 = new MenageCommun();
		mc1.setNumero(86006202L);
		mc1.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		mc1.setBlocageRemboursementAutomatique(false);
		mc1.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", "CCBPFRPPBDX"));
		mc1.setMouvementsDossier(new HashSet());
		mc1.setSituationsFamille(new HashSet());
		mc1.setDebiteurInactif(false);
		mc1.setLogCreationDate(new Timestamp(1199142000000L));
		mc1.setLogModifDate(new Timestamp(1199142000000L));
		mc1.setNumeroTelecopie("0219663629");
		mc1.setNumeroTelephonePortable("0219663999");
		mc1.setNumeroTelephonePrive("0219663623");
		mc1.setNumeroTelephoneProfessionnel("0219663625");
		mc1.setOfficeImpotId(10);
		mc1.setPersonneContact("MAURICE DUPONT");
		mc1.setAdressesTiers(new HashSet());
		mc1.setDocumentsFiscaux(new HashSet());
		mc1.setForsFiscaux(new HashSet());
		mc1.setRapportsObjet(new HashSet());
		mc1.setRapportsSujet(new HashSet());
		mc1.setTitulaireCompteBancaire("ERIC MONTAGNY");
		mc1 = hibernateTemplate.merge(mc1);

		Entreprise e0 = new Entreprise();
		e0.setNumero(127001L);
		e0.setMouvementsDossier(new HashSet());
		e0.setDebiteurInactif(false);
		e0.setLogCreationDate(new Timestamp(1199142000000L));
		e0.setLogModifDate(new Timestamp(1199142000000L));
		e0.setOfficeImpotId(10);
		e0.setAdressesTiers(new HashSet());
		e0.setDocumentsFiscaux(new HashSet());
		e0.setForsFiscaux(new HashSet());
		e0.setRapportsObjet(new HashSet());
		e0.setRapportsSujet(new HashSet());
		e0 = hibernateTemplate.merge(e0);

		PersonnePhysique pp12 = new PersonnePhysique();
		pp12.setNumero(12600008L);
		pp12.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp12.setBlocageRemboursementAutomatique(false);
		pp12.setComplementNom("Chopard");
		pp12.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", "CCBPFRPPBDX"));
		pp12.setMouvementsDossier(new HashSet());
		pp12.setSituationsFamille(new HashSet());
		pp12.setDebiteurInactif(true);
		pp12.setLogCreationDate(new Timestamp(1199142000000L));
		pp12.setLogModifDate(new Timestamp(1199142000000L));
		pp12.setDateNaissance(RegDate.get(1970, 1, 23));
		pp12.setNom("The full i107");
		pp12.setNumeroOfsNationalite(8212);
		pp12.setNumeroAssureSocial("7561234567897");
		pp12.setPrenomUsuel("De la mort");
		pp12.setSexe(Sexe.MASCULIN);
		pp12.setIdentificationsPersonnes(new HashSet());
		pp12.setNumeroTelecopie("0219663629");
		pp12.setNumeroTelephonePortable("0219663999");
		pp12.setNumeroTelephonePrive("0219663623");
		pp12.setNumeroTelephoneProfessionnel("0219663625");
		pp12.setOfficeImpotId(10);
		pp12.setPersonneContact("MAURICE DUPONT");
		pp12.setHabitant(false);
		pp12.setAdressesTiers(new HashSet());
		pp12.setDocumentsFiscaux(new HashSet());
		pp12.setDroitsAccesAppliques(new HashSet());
		pp12.setForsFiscaux(new HashSet());
		pp12.setRapportsObjet(new HashSet());
		pp12.setRapportsSujet(new HashSet());
		pp12.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp12 = hibernateTemplate.merge(pp12);

		PersonnePhysique pp13 = new PersonnePhysique();
		pp13.setNumero(12600002L);
		pp13.setAdresseCourrierElectronique("dupont@etat-vaud.ch");
		pp13.setBlocageRemboursementAutomatique(false);
		pp13.setComplementNom("Chopard");
		pp13.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", "CCBPFRPPBDX"));
		pp13.setMouvementsDossier(new HashSet());
		pp13.setSituationsFamille(new HashSet());
		pp13.setDebiteurInactif(false);
		pp13.setLogCreationDate(new Timestamp(1199142000000L));
		pp13.setLogModifDate(new Timestamp(1199142000000L));
		pp13.setDateNaissance(RegDate.get(1970, 1, 23));
		pp13.setNom("Martinez");
		pp13.setNumeroOfsNationalite(8212);
		pp13.setNumeroAssureSocial("7561234567897");
		pp13.setPrenomUsuel("Conchita");
		pp13.setSexe(Sexe.FEMININ);
		pp13.setIdentificationsPersonnes(new HashSet());
		pp13.setNumeroTelecopie("0219663629");
		pp13.setNumeroTelephonePortable("0219663999");
		pp13.setNumeroTelephonePrive("0219663623");
		pp13.setNumeroTelephoneProfessionnel("0219663625");
		pp13.setOfficeImpotId(10);
		pp13.setPersonneContact("MAURICE DUPONT");
		pp13.setHabitant(false);
		pp13.setAdressesTiers(new HashSet());
		pp13.setDocumentsFiscaux(new HashSet());
		pp13.setDroitsAccesAppliques(new HashSet());
		pp13.setForsFiscaux(new HashSet());
		pp13.setRapportsObjet(new HashSet());
		pp13.setRapportsSujet(new HashSet());
		pp13.setTitulaireCompteBancaire("ERIC MONTAGNY");
		pp13 = hibernateTemplate.merge(pp13);

		AutreCommunaute ac0 = new AutreCommunaute();
		ac0.setNumero(2800001L);
		ac0.setFormeJuridique(FormeJuridique.ASS);
		ac0.setNom("Communaute XYZ");
		ac0.setMouvementsDossier(new HashSet());
		ac0.setDebiteurInactif(false);
		ac0.setLogCreationDate(new Timestamp(1199142000000L));
		ac0.setLogModifDate(new Timestamp(1199142000000L));
		ac0.setNumeroTelephonePortable("Chopard");
		ac0.setOfficeImpotId(10);
		ac0.setAdressesTiers(new HashSet());
		ac0.setDocumentsFiscaux(new HashSet());
		ac0.setForsFiscaux(new HashSet());
		ac0.setRapportsObjet(new HashSet());
		ac0.setRapportsSujet(new HashSet());
		ac0 = hibernateTemplate.merge(ac0);

		if (withCollectivitesAdministratives) {

			CollectiviteAdministrative ca0 = new CollectiviteAdministrative();
			ca0.setNumero(2100001L);
			ca0.setMouvementsDossier(new HashSet());
			ca0.setDebiteurInactif(false);
			ca0.setLogCreationDate(new Timestamp(1199142000000L));
			ca0.setLogModifDate(new Timestamp(1199142000000L));
			ca0.setNumeroCollectiviteAdministrative(1013);
			ca0.setAdressesTiers(new HashSet());
			ca0.setDocumentsFiscaux(new HashSet());
			ca0.setForsFiscaux(new HashSet());
			ca0.setRapportsObjet(new HashSet());
			ca0.setRapportsSujet(new HashSet());
			ca0 = hibernateTemplate.merge(ca0);

			CollectiviteAdministrative ca1 = new CollectiviteAdministrative();
			ca1.setNumero(2100002L);
			ca1.setMouvementsDossier(new HashSet());
			ca1.setDebiteurInactif(false);
			ca1.setLogCreationDate(new Timestamp(1199142000000L));
			ca1.setLogModifDate(new Timestamp(1199142000000L));
			ca1.setNumeroCollectiviteAdministrative(10);
			ca1.setAdressesTiers(new HashSet());
			ca1.setDocumentsFiscaux(new HashSet());
			ca1.setForsFiscaux(new HashSet());
			ca1.setRapportsObjet(new HashSet());
			ca1.setRapportsSujet(new HashSet());
			ca1 = hibernateTemplate.merge(ca1);
		}

		AdresseSuisse as0 = new AdresseSuisse();
		as0.setDateDebut(RegDate.get(2002, 2, 12));
		as0.setLogModifDate(new Timestamp(1199142000000L));
		as0.setNumeroCasePostale(23);
		as0.setNumeroMaison("19");
		as0.setNumeroOrdrePoste(254);
		as0.setNumeroRue(1136139);
		as0.setPermanente(false);
		as0.setTexteCasePostale(TexteCasePostale.CASE_POSTALE);
		as0.setUsage(TypeAdresseTiers.COURRIER);
		pp6.addAdresseTiers(as0);
		pp6 = hibernateTemplate.merge(pp6);

		AdresseSuisse as1 = new AdresseSuisse();
		as1.setDateDebut(RegDate.get(2008, 1, 29));
		as1.setLogModifDate(new Timestamp(1199142000000L));
		as1.setNumeroMaison("12");
		as1.setNumeroOrdrePoste(528);
		as1.setNumeroRue(1131419);
		as1.setPermanente(false);
		as1.setUsage(TypeAdresseTiers.COURRIER);
		pp2.addAdresseTiers(as1);
		pp2 = hibernateTemplate.merge(pp2);

		AdresseSuisse as2 = new AdresseSuisse();
		as2.setDateDebut(RegDate.get(2008, 4, 15));
		as2.setLogModifDate(new Timestamp(1199142000000L));
		as2.setNumeroMaison("12");
		as2.setNumeroOrdrePoste(254);
		as2.setNumeroRue(1136137);
		as2.setPermanente(false);
		as2.setUsage(TypeAdresseTiers.COURRIER);
		pp13.addAdresseTiers(as2);
		pp13 = hibernateTemplate.merge(pp13);

		AdresseSuisse as3 = new AdresseSuisse();
		as3.setDateDebut(RegDate.get(2008, 1, 29));
		as3.setLogModifDate(new Timestamp(1199142000000L));
		as3.setNumeroMaison("12");
		as3.setNumeroOrdrePoste(571);
		as3.setPermanente(false);
		as3.setRue("Rue des terreaux");
		as3.setUsage(TypeAdresseTiers.COURRIER);
		pp0.addAdresseTiers(as3);
		pp0 = hibernateTemplate.merge(pp0);

		AdresseSuisse as4 = new AdresseSuisse();
		as4.setDateDebut(RegDate.get(2008, 1, 29));
		as4.setLogModifDate(new Timestamp(1199142000000L));
		as4.setNumeroMaison("12");
		as4.setNumeroOrdrePoste(571);
		as4.setPermanente(false);
		as4.setRue("Rue des terreaux");
		as4.setUsage(TypeAdresseTiers.COURRIER);
		mc0.addAdresseTiers(as4);
		mc0 = hibernateTemplate.merge(mc0);

		AdresseSuisse as5 = new AdresseSuisse();
		as5.setDateDebut(RegDate.get(2006, 2, 21));
		as5.setLogModifDate(new Timestamp(1199142000000L));
		as5.setNumeroMaison("12");
		as5.setNumeroOrdrePoste(254);
		as5.setNumeroRue(1136139);
		as5.setPermanente(false);
		as5.setUsage(TypeAdresseTiers.COURRIER);
		pp12.addAdresseTiers(as5);
		pp12 = hibernateTemplate.merge(pp12);

		AdresseSuisse as6 = new AdresseSuisse();
		as6.setDateDebut(RegDate.get(2006, 7, 1));
		as6.setLogModifDate(new Timestamp(1199142000000L));
		as6.setNumeroMaison("12");
		as6.setNumeroRue(1131419);
		as6.setNumeroOrdrePoste(528);
		as6.setPermanente(false);
		as6.setUsage(TypeAdresseTiers.COURRIER);
		pp1.addAdresseTiers(as6);
		pp1 = hibernateTemplate.merge(pp1);

		ForDebiteurPrestationImposable fdpi0 = new ForDebiteurPrestationImposable();
		fdpi0.setDateFin(RegDate.get(2008, 3, 31));
		fdpi0.setDateDebut(RegDate.get(2007, 1, 1));
		fdpi0.setMotifFermeture(MotifFor.INDETERMINE);
		fdpi0.setMotifOuverture(MotifFor.INDETERMINE);
		fdpi0.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		fdpi0.setLogModifDate(new Timestamp(1199142000000L));
		fdpi0.setNumeroOfsAutoriteFiscale(MockCommune.Echallens.getNoOFS());
		fdpi0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi0.addForFiscal(fdpi0);
		dpi0 = hibernateTemplate.merge(dpi0);

		ForDebiteurPrestationImposable fdpi1 = new ForDebiteurPrestationImposable();
		fdpi1.setDateDebut(RegDate.get(2008, 4, 1));
		fdpi1.setMotifOuverture(MotifFor.INDETERMINE);
		fdpi1.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
		fdpi1.setLogModifDate(new Timestamp(1199142000000L));
		fdpi1.setNumeroOfsAutoriteFiscale(MockCommune.Leysin.getNoOFS());
		fdpi1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi0.addForFiscal(fdpi1);
		dpi0 = hibernateTemplate.merge(dpi0);

		DeclarationImpotSource dis0 = new DeclarationImpotSource();
		dis0.setDateDebut(RegDate.get(2008, 1, 1));
		dis0.setDateFin(RegDate.get(2008, 3, 31));
		dis0.setDelais(new HashSet());
		dis0.setEtats(new HashSet());
		dis0.setLogCreationDate(new Timestamp(1199142000000L));
		dis0.setLogModifDate(new Timestamp(1199142000000L));
		dis0.setModeCommunication(ModeCommunication.PAPIER);
		dis0.setPeriode(pf6);
		dis0.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.addDeclaration(dis0);

		DeclarationImpotSource dis1 = new DeclarationImpotSource();
		dis1.setDateDebut(RegDate.get(2008, 4, 1));
		dis1.setDateFin(RegDate.get(2008, 6, 30));
		dis1.setDelais(new HashSet());
		dis1.setEtats(new HashSet());
		dis1.setLogCreationDate(new Timestamp(1199142000000L));
		dis1.setLogModifDate(new Timestamp(1199142000000L));
		dis1.setModeCommunication(ModeCommunication.PAPIER);
		dis1.setPeriode(pf6);
		dis1.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi0.addDeclaration(dis1);
		dpi0 = hibernateTemplate.merge(dpi0);

		DeclarationImpotOrdinairePP dio0 = new DeclarationImpotOrdinairePP();
		dio0.setDateDebut(RegDate.get(2005, 1, 1));
		dio0.setDateFin(RegDate.get(2005, 12, 31));
		dio0.setDelais(new HashSet());
		dio0.setEtats(new HashSet());
		dio0.setLibre(false);
		dio0.setLogCreationDate(new Timestamp(1136070000000L));
		dio0.setLogModifDate(new Timestamp(1199142000000L));
		dio0.setModeleDocument(md0);
		dio0.setNumeroOfsForGestion(5652);
		dio0.setNumero(1);
		dio0.setPeriode(pf3);
		dio0.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio0);
		mc1 = hibernateTemplate.merge(mc1);

		DeclarationImpotOrdinairePP dio1 = new DeclarationImpotOrdinairePP();
		dio1.setDateDebut(RegDate.get(2006, 1, 1));
		dio1.setDateFin(RegDate.get(2006, 12, 31));
		dio1.setDelais(new HashSet());
		dio1.setEtats(new HashSet());
		dio1.setLibre(false);
		dio1.setLogCreationDate(new Timestamp(1167606000000L));
		dio1.setLogModifDate(new Timestamp(1199142000000L));
		dio1.setModeleDocument(md0);
		dio1.setNumeroOfsForGestion(5652);
		dio1.setNumero(1);
		dio1.setPeriode(pf4);
		dio1.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio1);
		mc1 = hibernateTemplate.merge(mc1);

		DeclarationImpotOrdinairePP dio2 = new DeclarationImpotOrdinairePP();
		dio2.setDateDebut(RegDate.get(2007, 1, 1));
		dio2.setDateFin(RegDate.get(2007, 12, 31));
		dio2.setDelais(new HashSet());
		dio2.setEtats(new HashSet());
		dio2.setLibre(false);
		dio2.setLogCreationDate(new Timestamp(1199142000000L));
		dio2.setLogModifDate(new Timestamp(1199142000000L));
		dio2.setModeleDocument(md2);
		dio2.setNumeroOfsForGestion(5652);
		dio2.setNumero(1);
		dio2.setPeriode(pf5);
		dio2.setTypeContribuable(TypeContribuable.VAUDOIS_ORDINAIRE);
		mc1.addDeclaration(dio2);
		mc1 = hibernateTemplate.merge(mc1);

		DeclarationImpotSource dis2 = new DeclarationImpotSource();
		dis2.setDateDebut(RegDate.get(2008, 4, 1));
		dis2.setDateFin(RegDate.get(2008, 6, 30));
		dis2.setDelais(new HashSet());
		dis2.setEtats(new HashSet());
		dis2.setLogCreationDate(new Timestamp(1199142000000L));
		dis2.setLogModifDate(new Timestamp(1199142000000L));
		dis2.setModeCommunication(ModeCommunication.PAPIER);
		dis2.setPeriode(pf6);
		dis2.setPeriodicite(PeriodiciteDecompte.TRIMESTRIEL);
		dpi1.addDeclaration(dis2);
		dpi1 = hibernateTemplate.merge(dpi1);

		EtatDeclaration ed0 = new EtatDeclarationEmise((RegDate.get(2008, 3, 20)));
		ed0.setLogModifDate(new Timestamp(1199142000000L));
		dis0.addEtat(ed0);
		dis0 = hibernateTemplate.merge(dis0);

		EtatDeclaration ed1 = new EtatDeclarationSommee(RegDate.get(2008, 5, 15),RegDate.get(2008, 5, 18), null);
		ed1.setLogModifDate(new Timestamp(1199142000000L));
		dis0.addEtat(ed1);
		dis0 = hibernateTemplate.merge(dis0);

		EtatDeclaration ed2 = new EtatDeclarationRetournee(RegDate.get(2008, 5, 25), "TEST");
		ed2.setLogModifDate(new Timestamp(1199142000000L));
		dis0.addEtat(ed2);
		dis0 = hibernateTemplate.merge(dis0);

		EtatDeclaration ed3 = new EtatDeclarationEmise(RegDate.get(2008, 6, 20));
		ed3.setLogModifDate(new Timestamp(1199142000000L));
		dis1.addEtat(ed3);
		dis1 = hibernateTemplate.merge(dis1);

		EtatDeclaration ed4 = new EtatDeclarationEmise(RegDate.get(2006, 1, 15));
		ed4.setLogModifDate(new Timestamp(1199142000000L));
		dio0.addEtat(ed4);
		dio0 = hibernateTemplate.merge(dio0);

		EtatDeclaration ed5 = new EtatDeclarationRetournee(RegDate.get(2006, 4, 13), "TEST");
		ed5.setLogModifDate(new Timestamp(1199142000000L));
		dio0.addEtat(ed5);
		dio0 = hibernateTemplate.merge(dio0);

		EtatDeclaration ed6 = new EtatDeclarationEmise(RegDate.get(2007, 1, 16));
		ed6.setLogModifDate(new Timestamp(1199142000000L));
		dio1.addEtat(ed6);
		dio1 = hibernateTemplate.merge(dio1);

		EtatDeclaration ed7 = new EtatDeclarationSommee(RegDate.get(2007, 9, 15),RegDate.get(2007, 9, 18), null);
		ed7.setLogModifDate(new Timestamp(1199142000000L));
		dio1.addEtat(ed7);
		dio1 = hibernateTemplate.merge(dio1);

		EtatDeclaration ed8 = new EtatDeclarationEchue(RegDate.get(2007, 11, 1));
		ed8.setLogModifDate(new Timestamp(1199142000000L));
		dio1.addEtat(ed8);
		dio1 = hibernateTemplate.merge(dio1);

		EtatDeclaration ed9 = new EtatDeclarationEmise(RegDate.get(2007, 1, 15));
		ed9.setLogModifDate(new Timestamp(1199142000000L));
		dio2.addEtat(ed9);
		dio2 = hibernateTemplate.merge(dio2);

		EtatDeclaration ed10 = new EtatDeclarationEmise(RegDate.get(2008, 3, 20));
		ed10.setLogModifDate(new Timestamp(1199142000000L));
		dis2.addEtat(ed10);
		dis2 = hibernateTemplate.merge(dis2);

		DelaiDeclaration dd0 = new DelaiDeclaration();
		dd0.setCleArchivageCourrier(null);
		dd0.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd0.setDelaiAccordeAu(RegDate.get(2008, 4, 30));
		dd0.setLogModifDate(new Timestamp(1199142000000L));
		dd0.setDateTraitement(RegDate.get(2007, 4, 30));
		dis0.addDelai(dd0);
		dis0 = hibernateTemplate.merge(dis0);

		DelaiDeclaration dd1 = new DelaiDeclaration();
		dd1.setCleArchivageCourrier(null);
		dd1.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd1.setDelaiAccordeAu(RegDate.get(2008, 7, 31));
		dd1.setLogModifDate(new Timestamp(1199142000000L));
		dd1.setDateTraitement(RegDate.get(2007, 4, 30));
		dis1.addDelai(dd1);
		dis1 = hibernateTemplate.merge(dis1);

		DelaiDeclaration dd2 = new DelaiDeclaration();
		dd2.setCleArchivageCourrier(null);
		dd2.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd2.setDateDemande(RegDate.get(2008, 6, 25));
		dd2.setDateTraitement(RegDate.get(2008, 6, 25));
		dd2.setDelaiAccordeAu(RegDate.get(2008, 9, 30));
		dd2.setLogModifDate(new Timestamp(1199142000000L));
		dis1.addDelai(dd2);
		dis1 = hibernateTemplate.merge(dis1);

		DelaiDeclaration dd3 = new DelaiDeclaration();
		dd3.setCleArchivageCourrier(null);
		dd3.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd3.setDelaiAccordeAu(RegDate.get(2006, 3, 15));
		dd3.setLogModifDate(new Timestamp(1199142000000L));
		dd3.setDateTraitement(RegDate.get(2007, 4, 30));
		dio0.addDelai(dd3);
		dio0 = hibernateTemplate.merge(dio0);

		DelaiDeclaration dd4 = new DelaiDeclaration();
		dd4.setCleArchivageCourrier(null);
		dd4.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd4.setDateDemande(RegDate.get(2006, 2, 20));
		dd4.setDateTraitement(RegDate.get(2006, 2, 20));
		dd4.setDelaiAccordeAu(RegDate.get(2006, 7, 31));
		dd4.setLogModifDate(new Timestamp(1199142000000L));
		dio0.addDelai(dd4);
		dio0 = hibernateTemplate.merge(dio0);

		DelaiDeclaration dd5 = new DelaiDeclaration();
		dd5.setCleArchivageCourrier(null);
		dd5.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd5.setDelaiAccordeAu(RegDate.get(2007, 3, 15));
		dd5.setLogModifDate(new Timestamp(1199142000000L));
		dd5.setDateTraitement(RegDate.get(2007, 4, 30));
		dio1.addDelai(dd5);
		dio1 = hibernateTemplate.merge(dio1);

		DelaiDeclaration dd6 = new DelaiDeclaration();
		dd6.setCleArchivageCourrier(null);
		dd6.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd6.setDelaiAccordeAu(RegDate.get(2008, 3, 15));
		dd6.setLogModifDate(new Timestamp(1199142000000L));
		dd6.setDateTraitement(RegDate.get(2007, 4, 30));
		dio2.addDelai(dd6);
		dio2 = hibernateTemplate.merge(dio2);

		DelaiDeclaration dd7 = new DelaiDeclaration();
		dd7.setCleArchivageCourrier(null);
		dd7.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		dd7.setDateDemande(RegDate.get(2008, 4, 12));
		dd7.setDateTraitement(RegDate.get(2008, 4, 12));
		dd7.setDelaiAccordeAu(RegDate.get(2008, 9, 15));
		dd7.setLogModifDate(new Timestamp(1199142000000L));
		dio2.addDelai(dd7);
		dio2 = hibernateTemplate.merge(dio2);

		AppartenanceMenage am0 = new AppartenanceMenage();
		am0.setDateDebut(RegDate.get(1990, 7, 3));
		am0.setLogModifDate(new Timestamp(1199142000000L));
		am0.setObjetId(12600004L);
		am0.setSujetId(12600003L);
		am0 = hibernateTemplate.merge(am0);
		pp0.addRapportSujet(am0);
		mc0.addRapportObjet(am0);

		AppartenanceMenage am1 = new AppartenanceMenage();
		am1.setDateDebut(RegDate.get(1985, 2, 15));
		am1.setLogModifDate(new Timestamp(1199142000000L));
		am1.setObjetId(86006202L);
		am1.setSujetId(12300001L);
		am1 = hibernateTemplate.merge(am1);
		pp9.addRapportSujet(am1);
		mc1.addRapportObjet(am1);

		AppartenanceMenage am2 = new AppartenanceMenage();
		am2.setDateDebut(RegDate.get(1985, 2, 15));
		am2.setLogModifDate(new Timestamp(1199142000000L));
		am2.setObjetId(86006202L);
		am2.setSujetId(12300002L);
		am2 = hibernateTemplate.merge(am2);
		pp10.addRapportSujet(am2);
		mc1.addRapportObjet(am2);

		RapportPrestationImposable rpi0 = new RapportPrestationImposable();
		rpi0.setDateDebut(RegDate.get(2008, 1, 29));
		rpi0.setDateFin(RegDate.get(2008, 6, 25));
		rpi0.setLogModifDate(new Timestamp(1199142000000L));
		rpi0.setObjetId(1678432L);
		rpi0.setSujetId(12600001L);
		rpi0 = hibernateTemplate.merge(rpi0);
		pp2.addRapportSujet(rpi0);
		dpi0.addRapportObjet(rpi0);

		RapportPrestationImposable rpi1 = new RapportPrestationImposable();
		rpi1.setDateDebut(RegDate.get(2008, 1, 29));
		rpi1.setLogModifDate(new Timestamp(1199142000000L));
		rpi1.setObjetId(1678432L);
		rpi1.setSujetId(12600003L);
		rpi1 = hibernateTemplate.merge(rpi1);
		pp0.addRapportSujet(rpi1);
		dpi0.addRapportObjet(rpi1);

		Tutelle t0 = new Tutelle();
		t0.setDateDebut(RegDate.get(2006, 2, 23));
		t0.setLogModifDate(new Timestamp(1199142000000L));
		t0.setObjetId(12300002L);
		t0.setSujetId(34807810L);
		t0 = hibernateTemplate.merge(t0);
		pp8.addRapportSujet(t0);
		pp10.addRapportObjet(t0);

		ContactImpotSource cis0 = new ContactImpotSource();
		cis0.setDateDebut(RegDate.get(2000, 1, 1));
		cis0.setLogModifDate(new Timestamp(1199142000000L));
		cis0.setObjetId(1678432L);
		cis0.setSujetId(43308102L);
		cis0 = hibernateTemplate.merge(cis0);
		pp3.addRapportSujet(cis0);
		dpi0.addRapportObjet(cis0);

		ForFiscalPrincipalPP ffp0 = new ForFiscalPrincipalPP();
		ffp0.setDateDebut(RegDate.get(2002, 2, 12));
		ffp0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp0.setLogModifDate(new Timestamp(1199142000000L));
		ffp0.setModeImposition(ModeImposition.ORDINAIRE);
		ffp0.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp0.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp0.setNumeroOfsAutoriteFiscale(MockCommune.Bale.getNoOFS());
		ffp0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp0);
		pp6 = hibernateTemplate.merge(pp6);

		ForFiscalSecondaire ffs0 = new ForFiscalSecondaire();
		ffs0.setDateFin(RegDate.get(2007, 12, 31));
		ffs0.setDateDebut(RegDate.get(2002, 2, 12));
		ffs0.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs0.setLogModifDate(new Timestamp(1199142000000L));
		ffs0.setMotifFermeture(MotifFor.VENTE_IMMOBILIER);
		ffs0.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		ffs0.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs0.setNumeroOfsAutoriteFiscale(MockCommune.Renens.getNoOFS());
		ffs0.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp6.addForFiscal(ffs0);
		pp6 = hibernateTemplate.merge(pp6);

		ForFiscalSecondaire ffs1 = new ForFiscalSecondaire();
		ffs1.setDateDebut(RegDate.get(2004, 7, 1));
		ffs1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs1.setLogModifDate(new Timestamp(1199142000000L));
		ffs1.setMotifOuverture(MotifFor.ACHAT_IMMOBILIER);
		ffs1.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs1.setNumeroOfsAutoriteFiscale(MockCommune.Aubonne.getNoOFS());
		ffs1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp6.addForFiscal(ffs1);
		pp6 = hibernateTemplate.merge(pp6);

		ForFiscalPrincipalPP ffp1 = new ForFiscalPrincipalPP();
		ffp1.setDateFin(RegDate.get(2002, 2, 11));
		ffp1.setDateDebut(RegDate.get(2001, 2, 12));
		ffp1.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp1.setLogModifDate(new Timestamp(1199142000000L));
		ffp1.setModeImposition(ModeImposition.SOURCE);
		ffp1.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp1.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp1.setNumeroOfsAutoriteFiscale(MockCommune.Bern.getNoOFS());
		ffp1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp1);
		pp6 = hibernateTemplate.merge(pp6);

		ForFiscalPrincipalPP ffp2 = new ForFiscalPrincipalPP();
		ffp2.setDateFin(RegDate.get(2001, 2, 11));
		ffp2.setDateDebut(RegDate.get(2000, 2, 12));
		ffp2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp2.setLogModifDate(new Timestamp(1199142000000L));
		ffp2.setModeImposition(ModeImposition.SOURCE);
		ffp2.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);
		ffp2.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp2.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp2.setNumeroOfsAutoriteFiscale(MockCommune.Neuchatel.getNoOFS());
		ffp2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
		pp6.addForFiscal(ffp2);
		pp6 = hibernateTemplate.merge(pp6);

		ForFiscalPrincipalPP ffp3 = new ForFiscalPrincipalPP();
		ffp3.setDateDebut(RegDate.get(2008, 1, 29));
		ffp3.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp3.setLogModifDate(new Timestamp(1199142000000L));
		ffp3.setModeImposition(ModeImposition.SOURCE);
		ffp3.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp3.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp3.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ffp3.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp2.addForFiscal(ffp3);
		pp2 = hibernateTemplate.merge(pp2);

		ForFiscalPrincipalPP ffp4 = new ForFiscalPrincipalPP();
		ffp4.setDateFin(RegDate.get(1985, 2, 14));
		ffp4.setDateDebut(RegDate.get(1979, 2, 9));
		ffp4.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp4.setLogModifDate(new Timestamp(1199142000000L));
		ffp4.setModeImposition(ModeImposition.ORDINAIRE);
		ffp4.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp4.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp4.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp4.setNumeroOfsAutoriteFiscale(MockCommune.Bex.getNoOFS());
		ffp4.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp9.addForFiscal(ffp4);
		pp9 = hibernateTemplate.merge(pp9);

		ForFiscalPrincipalPP ffp5 = new ForFiscalPrincipalPP();
		ffp5.setDateFin(RegDate.get(1985, 2, 14));
		ffp5.setDateDebut(RegDate.get(1978, 10, 20));
		ffp5.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp5.setLogModifDate(new Timestamp(1199142000000L));
		ffp5.setModeImposition(ModeImposition.ORDINAIRE);
		ffp5.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp5.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp5.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp5.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
		ffp5.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp10.addForFiscal(ffp5);
		pp10 = hibernateTemplate.merge(pp10);

		ForFiscalPrincipalPP ffp6 = new ForFiscalPrincipalPP();
		ffp6.setDateDebut(RegDate.get(1985, 2, 15));
		ffp6.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp6.setLogModifDate(new Timestamp(1199142000000L));
		ffp6.setModeImposition(ModeImposition.ORDINAIRE);
		ffp6.setMotifOuverture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp6.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp6.setNumeroOfsAutoriteFiscale(MockCommune.CheseauxSurLausanne.getNoOFS());
		ffp6.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		mc1.addForFiscal(ffp6);
		mc1 = hibernateTemplate.merge(mc1);

		ForFiscalPrincipalPP ffp7 = new ForFiscalPrincipalPP();
		ffp7.setDateDebut(RegDate.get(1997, 6, 24));
		ffp7.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp7.setLogModifDate(new Timestamp(1199142000000L));
		ffp7.setModeImposition(ModeImposition.ORDINAIRE);
		ffp7.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp7.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp7.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
		ffp7.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp8.addForFiscal(ffp7);
		pp8 = hibernateTemplate.merge(pp8);

		ForFiscalPrincipalPP ffp8 = new ForFiscalPrincipalPP();
		ffp8.setDateDebut(RegDate.get(2008, 1, 29));
		ffp8.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp8.setLogModifDate(new Timestamp(1199142000000L));
		ffp8.setModeImposition(ModeImposition.SOURCE);
		ffp8.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp8.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp8.setNumeroOfsAutoriteFiscale(MockCommune.Chamblon.getNoOFS());
		ffp8.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		mc0.addForFiscal(ffp8);
		mc0 = hibernateTemplate.merge(mc0);

		ForFiscalPrincipalPP ffp9 = new ForFiscalPrincipalPP();
		ffp9.setDateDebut(RegDate.get(2008, 4, 15));
		ffp9.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp9.setLogModifDate(new Timestamp(1199142000000L));
		ffp9.setModeImposition(ModeImposition.SOURCE);
		ffp9.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp9.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp9.setNumeroOfsAutoriteFiscale(MockCommune.Croy.getNoOFS());
		ffp9.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp13.addForFiscal(ffp9);
		pp13 = hibernateTemplate.merge(pp13);


		ForFiscalPrincipalPP ffp10 = new ForFiscalPrincipalPP();
		ffp10.setDateDebut(RegDate.get(2006, 6, 5));
		ffp10.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp10.setLogModifDate(new Timestamp(1199142000000L));
		ffp10.setModeImposition(ModeImposition.SOURCE);
		ffp10.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
		ffp10.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp10.setNumeroOfsAutoriteFiscale(MockPays.Allemagne.getNoOFS());
		ffp10.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		pp1.addForFiscal(ffp10);
		pp1 = hibernateTemplate.merge(pp1);

		ForFiscalSecondaire ffs2 = new ForFiscalSecondaire();
		ffs2.setDateDebut(RegDate.get(2006, 6, 5));
		ffs2.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffs2.setLogModifDate(new Timestamp(1199142000000L));
		ffs2.setMotifOuverture(MotifFor.MAJORITE);
		ffs2.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffs2.setNumeroOfsAutoriteFiscale(MockCommune.Bussigny.getNoOFS());
		ffs2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp1.addForFiscal(ffs2);
		pp1 = hibernateTemplate.merge(pp1);

		ForFiscalPrincipalPP ffp11 = new ForFiscalPrincipalPP();
		ffp11.setDateFin(RegDate.get(1990, 7, 2));
		ffp11.setDateDebut(RegDate.get(1971, 12, 18));
		ffp11.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp11.setLogModifDate(new Timestamp(1199142000000L));
		ffp11.setModeImposition(ModeImposition.SOURCE);
		ffp11.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		ffp11.setMotifOuverture(MotifFor.MAJORITE);
		ffp11.setMotifRattachement(MotifRattachement.DOMICILE);
		ffp11.setNumeroOfsAutoriteFiscale(MockCommune.Lonay.getNoOFS());
		ffp11.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		pp0.addForFiscal(ffp11);
		pp0 = hibernateTemplate.merge(pp0);

		SituationFamilleMenageCommun sfmc0 = new SituationFamilleMenageCommun();
		sfmc0.setDateDebut(RegDate.get(1990, 7, 3));
		sfmc0.setEtatCivil(EtatCivil.MARIE);
		sfmc0.setLogModifDate(new Timestamp(1199142000000L));
		sfmc0.setNombreEnfants(2);
		sfmc0.setTarifApplicable(TarifImpotSource.NORMAL);
		sfmc0.setContribuablePrincipalId(12600003L);
		mc0.addSituationFamille(sfmc0);
		mc0 = hibernateTemplate.merge(mc0);

		SituationFamillePersonnePhysique sfpp0 = new SituationFamillePersonnePhysique();
		sfpp0.setDateDebut(RegDate.get(1960, 10, 20));
		sfpp0.setDateFin(RegDate.get(1985, 2, 14));
		sfpp0.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp0.setLogModifDate(new Timestamp(1199142000000L));
		sfpp0.setNombreEnfants(0);
		pp9.addSituationFamille(sfpp0);
		pp9 = hibernateTemplate.merge(pp9);

		SituationFamillePersonnePhysique sfpp1 = new SituationFamillePersonnePhysique();
		sfpp1.setDateDebut(RegDate.get(1961, 2, 9));
		sfpp1.setDateFin(RegDate.get(1985, 2, 14));
		sfpp1.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp1.setLogModifDate(new Timestamp(1199142000000L));
		sfpp1.setNombreEnfants(0);
		pp10.addSituationFamille(sfpp1);
		pp10 = hibernateTemplate.merge(pp10);

		SituationFamillePersonnePhysique sfpp2 = new SituationFamillePersonnePhysique();
		sfpp2.setDateDebut(RegDate.get(2005, 8, 29));
		sfpp2.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp2.setLogModifDate(new Timestamp(1199142000000L));
		sfpp2.setNombreEnfants(0);
		pp7.addSituationFamille(sfpp2);
		pp7 = hibernateTemplate.merge(pp7);

		SituationFamillePersonnePhysique sfpp3 = new SituationFamillePersonnePhysique();
		sfpp3.setDateDebut(RegDate.get(1979, 6, 24));
		sfpp3.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp3.setLogModifDate(new Timestamp(1199142000000L));
		sfpp3.setNombreEnfants(0);
		pp8.addSituationFamille(sfpp3);
		pp8 = hibernateTemplate.merge(pp8);

		SituationFamilleMenageCommun sfmc1 = new SituationFamilleMenageCommun();
		sfmc1.setDateDebut(RegDate.get(1985, 2, 15));
		sfmc1.setDateFin(RegDate.get(1985, 6, 1));
		sfmc1.setEtatCivil(EtatCivil.MARIE);
		sfmc1.setLogModifDate(new Timestamp(1199142000000L));
		sfmc1.setNombreEnfants(0);
		sfmc1.setContribuablePrincipalId(12300002L);
		mc1.addSituationFamille(sfmc1);
		mc1 = hibernateTemplate.merge(mc1);

		SituationFamilleMenageCommun sfmc2 = new SituationFamilleMenageCommun();
		sfmc2.setDateDebut(RegDate.get(1985, 6, 2));
		sfmc2.setEtatCivil(EtatCivil.MARIE);
		sfmc2.setLogModifDate(new Timestamp(1199142000000L));
		sfmc2.setNombreEnfants(1);
		sfmc2.setContribuablePrincipalId(12300002L);
		mc1.addSituationFamille(sfmc2);
		mc1 = hibernateTemplate.merge(mc1);

		SituationFamillePersonnePhysique sfpp4 = new SituationFamillePersonnePhysique();
		sfpp4.setDateDebut(RegDate.get(2008, 1, 29));
		sfpp4.setEtatCivil(EtatCivil.CELIBATAIRE);
		sfpp4.setLogModifDate(new Timestamp(1199142000000L));
		sfpp4.setNombreEnfants(0);
		pp2.addSituationFamille(sfpp4);
		pp2 = hibernateTemplate.merge(pp2);

		IdentificationPersonne ip0 = new IdentificationPersonne();
		ip0.setCategorieIdentifiant(CategorieIdentifiant.CH_AHV_AVS);
		ip0.setIdentifiant("15489652357");
		ip0.setLogModifDate(new Timestamp(1199142000000L));
		pp2.addIdentificationPersonne(ip0);
		pp2 = hibernateTemplate.merge(pp2);

		IdentificationPersonne ip1 = new IdentificationPersonne();
		ip1.setCategorieIdentifiant(CategorieIdentifiant.CH_ZAR_RCE);
		ip1.setIdentifiant("0784.7621/5");
		ip1.setLogModifDate(new Timestamp(1199142000000L));
		pp2.addIdentificationPersonne(ip1);
		pp2 = hibernateTemplate.merge(pp2);
	}
}
