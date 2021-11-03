package com.everythingbiig.ethereum;

import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.route53.PrivateHostedZone;
import software.amazon.awscdk.services.route53.PublicHostedZone;

public class EthereumBeaconChainProps {
    private String beaconChainEnvironment = null;
    private Vpc appVpc = null;
    private PublicHostedZone publicHostedZone = null;
    private PrivateHostedZone privateHostedZone = null;
    
    static EthereumBeaconChainPropsBuilder builder() {
        return new EthereumBeaconChainPropsBuilder();
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
    public Duration getTargetRegistrationDelay() {
        return Duration.seconds(15);
    }
    public String getBeaconChainEnvironment() {
        return beaconChainEnvironment;
    }
    public void setBeaconChainEnvironment(String beaconChainEnvironment) {
        this.beaconChainEnvironment = beaconChainEnvironment;
    }

    public static final class EthereumBeaconChainPropsBuilder {
        private String beaconChainEnvironment = null;
        private Vpc appVpc = null;
        private PublicHostedZone publicHostedZone = null;
        private PrivateHostedZone privateHostedZone = null;

        public EthereumBeaconChainPropsBuilder beaconChainEnvironment(String beaconChainEnvironment) {
            this.beaconChainEnvironment = beaconChainEnvironment;
            return this;
        }

        public EthereumBeaconChainPropsBuilder appVpc(Vpc appVpc) {
            this.appVpc = appVpc;
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
        
        public String getBeaconChainEnvironment() {
            return this.beaconChainEnvironment;
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

        public EthereumBeaconChainProps build() {
            EthereumBeaconChainProps props = new EthereumBeaconChainProps();
            props.setAppVpc(this.getAppVpc());
            props.setPublicHostedZone(this.getPublicHostedZone());
            props.setPrivateHostedZone(this.getPrivateHostedZone());
            props.setBeaconChainEnvironment(this.getBeaconChainEnvironment());
            return props;
        }

    }
}
