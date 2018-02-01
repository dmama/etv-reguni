package ch.vd.uniregctb.document;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.uniregctb.common.DelayedDownloadService;
import ch.vd.uniregctb.common.EditiqueDownloadService;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TypedDataContainer;

@Controller
public class DelayedDocumentDownloadController {

	private DelayedDownloadService delayedDownloadService;
	private EditiqueDownloadService editiqueDownloadService;

	public void setDelayedDownloadService(DelayedDownloadService delayedDownloadService) {
		this.delayedDownloadService = delayedDownloadService;
	}

	public void setEditiqueDownloadService(EditiqueDownloadService editiqueDownloadService) {
		this.editiqueDownloadService = editiqueDownloadService;
	}

	@RequestMapping(value = "/delayed-download.do", method = RequestMethod.GET)
	public String fetchDocument(HttpServletResponse response,
	                            HttpSession session,
	                            @RequestParam(value = "id") UUID id,
	                            @RequestParam(value = "remove", required = false, defaultValue = "true") boolean remove) throws IOException {
		final TypedDataContainer doc = delayedDownloadService.fetchDocument(id, remove);
		if (doc == null) {
			throw new ObjectNotFoundException("Document inconnu!");
		}
		editiqueDownloadService.download(doc, response);
		if (remove) {
			session.removeAttribute(DelayedDownloadService.SESSION_ATTRIBUTE_NAME);
			doc.close();
		}
		return null;
	}
}
