packer {
  required_plugins {
    amazon = {
      version = ">= 0.0.1"
      source = "github.com/hashicorp/amazon"
    }
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
  target_ami_name = "lighthouse-${formatdate("YYYYMMDDhhmmss", timestamp())}"
}

source "amazon-ebs" "lighthouse" {
  ami_name             = "${local.target_ami_name}"
  skip_create_ami      = "${var.skip_create_ami}"
  instance_type        = "t3a.small"
  region               = var.region
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
  source_ami_filter {
    filters = {
       virtualization-type = "hvm"
       name = "ubuntu/images/*ubuntu-focal-20.04-amd64-server-*"
       root-device-type = "ebs"
    }
    owners = ["099720109477"]
    most_recent = true
  }
}

build {
  sources = ["source.amazon-ebs.lighthouse"]

  provisioner "shell" {
    inline = [
      "mkdir -p /tmp/bin"
    ]
  }

  provisioner "file" {
    sources = [
      "src/main/resources/bin/general/attach-volume.sh",
      "src/main/resources/bin/general/detach-volume.sh",
      "src/main/resources/bin/general/format-volume.sh",
      "src/main/resources/bin/general/mount-volume.sh",
      "src/main/resources/bin/general/unmount-volume.sh"
    ]
    destination = "/tmp/bin/"
  }

  provisioner "shell" {
    scripts = [
      "ami/scripts/provisioner/install-aws-base.sh",
      "ami/scripts/provisioner/lighthouse/create-users.sh",
      "ami/scripts/provisioner/lighthouse/install-binaries.sh",
      "ami/scripts/provisioner/lighthouse/install-scripts.sh",
      "ami/scripts/provisioner/lighthouse/install-service.sh",
      "ami/scripts/provisioner/prometheus/install-prometheus.sh"
    ]
  }
}