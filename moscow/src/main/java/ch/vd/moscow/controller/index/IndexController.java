package ch.vd.moscow.controller.index;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class IndexController {

    @RequestMapping(value = "/index.do", method = RequestMethod.GET)
    public String index() throws Exception {
        return "index";
    }
}
