packer {
  required_plugins {
    amazon = {
      version = ">= 0.0.1"
      source = "github.com/hashicorp/amazon"
    }
  }
}

variable "source_amis" {
  type = map(string)
  default = {
    "us-east-1": "ami-0db6c6238a40c0681",
    "us-east-2": "ami-03b6c8bd55e00d5ed"
  }
}

variable "region" {
  type = string
  default = "us-east-1"
}

variable "skip_create_ami" {
  type = bool
  default = false
}

locals {
  target_ami_name = "goeth-${formatdate("YYYYMMDDhhmmss", timestamp())}"
  skip_create_ami = false
}

source "amazon-ebs" "geth" {
  ami_name             = "${local.target_ami_name}"
  skip_create_ami      = "${local.skip_create_ami}"
  instance_type        = "t3a.small"
  region               = var.region
  source_ami           = "${lookup(var.source_amis, var.region, "")}"
  ssh_username         = "ubuntu"
  ssh_interface        = "public_dns"
  communicator         = "ssh"
  associate_public_ip_address = true
  temporary_iam_instance_profile_policy_document {
      Statement {
          Action   = ["logs:*"]
          Effect   = "Allow"
          Resource = ["*"]
      }
      Version = "2012-10-17"
  }
}

build {
  sources = ["source.amazon-ebs.geth"]

  provisioner "shell" {
    inline = [
      "mkdir -p /tmp/units",
      "mkdir -p /tmp/bin"
    ]
  }

  provisioner "file" {
    source = "src/main/resources/units/geth.service"
    destination = "/tmp/units/"
  }

  provisioner "file" {
    sources = [
      "src/main/resources/bin/attach-goeth-volume.sh",
      "src/main/resources/bin/detach-goeth-volume.sh",
      "src/main/resources/bin/format-goeth-volume.sh",
      "src/main/resources/bin/mount-goeth-volume.sh",
      "src/main/resources/bin/unmount-goeth-volume.sh"
    ]
    destination = "/tmp/bin/"
  }

  provisioner "shell" {
    scripts = [
      "ami/scripts/provisioner/install-aws-base.sh",
      "ami/scripts/provisioner/geth/create-users.sh",
      "ami/scripts/provisioner/geth/install-binaries.sh",
      "ami/scripts/provisioner/geth/install-scripts.sh",
      "ami/scripts/provisioner/geth/install-service.sh"
    ]
  }
}