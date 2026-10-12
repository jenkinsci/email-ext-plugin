package hudson.plugins.emailext;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Sends an already built {@link MimeMessage} through the Amazon SES v2 {@code SendEmail} API as a raw message,
 * as an alternative to SMTP.
 */
@Restricted(NoExternalUse.class)
final class SesMailSender {

    /** Amazon SES rejects messages with more than 50 recipients, so larger lists are sent in batches. */
    static final int MAX_RECIPIENTS_PER_MESSAGE = 50;

    /** Headers the SMTP transport also leaves out, so that blind copy recipients stay hidden. */
    private static final String[] IGNORED_HEADERS = {"Bcc", "Content-Length"};

    private SesMailSender() {}

    /**
     * Sends {@code msg} to {@code recipients}.
     *
     * <p>Like the SMTP transport, the {@code Bcc} header is left out of the message, so the recipients are passed
     * explicitly as the SES destination rather than read from the message headers.
     *
     * @return the SES message id of every message sent, one per batch of recipients
     */
    static List<String> send(
            @NonNull SesV2Client client,
            @NonNull MailAccount account,
            @NonNull MimeMessage msg,
            @NonNull Address[] recipients)
            throws IOException, MessagingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out, IGNORED_HEADERS);
        SdkBytes raw = SdkBytes.fromByteArray(out.toByteArray());

        List<String> addresses = Arrays.stream(recipients)
                .map(SesMailSender::toAddress)
                .distinct()
                .toList();
        List<String> messageIds = new ArrayList<>();
        for (int i = 0; i < addresses.size(); i += MAX_RECIPIENTS_PER_MESSAGE) {
            List<String> batch = addresses.subList(i, Math.min(i + MAX_RECIPIENTS_PER_MESSAGE, addresses.size()));
            SendEmailRequest.Builder request = SendEmailRequest.builder()
                    .destination(d -> d.toAddresses(batch))
                    .content(c -> c.raw(r -> r.data(raw)));
            if (StringUtils.isNotBlank(account.getSesConfigurationSet())) {
                request.configurationSetName(account.getSesConfigurationSet());
            }
            messageIds.add(client.sendEmail(request.build()).messageId());
        }
        return messageIds;
    }

    /**
     * SES replaces any {@code Message-ID} header of a raw message with one derived from the SES message id, so this
     * is the value later emails must reference in {@code In-Reply-To} to be threaded with the one just sent.
     */
    static String toMessageIdHeader(@NonNull String region, @NonNull String sesMessageId) {
        String domain = "us-east-1".equals(region) ? "email.amazonses.com" : region + ".amazonses.com";
        return "<" + sesMessageId + "@" + domain + ">";
    }

    static SesV2Client createClient(@NonNull MailAccount account, Run<?, ?> run) {
        return SesV2Client.builder()
                .region(Region.of(account.getAwsRegion()))
                .credentialsProvider(credentialsProvider(account, run))
                .httpClientBuilder(ApacheHttpClient.builder())
                .build();
    }

    private static AwsCredentialsProvider credentialsProvider(MailAccount account, Run<?, ?> run) {
        String credentialsId = account.getAwsCredentialsId();
        if (StringUtils.isBlank(credentialsId)) {
            return DefaultCredentialsProvider.builder().build();
        }
        AmazonWebServicesCredentials credentials =
                CredentialsProvider.findCredentialById(credentialsId, AmazonWebServicesCredentials.class, run);
        if (credentials == null) {
            throw SdkClientException.create("Cannot find AWS credentials with id '" + credentialsId + "'");
        }
        return credentials;
    }

    private static String toAddress(Address address) {
        return address instanceof InternetAddress internetAddress ? internetAddress.getAddress() : address.toString();
    }
}
