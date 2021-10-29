package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.IPrincipal;
import software.amazon.awscdk.services.route53.PrivateHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;

public class EthereumBeaconChainProps {
    private String environment = null;
    private Vpc appVpc = null;
    private Vpc dmzVpc = null;
    private Vpc devVpc = null;
    private PublicHostedZone publicHostedZone = null;
    private PrivateHostedZone privateHostedZone = null;
    private IPrincipal administrationPrincipal = null;
    private IPeer administrationCidr = null;
    
    static EthereumBeaconChainPropsBuilder builder() {
        return new EthereumBeaconChainPropsBuilder();
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
    public Vpc getDevVpc() {
        return devVpc;
    }
    public void setDevVpc(Vpc devVpc) {
        this.devVpc = devVpc;
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
    public String getEnvironment() {
        return environment;
    }
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public static final class EthereumBeaconChainPropsBuilder {
        private String environment = null;
        private Vpc dmzVpc = null;
        private Vpc appVpc = null;
        private Vpc devVpc = null;
        private PublicHostedZone publicHostedZone = null;
        private PrivateHostedZone privateHostedZone = null;
        private IPrincipal administrationPrincipal = null;
        private IPeer administrationCidr = null;

        public EthereumBeaconChainPropsBuilder environment(String environment) {
            this.environment = environment;
            return this;
        }
        public EthereumBeaconChainPropsBuilder dmzVpc(Vpc dmzVpc) {
            this.dmzVpc = dmzVpc;
            return this;
        }

        public EthereumBeaconChainPropsBuilder appVpc(Vpc appVpc) {
            this.appVpc = appVpc;
            return this;
        }
        public EthereumBeaconChainPropsBuilder devVpc(Vpc devVpc) {
            this.devVpc = devVpc;
            return this;
        }

        public EthereumBeaconChainPropsBuilder publicHostedZone(PublicHostedZone publicHostedZone) {
            this.publicHostedZone = publicHostedZone;
            return this;
        }

        public EthereumBeaconChainPropsBuilder privateHostedZone(PrivateHostedZone privateHostedZone) {
            this.privateHostedZone = privateHostedZone;
            return this;
        }

        public EthereumBeaconChainPropsBuilder administrationPrincipal(IPrincipal administrationPrincipal) {
            this.administrationPrincipal = administrationPrincipal;
            return this;
        }

        public EthereumBeaconChainPropsBuilder administrationCidr(IPeer administrationCidr) {
            this.administrationCidr = administrationCidr;
            return this;
        }
        
        public String getEnvironment() {
            return this.environment;
        }
        
        public Vpc getDmzVpc() {
            return this.dmzVpc;
        }

        public Vpc getAppVpc() {
            return this.appVpc;
        }

        public Vpc getDevVpc() {
            return devVpc;
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
        public EthereumBeaconChainProps build() {
            EthereumBeaconChainProps props = new EthereumBeaconChainProps();
            props.setDmzVpc(this.getDmzVpc());
            props.setAppVpc(this.getAppVpc());
            props.setDevVpc(this.getDevVpc());
            props.setPublicHostedZone(this.getPublicHostedZone());
            props.setPrivateHostedZone(this.getPrivateHostedZone());
            props.setAdministrationPrincipal(this.getAdministrationPrincipal());
            props.setAdministrationCidr(this.getAdministrationCidr());
            props.setEnvironment(this.getEnvironment());
            return props;
        }

    }
}
