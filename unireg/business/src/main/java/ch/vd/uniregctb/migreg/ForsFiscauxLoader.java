package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.Constants;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeMigRegError;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class ForsFiscauxLoader extends SubElementsFetcher {
	private static final Logger LOGGER = Logger.getLogger(ForsFiscauxLoader.class);

	private static final String NO_CONTRIBUABLE = "FK_CONTRIBUABLENO";
	private static final String TYPE_FOR = "TYPE_FOR";
	private static final String MOTIF_OUVERTURE = "MOTIF_ENTREE";
	private static final String MOTIF_FERMETURE = "MOTIF_SORTIE";
	private static final String DATE_DEBUT_VALIDITE = "DAD_VALIDITE";
	private static final String DATE_FIN_VALIDITE = "DAF_VALIDITE";
	private static final String DATE_ANNULATION = "DAA_FOR_PRINC_CTB";
	private static final String PAYS_NO_OFS = "FK_PAYSNO_OFS";
	private static final String NO_COMMUNE = "fk_communeno";
	private static final String INDIGENT = "INDIGENT";
	private static final String IMPOT_DEPENSE = "IMPOT_DEPENSE_VD";
	private static final String SIGLE_CANTON = "FK_CANTONSIGLE";
	private static final String DATE_MUTATION = "DA_MUT";
	private static final String VISA_MUTATION = "VS_MUT";


	private Set<ForFiscal> forsFiscaux = new HashSet<ForFiscal>();
	private List<ForFiscalPrincipal> forsFiscauxPrincipaux = new ArrayList<ForFiscalPrincipal>();


	public ForsFiscauxLoader(HostMigratorHelper helper, StatusManager mgr, MigregErrorsManager errorsManager) {
		super(helper, mgr);
	}

	public ArrayList<MigrationError> loadForsFiscaux(ArrayList<Tiers> lstTiers, ArrayList<MigrationError> errors) throws Exception {

		ArrayList<Tiers> listTiersOk = new ArrayList<Tiers>();
		listTiersOk.addAll(lstTiers);
		long numeroCtbCourant = 0L;

		SqlRowSet fors = readAllForFiscauxListContribuable(lstTiers);
//		boolean firstForPrincipal = false;
//		ForFiscalPrincipal firstFfp = new ForFiscalPrincipal();
		ForFiscalPrincipal lastForFiscalPrincipal = null;
		while (fors != null && fors.next() && !mgr.interrupted()) {
			if (numeroCtbCourant != fors.getLong(NO_CONTRIBUABLE)){
				if (numeroCtbCourant > 0) {
					errors = addForsToCtb(numeroCtbCourant, lstTiers, errors);
				}
				numeroCtbCourant = fors.getLong(NO_CONTRIBUABLE);
				forsFiscaux = new HashSet<ForFiscal>();
				forsFiscauxPrincipaux = new ArrayList<ForFiscalPrincipal>();
				lastForFiscalPrincipal = null;
//				firstForPrincipal = false;
//				firstFfp = new ForFiscalPrincipal();
			}

			Integer motif = fors.getInt(MOTIF_OUVERTURE);
			Integer motifFin = fors.getInt(MOTIF_FERMETURE);
			//LOGGER.debug("Motif entrée="+motif);
			if (motif != 7 &&
				motif != 10 &&
				motif != 11 &&
				motif != 12 &&
				motif != 13 &&
				motif != 992 &&
				motif != 993 &&
				motif != 995 &&
				motif != 4 &&
				motif != 6 &&
				motif != 20 &&
				motif != 24 &&
				motif != 26 &&
				!(motif >= 51 && motif <= 989)) {

				motif = 7;

//				String message = "Un des fors du tiers " + numeroCtbCourant + " possède un motif d'entrée ["+motif+"] non reconnu";
//				MigrationError error = new MigrationError();
//				error.setNoContribuable(numeroCtbCourant);
//				error.setMessage(message);
//				error.setTypeErreur(TypeMigRegError.ERROR_APPLICATIVE);
//				errors.add(error);
//				continue;
			}

			ForFiscalRevenuFortune forFiscal;
			if (fors.getString(TYPE_FOR).equals("Principal")) {
				ForFiscalPrincipal forFiscalPrincipal = new ForFiscalPrincipal();
				forFiscal = forFiscalPrincipal;
				forFiscalPrincipal.setMotifRattachement(MotifRattachement.DOMICILE);

				//Tenir compte des sourciers mixtes. Mode d'imposition pour sourcier mixte : toujours mixte 137 alinea 2.
				forFiscalPrincipal.setModeImposition(ModeImposition.ORDINAIRE);
				if (fors.getString("SOURCIER_MIXTE").equals(Constants.OUI)) {
					forFiscalPrincipal.setModeImposition(ModeImposition.MIXTE_137_2);
				}

				if (RegDate.get(fors.getDate(DATE_ANNULATION)) != null
					|| motifFin == 1) {
					forFiscalPrincipal.setAnnule(true);
				}

				if (fors.getInt(PAYS_NO_OFS) != 0) {
					forFiscalPrincipal.setNumeroOfsAutoriteFiscale(fors.getInt(PAYS_NO_OFS));
					forFiscalPrincipal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
					if (fors.getString("SOURCIER_MIXTE").equals(Constants.OUI)) {
						forFiscalPrincipal.setModeImposition(ModeImposition.MIXTE_137_1);
					}
				}
				else {
//					Commune commune = serviceInfrastructureService.getCommuneByNumeroOfsEtendu(fors.getInt(FK_COMMUNE));
					if (fors.getString(SIGLE_CANTON).equals("VD")) {
						forFiscalPrincipal.setNumeroOfsAutoriteFiscale(fors.getInt(NO_COMMUNE));
						forFiscalPrincipal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);

						//Les modes d'imposition "à la dépense" et "indigent" ne peuvent être mis que sur un for vaudois.
						if (fors.getString(INDIGENT).equals(Constants.OUI)) {
							forFiscalPrincipal.setModeImposition(ModeImposition.INDIGENT);
						}
						if (fors.getString(IMPOT_DEPENSE).equals(Constants.OUI)) {
							forFiscalPrincipal.setModeImposition(ModeImposition.DEPENSE);
						}
					} else {
						forFiscalPrincipal.setNumeroOfsAutoriteFiscale(fors.getInt(NO_COMMUNE));
						forFiscalPrincipal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
						if (fors.getString("SOURCIER_MIXTE").equals(Constants.OUI)) {
							forFiscalPrincipal.setModeImposition(ModeImposition.MIXTE_137_1);
						}
					}
				}
				forFiscalPrincipal.setDateDebut(RegDate.get(fors.getDate(DATE_DEBUT_VALIDITE)));
				forFiscalPrincipal.setDateFin(RegDate.get(fors.getDate(DATE_FIN_VALIDITE)));
//				if (!firstForPrincipal && !forFiscalPrincipal.isAnnule()) {
//					firstFfp = forFiscalPrincipal;
//					firstForPrincipal = true;
//				}
			}
			else {
				ForFiscalSecondaire forFiscalSecondaire = new ForFiscalSecondaire();
//				if (RegDate.get(fors.getDate(DATE_DEBUT_VALIDITE)).isBeforeOrEqual(RegDate.get(fors.getDate(DATE_FIN_VALIDITE)))) {
//
//				}
				forFiscalSecondaire.setDateDebut(RegDate.get(fors.getDate(DATE_DEBUT_VALIDITE)));
				forFiscalSecondaire.setDateFin(RegDate.get(fors.getDate(DATE_FIN_VALIDITE)));


//				if (firstForPrincipal) {
//					if (forFiscalSecondaire.getDateDebut().isBefore(firstFfp.getDateDebut())) {
//						if (!firstFfp.getTypeAutoriteFiscale().equals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) &&
//							!firstFfp.getDateDebut().isAfter(forFiscalSecondaire.getDateDebut())) {
//							forFiscalSecondaire.setDateDebut(firstFfp.getDateDebut());
//						}
//					}
//				}
				forFiscal = forFiscalSecondaire;
				if (motif == 4) {
						forFiscalSecondaire.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
				}
				else {
						forFiscalSecondaire.setMotifRattachement(MotifRattachement.ACTIVITE_INDEPENDANTE);
				}
				forFiscalSecondaire.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				forFiscalSecondaire.setNumeroOfsAutoriteFiscale(fors.getInt(NO_COMMUNE));

				if(!Tiers.existForPrincipal(forsFiscauxPrincipaux, forFiscalSecondaire.getDateDebut(), forFiscalSecondaire.getDateFin())) {
					boolean dateMiseAJour = false;
					if (forsFiscauxPrincipaux.size() == 0) {
						forFiscalSecondaire.setAnnule(true);
					}
					else {
						if (forFiscalSecondaire.getDateFin() != null && forFiscalSecondaire.getDateFin().year() < 2003) {
							forFiscalSecondaire.setAnnule(true);
						}
						else {

							for (ForFiscalPrincipal fp : forsFiscauxPrincipaux) {
								if (!dateMiseAJour && forFiscalSecondaire.getDateDebut().isBefore(fp.getDateDebut())) {
									forFiscalSecondaire.setDateDebut(fp.getDateDebut());
								}
								dateMiseAJour = true;
								if (forFiscalSecondaire.getDateFin() == null &&
									MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(fp.getMotifFermeture()) &&
									lastForFiscalPrincipal != null && fp.equals(lastForFiscalPrincipal)) {
									forFiscalSecondaire.setDateFin(fp.getDateFin());
								}
								if (forFiscalSecondaire.getDateFin() == null &&
									MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT.equals(fp.getMotifFermeture()) &&
									lastForFiscalPrincipal != null && fp.equals(lastForFiscalPrincipal)) {
									forFiscalSecondaire.setDateFin(fp.getDateFin());
								}
							}
						}
					}
				}

			}

			//Un motif d'ouverture est obligatoire.
			forFiscal.setMotifOuverture(getMotifOuverture(motif));
			Assert.notNull(forFiscal.getMotifOuverture());
			if (forFiscal.getDateFin() != null) {
				if (getMotifFermeture(fors.getInt(MOTIF_FERMETURE)) == null) {
					if (MotifFor.ACHAT_IMMOBILIER.equals(forFiscal.getMotifOuverture())) {
						forFiscal.setMotifFermeture(MotifFor.VENTE_IMMOBILIER);
					}
					else {
						forFiscal.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
					}
				}
				else {
					forFiscal.setMotifFermeture(getMotifFermeture(fors.getInt(MOTIF_FERMETURE)));
				}
			}
			forFiscal.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			//La date de début de For est obligatoire.
			Assert.notNull(forFiscal.getDateDebut());
			//La date de début d'un for fiscal doit être antérieure ou égale à sa date de fin.
//			if (forFiscal.getDateFin() != null && forFiscal.getDateDebut().isAfter(forFiscal.getDateFin())) {
//				String message = "Un des fors du tiers " + numeroCtbCourant + " possède une date de début qui est après la date de fin: début = " + forFiscal.getDateDebut()
//				+ " fin = " + forFiscal.getDateFin();
//				MigrationError error = new MigrationError();
//				error.setNoContribuable(numeroCtbCourant);
//				error.setMessage(message);
//				errors.add(error);
//			}
			//Importation des attributs de logs
			forFiscal.setLogCreationDate(fors.getDate(DATE_MUTATION));
			forFiscal.setLogCreationUser(fors.getString(VISA_MUTATION));
			forFiscal.setLogModifMillis(fors.getDate(DATE_MUTATION).getTime());
			forFiscal.setLogModifUser(fors.getString(VISA_MUTATION));

			forsFiscaux.add(forFiscal);
			if (forFiscal instanceof ForFiscalPrincipal && !forFiscal.isAnnule()) {
				lastForFiscalPrincipal = (ForFiscalPrincipal) forFiscal;
				forsFiscauxPrincipaux.add((ForFiscalPrincipal) forFiscal);
			}
		}

		errors = addForsToCtb(numeroCtbCourant, lstTiers, errors);

		return errors;
	}

	private ArrayList<MigrationError> addForsToCtb(long numeroCtbCourant, ArrayList<Tiers> lstTiers, ArrayList<MigrationError> errors) throws Exception {
//		final List<Pair<Tiers, Tiers>> noMenageCommunsToDelete = new ArrayList<Pair<Tiers, Tiers>>();
		for (Tiers tiers : lstTiers) {
			if (tiers.getNumero().equals(numeroCtbCourant)) {
				Contribuable ctb = (Contribuable) tiers;
				for (ForFiscal f : forsFiscaux) {
					ctb.addForFiscal(f);
				}
				for (ForFiscal f : ctb.getForsFiscauxNonAnnules(false)) {
					if (f instanceof ForFiscalSecondaire && f.getDateFin() != null && f.getDateDebut().isAfter(f.getDateFin())) {
						if (Assujettissement.determine(ctb, RegDate.get().year()) == null || Assujettissement.determine(ctb, RegDate.get().year()).size() == 0) {
							f.setDateDebut(f.getDateFin().getOneDayBefore());
						}
					}
				}

				//Adapter la date de début du for secondaire lorsque celui-ci débute avant un for fiscal principal.
				//Dans le cas où le for secondaire est complètement hors période d'un for fiscal principal, le for secondaire
				//est annulé.
				ForsParType fpt = tiers.getForsParType(true);
				List<ForFiscalPrincipal> listFfp = fpt.principaux;
				List<ForFiscalSecondaire> listFfs = fpt.secondaires;
				ForFiscalPrincipal firstFfp = tiers.getPremierForFiscalPrincipal();
				for (ForFiscalSecondaire ffs : listFfs) {
					if (!Tiers.existForPrincipal(listFfp, ffs.getDateDebut(), ffs.getDateFin())) {
						if (ffs.getDateFin() != null && firstFfp.getDateDebut().isAfter(ffs.getDateFin())) {
							ffs.setAnnule(true);
							break;
						}
						if (firstFfp.getDateDebut().isAfter(ffs.getDateDebut())) {
							ffs.setDateDebut(firstFfp.getDateDebut());
							if (ffs.getDateFin() != null && ffs.getDateFin().isBeforeOrEqual(ffs.getDateDebut())) {
								ffs.setDateFin(firstFfp.getDateDebut());
							}
							break;
						}
					}
				}
				//Si le for secondaire est en cours et qu'aucun fors principaux ne sont ouverts, on termine le for secondaire à la
				//date de fermeture du dernier for principal.
				ForFiscalPrincipal lastFfp = ctb.getDernierForFiscalPrincipal();
				if (lastFfp != null && lastFfp.getDateFin() != null) {
					for (ForFiscalSecondaire ffs : listFfs) {
						if (!Tiers.existForPrincipal(listFfp, ffs.getDateDebut(), ffs.getDateFin())) {
							if (ffs.getDateFin() == null ||
								(ffs.getDateFin() != null && ffs.getDateFin().isAfter(lastFfp.getDateFin()))) {
								ffs.setDateFin(lastFfp.getDateFin());
								if (ffs.getMotifFermeture() == null) {
									if (MotifFor.ACHAT_IMMOBILIER.equals(ffs.getMotifOuverture())) {
										ffs.setMotifFermeture(MotifFor.VENTE_IMMOBILIER);
									}
									else {
										ffs.setMotifFermeture(MotifFor.FIN_EXPLOITATION);
									}
								}
								if (ffs.getDateDebut().isAfterOrEqual(lastFfp.getDateFin())) {
									ffs.setDateDebut(lastFfp.getDateFin());
								}
							}
						}
					}
				}
				for (ForFiscalSecondaire ffs : listFfs) {
					if (!Tiers.existForPrincipal(listFfp, ffs.getDateDebut(), ffs.getDateFin())) {
						ForFiscalPrincipal precedentFfp = null;
						for (ForFiscalPrincipal ffp : forsFiscauxPrincipaux) {
							if(precedentFfp != null &&
								precedentFfp.getDateFin() != null &&
								precedentFfp.getDateFin().isBefore(ffp.getDateDebut().getOneDayBefore()) &&
								precedentFfp.getDateDebut().isBefore(ffp.getDateDebut().getOneDayBefore()) &&
								!MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(precedentFfp.getMotifFermeture()) &&
								!MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT.equals(precedentFfp.getMotifFermeture())) {
								String message = "La date de fin du for fiscal principal ["+precedentFfp.getDateDebut()+"/"+precedentFfp.getDateFin()+"] est modifiée.";
								precedentFfp.setDateFin(ffp.getDateDebut().getOneDayBefore());
								Audit.warn(message);
							}
							precedentFfp = ffp;
						}
					}
				}

				//Enlever un jour à la date de fin d'un for fiscal d'un membre d'un couple si cette date de fin débute
				//le même jour que la date début du premier for du couple (éviter chevauchement dans Unireg).
				if (ctb instanceof MenageCommun) {

						final MenageCommun mc = (MenageCommun) ctb;
						PersonnePhysique principal = null;
						PersonnePhysique conjoint = null;

						// Récupère les composants du ménage

						final Set<PersonnePhysique> personnes = getComposantsMenage(mc);
						for (PersonnePhysique pp : personnes) {
							if (principal == null) {
								principal = pp;
							}
							else if (conjoint == null) {
								conjoint = pp;
							}
							else {
								Assert.fail("Trouvé >2 personnes physiques distinctes sur le ménage n°" + mc.getNumero());
							}
						}

						// Ajuste les dates de fors si nécessaire

						for (ForFiscal forCouple : mc.getForsFiscauxNonAnnules(false)) {
							// attention ! ces deux dates doivent être initialisées avant le premier appel à ajusteDateFinForsPersonneEtCouple()
							final RegDate dateDebutCouple = forCouple.getDateDebut();
							final RegDate dateFinCouple = forCouple.getDateFin();
							if (principal != null) {
								ajusteDateFinForsPersonneEtCouple(mc, principal, forCouple, dateDebutCouple, dateFinCouple);
							}
							if (conjoint != null) {
								ajusteDateFinForsPersonneEtCouple(mc, conjoint, forCouple, dateDebutCouple, dateFinCouple);
							}
						}

						// Valide le résultat

						ValidationResults results = new ValidationResults();
						if (principal != null) {
							results.merge(principal.validate());
						}
						if (conjoint != null) {
							results.merge(conjoint.validate());
						}
						if (results.hasErrors()) {

							// Problème de fors sur les ménage -> le ménage commun sera migré sous la forme dégradée d'un non-habitant

							LOGGER.debug("MenageCommun n°" + mc.getNumero() + " à reprendre en PP. Erreurs : "
								+ ArrayUtils.toString(results.getErrors().toArray()));
							MigrationError me = new MigrationError();
							me.setMessage("MenageCommun à reprendre en PP");
							me.setNoContribuable(mc.getNumero());
							me.setTypeErreur(TypeMigRegError.A_TRANSFORMER_EN_PP);
							errors.add(me);

						}
					}

				break;
			}
		}

		return errors;
	}

	/**
	 * Enlever un jour à la date de fin d'un for fiscal d'un membre d'un couple si cette date de fin débute le même jour que la date début
	 * du premier for du couple (permet d'éviter des chevauchement de fors).
	 *
	 * @param mc
	 *            le ménage commun considéré
	 * @param personne
	 *            la personne appartenant au ménage considéré
	 * @param forCouple
	 *            le for fiscal (du couple) considéré
	 * @param dateDebutCouple
	 *            la date de début initiale (avant ajustement) du for fiscal considéré
	 * @param dateFinCouple
	 *            la date de fin initiale (avant ajustement) du for fiscal considéré
	 */
	private void ajusteDateFinForsPersonneEtCouple(final MenageCommun mc, PersonnePhysique personne, ForFiscal forCouple, RegDate dateDebutCouple, RegDate dateFinCouple) {
		for (ForFiscal f : personne.getForsFiscauxNonAnnules(false)) {
			// ajuste la date de fin du for de la personne physique, si nécessaire
			if (f.getDateFin() != null && f.getDateFin().equals(dateDebutCouple)) {
				if (f.getDateDebut().isBeforeOrEqual(dateDebutCouple.getOneDayBefore())) {
					f.setDateFin(dateDebutCouple.getOneDayBefore());
				}
			}
			// ajuste la date de fin du for du ménage (et le rapport-entre-tiers associé), si nécessaire
			if (dateFinCouple != null && f.getDateDebut() != null && f.getDateDebut().equals(dateFinCouple)) {
				if (forCouple.getDateDebut().isBeforeOrEqual(dateFinCouple.getOneDayBefore())) {
					forCouple.setDateFin(dateFinCouple.getOneDayBefore());
					RapportEntreTiers appartenance = personne.getRapportSujetValidAt(dateFinCouple,
							TypeRapportEntreTiers.APPARTENANCE_MENAGE);
					if (appartenance != null) {
						appartenance.setDateFin(dateFinCouple.getOneDayBefore());
					}
				}
			}
		}
	}

	private Set<PersonnePhysique> getComposantsMenage(final MenageCommun mc) {

		// on évite de passer par le tiers service car cela implique un appel à host-interface
		//ensemble = helper.tiersService.getEnsembleTiersCouple(mc, null);

		final Set<PersonnePhysique> personnes = TiersHelper.getComposantsMenage(mc, null);
		if (personnes == null) {
			return Collections.emptySet();
		}
		else {
			return personnes;
		}
	}

