package io.alpenglow.acme;


import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;

import javax.swing.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.util.Locale;

import static java.util.Objects.requireNonNull;
import static org.shredzone.acme4j.Status.*;

interface Acme {
  String userhome = System.getProperty("user.home");
  String ACCOUNT_OKYC_IT_PEM = "account.okyc.it.pem";
  String DOMAIN_OKYC_IT_PEM = "domain.okyc.it.pem";
  String OKYC_IT_CSR = "okyc.it.csr";
  String OKYC_IT_CRT = "okyc.it.crt";
  Path okycItPem = Path.of(userhome, ".ssh", ACCOUNT_OKYC_IT_PEM);
  Path okycItDomainPem = Path.of(userhome, ".ssh", DOMAIN_OKYC_IT_PEM);
  Path okycItCsr = Path.of(userhome, ".ssh", OKYC_IT_CSR);
  Path okycItCrt = Path.of(userhome, ".ssh", OKYC_IT_CRT);
  String SECP_256_R_1 = "secp256r1";

  static void main(String... args) throws AcmeException, InterruptedException, IOException {
    Security.addProvider(new BouncyCastleProvider());

    var session = new Session("acme://letsencrypt.org/staging");
    session.setLocale(Locale.ITALY);
    var meta = session.getMetadata();
    var tos = meta.getTermsOfService();
    var website = meta.getWebsite();

    //var accountKeyPair = KeyPairUtils.createECKeyPair("secp256r1");

    KeyPair accountKeyPair;
    if (Files.notExists(okycItPem)) {
      try (var file = new FileWriter(okycItPem.toFile())) {
        accountKeyPair = KeyPairUtils.createECKeyPair("secp256r1");
        KeyPairUtils.writeKeyPair(accountKeyPair, file);
      } catch (IOException e) {
        Files.deleteIfExists(okycItPem);
        throw new RuntimeException(e);
      }
    } else {
      try (var file = new FileReader(okycItPem.toFile())) {
        accountKeyPair = KeyPairUtils.readKeyPair(file);
      }
    }
    var account = new AccountBuilder()
      .addContact("mailto:team@cherrychain.it")
      .agreeToTermsOfService()
      .useKeyPair(accountKeyPair)
      .create(session);

    var location = account.getLocation();

    var loggedIn = session.login(location, accountKeyPair);

    var inAccount = loggedIn.getAccount();

    var order = inAccount.newOrder()
      .domains(
        "okyc.it",
        "*.okyc.it"
      )
      //.notAfter(Instant.now().plus(Duration.ofDays(1)))
      .create();

    var domainKeyPair = KeyPairUtils.createECKeyPair(SECP_256_R_1);
    var csrBuilder = new CSRBuilder();
    csrBuilder.addDomains(
      "okyc.it",
      "*.okyc.it"
    );
    csrBuilder.setOrganization("cetif");
    csrBuilder.sign(domainKeyPair);
    var csr = csrBuilder.getEncoded();
    csrBuilder.write(new FileWriter(okycItCsr.toFile()));

    System.out.println("Authorizations: " + order.getAuthorizations().size());
    for (var authorization : order.getAuthorizations()) {
      if (authorization.getStatus() == PENDING) {
        Dns01Challenge challenge = authorization.findChallenge(Dns01Challenge.class);
        String domain = authorization.getIdentifier().getDomain();
        String digest = requireNonNull(challenge).getDigest();
        String rrName = Dns01Challenge.toRRName(authorization.getIdentifier());
        System.out.println(digest + " for domain " + domain + " for rr-name " + rrName);

        int option = JOptionPane.showConfirmDialog(null,
          "Accetta la sfida",
          "Prepare Challenge",
          JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) {
          throw new AcmeException("User cancelled the challenge");
        }
        System.out.println("Parto!");

        if (challenge.getStatus() == VALID) break;

        challenge.trigger();

        if (authorization.getStatus() == Status.VALID) {
          break;
        }

        //noinspection RedundantExplicitVariableType
        int attempts = 10;
        while (challenge.getStatus() != Status.VALID && attempts-- > 0) {
          if (challenge.getStatus() == INVALID) {
            var json = requireNonNull(challenge.getError()).asJSON();
            System.out.println("Invalid: " + json);
            System.out.println(challenge.getJSON());
          }
          Thread.sleep(3000);
          challenge.update();
        }
      }
    }
    order.execute(csr);

    var certificate = order.getCertificate();
    try (var file = new FileWriter(OKYC_IT_CRT)) {
      requireNonNull(certificate).writeCertificate(file);
    }
  }
}
