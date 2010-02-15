package ch.vd.uniregctb.print;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import sun.misc.BASE64Encoder;

public class PrintPCLManagerImpl implements PrintPCLManager{

	private static boolean localApp;

	public static boolean isLocalApp() {
		return localApp;
	}

	public void setLocalApp(String localApp) {
		PrintPCLManagerImpl.localApp = "true".equalsIgnoreCase(localApp) || "1".equals(localApp) || "yes".equalsIgnoreCase(localApp);
	}


	/**
	 * Ouvre un flux X-CHVD ou PCL
	 * en fonction du boolean localApp
	 *
	 * @param request
	 * @param response
	 * @param pcl
	 * @throws IOException
	 */
	public void openPclStream(HttpServletRequest request, HttpServletResponse response, byte[] pcl) throws IOException {

		if(isLocalApp()) {
			openPclWithLocalAppStream(request, response, pcl);
		}
		else {
			openPclWithoutLocalAppStream(request, response, pcl);
		}
	}

	/**
	 * Ouvre un flux PCL
	 *
	 * @param request
	 * @param response
	 * @param pcl
	 * @throws IOException
	 */
	private void openPclWithoutLocalAppStream(HttpServletRequest request, HttpServletResponse response, byte[] pcl) throws IOException {

		response.setContentType("application/pcl");
		response.setHeader("Content-disposition", "attachment");
		response.setHeader( "Pragma", "public" );
		response.setHeader("cache-control", "no-cache");
		response.setHeader("Cache-control", "must-revalidate");
		response.setContentLength(pcl.length);

		ServletOutputStream out = response.getOutputStream();
		for( int i = 0; i < pcl.length; i++ ) {
			response.getOutputStream().write( pcl[i] );
		}

		out.flush();
		out.close();
	}


	/**
	 * Ouvre un flux X-CHVD - PCL
	 *
	 * @param request
	 * @param response
	 * @param pcl
	 * @throws IOException
	 */
	public void openPclWithLocalAppStream(HttpServletRequest request, HttpServletResponse response, byte[] pcl) throws IOException {

		response.setContentType("application/x-chvd");
		response.setHeader("Content-disposition", "inline; filename=\"print.chvd\"");

		String debutCorps = construitDebutCorps();
		response.setBufferSize(debutCorps.length());

		ServletOutputStream out = response.getOutputStream();
		out.write(debutCorps.getBytes());

		BASE64Encoder be = new BASE64Encoder();
		String pclAsString = be.encodeBuffer(pcl);
		out.write(pclAsString.getBytes());

		String finCorps = construitFinCorps();
		out.write(finCorps.getBytes());
		out.flush();
		out.close();
	}

	private String construitDebutCorps() {
		String tete = 	"<?xml version=\"1.0\"?>" +
						"<tasklist name=\"printPCLList\">" +
						"	<task name=\"printPCL\" action=\"print\">" +
						"		<print>" +
						"			<parameter>";
		return tete;
	}

	private String construitFinCorps() {
		String pieds = 	"			</parameter>" +
						"		</print>" +
						"	</task>" +
						"</tasklist>";
		return pieds;
	}

}