//	private void removeRapportAppartenance(final MenageCommun mc, PersonnePhysique ppPrinc) {
//		List<RapportEntreTiers> listRap = new ArrayList<RapportEntreTiers>();
//		for (RapportEntreTiers r : ppPrinc.getRapportsSujet()) {
//			if (r instanceof AppartenanceMenage) {
//				if (r.getObjet().equals(mc)) {
//					listRap.add(r);
//				}
//			}
//		}
//		for (RapportEntreTiers rap : listRap) {
//			ppPrinc.getRapportsSujet().remove(rap);
//			helper.rapportEntreTiersDAO.remove(rap.getId());
//		}
//	}

	@SuppressWarnings("deprecation")
	private MotifFor getMotifOuverture(int motifOuverture) {
		switch (motifOuverture) {
		case 4:
			return MotifFor.ACHAT_IMMOBILIER;
		case 6:
			return MotifFor.DEBUT_EXPLOITATION;
		case 7:
			return MotifFor.INDETERMINE;
		case 10:
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		case 11:
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		case 12:
			return MotifFor.VEUVAGE_DECES;
		case 13:
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		case 20:
		case 24:
			return MotifFor.FUSION_COMMUNES;
		case 992:
			return MotifFor.ARRIVEE_HC;
		case 993:
			return MotifFor.ARRIVEE_HC;
		case 995:
			return MotifFor.ARRIVEE_HS;
		default:
			if (motifOuverture >= 51 && motifOuverture <= 989) {
				return MotifFor.DEMENAGEMENT_VD;
			}
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	private MotifFor getMotifFermeture(int motifFermeture) {
		switch (motifFermeture) {
		case 1:
			return MotifFor.INDETERMINE;
		case 2:
			return MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		case 3:
			return MotifFor.VENTE_IMMOBILIER;
		case 5:
			return MotifFor.FIN_EXPLOITATION;
		case 8:
			return MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION;
		case 9:
			return MotifFor.VEUVAGE_DECES;
		case 14:
			return MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT;
		case 20:
		case 23:
			return MotifFor.FUSION_COMMUNES;
		case 992:
			return MotifFor.DEPART_HC;
		case 995:
		case 999:
			return MotifFor.DEPART_HS;
		default:
			if (motifFermeture >= 51 && motifFermeture <= 989) {
				return MotifFor.DEMENAGEMENT_VD;
			}
			return null;
		}
	}

	private SqlRowSet readAllForFiscauxListContribuable(ArrayList<Tiers> listCtb) throws Exception {
		StringBuilder sbCtb = new StringBuilder();
		for (Tiers tiers : listCtb) {
			sbCtb.append(tiers.getNumero());
			sbCtb.append(",");
		}
		if (sbCtb.length() == 0) {
			return null;
		}
		sbCtb.deleteCharAt(sbCtb.lastIndexOf(","));
		String query =
			"Select 'Principal' as TYPE_FOR, " +
			" A.DAD_VALIDITE, " +
			" A.DAF_VALIDITE, " +
			" A.MOTIF_ENTREE," +
			" A.MOTIF_SORTIE," +
			" '' as FOR_GESTION," +
			" A.DAA_FOR_PRINC_CTB," +
			" A.FK_COMMUNENO," +
			" A.FK_PAYSNO_OFS," +
			" A.FK_CONTRIBUABLENO," +
			" A.DA_MUT," +
			" A.VS_MUT," +
			" B.INDIGENT," +
			" B.IMPOT_DEPENSE_VD," +
			" B.SOURCIER_MIXTE," +
			" C.FK_CANTONSIGLE" +
			" FROM "+helper.getTableDb2("FOR_PRINCIPAL_CONT")+" A" +
			" LEFT JOIN "+helper.getTableDb2("COMMUNE")+" C " +
				"ON A.FK_COMMUNENO=C.NO_TECHNIQUE, "+
			helper.getTableDb2("CONTRIBUABLE")+" B" +
			" WHERE A.FK_CONTRIBUABLENO = B.NO_CONTRIBUABLE" +
			" AND (A.DAD_VALIDITE <> '1800-01-01' AND A.DAF_VALIDITE <> '1800-01-01')" +
			" AND B.NO_CONTRIBUABLE in (" +sbCtb.toString()+")"+
			" UNION " +
			"Select 'Secondaire' as TYPE_FOR," +
			" A.DAD_VALIDITE," +
			" A.DAF_VALIDITE," +
			" A.MOTIF_ENTREE," +
			" A.MOTIF_SORTIE," +
			" A.FOR_GESTION," +
			" '0001-01-01' as DAA_FOR_PRINC_CTB," +
			" A.FK_COMMUNENO," +
			" 0 AS FK_PAYSNO_OFS," +
			" A.FK_CONTRIBUABLENO," +
			" A.DA_MUT," +
			" A.VS_MUT," +
			" B.INDIGENT," +
			" B.IMPOT_DEPENSE_VD," +
			" B.SOURCIER_MIXTE," +
			" C.FK_CANTONSIGLE" +
			" From "+helper.getTableDb2("FOR_SPECIAL_IND")+" A" +
				" LEFT JOIN "+helper.getTableDb2("COMMUNE")+" C " +
				"ON A.FK_COMMUNENO=C.NO_TECHNIQUE, "+
			helper.getTableDb2("CONTRIBUABLE")+" B" +
			" WHERE A.FK_CONTRIBUABLENO = B.NO_CONTRIBUABLE" +
			" AND (A.DAD_VALIDITE <> '1800-01-01' AND A.DAF_VALIDITE <> '1800-01-01')" +
			" AND B.NO_CONTRIBUABLE  in (" +sbCtb.toString()+")"+
			" order by FK_CONTRIBUABLENO, TYPE_FOR, DAD_VALIDITE";
		HostMigratorHelper.SQL_LOG.debug("Query: "+query);

		return helper.db2Template.queryForRowSet(query);
	}

}
