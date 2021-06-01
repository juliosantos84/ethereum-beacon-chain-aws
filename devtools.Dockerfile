FROM hashicorp/packer:latest as packer

FROM ubuntu:focal

COPY --from=packer /bin/packer /bin/packer

