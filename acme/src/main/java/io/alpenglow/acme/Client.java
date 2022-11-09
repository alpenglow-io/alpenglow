package io.alpenglow.acme;

import org.apache.logging.log4j.core.impl.ThreadContextDataInjector;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public interface Client {
  // File name of the User Key Pair
  static final File USER_KEY_FILE = new File("user.key");

  // File name of the Domain Key Pair
  static final File DOMAIN_KEY_FILE = new File("domain.key");

  // File name of the CSR
  static final File DOMAIN_CSR_FILE = new File("domain.csr");

  // File name of the signed certificate
  static final File DOMAIN_CHAIN_FILE = new File("domain-chain.crt");

  //Challenge type to be used
  static final ChallengeType CHALLENGE_TYPE = ChallengeType.HTTP;

  // RSA key size of generated key pairs
  static final int KEY_SIZE = 2048;

  static final Logger LOG = LoggerFactory.getLogger(Client.class);

  enum ChallengeType {HTTP, DNS}

  /**
   * Generates a certificate for the given domains. Also takes care for the registration
   * process.
   *
   * @param domains Domains to get a common certificate for
   */
  public default void fetchCertificate(Collection<String> domains) throws IOException, AcmeException {
    // Load the user key file. If there is no key file, create a new one.
    KeyPair userKeyPair = loadOrCreateUserKeyPair();

    // Create a session for Let's Encrypt.
    // Use "acme://letsencrypt.org" for production server
    Session session = new Session("acme://letsencrypt.org/staging");

    // Get the Account.
    // If there is no account yet, create a new one.
    Account acct = findOrRegisterAccount(session, userKeyPair);

    // Load or create a key pair for the domains. This should not be the userKeyPair!
    KeyPair domainKeyPair = loadOrCreateDomainKeyPair();

    // Order the certificate
    Order order = acct.newOrder().domains(domains).create();

    // Perform all required authorizations
    for (Authorization auth : order.getAuthorizations()) {
      authorize(auth);
    }

    // Generate a CSR for all of the domains, and sign it with the domain key pair.
    CSRBuilder csrb = new CSRBuilder();
    csrb.addDomains(domains);
    csrb.sign(domainKeyPair);

    // Write the CSR to a file, for later use.
    try (Writer out = new FileWriter(Acme.okycItCsr.toFile())) {
      csrb.write(out);
    }

    // Order the certificate
    order.execute(csrb.getEncoded());

    // Wait for the order to complete
    try {
      int attempts = 10;
      while (order.getStatus() != Status.VALID && attempts-- > 0) {
        // Did the order fail?
        if (order.getStatus() == Status.INVALID) {
          LOG.error("Order has failed, reason: {}", order.getError());
          throw new AcmeException("Order failed... Giving up.");
        }

        // Wait for a few seconds
        Thread.sleep(3000L);

        // Then update the status
        order.update();
      }
    } catch (InterruptedException ex) {
      LOG.error("interrupted", ex);
      Thread.currentThread().interrupt();
    }

    // Get the certificate
    Certificate certificate = order.getCertificate();

    LOG.info("Success! The certificate for domains {} has been generated!", domains);
    LOG.info("Certificate URL: {}", certificate.getLocation());

    // Write a combined file containing the certificate and chain.
    try (FileWriter fw = new FileWriter(Acme.okycItCrt.toFile())) {
      certificate.writeCertificate(fw);
    }

    // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
    // DOMAIN_CHAIN_FILE for the requested domains.
  }

  /**
   * Loads a user key pair from {@link #USER_KEY_FILE}. If the file does not exist, a
   * new key pair is generated and saved.
   * <p>
   * Keep this key pair in a safe place! In a production environment, you will not be
   * able to access your account again if you should lose the key pair.
   *
   * @return User's {@link KeyPair}.
   */
  private KeyPair loadOrCreateUserKeyPair() throws IOException {
    File userKeyFile = Acme.okycItPem.toFile();
    if (userKeyFile.exists()) {
      // If there is a key file, read it
      try (FileReader fr = new FileReader(userKeyFile)) {
        return KeyPairUtils.readKeyPair(fr);
      }

    } else {
      // If there is none, create a new key pair and save it
      KeyPair userKeyPair = KeyPairUtils.createECKeyPair(Acme.SECP_256_R_1);
      try (FileWriter fw = new FileWriter(userKeyFile)) {
        KeyPairUtils.writeKeyPair(userKeyPair, fw);
      }
      return userKeyPair;
    }
  }

  /**
   * Loads a domain key pair from {@link #DOMAIN_KEY_FILE}. If the file does not exist,
   * a new key pair is generated and saved.
   *
   * @return Domain {@link KeyPair}.
   */
  private KeyPair loadOrCreateDomainKeyPair() throws IOException {
    File domainKeyFile = Acme.okycItDomainPem.toFile();
    if (domainKeyFile.exists()) {
      try (FileReader fr = new FileReader(domainKeyFile)) {
        return KeyPairUtils.readKeyPair(fr);
      }
    } else {
      KeyPair domainKeyPair = KeyPairUtils.createECKeyPair(Acme.SECP_256_R_1);
      try (FileWriter fw = new FileWriter(domainKeyFile)) {
        KeyPairUtils.writeKeyPair(domainKeyPair, fw);
      }
      return domainKeyPair;
    }
  }

  /**
   * Finds your {@link Account} at the ACME server. It will be found by your user's
   * public key. If your key is not known to the server yet, a new account will be
   * created.
   * <p>
   * This is a simple way of finding your {@link Account}. A better way is to get the
   * URL of your new account with {@link Account#getLocation()} and store it somewhere.
   * If you need to get access to your account later, reconnect to it via {@link
   * Session#login(URL, KeyPair)} by using the stored location.
   *
   * @param session {@link Session} to bind with
   * @return {@link Account}
   */
  private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException {
    // Ask the user to accept the TOS, if server provides us with a link.
    URI tos = session.getMetadata().getTermsOfService();
    if (tos != null) {
      acceptAgreement(tos);
    }

    Account account = new AccountBuilder()
      .agreeToTermsOfService()
      .useKeyPair(accountKey)
      .create(session);
    LOG.info("Registered a new user, URL: {}", account.getLocation());

    return account;
  }

  /**
   * Authorize a domain. It will be associated with your account, so you will be able to
   * retrieve a signed certificate for the domain later.
   *
   * @param auth {@link Authorization} to perform
   */
  private void authorize(Authorization auth) throws AcmeException {
    LOG.info("Authorization for domain {}", auth.getIdentifier().getDomain());

    // The authorization is already valid. No need to process a challenge.
    if (auth.getStatus() == Status.VALID) {
      return;
    }

    // Find the desired challenge and prepare it.
    var challenge = switch (CHALLENGE_TYPE) {
      case HTTP -> httpChallenge(auth);
      case DNS -> dnsChallenge(auth);
      default -> throw new AcmeException("No challenge found");
    };

    // If the challenge is already verified, there's no need to execute it again.
    if (challenge.getStatus() == Status.VALID) {
      return;
    }

    // Now trigger the challenge.
    challenge.trigger();

    // Poll for the challenge to complete.
    try {
      int attempts = 10;
      while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
        // Did the authorization fail?
        if (challenge.getStatus() == Status.INVALID) {
          LOG.error("Challenge has failed, reason: {}", challenge.getError());
          throw new AcmeException("Challenge failed... Giving up.");
        }

        // Wait for a few seconds
        Thread.sleep(3000L);

        // Then update the status
        challenge.update();
      }
    } catch (InterruptedException ex) {
      LOG.error("interrupted", ex);
      Thread.currentThread().interrupt();
    }

    // All reattempts are used up and there is still no valid authorization?
    if (challenge.getStatus() != Status.VALID) {
      throw new AcmeException("Failed to pass the challenge for domain "
        + auth.getIdentifier().getDomain() + ", ... Giving up.");
    }

    LOG.info("Challenge has been completed. Remember to remove the validation resource.");
    completeChallenge("Challenge has been completed.\nYou can remove the resource again now.");
  }

  /**
   * Prepares a HTTP challenge.
   * <p>
   * The verification of this challenge expects a file with a certain content to be
   * reachable at a given path under the domain to be tested.
   * <p>
   * This example outputs instructions that need to be executed manually. In a
   * production environment, you would rather generate this file automatically, or maybe
   * use a servlet that returns {@link Http01Challenge#getAuthorization()}.
   *
   * @param auth {@link Authorization} to find the challenge in
   * @return {@link Challenge} to verify
   */
  public default Challenge httpChallenge(Authorization auth) throws AcmeException {
    // Find a single http-01 challenge
    Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
    if (challenge == null) {
      throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
    }

    // Output the challenge, wait for acknowledge...
    LOG.info("Please create a file in your web server's base directory.");
    LOG.info("It must be reachable at: http://{}/.well-known/acme-challenge/{}",
      auth.getIdentifier().getDomain(), challenge.getToken());
    LOG.info("File name: {}", challenge.getToken());
    LOG.info("Content: {}", challenge.getAuthorization());
    LOG.info("The file must not contain any leading or trailing whitespaces or line breaks!");
    LOG.info("If you're ready, dismiss the dialog...");

    StringBuilder message = new StringBuilder();
    message.append("Please create a file in your web server's base directory.\n\n");
    message.append("http://")
      .append(auth.getIdentifier().getDomain())
      .append("/.well-known/acme-challenge/")
      .append(challenge.getToken())
      .append("\n\n");
    message.append("Content:\n\n");
    message.append(challenge.getAuthorization());
    acceptChallenge(message.toString());

    return challenge;
  }

  /**
   * Prepares a DNS challenge.
   * <p>
   * The verification of this challenge expects a TXT record with a certain content.
   * <p>
   * This example outputs instructions that need to be executed manually. In a
   * production environment, you would rather configure your DNS automatically.
   *
   * @param auth {@link Authorization} to find the challenge in
   * @return {@link Challenge} to verify
   */
  public default Challenge dnsChallenge(Authorization auth) throws AcmeException {
    // Find a single dns-01 challenge
    Dns01Challenge challenge = auth.findChallenge(Dns01Challenge.TYPE);
    if (challenge == null) {
      throw new AcmeException("Found no " + Dns01Challenge.TYPE + " challenge, don't know what to do...");
    }

    // Output the challenge, wait for acknowledge...
    LOG.info("Please create a TXT record:");
    LOG.info("{} IN TXT {}",
      Dns01Challenge.toRRName(auth.getIdentifier()), challenge.getDigest());
    LOG.info("If you're ready, dismiss the dialog...");

    StringBuilder message = new StringBuilder();
    message.append("Please create a TXT record:\n\n");
    message.append(Dns01Challenge.toRRName(auth.getIdentifier()))
      .append(" IN TXT ")
      .append(challenge.getDigest());
    acceptChallenge(message.toString());

    return challenge;
  }

  /**
   * Presents the instructions for preparing the challenge validation, and waits for
   * dismissal. If the user cancelled the dialog, an exception is thrown.
   *
   * @param message Instructions to be shown in the dialog
   */
  public default void acceptChallenge(String message) throws AcmeException {
    int option = JOptionPane.showConfirmDialog(null,
      message,
      "Prepare Challenge",
      JOptionPane.OK_CANCEL_OPTION);
    if (option == JOptionPane.CANCEL_OPTION) {
      throw new AcmeException("User cancelled the challenge");
    }
  }

  /**
   * Presents the instructions for removing the challenge validation, and waits for
   * dismissal.
   *
   * @param message Instructions to be shown in the dialog
   */
  public default void completeChallenge(String message) throws AcmeException {
    JOptionPane.showMessageDialog(null,
      message,
      "Complete Challenge",
      JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Presents the user a link to the Terms of Service, and asks for confirmation. If the
   * user denies confirmation, an exception is thrown.
   *
   * @param agreement {@link URI} of the Terms of Service
   */
  public default void acceptAgreement(URI agreement) throws AcmeException {
    int option = JOptionPane.showConfirmDialog(null,
      "Do you accept the Terms of Service?\n\n" + agreement,
      "Accept ToS",
      JOptionPane.YES_NO_OPTION);
    if (option == JOptionPane.NO_OPTION) {
      throw new AcmeException("User did not accept Terms of Service");
    }
  }

  /**
   * Invokes this example.
   *
   * @param args Domains to get a certificate for
   */
  public static void main(String... args) {
    LOG.info("Starting up...");

    Security.addProvider(new BouncyCastleProvider());

    Collection<String> domains = List.of("okyc.it", "*.okyc.it");
    try {
      new Client() {
        {
          fetchCertificate(domains);
        }
      };
    } catch (Exception ex) {
      LOG.error("Failed to get a certificate for domains " + domains, ex);
    }
  }

}
