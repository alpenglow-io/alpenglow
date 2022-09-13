package io.alpenglow.aws;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.asset.FileAsset;
import com.pulumi.aws.ec2.*;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupEgressArgs;
import com.pulumi.aws.ec2.inputs.DefaultSecurityGroupIngressArgs;
import com.pulumi.aws.s3.*;
import com.pulumi.aws.s3.inputs.BucketWebsiteArgs;
import com.pulumi.command.remote.Command;
import com.pulumi.command.remote.CommandArgs;
import com.pulumi.command.remote.inputs.ConnectionArgs;
import com.pulumi.core.Output;
import com.pulumi.resources.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.alpenglow.aws.Namespace.PRIVATE_KEY;
import static io.alpenglow.aws.Namespace.PUBLIC_KEY;

enum Namespace implements Main {
  ;
  static final String PUBLIC_KEY = System.getenv("LOCAL_PUBLICKEY");
  static final String PRIVATE_KEY = System.getenv("LOCAL_PRIVATEKEY");
}

public sealed interface Main {
  static void main(String[] args) {
    Pulumi.run(aws -> {
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

      var bucket = new Bucket("demo-bucket",
        BucketArgs.builder()
          .website(
            BucketWebsiteArgs.builder()
              .indexDocument("index.html")
              .build()
          )
          .build(),
        CustomResourceOptions.builder()
          .dependsOn(installDocker)
          .build()
      );

      var resource = new BucketObject("assets",
        BucketObjectArgs.builder()
          .bucket(bucket.bucket())
          .acl("public-read")
          .source(new FileAsset("/dev/null"))
          .key("assets/")
          .build(),
        CustomResourceOptions.builder()
          .parent(bucket)
          .build()
      );

      var policy = new BucketPolicy("demo-bucket-policy",
        BucketPolicyArgs.builder()
          .bucket(bucket.bucket())
          .policy(bucket.bucket().applyValue("""
            {
              "Version": "2012-10-17",
              "Statement": [{
                "Effect": "Allow",
                "Principal": "*",
                "Action": ["s3:GetObject"],
                "Resource": ["arn:aws:s3:::%s/*"]
              }]
            }
            """::formatted)
          )
          .build()
      );

      try (
        final var dist = Files.walk(Path.of("/home/guada/prjs/alpenglow-io/io.alpenglow/io.alpenglow.webapp/dist/"), 1);
        final var assets = Files.walk(Path.of("/home/guada/prjs/alpenglow-io/io.alpenglow/io.alpenglow.webapp/dist/assets"), 1)
      ) {
        walk(aws, bucket, dist, null);
        walk(aws, bucket, assets, resource);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      aws.export("ec2PublicDNS", ec2.publicDns());
      aws.export("bucketEndpoint", bucket.websiteEndpoint());
      aws.export("policy", policy.getId());
    });
  }

  static void walk(Context aws, Bucket bucket, Stream<Path> dist, Resource resource) throws IOException {
    for (var path : dist.toList()) {
      if (Files.isRegularFile(path) && resource == null) {
        var object = new BucketObject(path.toFile().getName(),
          BucketObjectArgs.builder()
            .bucket(bucket.bucket())
            .contentType(Files.probeContentType(path))
            .acl("public-read")
            .source(new FileAsset(path.toString()))
            .build(),
          CustomResourceOptions.builder()
            .parent(bucket)
            .build()
        );

        aws.export(path.toFile().getName(), object.bucket());
      } else if (Files.isRegularFile(path) && resource != null) {
        var object = new BucketObject(path.toFile().getName(),
          BucketObjectArgs.builder()
            .bucket(bucket.bucket())
            .contentType(Files.probeContentType(path))
            .acl("public-read")
            .source(new FileAsset(path.toString()))
            .build(),
          CustomResourceOptions.builder()
            .parent(resource)
            .build()
        );

        aws.export(path.toFile().getName(), object.bucket());
      }
    }
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

sealed interface S3 {
  static ComponentResource folder(String name, ComponentResourceOptions options) {
    return new Folder(name, options);
  }
}

final class Folder extends ComponentResource implements S3 {
  private final Output<String> bucket;

  public Folder(String name, ComponentResourceOptions options) {
    super("example:S3Folder", name, ResourceArgs.Empty, options);

    var bucket = new Bucket(name, BucketArgs.Empty, CustomResourceOptions.builder().parent(this).build());
    this.bucket = bucket.bucket();

    this.registerOutputs(Map.of());
  }
}

