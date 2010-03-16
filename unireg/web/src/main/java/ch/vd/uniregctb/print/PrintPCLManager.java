package ch.vd.uniregctb.print;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PrintPCLManager {


	/**
	 * Ouvre un flux PCL
	 *
	 * @param request
	 * @param response
	 * @param pcl
	 * @throws IOException
	 */
	public void openPclStream(HttpServletRequest request, HttpServletResponse response, byte[] pcl) throws IOException;

}
