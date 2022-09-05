package io.alpenglow.aws;

import com.pulumi.Pulumi;
import com.pulumi.aws.ec2.DefaultSecurityGroup;
import com.pulumi.aws.ec2.DefaultSecurityGroupArgs;
import com.pulumi.aws.ec2.DefaultSubnet;
import com.pulumi.aws.ec2.DefaultSubnetArgs;
import com.pulumi.aws.ec2.DefaultVpc;
import com.pulumi.aws.ec2.Instance;
import com.pulumi.aws.ec2.InstanceArgs;
import com.pulumi.aws.ec2.KeyPair;
import com.pulumi.aws.ec2.KeyPairArgs;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupIngressArgs;
import com.pulumi.command.remote.Command;
import com.pulumi.command.remote.CommandArgs;
import com.pulumi.command.remote.CopyFile;
import com.pulumi.command.remote.CopyFileArgs;
import com.pulumi.command.remote.inputs.ConnectionArgs;
import com.pulumi.resources.CustomResourceOptions;
import com.pulumi.resources.Resource;

import java.util.List;

import static io.alpenglow.aws.Namespace.AWS_ACCESS_KEY;
import static io.alpenglow.aws.Namespace.AWS_REGION;
import static io.alpenglow.aws.Namespace.AWS_SECRET_ACCESS_KEY;
import static io.alpenglow.aws.Namespace.LOCAL_FILE1;
import static io.alpenglow.aws.Namespace.LOCAL_FILE2;
import static io.alpenglow.aws.Namespace.PRIVATE_KEY;
import static io.alpenglow.aws.Namespace.PUBLIC_KEY;

enum Namespace implements Main {
  ;
  static final String PUBLIC_KEY = System.getenv("LOCAL_PUBLICKEY");
  static final String PRIVATE_KEY = System.getenv("LOCAL_PRIVATEKEY");
  static final String AWS_SECRET_ACCESS_KEY = System.getenv("AWS_SECRET_ACCESS_KEY");
  static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
  static final String AWS_REGION = System.getenv("AWS_REGION");
  static final String LOCAL_FILE1 = System.getenv("LOCAL_FILE1");
  static final String LOCAL_FILE2 = System.getenv("LOCAL_FILE2");
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

      final var copyFile1 = new CopyFile("file-1",
        CopyFileArgs.builder()
          .connection(connection)
          .localPath(LOCAL_FILE1)
          .remotePath("remote_file1.jpg")
          .build(),
        CustomResourceOptions.builder()
          .dependsOn(ec2)
          .build()
      );

      final var copyFile2 = new CopyFile("file-2",
        CopyFileArgs.builder()
          .connection(connection)
          .localPath(LOCAL_FILE2)
          .remotePath("remote_file2.jpg")
          .build(),
        CustomResourceOptions.builder()
          .dependsOn(copyFile1)
          .build()
      );

      final var aptUpdate = new Command("apt-update",
        CommandArgs.builder()
          .connection(connection)
          .create("sudo apt-get update")
          .build(),
        CustomResourceOptions.builder()
          .dependsOn(copyFile2)
          .build()
      );

      final var aptInstallCerts = new Command("apt-install-certs",
        CommandArgs.builder()
          .connection(connection)
          .create("""
            sudo apt-get install \\
                ca-certificates \\
                curl \\
                gnupg \\
                lsb-release \\
                -y
            """)
          .build(),
        CustomResourceOptions.builder()
          .dependsOn(aptUpdate)
          .build()
      );

      final var mkdir = command("mkdir", "sudo mkdir -p /etc/apt/keyrings", connection, aptInstallCerts);
      final var curl = command("curl", "curl -fsSL https://download.docker.com/linux/ubuntu/gpg > docker", connection, mkdir);
      final var gpg = command("gpg", "gpg --no-tty --dearmor docker > /dev/null", connection, curl);
      final var mv = command("mv", "sudo mv ./docker.gpg /etc/apt/keyrings", connection, gpg);
      final var echo = command("echo", """
        echo \\
              "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \\
              $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
        """, connection, mv);

      final var aptUpdate2 = command("apt-update-2", "sudo apt-get update", connection, echo);
      final var aptInstall2 = command("apt-install-2", "sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin -y", connection, aptUpdate2);
      final var groupAdd = command("sudoer", "sudo usermod -aG docker $USER", connection, aptInstall2);

      aws.export("groupAdd", groupAdd.stdout());
    });
  }

  static Command command(String name, String terminal, ConnectionArgs connection, Resource dependency) {
    return new Command(name,
      CommandArgs.builder()
        .connection(connection)
        .create(terminal)
        .build(),
      CustomResourceOptions.builder()
        .dependsOn(dependency)
        .build()
    );
  }
}
