package crmdna.email;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.OverQuotaException;
import crmdna.common.Utils;
import crmdna.common.config.ConfigCRMDNA;
import crmdna.user.User;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Logger;

public class GAEEmail {

    public static void send(EmailProp emailProp)
            throws UnsupportedEncodingException, MessagingException {

        // Client.ensureValidClient(client);

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        Message message = new MimeMessage(session);

        String fromEmail = ConfigCRMDNA.get().toProp().fromEmail;
        if (fromEmail == null)
            fromEmail = User.SUPER_USER;

        Utils.ensureValidEmail(fromEmail);

        String fromNickName = fromEmail;

        InternetAddress from = new InternetAddress(fromEmail, fromNickName);
        message.setFrom(from);

        for (String email : emailProp.toEmailAddresses) {
            Utils.ensureValidEmail(email);
            InternetAddress to = new InternetAddress(email, email);
            message.addRecipient(Message.RecipientType.TO, to);
        }

        String applicationId = ApiProxy.getCurrentEnvironment().getAppId();

        if (applicationId != null)
            emailProp.subject = applicationId + ": " + emailProp.subject;
        message.setSubject(emailProp.subject);

        Multipart mp = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(emailProp.bodyHtml, "text/html");
        mp.addBodyPart(htmlPart);

        if (emailProp.csvAttachmentData != null) {

            MimeBodyPart attachment = new MimeBodyPart();
            attachment.setFileName(emailProp.attachmentName);

            attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(
                    emailProp.csvAttachmentData.getBytes(), "text/csv")));

            mp.addBodyPart(attachment);
        }

        message.setContent(mp);

        Logger logger = Logger.getLogger(GAEEmail.class.getName());
        //logger.info("emailProp: " + new Gson().toJson(emailProp));

        //handle quota exceeded exception
        try {
            Transport.send(message);
        } catch (OverQuotaException e) {
            logger.severe(Utils.stackTraceToString(e));
        }
    }
}
