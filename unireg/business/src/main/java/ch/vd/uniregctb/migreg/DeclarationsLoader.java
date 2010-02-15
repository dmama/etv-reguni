package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class DeclarationsLoader extends SubElementsFetcher {

	private static final Logger LOGGER = Logger.getLogger(DeclarationsLoader.class);

	private static final String DI_NOSEQ = "NO_SEQUENCE";
	private static final String DI_TYPE_QUITTANCE = "TYPE_QUITTANCE";
	private static final String DI_NUMERO_ANNEE = "NO_PAR_ANNEE";
	private static final String DI_QUALIFICATION = "QUALIFICATION";
	private static final String DI_COMMUNE_GESTION = "FK_COMMUNENO";
	private static final String DI_NO_CONTRIBUABLE = "FK_FK_CONTNO";
	private static final String DI_ANNEE_FISCALE = "ANNEE_FISCALE";
	private static final String DI_VISA = "DI_VISA";
	private static final String DI_DATE_MUT = "DI_DAMUT";
	private static final String DI_DELAI_ECHEANCE = "DELAI_ECHEANCE";
	private static final String DI_DATE_ENVOI = "DATE_ENVOI_DI";
	//private static final String DI_HEURE_MUT = "DI_HRMUT";
	//private static final String DI_NO_MAJ = "DI_NOMAJ";

	private static final String DEM_DELAI_NOSEQ = "DEMDEL_NOSEQ";
	private static final String DEM_DELAI_DATE_ACCORD = "DELAI_ACCORDE";
	private static final String DEM_DELAI_DATE_DEMANDE = "DA_DEMANDE";
	private static final String DEM_DELAI_DATE_TRAITEMENT = "DA_ENREGISTREMENT";
	private static final String DEM_DELAI_DATE_ANNULATION = "DA_ANNULATION";
	private static final String DEM_DELAI_VISA = "DELDI_VISA";
	private static final String DEM_DELAI_DATE_MUT = "DELDI_DAMUT";
	//private static final String DEM_DELAI_HEURE_MUT = "DELDI_HRMUT";
	//private static final String DEM_DELAI_NO_MAJ = "DELDI_NOMAJ";

	private static final String ETAT_DI_NOSEQ = "ETDI_NOSEQ";
	private static final String ETAT_DI_DATE_ATTRIB = "DA_ATTRIBUTION";
	private static final String ETAT_DI_CODE_ETAT = "CODE_ETAT";
	private static final String ETAT_DI_VISA = "ETDI_VISA";
	private static final String ETAT_DI_DATE_MUT = "ETDI_DAMUT";
	//private static final String ETAT_DI_HEURE_MUT = "ETDI_HRMUT";
	//private static final String ETAT_DI_NO_MAJ = "ETDI_NOMAJ";


	public DeclarationsLoader(HostMigratorHelper helper, StatusManager mgr) {
		super(helper, mgr);
	}

	public ArrayList<MigrationError> loadDIListCtb(ArrayList<Tiers> listTiers, ArrayList<MigrationError> errors) throws Exception {

		Set<Declaration> setDi = new HashSet<Declaration>();
		Set<EtatDeclaration> setEtDi = new HashSet<EtatDeclaration>();
		Set<DelaiDeclaration> setDelaiDi = new HashSet<DelaiDeclaration>();

		CollectiviteAdministrative cedi = helper.tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(
				ServiceInfrastructureService.noCEDI, true);
		Assert.notNull(cedi); // créé à l'initialisation par HostMigrationManager.saveCEDI()

//		try {
			SqlRowSet rsDi = readDiListCtb(listTiers);

			int saveNoSeqDi = 0;
			int saveNoSeqEtat = 0;
			int saveNoSeqDemDel = 0;
			boolean diAnnulee = false;
			Long saveNoCtb = 0L;
			String typeQuittancePrecedent = "";
			DeclarationImpotOrdinaire di = null;

			while (rsDi != null && rsDi.next() && !mgr.interrupted()) {
				//On ne migre pas les DI antérieures à 2003
				if (rsDi.getInt(DI_ANNEE_FISCALE)<2003) {
					continue;
				}
				if (saveNoSeqDi != rsDi.getInt(DI_NOSEQ)) {
					if (saveNoSeqDi != 0) {
						di.setAnnule(diAnnulee);
						di.setEtats(setEtDi);
						di.setDelais(setDelaiDi);
						setDi.add(di);
					}
					saveNoSeqDi = rsDi.getInt(DI_NOSEQ);
					di = new DeclarationImpotOrdinaire();
					diAnnulee = false;

					// Période fiscale
					final PeriodeFiscale pf = helper.periodeFiscaleDAO.getPeriodeFiscaleByYear(rsDi.getInt(DI_ANNEE_FISCALE));
					Assert.notNull(pf, "Période fiscale inexistante pour l'année "+rsDi.getInt(DI_ANNEE_FISCALE));
					di.setPeriode(pf);
					//Les dates de début et de fin de déclaration sont mises au 1er janvier et 31 décembre de la période fiscale
					di.setDateDebut(RegDate.get(pf.getAnnee(),1,1));
					di.setDateFin(RegDate.get(pf.getAnnee(),12,31));

					// Modèle de document
					final TypeDocument typeDocument;
					if (typeQuittancePrecedent != null && typeQuittancePrecedent.equals("E")) {
						typeDocument = TypeDocument.DECLARATION_IMPOT_VAUDTAX;
					}
					else {
						typeDocument = TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH;
					}
					final ModeleDocument modele = helper.modeleDocumentDAO.getModelePourDeclarationImpotOrdinaire(pf, typeDocument, true);
					Assert.notNull(modele, "Modèle de document " + typeDocument + " inexistant pour l'année " + pf.getAnnee());
					di.setModeleDocument(modele);

					typeQuittancePrecedent = rsDi.getString(DI_TYPE_QUITTANCE);
					di.setQualification(getQualification(rsDi.getString(DI_QUALIFICATION)));
					di.setNumero(rsDi.getInt(DI_NUMERO_ANNEE));
					di.setNumeroOfsForGestion(rsDi.getInt(DI_COMMUNE_GESTION));
					di.setLogCreationDate(rsDi.getDate(DI_DATE_MUT));
					di.setLogModifMillis(rsDi.getDate(DI_DATE_MUT).getTime());
					di.setLogCreationUser(rsDi.getString(DI_VISA));
					di.setLogModifUser(rsDi.getString(DI_VISA));
					di.setRetourCollectiviteAdministrative(cedi);
					setEtDi = new HashSet<EtatDeclaration>();
					setDelaiDi = new HashSet<DelaiDeclaration>();

					//Un délai par défaut est créé à la date d'échance de la DI.
					DelaiDeclaration demDel = new DelaiDeclaration();
					demDel.setDelaiAccordeAu(RegDate.get(rsDi.getDate(DI_DELAI_ECHEANCE)));
					demDel.setDateDemande(RegDate.get(rsDi.getDate(DI_DATE_ENVOI)));
					demDel.setDateTraitement(RegDate.get(rsDi.getDate(DI_DATE_ENVOI)));
//					demDel.setLogCreationDate(new Date());
//					demDel.setLogCreationUser(helper.);
//					demDel.setLogModifUser(rsDi.getString(DEM_DELAI_VISA));
					setDelaiDi.add(demDel);

					saveNoSeqEtat = 0;
					saveNoSeqDemDel = 0;
				}
				if (saveNoSeqDi == rsDi.getInt(DI_NOSEQ)&&
					saveNoSeqEtat != rsDi.getInt(ETAT_DI_NOSEQ)){
					if (getTypeEtatDiUnireg(rsDi.getInt(ETAT_DI_CODE_ETAT)) == null) {
						diAnnulee = true;
					} else {
						saveNoSeqEtat = rsDi.getInt(ETAT_DI_NOSEQ);
						EtatDeclaration etDi = new EtatDeclaration();
						etDi.setDateObtention(RegDate.get(rsDi.getDate(ETAT_DI_DATE_ATTRIB)));
						etDi.setEtat(getTypeEtatDiUnireg(rsDi.getInt(ETAT_DI_CODE_ETAT)));
						etDi.setLogCreationDate(rsDi.getDate(ETAT_DI_DATE_MUT));
						etDi.setLogModifMillis(rsDi.getDate(ETAT_DI_DATE_MUT).getTime());
						etDi.setLogCreationUser(rsDi.getString(ETAT_DI_VISA));
						etDi.setLogModifUser(rsDi.getString(ETAT_DI_VISA));
						setEtDi.add(etDi);
					}
				}
				if (saveNoSeqDi == rsDi.getInt(DI_NOSEQ)&&
					saveNoSeqDemDel != rsDi.getInt(DEM_DELAI_NOSEQ)&&
					rsDi.getInt(DEM_DELAI_NOSEQ) > 0) {
					saveNoSeqDemDel = rsDi.getInt(DEM_DELAI_NOSEQ);
					DelaiDeclaration demDel = new DelaiDeclaration();
					demDel.setDelaiAccordeAu(RegDate.get(rsDi.getDate(DEM_DELAI_DATE_ACCORD)));
					demDel.setDateDemande(RegDate.get(rsDi.getDate(DEM_DELAI_DATE_DEMANDE)));
					demDel.setDateTraitement(RegDate.get(rsDi.getDate(DEM_DELAI_DATE_TRAITEMENT)));
					demDel.setLogCreationDate(rsDi.getDate(DEM_DELAI_DATE_MUT));
					demDel.setLogModifMillis(rsDi.getDate(DEM_DELAI_DATE_MUT).getTime());
					demDel.setLogCreationUser(rsDi.getString(DEM_DELAI_VISA));
					demDel.setLogModifUser(rsDi.getString(DEM_DELAI_VISA));
					if (RegDate.get(rsDi.getDate(DEM_DELAI_DATE_ANNULATION)) != null) {
						demDel.setAnnule(true);
					}
					setDelaiDi.add(demDel);
				}
				if (!saveNoCtb.equals(rsDi.getLong(DI_NO_CONTRIBUABLE))) {
					if (saveNoCtb > 0) {
						for (Tiers tiers : listTiers) {
							if (tiers.getNumero().equals(saveNoCtb)) {
								Contribuable ctb = (Contribuable) tiers;
								ctb.addAllDeclarations(setDi);
								break;
							}
						}
					}
					setDi = new HashSet<Declaration>();
					saveNoCtb = rsDi.getLong(DI_NO_CONTRIBUABLE);
				}

				//recherche du type de contribuable
				for (Tiers tiers : listTiers) {
					if (tiers.getNumero().equals(saveNoCtb)) {
						final Contribuable ctb = (Contribuable) tiers;
						final Assujettissement assujettissement = getAssujettissementAt(ctb, di.getDateFin());
						if (assujettissement != null) {
							final TypeContribuable typeContribuable = getTypeContribuable(assujettissement);
							final RegDate dateDebut = assujettissement.getDateDebut();
							final RegDate dateFin = assujettissement.getDateFin();
							di.setDateDebut(dateDebut);
							di.setDateFin(dateFin);
							di.setTypeContribuable(typeContribuable);
						}
						break;
					}
				}
			}
			if (di != null) {
				di.setAnnule(diAnnulee);
				di.setEtats(setEtDi);
				di.setDelais(setDelaiDi);
				setDi.add(di);
				for (Tiers tiers : listTiers) {
					if (tiers.getNumero().equals(saveNoCtb)) {
						Contribuable ctb = (Contribuable) tiers;
						ctb.addAllDeclarations(setDi);
						ValidationResults results = tiers.validate();
						if (results != null && results.hasErrors()) {
							int saveAnnee = 0;
							TypeEtatDeclaration saveTypeEtat = null;
							Declaration diToKeep = null;
							//On ne migre que la dernière déclaration lorsque pour une même année on trouve plusieurs déclarations
							//et que celles-ci possèdent le même état.
							//On ne migre également que la dernière lorsque les déclarations sont l'une échue et l'autre retournée ou inversément
							//On ne tient pas compte des déclarations annulées pour ces contrôles.
							for (Declaration diMigree : tiers.getDeclarationsSorted()) {
								if (diMigree.isAnnule()) {
									continue;
								}
								if (saveAnnee != diMigree.getPeriode().getAnnee()) {
									saveAnnee = diMigree.getPeriode().getAnnee();
									saveTypeEtat = diMigree.getDernierEtat().getEtat();
								}
								else {
									if (!saveTypeEtat.equals(diMigree.getDernierEtat().getEtat())) {
										if ((saveTypeEtat.equals(TypeEtatDeclaration.ECHUE) || saveTypeEtat.equals(TypeEtatDeclaration.RETOURNEE)) &&
												(diMigree.getDernierEtat().getEtat().equals(TypeEtatDeclaration.ECHUE) || diMigree.getDernierEtat().getEtat().equals(TypeEtatDeclaration.RETOURNEE))) {

										} else {
											diToKeep = null;
											break;
										}
									}
									diToKeep = diMigree;
								}
							}
							if (diToKeep != null) {
								for(Declaration declarationToRemove : tiers.getDeclarations()) {
									if (!declarationToRemove.equals(diToKeep) && declarationToRemove.getDateDebut().year() == diToKeep.getDateDebut().year()) {
										tiers.getDeclarations().remove(declarationToRemove);
										helper.tiersDAO.getHibernateTemplate().delete(declarationToRemove);
										break;
									}
								}
							}
						}
						break;
					}
				}
			}
//		}
//		catch (Exception e) {
//			Audit.error("Lecture des DI impossible. Cause : "+e.getMessage());
//		}

		return errors;
	}

	/**
	 * @return l'assujettissement valide à la date et sur le contribuable spécifiés, ou <b>null</b> si le contribuable n'est pas assujetti à
	 *         cette date.
	 */
	private Assujettissement getAssujettissementAt(Contribuable ctb, RegDate date) {

		if (date == null) {
			return null;
		}

		final int annee = date.year();

		List<Assujettissement> assujettissements = null;
		try {
			assujettissements = Assujettissement.determine(ctb, annee);
		}
		catch (Exception e) {
			LOGGER.warn("Impossible de calculer les dates de la DI pour le contribuable n°" + ctb.getNumero() + " et l'année "
					+ annee + ". La DI est migrée avec les valeurs par défaut. Message = " + e.getMessage());
		}

		Assujettissement assujettissement = null;

		if (assujettissements != null) {
			for (Assujettissement a : assujettissements) {
				if (a.isValidAt(date)) {
					assujettissement = a;
					break;
				}
			}

		}

		return assujettissement;
	}

	/**
	 * Permet de determiner le type de contribuable à partir de son assujettissement.
	 */
	private  TypeContribuable getTypeContribuable(Assujettissement assujettissement) {
		// On recupere le for principal valide à la date N

		if (assujettissement == null) {
			return null;
		}

		if (assujettissement instanceof VaudoisOrdinaire) {
			return TypeContribuable.VAUDOIS_ORDINAIRE;
		}
		else if (assujettissement instanceof VaudoisDepense) {
			return TypeContribuable.VAUDOIS_DEPENSE;
		}
		else if (assujettissement instanceof HorsCanton) {
			return TypeContribuable.HORS_CANTON;
		}
		else if (assujettissement instanceof HorsSuisse) {
			return TypeContribuable.HORS_SUISSE;
		}

		return null;
	}

	private Qualification getQualification(String qualificationDi) {
		if (qualificationDi == null) {
			return null;
		}
		if (qualificationDi.equals("SM")) {
			return Qualification.SEMI_MANUEL;
		}
		if (qualificationDi.equals("SA")) {
			return Qualification.SEMI_AUTOMATIQUE;
		}
		if (qualificationDi.equals("A")) {
			return Qualification.AUTOMATIQUE;
		}
		if (qualificationDi.equals("M")) {
			return Qualification.MANUEL;
		}
		if (qualificationDi.equals("C1")) {
			return Qualification.COMPLEXE_1;
		}
		if (qualificationDi.equals("C2")) {
			return Qualification.COMPLEXE_2;
		}
		return null;
	}

	private SqlRowSet readDiListCtb(ArrayList<Tiers> lstTiers) throws Exception {
		StringBuilder sbCtb = new StringBuilder();
		for (Tiers tiers : lstTiers) {
			sbCtb.append(tiers.getNumero());
			sbCtb.append(",");
		}
		if (sbCtb.length() == 0) {
			return null;
		}
		sbCtb.deleteCharAt(sbCtb.lastIndexOf(","));
		String query =
			"Select " +
			"A.NO_SEQUENCE" +
			",A.DELAI_SOMMATION" +
			",A.DA_ENVOI_SOMMATION" +
			",A.NO_SEQUENCE" +
			",A.ANNEE_FISCALE" +
			",A.NO_PAR_ANNEE" +
			",A.MODE_IMPOSITION" +
			",A.DATE_ENVOI_DI" +
			",A.DELAI_ECHEANCE" +
			",A.TYPE_QUITTANCE" +
			",A.QUALIFICATION" +
			",A.FK_COMMUNENO" +
			",A.FK_FK_CONTNO" +
			",A.VS_MUT AS DI_VISA" +
			",A.DA_MUT AS DI_DAMUT" +
			",A.HR_MUT AS DI_HRMUT" +
			",A.NO_MAJ AS DI_NOMAJ" +
			",B.NO_SEQUENCE AS ETDI_NOSEQ" +
			",B.DA_ATTRIBUTION" +
			",B.CODE_ETAT" +
			",B.VS_MUT AS ETDI_VISA" +
			",B.DA_MUT AS ETDI_DAMUT" +
			",B.HR_MUT AS ETDI_HRMUT" +
			",B.NO_MAJ AS ETDI_NOMAJ" +
			",C.NO_SEQUENCE AS DEMDEL_NOSEQ" +
			",C.DA_ENREGISTREMENT" +
			",C.DA_ANNULATION" +
			",C.DA_DEMANDE" +
			",C.DELAI_ACCORDE" +
			",C.VS_MUT AS DELDI_VISA" +
			",C.DA_MUT AS DELDI_DAMUT" +
			",C.HR_MUT AS DELDI_HRMUT" +
			",C.NO_MAJ AS DELDI_NOMAJ" +
			" FROM "+helper.getTableDb2("DECLARATION_IMPOT")+" A" +
			" LEFT JOIN "+helper.getTableDb2("DEMANDE_DELAI")+" C" +
								" ON A.FK_FK_CONTNO = C.FK_FK_CONTNO" +
								" AND A.ANNEE_FISCALE = C.FK_DI_AN_FISC" +
								" AND A.NO_PAR_ANNEE = C.FK_DI_NO_PAR_AN" +
			" ,"+helper.getTableDb2("ETAT_DI")+" B" +
			" WHERE A.FK_FK_CONTNO in ("+ sbCtb.toString()+")" +
			" AND A.FK_CONTRIBUABLENO = B.FK_FK_CONTNO" +
			" AND A.ANNEE_FISCALE = B.FK_DI_AN_FISC" +
			" AND A.NO_PAR_ANNEE = B.FK_DI_NO_PAR_AN" +
			" ORDER BY A.FK_FK_CONTNO, A.ANNEE_FISCALE, A.NO_PAR_ANNEE, B.CODE_ETAT";

		HostMigratorHelper.SQL_LOG.debug("Query: "+query);
		return helper.db2Template.queryForRowSet(query);
	}

	private TypeEtatDeclaration getTypeEtatDiUnireg(int typeEtatDiRegPP) {
		Assert.isTrue((typeEtatDiRegPP > 0 && typeEtatDiRegPP < 6), "Ce type d'état de la DI n'est pas pris en compte");

		switch (typeEtatDiRegPP){
			case 1:
				return TypeEtatDeclaration.EMISE;
			case 2:
				return TypeEtatDeclaration.RETOURNEE;
			case 3:
				return TypeEtatDeclaration.SOMMEE;
			case 4:
				return null;
			case 5:
				return TypeEtatDeclaration.ECHUE;
			default:
				return null;
		}

	}

}
