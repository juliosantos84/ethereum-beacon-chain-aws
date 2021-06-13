package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.route53.PrivateHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;

public class EthereumStackProps {
    private Vpc appVpc = null;
    private Vpc dmzVpc = null;
    private PublicHostedZone publicHostedZone = null;
    private PrivateHostedZone privateHostedZone = null;
    private IPrincipal administrationPrincipal = null;
    private IPeer administrationCidr = null;
    
    static EthereumPropsBuilder builder() {
        return new EthereumPropsBuilder();
    }
    public Vpc getDmzVpc() {
        return dmzVpc;
    }
    public void setDmzVpc(Vpc dmzVpc) {
        this.dmzVpc = dmzVpc;
    }
    public Vpc getAppVpc() {
        return appVpc;
    }
    public void setAppVpc(Vpc appVpc) {
        this.appVpc = appVpc;
    }
    public PublicHostedZone getPublicHostedZone() {
        return publicHostedZone;
    }
    public void setPublicHostedZone(PublicHostedZone publicHostedZone) {
        this.publicHostedZone = publicHostedZone;
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
    public IPrincipal getAdministrationPrincipal() {
        return administrationPrincipal;
    }
    public void setAdministrationPrincipal(IPrincipal administrationPrincipal) {
        this.administrationPrincipal = administrationPrincipal;
    }
    public Duration getTargetRegistrationDelay() {
        return Duration.seconds(15);
    }
    public Integer getAdministrationPort() {
        return Integer.valueOf(22);
    }
    
    public static final class EthereumPropsBuilder {
        private Vpc dmzVpc = null;
        private Vpc appVpc = null;
        private PublicHostedZone publicHostedZone = null;
        private PrivateHostedZone privateHostedZone = null;
        private IPrincipal administrationPrincipal = null;
        private IPeer administrationCidr = null;

        public EthereumPropsBuilder dmzVpc(Vpc dmzVpc) {
            this.dmzVpc = dmzVpc;
            return this;
        }

        public EthereumPropsBuilder appVpc(Vpc appVpc) {
            this.appVpc = appVpc;
            return this;
        }

        public EthereumPropsBuilder publicHostedZone(PublicHostedZone publicHostedZone) {
            this.publicHostedZone = publicHostedZone;
            return this;
        }

        public EthereumPropsBuilder privateHostedZone(PrivateHostedZone privateHostedZone) {
            this.privateHostedZone = privateHostedZone;
            return this;
        }

        public EthereumPropsBuilder administrationPrincipal(IPrincipal administrationPrincipal) {
            this.administrationPrincipal = administrationPrincipal;
            return this;
        }

        public EthereumPropsBuilder administrationCidr(IPeer administrationCidr) {
            this.administrationCidr = administrationCidr;
            return this;
        }

        public Vpc getDmzVpc() {
            return dmzVpc;
        }

        public Vpc getAppVpc() {
            return this.appVpc;
        }

        public PublicHostedZone getPublicHostedZone() {
            return this.publicHostedZone;
        }

        public PrivateHostedZone getPrivateHostedZone() {
            return this.privateHostedZone;
        }

        public IPeer getAdministrationCidr() {
            return this.administrationCidr;
        }

        public IPrincipal getAdministrationPrincipal() {
            return administrationPrincipal;
        }
        public void setAdministrationPrincipal(IPrincipal administrationPrincipal) {
            this.administrationPrincipal = administrationPrincipal;
        }

        public EthereumStackProps build() {
            EthereumStackProps props = new EthereumStackProps();
            props.setDmzVpc(this.getDmzVpc());
            props.setAppVpc(this.getAppVpc());
            props.setPublicHostedZone(this.getPublicHostedZone());
            props.setPrivateHostedZone(this.getPrivateHostedZone());
            props.setAdministrationPrincipal(this.getAdministrationPrincipal());
            props.setAdministrationCidr(this.getAdministrationCidr());
            return props;
        }

    }
}
