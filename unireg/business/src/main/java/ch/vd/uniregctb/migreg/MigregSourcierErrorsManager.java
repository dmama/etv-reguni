package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class MigregSourcierErrorsManager extends MigregErrorsManager {

	public MigregSourcierErrorsManager(HostMigratorHelper helper, int ctbStart, int ctbEnd) throws Exception {
		super(helper, ctbStart, ctbEnd);
	}

	@Override
	protected List<Long> getHostCtbsInRange(int debut, int fin) {
		List<Long> list = new ArrayList<Long>();

		JdbcTemplate template = new JdbcTemplate(this.helper.db2DataSource);
		SqlRowSet rows = template.queryForRowSet("SELECT C.NO_SOURCIER FROM " +
			helper.getTableIs("employeur")+" A, " +
			helper.getTableIs("rapport_travail")+" B, " +
			helper.getTableIs("sourcier")+" C, "+
			helper.getTableIs("cpte_annuel_sour")+" D, "+
			helper.getTableIs("detail_facture")+ " E, "+
			helper.getTableIs("histo_rapport_trav")+" F "+
			" WHERE " +
			"((C.CATEGORIE_SOURCIER = 'A + B')"+
			" AND (E.DA_ENTREE_DECLAREE >= '01.01.2008')"+
			" AND NOT (E.DA_CTBLN_IMPOT = '01.01.0001')"+
			" AND (F.DAA_HRT = '01.01.0001')"+
			" AND ((F.DAF_HRT = '01.01.0001')"+
			" OR (F.DAF_HRT >= '01.01.2009')))"+
			" AND (A.NO_EMPLOYEUR = B.FK_EMPNO)"+
			" AND (B.FK_SOUNO = C.NO_SOURCIER)"+
			" AND (B.FK_EMPNO = D.FK_RAT_FKEMPNO)"+
			" AND (B.FK_SOUNO = D.FK_RAT_FKSOUNO)"+
			" AND (D.AN_IMPOSITION = E.FK_CASANIMP)"+
			" AND (D.FK_RAT_FKEMPNO = E.FK_CAS_RAT_EMPNO)"+
			" AND (D.FK_RAT_FKSOUNO = E.FK_CAS_RAT_SOUNO)"+
			" AND (B.FK_EMPNO = F.FK_RAT_EMPNO)"+
			" AND (B.FK_SOUNO = F.FK_RAT_SOUNO)"+
			" AND NO_SOURCIER >= "	+ debut +
			" AND NO_SOURCIER <= " + fin);
		while (rows.next()) {
			int nb = rows.getInt(1);
			list.add(new Long(nb));
		}
		return list;
	}

}
