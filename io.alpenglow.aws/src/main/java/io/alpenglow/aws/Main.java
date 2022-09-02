package io.alpenglow.aws;

import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.*;

enum Namespace implements Main {
  ;
  static final String PUBLIC_KEY = System.getenv("LOCAL_PUBLICKEY");
}

public sealed interface Main {
  static void main(String[] args) {
    Pulumi.run(aws -> {
      final var vpc = new DefaultVpc("demo-vpc");
      final var subnet = new DefaultSubnet("demo-subnet", DefaultSubnetArgs.builder().availabilityZone("eu-south-1a").build());
      final var securityGroup = new DefaultSecurityGroup("demo-security-group",
        DefaultSecurityGroupArgs.builder()
          .vpcId(vpc.getId())
          .build()
      );

      final var keyPair = new KeyPair("demo-keypair", KeyPairArgs.builder()
        .publicKey(Namespace.PUBLIC_KEY)
        .build()
      );

      final var ec2 = securityGroup.getId().applyValue(securityGroupId ->
        new Instance("demo-server",
          InstanceArgs.builder()
            .ami("ami-0fdd5958b81f36e68") // Ubuntu Server 22.04 AMD64
            .instanceType("t3.micro")
            .subnetId(subnet.getId())
            .keyName(keyPair.keyName())
            .vpcSecurityGroupIds(securityGroupId)
            .build()
        )
      );
    });
  }
}
