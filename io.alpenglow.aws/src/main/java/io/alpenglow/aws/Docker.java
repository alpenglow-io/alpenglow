package io.alpenglow.aws;

public enum Docker {
  Companion;

  public final String install = """
    sudo apt-get update &&
    sudo apt-get install \\
                ca-certificates \\
                curl \\
                gnupg \\
                lsb-release \\
                -y &&
    sudo mkdir -p /etc/apt/keyrings &&
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg > docker &&
    gpg --no-tty --dearmor docker > /dev/null &&
    sudo mv ./docker.gpg /etc/apt/keyrings &&
    echo \\
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \\
      $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null &&
    sudo apt-get update &&
    sudo apt-get install docker-ce docker-ce-cli containerd.io docker-compose-plugin -y &&
    sudo usermod -aG docker $USER
    """;
}
