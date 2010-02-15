package ch.vd.uniregctb.migreg;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class TutellesLoader extends HostMigrator  {

	private static final Logger LOGGER = Logger.getLogger(TutellesLoader.class);

	private static final String PUPILLE_NO_INDIVIDU = "FK_INDNO_IMPOSE";
	private static final String TUTEUR_NO_INDIVIDU = "FK_INDNO_CONFIE";
	private static final String TUTEUR_NO_OTG = "FK_COLADMNO_CONFIE";
	private static final String COLADM_IMPOSE = "FK_COLADMNO_IMPOSE";

	private static final String TUTELLE_TYPE = "CO_MANDAT";
	private static final String DATE_DEBUT = "DA_ATTRIBUTION";
	private static final String DATE_FIN = "DA_RESILIATION";
	private static final String DATE_ANNULATION = "DA_ANNULATION";
	private static final String DATE_MUTATION = "DA_MUT";
	private static final String VISA_MUTATION = "VS_MUT";

	public TutellesLoader(HostMigratorHelper helper, MigregStatusManager mgr) {
		super(helper, mgr);
	}

	@Override
	public int migrate() throws Exception {

		int nb = 0;

		setRunningMessage("Chargement des tutelles...");

		// Chargement des tutelles
		beginTransaction();

		nb = loadTutelles();

		endTransaction();
		return nb;
	}

	private int loadTutelles() {

		LOGGER.info("Debut du traitement des tutelles");

		SqlRowSet srs = readAllTutelles();
		int nbTutelleSave = 0;
		while (srs != null && srs.next() && !isInterrupted()) {

			Long noIndividuPupille = srs.getLong(PUPILLE_NO_INDIVIDU);
			Long noIndividuTuteur = srs.getLong(TUTEUR_NO_INDIVIDU);

			try {
				List<RapportEntreTiers> listRap = helper.rapportEntreTiersDAO.getRepresentationLegaleAvecTuteurEtPupille(noIndividuTuteur, noIndividuPupille, true);
				boolean isRapportDejaExistant = false;
				final TypeRapportEntreTiers typeRapportTutellaire = getTypeRapportTutellaire(srs.getString(TUTELLE_TYPE));
				for (RapportEntreTiers rep : listRap) {
					if (rep.getDateDebut().compareTo(RegDate.get(srs.getDate(DATE_DEBUT))) == 0
							&& rep.getType().compareTo(typeRapportTutellaire) == 0) {
						isRapportDejaExistant = true;
					}
				}
				if (isRapportDejaExistant) {
					continue;
				}
				//Le tiers pupille doit exister
				PersonnePhysique pupille = helper.tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuPupille);
				if (pupille == null) {
	//				Audit.error("Le tiers pupille avec le numéro d'habitant "+srs.getLong(PUPILLE_NO_INDIVIDU)+" n'est pas trouvé dans la base");
					continue;
				}

	//			Assert.notNull(pupille,"Le tiers pupille avec le numéro d'habitant "+srs.getLong(PUPILLE_NO_INDIVIDU)+" n'est pas trouvé dans la base");
				//Le tuteur est un habitant ou L'office du tuteur général.
				Tiers tuteur = null;
				if (srs.getLong(TUTEUR_NO_INDIVIDU) > 0) {
					//Le tiers tuteur doit exister.
					tuteur = helper.tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividuTuteur);
					if (tuteur == null) {
						Audit.error("Le tiers tuteur avec le numéro d'habitant "+srs.getLong(TUTEUR_NO_INDIVIDU)+" n'est pas trouvé dans la base");
						continue;
					}
	//				Assert.notNull(tuteur, "Le tiers tuteur avec le numéro d'habitant "+srs.getLong(TUTEUR_NO_INDIVIDU)+" n'est pas trouvé dans la base");
				}
				else if (srs.getInt(TUTEUR_NO_OTG) > 0){

					try {
						tuteur = helper.tiersService.getCollectiviteAdministrative(srs.getInt(TUTEUR_NO_OTG));
					}
					catch (Exception e) {
						Audit.error("Problème à la lecture de l'office du tuteur général");
						continue;
					}
				}
				else {
					Audit.error("Pas de tuteur pour pupille : "+pupille.getNumero());
					continue;
				}

				final RegDate dateDebut = RegDate.get(srs.getDate(DATE_DEBUT));
				RegDate dateFin = null;
				if (!DateHelper.isNullDate(srs.getDate(DATE_FIN))) {
					dateFin = RegDate.get(srs.getDate(DATE_FIN));
				}

				final RepresentationLegale tutelle = (RepresentationLegale) typeRapportTutellaire.newInstance();
				tutelle.setDateDebut(dateDebut);
				tutelle.setDateFin(dateFin);
				tutelle.setSujet(pupille);
				tutelle.setObjet(tuteur);
				tutelle.setAnnule(false);
				if (RegDate.get(srs.getDate(DATE_ANNULATION)) != null) {
					tutelle.setAnnule(true);
				}
				tutelle.setLogCreationDate(srs.getDate(DATE_MUTATION));
				tutelle.setLogModifMillis(srs.getDate(DATE_MUTATION).getTime());
				tutelle.setLogCreationUser(srs.getString(VISA_MUTATION).trim());
				tutelle.setLogModifUser(srs.getString(VISA_MUTATION).trim());
				if (srs.getLong(COLADM_IMPOSE) > 0) {
					try {
					tutelle.setAutoriteTutelaire(helper.tiersService.getCollectiviteAdministrative(srs.getInt(COLADM_IMPOSE)));
					} catch (Exception e) {
						Audit.error("Problème à la lecture de la représentation légale "+srs.getLong(COLADM_IMPOSE));
						continue;
					}
				}

				helper.tiersDAO.save(tutelle);
				nbTutelleSave++;

				doCommit();
			}
			catch (Exception e) {
				LOGGER.error(e,e);
				Audit.error("Impossible de sauver le rapport entre tiers de TUTELLE pour "+noIndividuPupille+" / "+noIndividuTuteur);
				//TODO(GDY) : Migreg Error
			}
		}
		LOGGER.info("Fin du traitement des tutelles. Tutelles: "+nbTutelleSave);

		return nbTutelleSave;
	}

	private TypeRapportEntreTiers getTypeRapportTutellaire(String typeTutelle) {

		Assert.notNull(typeTutelle,"Le type de tutelle doit être renseigné");

		if (typeTutelle.equals("T")) {
			return TypeRapportEntreTiers.TUTELLE;
		}
		if (typeTutelle.equals("C")) {
			return TypeRapportEntreTiers.CURATELLE;
		}
		if (typeTutelle.equals("L")) {
			return TypeRapportEntreTiers.CONSEIL_LEGAL;
		}
		return null;

	}

	private SqlRowSet readAllTutelles() {
		LOGGER.info("Lecture des mandats tutelle");
		String query =
			"SELECT " +
			"A.NO_SEQUENCE " +
			",A.CO_MANDAT " +
			",A.DA_ATTRIBUTION " +
			",A.DA_RESILIATION " +
			",A.DA_ANNULATION " +
			",A.MOTIF " +
			",A.NOM_CONTACT " +
			",A.PRENOM_CONTACT " +
			",A.NO_TEL_CONTACT " +
			",A.NO_FAX_CONTACT " +
			",A.DA_MUT " +
			",A.HR_MUT " +
			",A.VS_MUT " +
			",A.NO_MAJ " +
			",A.FK_INDNO_IMPOSE " +
			",A.FK_INDNO " +
			",A.FK_ADR_INDNO " +
			",A.FK_INDNO_CONFIE " +
			",A.FK_COLADMNO_CONFIE " +
			",A.FK_COLADMNO_IMPOSE " +
		"FROM "+helper.getTableDb2("MANDAT_TUTELLE")+" A " +
		"ORDER BY A.FK_INDNO_IMPOSE ";

		HostMigratorHelper.SQL_LOG.debug("Query: "+query);
		return helper.db2Template.queryForRowSet(query);
	}

}
