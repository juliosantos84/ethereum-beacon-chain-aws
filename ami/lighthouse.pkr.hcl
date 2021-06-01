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

source "amazon-ebs" "lighthouse" {
  ami_name             = "lighthouse {{timestamp}}"
  instance_type        = "t3a.small"
  region               = var.region
  source_ami           = "${lookup(var.source_amis, var.region, "")}"
  ssh_username         = "ubuntu"
  ssh_interface        = "session_manager"
  communicator         = "ssh"
  # iam_instance_profile = "myinstanceprofile"
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
  sources = ["source.amazon-ebs.lighthouse"]

  provisioner "shell" {
    inline = ["echo Connected via SSM at '${build.User}@${build.Host}:${build.Port}'"]
  }
}