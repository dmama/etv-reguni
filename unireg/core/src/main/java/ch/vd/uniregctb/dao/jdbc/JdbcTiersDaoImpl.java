package ch.vd.uniregctb.dao.jdbc;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.DroitAcces;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Etablissement;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.IdentificationPersonne;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;

public class JdbcTiersDaoImpl implements JdbcTiersDao {

	private static final TiersMapper ROW_MAPPER = new TiersMapper();

	private JdbcAdresseTiersDao adresseDao = new JdbcAdresseTiersDaoImpl();
	private JdbcDeclarationDao declarationDao = new JdbcDeclarationDaoImpl();
	private JdbcForFiscalDao forFiscalDao = new JdbcForFiscalDaoImpl();
	private JdbcRapportEntreTiersDao retDao = new JdbcRapportEntreTiersDaoImpl();
	private JdbcSituationFamilleDao sfDao = new JdbcSituationFamilleDaoImpl();
	private JdbcIdentificationPersonneDao ipDao = new JdbcIdentificationPersonneDaoImpl();
	private JdbcPeriodiciteDao pDao = new JdbcPeriodiciteDaoImpl();

	private DataSource dataSource;

	@Override
	public Tiers get(long tiersId, Set<TiersDAO.Parts> parts) {

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		final Tiers tiers = get(tiersId, template);
		if (tiers == null) {
			return null;
		}

		if (parts != null && parts.contains(TiersDAO.Parts.ADRESSES)) {
			Set<AdresseTiers> adresses = adresseDao.getForTiers(tiersId, template);
			for (AdresseTiers a : adresses) {
				a.setTiers(tiers);
			}
			tiers.setAdressesTiers(adresses);
		}

		if (parts != null && parts.contains(TiersDAO.Parts.DECLARATIONS)) {
			Set<Declaration> dis = declarationDao.getForTiers(tiersId, true, template);
			for (Declaration d : dis) {
				d.setTiers(tiers);
			}
			tiers.setDeclarations(dis);
		}

		if (parts != null && parts.contains(TiersDAO.Parts.FORS_FISCAUX)) {
			Set<ForFiscal> fors = forFiscalDao.getForTiers(tiersId, template);
			for (ForFiscal f : fors) {
				f.setTiers(tiers);
			}
			tiers.setForsFiscaux(fors);
		}

		if (parts != null && parts.contains(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS)) {
			Set<RapportEntreTiers> rapportsObjets = retDao.getForTiersObjet(tiersId, template);
			tiers.setRapportsObjet(rapportsObjets);
			Set<RapportEntreTiers> rapportsSujets = retDao.getForTiersSujet(tiersId, template);
			tiers.setRapportsSujet(rapportsSujets);
		}

		if (parts != null && parts.contains(TiersDAO.Parts.SITUATIONS_FAMILLE)) {
			if (tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				Set<SituationFamille> situations = sfDao.getForTiers(tiersId, template);
				for (SituationFamille s : situations) {
					s.setContribuable(contribuable);
				}
				contribuable.setSituationsFamille(situations);
			}
		}

		{ // on veut toujours les identifications de personnes
			if (tiers instanceof PersonnePhysique) {
				PersonnePhysique pp = (PersonnePhysique) tiers;
				Set<IdentificationPersonne> idents = ipDao.getForTiers(tiersId, template);
				for (IdentificationPersonne i : idents) {
					i.setPersonnePhysique(pp);
				}
				pp.setIdentificationsPersonnes(idents);
			}
		}

		{ // on veut toujours les périodicités des débiteurs
			if (tiers instanceof DebiteurPrestationImposable) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				Set<Periodicite> periodicites = pDao.getForTiers(tiersId, template);
				for (Periodicite p : periodicites) {
					p.setDebiteur(dpi);
				}
				dpi.setPeriodicites(periodicites);
			}
		}

