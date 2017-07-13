package gr.hcg;

/**
 * Created by serafeim on 13/7/2017.
 */
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    //https://jira.spring.io/browse/SPR-14651
    //Spring 4.3.5 supports RedirectAttributes
    @ExceptionHandler(MultipartException.class)
    public String handleError1(MultipartException e, Model model) {

        model.addAttribute("message", e.getCause().getMessage());
        return "home";

    }

    /* Spring < 4.3.5
	@ExceptionHandler(MultipartException.class)
    public String handleError2(MultipartException e) {

        return "redirect:/errorPage";

    }*/

}