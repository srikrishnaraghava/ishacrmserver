package crmdna.api.servlet.mail2;

import crmdna.mail2.Mail;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class MandrillWebhookServlet extends HttpServlet {
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Mandrill web hook servlet (get call)");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        Logger logger = Logger.getLogger(MandrillWebhookServlet.class.getName());

        try {
            resp.setContentType("text/plain");

            StringBuilder builder = new StringBuilder();
            String line = null;

            BufferedReader reader = req.getReader();
            while ((line = reader.readLine()) != null)
                builder.append(line);

            String postData = URLDecoder.decode(builder.toString(), "UTF-8");

            logger.info("post data: [" + postData + "]");
            Mail.processWebhookEvents(postData);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception occured", ex);
        }
    }
}
