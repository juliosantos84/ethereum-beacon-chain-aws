package com.everythingbiig.ethereum;

import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.route53.PrivateHostedZone;

public class GoethProps {
    private Vpc targetVpc = null;
    private PrivateHostedZone privateHostedZone = null;
    private IPeer administrationCidr = null;
    
    static GoethPropsBuilder builder() {
        return new GoethPropsBuilder();
    }
    public Vpc getTargetVpc() {
        return targetVpc;
    }
    public void setTargetVpc(Vpc targetVpc) {
        this.targetVpc = targetVpc;
    }
    public PrivateHostedZone getPrivateHostedZone() {
        return privateHostedZone;
    }
    public void setPrivateHostedZone(PrivateHostedZone privateHostedZone) {
        this.privateHostedZone = privateHostedZone;
    }
    public IPeer getAdministrationCidr() {
        return administrationCidr;
    }
    public void setAdministrationCidr(IPeer administrationCidr) {
        this.administrationCidr = administrationCidr;
    }
    public static final class GoethPropsBuilder {
        private Vpc targetVpc = null;
        private PrivateHostedZone privateHostedZone = null;
        private IPeer administrationCidr = null;

        public GoethPropsBuilder targetVpc(Vpc targetVpc) {
            this.targetVpc = targetVpc;
            return this;
        }

        public GoethPropsBuilder privateHostedZone(PrivateHostedZone privateHostedZone) {
            this.privateHostedZone = privateHostedZone;
            return this;
        }

        public GoethPropsBuilder administrationCidr(IPeer administrationCidr) {
            this.administrationCidr = administrationCidr;
            return this;
        }

        public Vpc getTargetVpc() {
            return this.targetVpc;
        }

        public PrivateHostedZone getPrivateHostedZone() {
            return this.privateHostedZone;
        }

        public IPeer getAdministrationCidr() {
            return this.administrationCidr;
        }

        public GoethProps build() {
            GoethProps props = new GoethProps();
            props.setTargetVpc(this.getTargetVpc());
            props.setPrivateHostedZone(this.getPrivateHostedZone());
            props.setAdministrationCidr(this.getAdministrationCidr());
            return props;
        }

    }
}
