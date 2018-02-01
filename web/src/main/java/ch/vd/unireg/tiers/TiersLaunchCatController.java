package ch.vd.unireg.tiers;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TiersLaunchCatController {

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersLaunchCatController.class);

	@RequestMapping(value = "/tiers/launchcat.do")
	@ResponseBody
	public String launchCat(@RequestParam("numero") int numero, HttpServletResponse response) throws Exception {

		LOGGER.info("TiersLaunchCatController : launch cat for " + numero);

		ServletOutputStream out = response.getOutputStream();
		response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

		// XML Content
		final StringBuilder str = new StringBuilder();
		str.append("<?xml version=\"1.0\" ?>\n");
		str.append("<tasklist name=\"Launch Cat\">\n");
		str.append("	<task name=\"Launch Cat\" action=\"execute\">\n");
		str.append("		<execute path=\"C:\\cat\" wait=\"true\" name=\"cat.exe\">\n");
		str.append("			<parameter>-numeroContribuable</parameter>\n");
		str.append("			<parameter>").append(numero).append("</parameter>\n");
		str.append("		</execute>\n");
		str.append("	</task>\n");
		str.append("</tasklist>\n");

		// Headers
		response.setContentType("application/x-chvd");
		response.setHeader("Content-disposition", "attachment;filename=\"launchcat.chvd\"");
		response.setHeader("Pragma", "public");
		response.setHeader("cache-control", "no-cache");
		response.setHeader("Cache-control", "must-revalidate");
		response.setContentLength(str.length());

		// Output Stream
		try (OutputStreamWriter os = new OutputStreamWriter(out)) {
			os.write(str.toString(), 0, str.length());
		}
		return null;
	}
}