		if (tiers instanceof Contribuable) {
			((Contribuable) tiers).setMouvementsDossier(Collections.<MouvementDossier>emptySet()); // TODO (msi) voir s'il faut renseigner cette collection
			if (tiers instanceof PersonnePhysique) {
				((PersonnePhysique) tiers).setDroitsAccesAppliques(Collections.<DroitAcces>emptySet());
			}
		}

		return tiers;
	}

	@Override
	public List<Tiers> getBatch(Collection<Long> ids, Set<TiersDAO.Parts> parts) {

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		final List<Tiers> list = getList(ids, template);

		if (parts != null && parts.contains(TiersDAO.Parts.ADRESSES)) {
			Map<Long, Set<AdresseTiers>> map = adresseDao.getForTiers(ids, template);
			for (Tiers t : list) {
				Set<AdresseTiers> adresses = map.get(t.getId());
				if (adresses == null) {
					adresses = Collections.emptySet();
				}
				else {
					for (AdresseTiers a : adresses) {
						a.setTiers(t);
					}
				}
				t.setAdressesTiers(adresses);
			}
		}

		if (parts != null && parts.contains(TiersDAO.Parts.DECLARATIONS)) {
			Map<Long, Set<Declaration>> map = declarationDao.getForTiers(ids, true, template);
			for (Tiers t : list) {
				Set<Declaration> declarations = map.get(t.getId());
				if (declarations == null) {
					declarations = Collections.emptySet();
				}
				else {
					for (Declaration d : declarations) {
						d.setTiers(t);
					}
				}
				t.setDeclarations(declarations);
			}
		}

		if (parts != null && parts.contains(TiersDAO.Parts.FORS_FISCAUX)) {
			Map<Long, Set<ForFiscal>> map = forFiscalDao.getForTiers(ids, template);
			for (Tiers t : list) {
				Set<ForFiscal> fors = map.get(t.getId());
				if (fors == null) {
					fors = Collections.emptySet();
				}
				else {
					for (ForFiscal f : fors) {
						f.setTiers(t);
					}
				}
				t.setForsFiscaux(fors);
			}
		}

		if (parts != null && parts.contains(TiersDAO.Parts.RAPPORTS_ENTRE_TIERS)) {
			Map<Long, Set<RapportEntreTiers>> map = retDao.getForTiersObjet(ids, template);
			for (Tiers t : list) {
				Set<RapportEntreTiers> rapports = map.get(t.getId());
				if (rapports == null) {
					rapports = Collections.emptySet();
				}
				t.setRapportsObjet(rapports);
			}
			map = retDao.getForTiersSujet(ids, template);
			for (Tiers t : list) {
				Set<RapportEntreTiers> rapports = map.get(t.getId());
				if (rapports == null) {
					rapports = Collections.emptySet();
				}
				t.setRapportsSujet(rapports);
			}
		}

		if (parts != null && parts.contains(TiersDAO.Parts.SITUATIONS_FAMILLE)) {
			Map<Long, Set<SituationFamille>> map = sfDao.getForTiers(ids, template);
			for (Tiers t : list) {
				if (t instanceof Contribuable) {
					Contribuable contribuable = (Contribuable) t;
					Set<SituationFamille> situations = map.get(t.getId());
					if (situations == null) {
						situations = Collections.emptySet();
					}
					else {
						for (SituationFamille s : situations) {
							s.setContribuable(contribuable);
						}
					}
					contribuable.setSituationsFamille(situations);
				}
			}
		}

		{ // on veut toujours les identifications de personnes
			Map<Long, Set<IdentificationPersonne>> map = ipDao.getForTiers(ids, template);
			for (Tiers t : list) {
				if (t instanceof PersonnePhysique) {
					PersonnePhysique pp = (PersonnePhysique) t;
					Set<IdentificationPersonne> identifications = map.get(t.getId());
					if (identifications == null) {
						identifications = Collections.emptySet();
					}
					else {
						for (IdentificationPersonne i : identifications) {
							i.setPersonnePhysique(pp);
						}
					}
					pp.setIdentificationsPersonnes(identifications);
				}
			}
		}

		{ // on veut toujours les périodicités des débiteurs
			Map<Long, Set<Periodicite>> map = pDao.getForTiers(ids, template);
			for (Tiers t : list) {
				if (t instanceof DebiteurPrestationImposable) {
					DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) t;
					Set<Periodicite> periodicites = map.get(t.getId());
					if (periodicites == null) {
						periodicites = Collections.emptySet();
					}
					else {
						for (Periodicite p : periodicites) {
							p.setDebiteur(dpi);
						}
					}
					dpi.setPeriodicites(periodicites);
				}
			}
		}

		for (Tiers tiers : list) {
			if (tiers instanceof Contribuable) {
				((Contribuable) tiers).setMouvementsDossier(Collections.<MouvementDossier>emptySet()); // TODO (msi) voir s'il faut renseigner cette collection
				if (tiers instanceof PersonnePhysique) {
					((PersonnePhysique) tiers).setDroitsAccesAppliques(Collections.<DroitAcces>emptySet());
				}
			}
		}

		return list;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Tiers get(long tiersId, JdbcTemplate template) {
		return (Tiers) DataAccessUtils.uniqueResult(template.query(TiersMapper.selectById(), new Object[]{tiersId}, ROW_MAPPER));
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Tiers> getList(final Collection<Long> tiersId, final JdbcTemplate template) {
		// Découpe la requête en sous-requêtes si nécessaire
		return CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Tiers>() {
			@Override
			public List<Tiers> process(List<Long> ids) {
				return template.query(TiersMapper.selectByIds(ids), ROW_MAPPER);
			}
		});
	}

	private static class TiersMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"TIERS_TYPE, " + // 1
				"NUMERO, " + // 2
				"AC_FORME_JURIDIQUE, " + // 3
				"AC_NOM, " + // 4
				"ADRESSE_BIC_SWIFT, " + // 5
				"ADRESSE_EMAIL, " + // 6
				"ANCIEN_NUMERO_SOURCIER, " + // 7
				"ANNULATION_DATE, " + // 8
				"ANNULATION_USER, " + // 9
				"BLOC_REMB_AUTO, " + // 10
				"CATEGORIE_IMPOT_SOURCE, " + // 11
				"COMPLEMENT_NOM, " + // 12
				"DATE_DECES, " + // 13
				"DATE_LIMITE_EXCLUSION, " + // 14
				"DEBITEUR_INACTIF, " + // 15
				"DISTRICT_FISCAL_ID, " + // 16
				"DPI_NOM1, " + // 17
				"DPI_NOM2, " + // 18
				"INDEX_DIRTY, " + // 19
				"LOGICIEL_ID, " + // 20
				"LOG_CDATE, " + // 21
				"LOG_CUSER, " + // 22
				"LOG_MDATE, " + // 23
				"LOG_MUSER, " + // 24
				"MAJORITE_TRAITEE, " + // 25
				"MODE_COM, " + // 26
				"NH_CAT_ETRANGER, " + // 27
				"NH_DATE_DEBUT_VALID_AUTORIS, " + // 28
				"NH_DATE_NAISSANCE, " + // 29
				"NH_NOM, " + // 30
				"NH_NO_OFS_COMMUNE_ORIGINE, " + // 31
				"NH_NO_OFS_NATIONALITE, " + // 32
				"NH_NUMERO_ASSURE_SOCIAL, " + // 33
				"NH_PRENOM, " + // 34
				"NH_SEXE, " + // 35
				"NUMERO_CA, " + // 36
				"NUMERO_COMPTE_BANCAIRE, " + // 37
				"NUMERO_ETABLISSEMENT, " + // 38
				"NUMERO_INDIVIDU, " + // 39
				"NUMERO_TELECOPIE, " + // 40
				"NUMERO_TEL_PORTABLE, " + // 41
				"NUMERO_TEL_PRIVE, " + // 42
				"NUMERO_TEL_PROF, " + // 43
				"OID, " + // 44
				"PERIODE_DECOMPTE, " + // 45
				"PERIODICITE_DECOMPTE, " + // 46
				"PERSONNE_CONTACT, " + // 47
				"PP_HABITANT, " + // 48
				"REGION_FISCALE_ID, " + // 49
				"REINDEX_ON, " + // 50
				"SANS_LISTE_RECAP, " + // 51
				"SANS_RAPPEL, " + // 52
				"TITULAIRE_COMPTE_BANCAIRE " + // 53
				"from TIERS";

		public static String selectById() {
			return BASE_SELECT + " where NUMERO = ?";
		}

		public static String selectByIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where NUMERO in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String tiersType = rs.getString(1);
			final Tiers res;
			
			if (tiersType.equals("PersonnePhysique")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final long temp7 = rs.getLong(7);
				final Long ancienNumeroSourcier = (rs.wasNull() ? null : temp7);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp13 = rs.getInt(13);
				final RegDate dateDeces = (rs.wasNull() ? null : RegDate.fromIndex(temp13, false));
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final boolean temp25 = rs.getBoolean(25);
				final Boolean majoriteTraitee = (rs.wasNull() ? null : temp25);
				final String temp27 = rs.getString(27);
				final CategorieEtranger nhCatEtranger = (rs.wasNull() ? null : Enum.valueOf(CategorieEtranger.class, temp27));
				final int temp28 = rs.getInt(28);
				final RegDate nhDateDebutValidAutoris = (rs.wasNull() ? null : RegDate.fromIndex(temp28, false));
				final int temp29 = rs.getInt(29);
				final RegDate nhDateNaissance = (rs.wasNull() ? null : RegDate.fromIndex(temp29, false));
				final String nhNom = rs.getString(30);
				final int temp31 = rs.getInt(31);
				final Integer nhNoOfsCommuneOrigine = (rs.wasNull() ? null : temp31);
				final int temp32 = rs.getInt(32);
				final Integer nhNoOfsNationalite = (rs.wasNull() ? null : temp32);
				final String nhNumeroAssureSocial = rs.getString(33);
				final String nhPrenom = rs.getString(34);
				final String temp35 = rs.getString(35);
				final Sexe nhSexe = (rs.wasNull() ? null : Enum.valueOf(Sexe.class, temp35));
				final String numeroCompteBancaire = rs.getString(37);
				final long temp39 = rs.getLong(39);
				final Long numeroIndividu = (rs.wasNull() ? null : temp39);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final boolean temp48 = rs.getBoolean(48);
				final Boolean ppHabitant = (rs.wasNull() ? null : temp48);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				PersonnePhysique o = new PersonnePhysique();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAncienNumeroSourcier(ancienNumeroSourcier);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateDeces(dateDeces);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setMajoriteTraitee(majoriteTraitee);
				o.setCategorieEtranger(nhCatEtranger);
				o.setDateDebutValiditeAutorisation(nhDateDebutValidAutoris);
				o.setDateNaissance(nhDateNaissance);
				o.setNom(nhNom);
				o.setNumeroOfsCommuneOrigine(nhNoOfsCommuneOrigine);
				o.setNumeroOfsNationalite(nhNoOfsNationalite);
				o.setNumeroAssureSocial(nhNumeroAssureSocial);
				o.setPrenom(nhPrenom);
				o.setSexe(nhSexe);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroIndividu(numeroIndividu);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setHabitant(ppHabitant);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("AutreCommunaute")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String temp3 = rs.getString(3);
				final FormeJuridique acFormeJuridique = (rs.wasNull() ? null : Enum.valueOf(FormeJuridique.class, temp3));
				final String acNom = rs.getString(4);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final String numeroCompteBancaire = rs.getString(37);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				AutreCommunaute o = new AutreCommunaute();
				o.setNumero(numero);
				o.setFormeJuridique(acFormeJuridique);
				o.setNom(acNom);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("Entreprise")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final String numeroCompteBancaire = rs.getString(37);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				Entreprise o = new Entreprise();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("MenageCommun")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final String numeroCompteBancaire = rs.getString(37);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				MenageCommun o = new MenageCommun();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("CollectiviteAdministrative")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final int temp16 = rs.getInt(16);
				final Integer districtFiscalId = (rs.wasNull() ? null : temp16);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final int temp36 = rs.getInt(36);
				final Integer numeroCa = (rs.wasNull() ? null : temp36);
				final String numeroCompteBancaire = rs.getString(37);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final int temp49 = rs.getInt(49);
				final Integer regionFiscaleId = (rs.wasNull() ? null : temp49);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				CollectiviteAdministrative o = new CollectiviteAdministrative();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIdentifiantDistrictFiscal(districtFiscalId);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroCollectiviteAdministrative(numeroCa);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setIdentifiantRegionFiscale(regionFiscaleId);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("Etablissement")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String complementNom = rs.getString(12);
				final int temp14 = rs.getInt(14);
				final RegDate dateLimiteExclusion = (rs.wasNull() ? null : RegDate.fromIndex(temp14, false));
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final String numeroCompteBancaire = rs.getString(37);
				final long temp38 = rs.getLong(38);
				final Long numeroEtablissement = (rs.wasNull() ? null : temp38);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String personneContact = rs.getString(47);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final String titulaireCompteBancaire = rs.getString(53);
			
				Etablissement o = new Etablissement();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setComplementNom(complementNom);
				o.setDateLimiteExclusionEnvoiDeclarationImpot(dateLimiteExclusion);
				o.setDebiteurInactif(debiteurInactif);
				o.setIndexDirty(indexDirty);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroEtablissement(numeroEtablissement);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPersonneContact(personneContact);
				o.setReindexOn(reindexOn);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else if (tiersType.equals("DebiteurPrestationImposable")) {
			
				final long temp2 = rs.getLong(2);
				final Long numero = (rs.wasNull() ? null : temp2);
				final String adresseBicSwift = rs.getString(5);
				final String adresseEmail = rs.getString(6);
				final Date annulationDate = rs.getTimestamp(8);
				final String annulationUser = rs.getString(9);
				final boolean temp10 = rs.getBoolean(10);
				final Boolean blocRembAuto = (rs.wasNull() ? null : temp10);
				final String temp11 = rs.getString(11);
				final CategorieImpotSource categorieImpotSource = (rs.wasNull() ? null : Enum.valueOf(CategorieImpotSource.class, temp11));
				final String complementNom = rs.getString(12);
				final boolean temp15 = rs.getBoolean(15);
				final Boolean debiteurInactif = (rs.wasNull() ? null : temp15);
				final String dpiNom1 = rs.getString(17);
				final String dpiNom2 = rs.getString(18);
				final boolean temp19 = rs.getBoolean(19);
				final Boolean indexDirty = (rs.wasNull() ? null : temp19);
				final long temp20 = rs.getLong(20);
				final Long logicielId = (rs.wasNull() ? null : temp20);
				final Date logCdate = rs.getTimestamp(21);
				final String logCuser = rs.getString(22);
				final Timestamp logMdate = rs.getTimestamp(23);
				final String logMuser = rs.getString(24);
				final String temp26 = rs.getString(26);
				final ModeCommunication modeCom = (rs.wasNull() ? null : Enum.valueOf(ModeCommunication.class, temp26));
				final String numeroCompteBancaire = rs.getString(37);
				final String numeroTelecopie = rs.getString(40);
				final String numeroTelPortable = rs.getString(41);
				final String numeroTelPrive = rs.getString(42);
				final String numeroTelProf = rs.getString(43);
				final int temp44 = rs.getInt(44);
				final Integer oid = (rs.wasNull() ? null : temp44);
				final String temp45 = rs.getString(45);
				final PeriodeDecompte periodeDecompte = (rs.wasNull() ? null : Enum.valueOf(PeriodeDecompte.class, temp45));
				final String temp46 = rs.getString(46);
				final PeriodiciteDecompte periodiciteDecompte = (rs.wasNull() ? null : Enum.valueOf(PeriodiciteDecompte.class, temp46));
				final String personneContact = rs.getString(47);
				final int temp50 = rs.getInt(50);
				final RegDate reindexOn = (rs.wasNull() ? null : RegDate.fromIndex(temp50, false));
				final boolean temp51 = rs.getBoolean(51);
				final Boolean sansListeRecap = (rs.wasNull() ? null : temp51);
				final boolean temp52 = rs.getBoolean(52);
				final Boolean sansRappel = (rs.wasNull() ? null : temp52);
				final String titulaireCompteBancaire = rs.getString(53);
			
				DebiteurPrestationImposable o = new DebiteurPrestationImposable();
				o.setNumero(numero);
				o.setAdresseBicSwift(adresseBicSwift);
				o.setAdresseCourrierElectronique(adresseEmail);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setBlocageRemboursementAutomatique(blocRembAuto);
				o.setCategorieImpotSource(categorieImpotSource);
				o.setComplementNom(complementNom);
				o.setDebiteurInactif(debiteurInactif);
				o.setNom1(dpiNom1);
				o.setNom2(dpiNom2);
				o.setIndexDirty(indexDirty);
				o.setLogicielId(logicielId);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setModeCommunication(modeCom);
				o.setNumeroCompteBancaire(numeroCompteBancaire);
				o.setNumeroTelecopie(numeroTelecopie);
				o.setNumeroTelephonePortable(numeroTelPortable);
				o.setNumeroTelephonePrive(numeroTelPrive);
				o.setNumeroTelephoneProfessionnel(numeroTelProf);
				o.setOfficeImpotId(oid);
				o.setPeriodeDecompteAvantMigration(periodeDecompte);
				o.setPeriodiciteDecompteAvantMigration(periodiciteDecompte);
				o.setPersonneContact(personneContact);
				o.setReindexOn(reindexOn);
				o.setSansListeRecapitulative(sansListeRecap);
				o.setSansRappel(sansRappel);
				o.setTitulaireCompteBancaire(titulaireCompteBancaire);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + tiersType + "]");
			}
			
			return res;
		}
	}

	@Override
    @SuppressWarnings("unchecked")
    public Set<Long> getNumerosIndividu(final Set<Long> tiersIds, final boolean includesComposantsMenage) {

	    final JdbcTemplate template = new JdbcTemplate(dataSource);

	    final Set<Long> numeros = new HashSet<Long>(tiersIds.size());

	    if (includesComposantsMenage) {
			// Découpe la requête en sous-requêtes si nécessaire
			numeros.addAll(CollectionsUtils.splitAndProcess(tiersIds, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Long>() {
				@Override
				public List<Long> process(List<Long> ids) {
					return template.query(NumerosComposantsMapper.selectByIds(ids), new NumerosComposantsMapper());
				}
			}));
	    }

	    numeros.addAll(CollectionsUtils.splitAndProcess(tiersIds, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Long>() {
			@Override
		    public List<Long> process(List<Long> ids) {
			    return template.query(NumerosIndividusMapper.selectByIds(ids), new NumerosIndividusMapper());
		    }
	    }));

	    return numeros;
    }

	private static class NumerosComposantsMapper implements RowMapper {

		private static final String BASE_SELECT = "select " +
				"t.NUMERO_INDIVIDU " +
				"from TIERS t, RAPPORT_ENTRE_TIERS r " +
				"where t.NUMERO = r.TIERS_SUJET_ID " +
				"and r.ANNULATION_DATE is null " +
				"and r.RAPPORT_ENTRE_TIERS_TYPE = 'AppartenanceMenage' " +
				"and t.NUMERO_INDIVIDU is not null";

		public static String selectByIds(Collection<Long> tiersId) {
			return BASE_SELECT + " and r.TIERS_OBJET_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getLong(1);
		}
	}

	private static class NumerosIndividusMapper implements RowMapper {

		private static final String BASE_SELECT = "select " +
				"t.NUMERO_INDIVIDU " +
				"from TIERS t " +
				"where t.NUMERO_INDIVIDU is not null";

		public static String selectByIds(Collection<Long> tiersId) {
			return BASE_SELECT + " and t.NUMERO in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getLong(1);
		}
	}
	
	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
}

