package ch.vd.uniregctb.tiers;

import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class TiersLaunchCatController extends AbstractController {

	private static final Logger LOGGER = Logger.getLogger(TiersLaunchCatController.class);

	@Override
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String num = request.getParameter("numero");
		Integer numero = Integer.parseInt(num);
		LOGGER.info("TiersLaunchCatController : launch cat for "+numero);

		// XML Content
		StringBuffer str = new StringBuffer();
		str.append("<?xml version=\"1.0\" ?>\n");
		str.append("<tasklist name=\"Launch Cat\">\n");
		str.append("	<task name=\"Launch Cat\" action=\"execute\">\n");
		str.append("		<execute path=\"C:\\cat\" wait=\"true\" name=\"cat.exe\">\n");
		str.append("			<parameter>-numeroContribuable</parameter>\n");
		str.append("			<parameter>"+numero+"</parameter>\n");
		str.append("		</execute>\n");
		str.append("	</task>\n");
		str.append("</tasklist>\n");

		// Headers
		response.setContentType("application/x-chvd");
		response.setHeader("Content-disposition", "attachment;filename=\"launchcat.chvd\"");
		response.setHeader( "Pragma", "public" );
		response.setHeader("cache-control", "no-cache");
		response.setHeader("Cache-control", "must-revalidate");
		response.setContentLength(str.length());

		// Output Stream
		OutputStreamWriter os = new OutputStreamWriter(response.getOutputStream());
		os.write(str.toString(), 0, str.length());
		os.close();

		return null;
	}


}
