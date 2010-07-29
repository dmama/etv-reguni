package ch.vd.uniregctb.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.common.CollectionsUtils;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeContribuable;

public class JdbcDeclarationDaoImpl implements JdbcDeclarationDao {

	private JdbcEtatDeclarationDao etatDeclarationDao = new JdbcEtatDeclarationDaoImpl();

	@SuppressWarnings({"unchecked"})
	public Declaration get(long forId, boolean withEtats, JdbcTemplate template) {
		final DeclarationMapper mapper = new DeclarationMapper(template);

		final Pair<Long, Declaration> pair = (Pair<Long, Declaration>) DataAccessUtils.uniqueResult(template.query(DeclarationMapper.selectById(), new Object[]{forId}, mapper));
		final Declaration declaration = pair.getSecond();
		if (declaration == null) {
			return null;
		}

		if (withEtats) {
			final Set<EtatDeclaration> etats = etatDeclarationDao.getForDeclaration(declaration.getId(), template);
			for (EtatDeclaration e : etats) {
				e.setDeclaration(declaration);
			}
			declaration.setEtats(etats);
		}

		return declaration;
	}

	@SuppressWarnings({"unchecked"})
	public Set<Declaration> getForTiers(long tiersId, boolean withEtats, JdbcTemplate template) {
		final DeclarationMapper mapper = new DeclarationMapper(template);

		final List<Pair<Long, Declaration>> list = template.query(DeclarationMapper.selectByTiersId(), new Object[]{tiersId}, mapper);

		final HashSet<Long> declarationIds = new HashSet<Long>(list.size());
		final HashSet<Declaration> set = new HashSet<Declaration>(list.size());
		for (Pair<Long, Declaration> pair : list) {
			final Declaration declaration = pair.getSecond();
			set.add(declaration);

			if (withEtats) {
				declarationIds.add(declaration.getId());
			}
		}

		if (withEtats) {
			final Map<Long, Set<EtatDeclaration>> map = etatDeclarationDao.getForDeclarations(declarationIds, template);
			for (Declaration declaration : set) {
				Set<EtatDeclaration> etats = map.get(declaration.getId());
				if (etats == null) {
					etats = Collections.emptySet();
				}
				else {
					for (EtatDeclaration e : etats) {
						e.setDeclaration(declaration);
					}
				}
				declaration.setEtats(etats);
			}
		}

		return set;
	}

	@SuppressWarnings({"unchecked"})
	public Map<Long, Set<Declaration>> getForTiers(Collection<Long> tiersId, boolean withEtats, final JdbcTemplate template) {

		final DeclarationMapper mapper = new DeclarationMapper(template);

		// Découpe la requête en sous-requêtes si nécessaire
		final List<Pair<Long, Declaration>> list = CollectionsUtils.splitAndProcess(tiersId, JdbcDaoUtils.MAX_IN_SIZE, new CollectionsUtils.SplitCallback<Long, Pair<Long, Declaration>>() {
			public List<Pair<Long, Declaration>> process(List<Long> ids) {
				return template.query(DeclarationMapper.selectByTiersIds(ids), mapper);
			}
		});

		final HashSet<Long> declarationIds = new HashSet<Long>(list.size());
		final HashMap<Long, Set<Declaration>> map = new HashMap<Long, Set<Declaration>>();
		for (Pair<Long, Declaration> pair : list) {
			Set<Declaration> set = map.get(pair.getFirst());
			if (set == null) {
				set = new HashSet<Declaration>();
				map.put(pair.getFirst(), set);
			}

			final Declaration declaration = pair.getSecond();
			set.add(declaration);

			if (withEtats) {
				declarationIds.add(declaration.getId());
			}
		}

		if (withEtats) {
			final Map<Long, Set<EtatDeclaration>> mapEtats = etatDeclarationDao.getForDeclarations(declarationIds, template);
			for (Set<Declaration> declarations : map.values()) {
				for (Declaration declaration : declarations) {
					Set<EtatDeclaration> etats = mapEtats.get(declaration.getId());
					if (etats == null) {
						etats = Collections.emptySet();
					}
					else {
						for (EtatDeclaration e : etats) {
							e.setDeclaration(declaration);
						}
					}
					declaration.setEtats(etats);
				}
			}
		}

		return map;
	}

