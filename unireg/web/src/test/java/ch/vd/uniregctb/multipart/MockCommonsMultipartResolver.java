package ch.vd.uniregctb.multipart;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

public class MockCommonsMultipartResolver extends CommonsMultipartResolver {
	private final List<MockFileItem> fileItems;
	public MockCommonsMultipartResolver(List<MockFileItem> fileItems) {
		this.fileItems = fileItems;
	}
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new MockFileUpload(fileItemFactory, fileItems);
	}
}
class MockFileUpload extends ServletFileUpload {
	private final List<MockFileItem> fileItems;
	public MockFileUpload(FileItemFactory fileItemFactory, List<MockFileItem> fileItems) {
		super(fileItemFactory);
		this.fileItems = fileItems;
	}
	@Override
	public List<MockFileItem> parseRequest(HttpServletRequest request)
			throws FileUploadException {
		return fileItems;
	}

}
