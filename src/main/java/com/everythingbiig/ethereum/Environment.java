package com.everythingbiig.ethereum;

public class Environment {

    static {
        String env = System.getenv("BEACON_CHAIN_ENV");
        System.out.println("BEACON_CHAIN_ENV="+env);
        if (!(("dev".equals(env) || "testnet".equals(env) || "mainnet".equals(env)))) {
            throw new IllegalArgumentException("BEACON_CHAIN_ENV must be one of [dev|testnet|mainnet]");
        }
    }

    private Environment() {
    }

    public static String env() {
        return System.getenv("BEACON_CHAIN_ENV");
    }

    public static boolean createBastion() {
        return "false".equals(System.getenv("CREATE_BASTION"));
    }

    public static String bastionAllowedCidr() {
        return System.getenv("BASTION_ALLOWED_CIDR");
    }

    public static String bastionRecordName() {
        return System.getenv("BASTION_RECORD_NAME");
    }

    public static String dmzVpcCidr() {
        return System.getenv("DMZ_VPC_CIDR");
    }

    public static String appVpcCidr() {
        return System.getenv("APP_VPC_CIDR");
    }

    public static String azCount() {
        return System.getenv("AZ_COUNT");
    }

    public static String publicHostedZone() {
        return System.getenv("PUBLIC_HOSTED_ZONE");
    }

    public static String privateHostedZone() {
        return System.getenv("PRIVATE_HOSTED_ZONE");
    }

    public static String lighthouseAmi() {
        return System.getenv("LIGHTHOUSE_AMI");
    }

    public static String lighthouseRecordName() {
        return System.getenv("LIGHTHOUSE_RECORD_NAME");
    }

    public static String lighthouseKeyPair() {
        return System.getenv("LIGHTHOUSE_KEY_PAIR");
    }

    public static String goethAmi() {
        return System.getenv("GOETH_AMI");
    }

    public static String goethRecordName() {
        return System.getenv("GOETH_RECORD_NAME");
    }

    public static String goethKeyPair() {
        return System.getenv("GOETH_KEY_PAIR");
    }
}
