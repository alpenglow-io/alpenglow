package io.alpenglow.aws;

import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.*;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupIngressArgs;
import com.pulumi.command.remote.CopyFile;
import com.pulumi.command.remote.CopyFileArgs;
import com.pulumi.command.remote.inputs.ConnectionArgs;
import com.pulumi.resources.CustomResourceOptions;

import java.util.Arrays;
import java.util.Base64;

enum Namespace implements Main {
  ;
  static final String PUBLIC_KEY = System.getenv("LOCAL_PUBLICKEY");
  static final String PRIVATE_KEY = System.getenv("LOCAL_PRIVATEKEY");
}

public sealed interface Main {
  static void main(String[] args) {
    Pulumi.run(aws -> {
      final var config = aws.config();
      /*
      final var privateKey = config.requireSecret("privateKey").applyValue(it ->
        it.startsWith(RSA_PRIVATE_KEY) ? it : Arrays.toString(Base64.getDecoder().decode(it))
      );
      *
       */

      final var vpc = new DefaultVpc("demo-vpc");
      final var subnet = new DefaultSubnet("demo-subnet", DefaultSubnetArgs.builder().availabilityZone("eu-south-1a").build());
      final var securityGroup = new DefaultSecurityGroup("demo-security-group",
        DefaultSecurityGroupArgs.builder()
          .vpcId(vpc.getId())
          .ingress(
            DefaultSecurityGroupIngressArgs.builder()
              .protocol("tcp")
              .fromPort(22)
              .toPort(22)
              .cidrBlocks("0.0.0.0/0")
              .build(),
            DefaultSecurityGroupIngressArgs.builder()
              .protocol("tcp")
              .fromPort(80)
              .toPort(80)
              .cidrBlocks("0.0.0.0/0")
              .build()
          )
          .build()
      );

      final var keyPair = new KeyPair("demo-keypair", KeyPairArgs.builder()
        .publicKey(Namespace.PUBLIC_KEY)
        .build()
      );

      securityGroup.getId().applyValue(securityGroupId -> {
        final var ec2 = new Instance("demo-server",
          InstanceArgs.builder()
            .ami("ami-0fdd5958b81f36e68") // Ubuntu Server 22.04 AMD64
            .instanceType("t3.micro")
            .subnetId(subnet.getId())
            .keyName(keyPair.keyName())
            .vpcSecurityGroupIds(securityGroupId)
            .build()
        );

        final var connection = ConnectionArgs.builder()
          .host(ec2.publicIp())
          .user("ubuntu")
          .privateKey(Namespace.PRIVATE_KEY)
          .build();

        final var copyAdoptium = new CopyFile("adoptium",
          CopyFileArgs.builder()
            .connection(connection)
            .localPath("/home/guada/sdk/adoptium.png")
            .remotePath("adoptium.png")
            .build()
        );

        final var copyLogo = new CopyFile("adoptium-logo",
          CopyFileArgs.builder()
            .connection(connection)
            .localPath("/home/guada/sdk/adoptium_logo.svg")
            .remotePath("adoptium_logo.svg")
            .build(),
          CustomResourceOptions.builder()
            .dependsOn(copyAdoptium)
            .build()
        );

        aws.export("copiedLogo", copyLogo.remotePath());
        return "none";
      });

    });
  }
}