	private static class DeclarationMapper implements RowMapper {
		private static final String BASE_SELECT = "select " +
				"DOCUMENT_TYPE, " + // 1
				"ID, " + // 2
				"ANNULATION_DATE, " + // 3
				"ANNULATION_USER, " + // 4
				"DATE_DEBUT, " + // 5
				"DATE_FIN, " + // 6
				"DATE_IMPR_CHEMISE_TO, " + // 7
				"DELAI_RETOUR_IMPRIME, " + // 8
				"LOG_CDATE, " + // 9
				"LOG_CUSER, " + // 10
				"LOG_MDATE, " + // 11
				"LOG_MUSER, " + // 12
				"MODELE_DOC_ID, " + // 13
				"MODE_COM, " + // 14
				"NOM_DOCUMENT, " + // 15
				"NO_OFS_FOR_GESTION, " + // 16
				"NUMERO, " + // 17
				"PERIODE_ID, " + // 18
				"PERIODICITE, " + // 19
				"QUALIFICATION, " + // 20
				"RETOUR_COLL_ADMIN_ID, " + // 21
				"SANS_RAPPEL, " + // 22
				"TIERS_ID, " + // 23
				"TYPE_CTB " + // 24
				"from DECLARATION";

		private JdbcPeriodeFiscaleDao pfDao = new JdbcPeriodeFiscaleDaoImpl();
		private JdbcModeleDocumentDao mdDao = new JdbcModeleDocumentDaoImpl();
		private Map<Long, PeriodeFiscale> periodesCache = new HashMap<Long, PeriodeFiscale>();
		private Map<Long, ModeleDocument> docCache = new HashMap<Long, ModeleDocument>();
		private JdbcTemplate template;

		private DeclarationMapper(JdbcTemplate template) {
			this.template = template;
		}

		public static String selectById() {
			return BASE_SELECT + " where ID = ?";
		}

		public static String selectByTiersId() {
			return BASE_SELECT + " where TIERS_ID = ?";
		}

