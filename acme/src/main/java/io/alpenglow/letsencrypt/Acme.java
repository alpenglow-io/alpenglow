package io.alpenglow.letsencrypt;


import io.alpenglow.letsencrypt.sec.Pem;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.letsencrypt.LetsEncrypt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

interface Acme {
  String userhome = System.getProperty("user.home");
  String ACCOUNT_PEM = "account.okyc.it.pem";
  String DOMAIN_PEM = "domain.okyc.it.pem";
  String DOMAIN_CSR = "domain.okyc.it.csr";
  String DOMAIN_CRT = "domain.okyc.it.crt";
  Path accountPem = Path.of(userhome, ".ssh", ACCOUNT_PEM);
  Path domainPem = Path.of(userhome, ".ssh", DOMAIN_PEM);
  Path domainCsr = Path.of(userhome, ".ssh", DOMAIN_CSR);
  Path domainCrt = Path.of(userhome, ".ssh", DOMAIN_CRT);
  String SECP_256_R_1 = "secp256r1";

  static void main(String... args) {
    Security.addProvider(new BouncyCastleProvider());

    LetsEncrypt.stage()
      .account(Pem.read(accountPem).orCreate())
      .order("okyc.it", "*.okyc.it")
      .challenge((recordName, digest) -> System.out.format("%s: %s\n", recordName, digest))
      .sign(Pem.read(domainPem).orCreate())
      .certificate(() -> Files.newBufferedWriter(domainCrt));
  }
}
