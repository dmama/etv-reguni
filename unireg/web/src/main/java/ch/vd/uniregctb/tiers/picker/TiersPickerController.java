package ch.vd.uniregctb.tiers.picker;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping(value = "/tiers/picker")
public class TiersPickerController {

	@RequestMapping(value = "/tiers-picker.do", method = RequestMethod.GET)
	public String tiersPicker() {
		return "tiers/picker/tiers-picker";
	}
}
