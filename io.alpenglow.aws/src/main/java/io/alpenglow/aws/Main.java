package io.alpenglow.aws;

import com.pulumi.Pulumi;
import com.pulumi.aws.appstream.ImageBuilder;
import com.pulumi.aws.appstream.ImageBuilderArgs;
import com.pulumi.aws.ec2.*;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupIngressArgs;
import com.pulumi.command.remote.Command;
import com.pulumi.command.remote.CommandArgs;
import com.pulumi.command.remote.inputs.ConnectionArgs;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.Resource;
import com.pulumi.resources.StackReference;
import com.pulumi.resources.StackReferenceArgs;

import java.util.List;

import static io.alpenglow.aws.Namespace.*;

enum Namespace implements Main {
  ;
  static final String PUBLIC_KEY = System.getenv("LOCAL_PUBLICKEY");
  static final String PRIVATE_KEY = System.getenv("LOCAL_PRIVATEKEY");
  static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
  static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
  static final String AWS_REGION = System.getenv("AWS_REGION");
}

public sealed interface Main {
  static void main(String[] args) {
    Pulumi.run(aws -> {
      System.out.format("AWS Secret %s, Access %s, Region %s\n", AWS_SECRET_ACCESS_KEY, AWS_ACCESS_KEY, AWS_REGION);

      final var config = aws.config();
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
          .egress(
            DefaultSecurityGroupEgressArgs.builder()
              .protocol("-1")
              .fromPort(0)
              .toPort(0)
              .cidrBlocks("0.0.0.0/0")
              .build()
          )
          .build()
      );

      final var keyPair = new KeyPair("demo-keypair", KeyPairArgs.builder()
        .publicKey(PUBLIC_KEY)
        .build()
      );

      final var ec2 = new Instance("demo-server",
        InstanceArgs.builder()
          .ami("ami-0fdd5958b81f36e68") // Ubuntu Server 22.04 AMD64
          .instanceType("t3.micro")
          .subnetId(subnet.getId())
          .keyName(keyPair.keyName())
          .vpcSecurityGroupIds(securityGroup.getId().applyValue(List::of))
          .build()
      );

      final var connection =
        ConnectionArgs.builder()
          .host(ec2.publicIp())
          .user("ubuntu")
          .privateKey(PRIVATE_KEY)
          .build();

      var upgrade = command("upgrade", "sudo apt update && sudo apt dist-upgrade -y && sudo apt autoremove -y", connection, ec2);
      var installDocker = command("install-docker", Docker.Companion.install, connection, upgrade);

      aws.export("ec2PublicDNS", ec2.publicDns());
    });
  }

  static Command command(String name, String terminal, ConnectionArgs connection, Resource resource) {
    return new Command(name,
      CommandArgs.builder()
        .connection(connection)
        .create(terminal)
        .build(),
      CustomResourceOptions.builder()
        .dependsOn(resource)
        .build()
    );
  }
}
