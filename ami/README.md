# AMIs

## requirements
- packer && terraform
```bash
brew tap hashicorp/tap && brew install hashicorp/tap/packer && brew install hashicorp/tap/terraform
```
- HCL extension for Visual Studio
- Hashicorp Terraform extension for Visual Studio

## configuration
- configure TF language server `terraform plan && terraform-ls`
- init the providers `packer init lighthouse.pkr.hcl`

## building
- build `bin/build-images.sh`