		public static String selectByTiersIds(Collection<Long> tiersId) {
			return BASE_SELECT + " where TIERS_ID in " + JdbcDaoUtils.buildInClause(tiersId);
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			
			final String documentType = rs.getString(1);
			final long temp23 = rs.getLong(23);
			final Long tiersId = (rs.wasNull() ? null : temp23);
			final Declaration res;
			
			if (documentType.equals("DI")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date dateImprChemiseTo = rs.getTimestamp(7);
				final int temp8 = rs.getInt(8);
				final RegDate delaiRetourImprime = (rs.wasNull() ? null : RegDate.fromIndex(temp8, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp13 = rs.getLong(13);
				final ModeleDocument modeleDocId = (rs.wasNull() ? null : getModeleDocument(temp13));
				final String nomDocument = rs.getString(15);
				final int temp16 = rs.getInt(16);
				final Integer noOfsForGestion = (rs.wasNull() ? null : temp16);
				final int temp17 = rs.getInt(17);
				final Integer numero = (rs.wasNull() ? null : temp17);
				final long temp18 = rs.getLong(18);
				final PeriodeFiscale periodeId = (rs.wasNull() ? null : getPeriodeFiscale(temp18));
				final String temp20 = rs.getString(20);
				final Qualification qualification = (rs.wasNull() ? null : Enum.valueOf(Qualification.class, temp20));
				final long temp21 = rs.getLong(21);
				final Long retourCollAdminId = (rs.wasNull() ? null : temp21);
				final String temp24 = rs.getString(24);
				final TypeContribuable typeCtb = (rs.wasNull() ? null : Enum.valueOf(TypeContribuable.class, temp24));
			
				DeclarationImpotOrdinaire o = new DeclarationImpotOrdinaire();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setDateImpressionChemiseTaxationOffice(dateImprChemiseTo);
				o.setDelaiRetourImprime(delaiRetourImprime);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setModeleDocument(modeleDocId);
				o.setNomDocument(nomDocument);
				o.setNumeroOfsForGestion(noOfsForGestion);
				o.setNumero(numero);
				o.setPeriode(periodeId);
				o.setQualification(qualification);
				o.setRetourCollectiviteAdministrativeId(retourCollAdminId);
				o.setTypeContribuable(typeCtb);
				res = o;
			}
			else if (documentType.equals("LR")) {
			
				final long temp2 = rs.getLong(2);
				final Long id = (rs.wasNull() ? null : temp2);
				final Date annulationDate = rs.getTimestamp(3);
				final String annulationUser = rs.getString(4);
				final int temp5 = rs.getInt(5);
				final RegDate dateDebut = (rs.wasNull() ? null : RegDate.fromIndex(temp5, false));
				final int temp6 = rs.getInt(6);
				final RegDate dateFin = (rs.wasNull() ? null : RegDate.fromIndex(temp6, false));
				final Date logCdate = rs.getTimestamp(9);
				final String logCuser = rs.getString(10);
				final Timestamp logMdate = rs.getTimestamp(11);
				final String logMuser = rs.getString(12);
				final long temp13 = rs.getLong(13);
				final ModeleDocument modeleDocId = (rs.wasNull() ? null : getModeleDocument(temp13));
				final String temp14 = rs.getString(14);
				final ModeCommunication modeCom = (rs.wasNull() ? null : Enum.valueOf(ModeCommunication.class, temp14));
				final String nomDocument = rs.getString(15);
				final long temp18 = rs.getLong(18);
				final PeriodeFiscale periodeId = (rs.wasNull() ? null : getPeriodeFiscale(temp18));
				final String temp19 = rs.getString(19);
				final PeriodiciteDecompte periodicite = (rs.wasNull() ? null : Enum.valueOf(PeriodiciteDecompte.class, temp19));
				final boolean temp22 = rs.getBoolean(22);
				final Boolean sansRappel = (rs.wasNull() ? null : temp22);
			
				DeclarationImpotSource o = new DeclarationImpotSource();
				o.setId(id);
				o.setAnnulationDate(annulationDate);
				o.setAnnulationUser(annulationUser);
				o.setDateDebut(dateDebut);
				o.setDateFin(dateFin);
				o.setLogCreationDate(logCdate);
				o.setLogCreationUser(logCuser);
				o.setLogModifDate(logMdate);
				o.setLogModifUser(logMuser);
				o.setModeleDocument(modeleDocId);
				o.setModeCommunication(modeCom);
				o.setNomDocument(nomDocument);
				o.setPeriode(periodeId);
				o.setPeriodicite(periodicite);
				o.setSansRappel(sansRappel);
				res = o;
			}
			else {
				throw new IllegalArgumentException("Type inconnu = [" + documentType + "]");
			}
			
			final Pair<Long, Declaration> pair = new Pair<Long, Declaration>();
			pair.setFirst(tiersId);
			pair.setSecond(res);
			
			return pair;
		}

		private PeriodeFiscale getPeriodeFiscale(Long periodeId) {
			PeriodeFiscale periode = periodesCache.get(periodeId);
			if (periode == null) {
				periode = pfDao.get(periodeId, template);
				periodesCache.put(periodeId, periode);
			}
			return periode;
		}

		private ModeleDocument getModeleDocument(Long modeleDocId) {
			ModeleDocument doc = docCache.get(modeleDocId);
			if (doc == null) {
				doc = mdDao.get(modeleDocId, template);
				docCache.put(modeleDocId, doc);
			}
			return doc;
		}
	}
}
