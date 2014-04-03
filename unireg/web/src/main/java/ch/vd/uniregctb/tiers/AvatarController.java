package ch.vd.uniregctb.tiers;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ch.vd.unireg.avatars.AvatarService;
import ch.vd.unireg.avatars.ImageData;
import ch.vd.unireg.avatars.TypeAvatar;

@Controller
public class AvatarController {

	private AvatarService avatarService;
	private TiersService tiersService;

	public void setAvatarService(AvatarService avatarService) {
		this.avatarService = avatarService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@RequestMapping(value = "/tiers/avatar.do", method = RequestMethod.GET)
	@Transactional(rollbackFor = Throwable.class)
	public String getAvatar(@RequestParam(value = "noTiers", required = false) Long noTiers,
	                        @RequestParam(value = "type", required = false) TypeAvatar type,
	                        @RequestParam(value = "link", required = false, defaultValue = "false") boolean withLink,
	                        HttpServletResponse response) throws Exception {

		if (noTiers == null && type == null) {
			throw new IllegalArgumentException("Either noTiers or type should be provided");
		}

		try (ImageData data = getImageData(noTiers, type, withLink)) {
			response.reset(); // pour éviter l'exception 'getOutputStream() has already been called for this response'

			response.setHeader("Content-disposition", "inline");
			response.setHeader("Pragma", "public");
			response.setHeader("cache-control", "no-cache");
			response.setHeader("Cache-control", "must-revalidate");
			response.setContentType(data.getMimeType());

			try (ServletOutputStream out = response.getOutputStream()) {
				IOUtils.copy(data.getDataStream(),out);
	            out.flush();
			}
		}
		return null;
	}

	private ImageData getImageData(Long noTiers, TypeAvatar type, boolean withLink) {
		if (type != null) {
			return avatarService.getAvatar(type, withLink);
		}
		else {
			final Tiers tiers = tiersService.getTiers(noTiers);
			return avatarService.getAvatar(tiers, withLink);
		}
	}
}
