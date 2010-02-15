package ch.vd.uniregctb.couple.manager;

import java.util.Date;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.couple.view.CoupleRecapView;
import ch.vd.uniregctb.couple.view.TypeUnion;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.tiers.TiersService;

@ContextConfiguration(locations = {
		"classpath:ch/vd/uniregctb/couple/manager/config.xml"
})
public class UNIREG1521 extends BusinessTest {
	
	CoupleRecapManager mngr;
	
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tiersService = getBean(TiersService.class, "tiersService");
		mngr = getBean(CoupleRecapManager.class, "coupleRecapManager");
	}
	
	@Test 
	public void test () throws Exception {
		loadDatabase("UNIREG1521.xml");
		
		TiersGeneralView viewTiers1 = new TiersGeneralView();
		viewTiers1.setNumero(10523758L);
		TiersGeneralView viewTiers2 = new TiersGeneralView();
		viewTiers2.setNumero(10523759L);
		TiersGeneralView viewMC = new TiersGeneralView();
		viewMC.setNumero(10055098L);
		
		CoupleRecapView view = new CoupleRecapView();
		
		view.setDateCoupleExistant(RegDate.get());
		view.setDateDebut(new Date());
		view.setNouveauCtb(false);
		view.setPremierePersonne(viewTiers1);
		view.setSecondePersonne(viewTiers2);
		view.setTroisiemeTiers(viewMC);
		view.setTypeUnion(TypeUnion.COUPLE);
		
		tiersService.getTiers(viewMC.getNumero());
		try {
			mngr.save(view);
		} catch (ClassCastException e) {
			throw e;
		} catch (Exception e) {
			logger.warn("Une exception autre que ClassCastException a été levé. ça ne nous interesse pas", e);
		}
	}
	
    public static void main(String[] args) throws Exception
    {
//    	// Export du fichier dbunit utile pour reproduire le cas
//    	Connection jdbcConnection = null;
//        try {
//        	Class driverClass = Class.forName("oracle.jdbc.driver.OracleDriver");
//            jdbcConnection = DriverManager.getConnection(
//                    "jdbc:oracle:thin:@grominet.etat-de-vaud.ch:1521:ORCL", "xsifnr", "unireg");
//			IDatabaseConnection connection = createNewConnection(jdbcConnection);
//			// partial database export
//	        QueryDataSet partialDataSet = new QueryDataSet(connection);
//	        partialDataSet.addTable("TIERS", "select * from tiers where numero in(10523758, 10523759, 10055098)");
//	        XmlDataSet.write(partialDataSet, new FileOutputStream("C:/UNIREG1521.xml"));
//		}
//		catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//		finally {
//			JdbcUtils.closeConnection(jdbcConnection);
//		}
    }

}
