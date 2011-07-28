package ch.vd.uniregctb.lr;

import java.util.Map;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;

import static junit.framework.Assert.assertEquals;

/**
 * Test case du controlleur spring du meme nom.
 *
 * @author xcifde
 */
public class ListeRecapEditDelaiControllerTest extends AbstractLrControllerTest {

	/**
	 * Le nom du controller a tester.
	 */
	private final static String CONTROLLER_NAME = "lrEditDelaiController";

	private ListeRecapEditDelaiController controller;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		controller = getBean(ListeRecapEditDelaiController.class, CONTROLLER_NAME);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testShowForm() throws Exception {
		//
		//LR 1
		//
		request.setMethod("GET");
		request.addParameter("idLR", idLR1.toString());
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);
	}

	/**
	 * @throws Exception
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testOnSubmitAddDelai() throws Exception {

		{
			DeclarationImpotSource lr = lrDAO.get(idLR1);
			assertEquals(2, lr.getDelais().size());
		}

		request.setMethod("POST");
		request.addParameter("idLR", idLR1.toString());
		request.addParameter("dateDemande", RegDateHelper.dateToDisplayString(RegDate.get(2008, 1, 1)));
		request.addParameter("dateTraitement", RegDateHelper.dateToDisplayString(RegDate.get(2008, 1, 1)));
		request.addParameter("delaiAccordeAu", RegDateHelper.dateToDisplayString(RegDate.get(2008, 1, 1)));
		request.addParameter("confirmationEcrite", "0");
		ModelAndView mav = controller.handleRequest(request, response);
		Map<?, ?> model = mav.getModel();
		Assert.assertTrue(model != null);

		{
			DeclarationImpotSource lr = lrDAO.get(idLR1);
			assertEquals(3, lr.getDelais().size());
		}
	}

}
