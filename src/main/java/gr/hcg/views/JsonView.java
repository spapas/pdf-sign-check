package gr.hcg.views;

import com.google.gson.Gson;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JsonView {

    public static ModelAndView Render(Object model, HttpServletResponse response) {

        Gson gson = new Gson();
        String json = gson.toJson(model);


        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            //response.setHeader("Content-Disposition", "attachment; filename=foo.json");
            response.getWriter().write(json);
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }


        /*
        try {
            //jsonConverter.write(model, jsonMimeType, new ServletServerHttpResponse(response));
        } catch (HttpMessageNotWritableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        return null;
    }
}