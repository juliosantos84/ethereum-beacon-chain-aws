package com.everythingbiig.ethereum;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Size;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.core.Tags;
import software.amazon.awscdk.services.autoscaling.ApplyCloudFormationInitOptions;
import software.amazon.awscdk.services.autoscaling.AutoScalingGroup;
import software.amazon.awscdk.services.autoscaling.RollingUpdateOptions;
import software.amazon.awscdk.services.autoscaling.Signals;
import software.amazon.awscdk.services.autoscaling.SignalsOptions;
import software.amazon.awscdk.services.autoscaling.UpdatePolicy;
import software.amazon.awscdk.services.cloudwatch.Alarm;
import software.amazon.awscdk.services.cloudwatch.ComparisonOperator;
import software.amazon.awscdk.services.cloudwatch.Metric;
import software.amazon.awscdk.services.cloudwatch.Statistic;
import software.amazon.awscdk.services.ec2.CloudFormationInit;
import software.amazon.awscdk.services.ec2.IMachineImage;
import software.amazon.awscdk.services.ec2.IPeer;
import software.amazon.awscdk.services.ec2.IVolume;
import software.amazon.awscdk.services.ec2.InitCommand;
import software.amazon.awscdk.services.ec2.InitCommandOptions;
import software.amazon.awscdk.services.ec2.InitElement;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.LookupMachineImageProps;
import software.amazon.awscdk.services.ec2.MachineImage;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Volume;
import software.amazon.awscdk.services.elasticloadbalancingv2.AddApplicationTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;



public class EthereumBeaconChainNode extends Stack {
    static final Integer    GOETH_PORT                  = Integer.valueOf(30303);
    static final Integer    GOETH_RPC_PORT                  = Integer.valueOf(8545);
    static final Integer    GRAFANA_PORT                = Integer.valueOf(3000);
    static final Integer    SSH_PORT                    = Integer.valueOf(22);

    static final String     VPC_CIDR                    = "10.1.0.0/16";
    static final Integer    MIN_GETH_INSTANCES          = Integer.valueOf(0);
    static final Integer    MAX_GETH_INSTANCES          = Integer.valueOf(1);

    static final IPeer      VPC_CIDR_PEER               = Peer.ipv4(VPC_CIDR);
    static final Duration   TARGET_DEREGISTRATION_DELAY = Duration.seconds(15);

    private ApplicationLoadBalancer privateLoadBalancer                        = null;
    private SecurityGroup       autoscalingGroupSecurityGroup         = null;
    private AutoScalingGroup    autoscalingGroup      = null;
    private List<IVolume>       volumes        = null;
    private EthereumBeaconChainProps ethBeaconChainProps = null;
    private Alarm alarmCpuLow = null;
    private Alarm alarmCpuHigh = null;

    public EthereumBeaconChainNode(final Construct scope, final String id) {
        this(scope, id, null, null);
    }

    public EthereumBeaconChainNode(final Construct scope, final String id, final EthereumBeaconChainProps goethProps, final StackProps props) {
        super(scope, id, props);

        this.ethBeaconChainProps = goethProps;

        // Configure a persistent volume for chain data
        createVolumes();

        // Autoscaling group for ETH backend
        createAutoscalingGroup();

        if (enableSessionManager()) {
            allowSessionManagerAccess();
        }

        // Allow the ASG instances to describe and attach to volumes
        allowVolumeAttachmentToAsg();
        
        // Configure a load balancer
        if (createLoadbalancer()) {
            createPrivateLoadBalancer();
        }

        if (createAlarms()) {
            createCloudWatchAlarms();
        }
    }

    protected void createCloudWatchAlarms() {
        this.alarmCpuLow = Alarm.Builder.create(this, "cpuLowAlarm")
            .alarmDescription("Fires when CPU utilization falls below the configured threshold.")
            .alarmName("beaconChainCpuLow")
            .metric(Metric.Builder.create()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .statistic(Statistic.AVERAGE.name())
                .period(Duration.minutes(5))
                .build()
            )
            .datapointsToAlarm(2)
            .evaluationPeriods(2)
            .comparisonOperator(ComparisonOperator.LESS_THAN_THRESHOLD)
            .threshold(Integer.valueOf(40))
            .build();
        this.alarmCpuHigh = Alarm.Builder.create(this, "cpuHigh")
            .alarmDescription("Fires when CPU utilization rises above the configured threshold.")
            .alarmName("beaconChainCpuHigh")
            .metric(Metric.Builder.create()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .statistic(Statistic.AVERAGE.name())
                .period(Duration.minutes(5))
                .build()
            )
            .datapointsToAlarm(2)
            .evaluationPeriods(2)
            .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
            .threshold(Integer.valueOf(90))
            .build();
    }

    private void allowSessionManagerAccess() {
        // Allow session manager connections
        // TODO does this need to be more restrictive?
        this.getAutoscalingGroup().getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ec2-instance-connect:SendSSHPublicKey"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                    String.format("arn:aws:ec2:%s:%s:instance/*", getRegion(), getAccount())))
                .build());
        this.getAutoscalingGroup().getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ssm:StartSession"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList(
                    String.format("arn:aws:ec2:%s:%s:instance/*", getRegion(), getAccount())))
                .build());
    }

    private void allowVolumeAttachmentToAsg() {
        if(this.autoscalingGroup != null) {
            this.autoscalingGroup.getGrantPrincipal()
            .addToPrincipalPolicy(PolicyStatement.Builder.create()
                .actions(Arrays.asList("ec2:DescribeVolumes"))
                .effect(Effect.ALLOW)
                .resources(Arrays.asList("*"))
                .build());
        }
        if(this.volumes != null) {
            for(IVolume vol : this.createVolumes()) {
                vol.grantAttachVolumeByResourceTag(
                    this.autoscalingGroup.getGrantPrincipal(), 
                    Arrays.asList(this.autoscalingGroup));
                vol.grantDetachVolumeByResourceTag(
                    this.autoscalingGroup.getGrantPrincipal(), 
                    Arrays.asList(this.autoscalingGroup));
            }
        }
    }

    protected List<String> getSinleAvailabilityZone(){
        return getAvailabilityZones().subList(0, 1);
    }

    protected List<IVolume> createVolumes() {
        if (this.volumes == null && this.ethBeaconChainProps.getAppVpc() != null) {
            
            this.volumes = new ArrayList<IVolume>();
            for(String az : getSinleAvailabilityZone()) {
                IVolume vol = Volume.Builder.create(this, "chaindataVolume"+az)
                .volumeName("chaindataVolume-"+az)
                .volumeType(software.amazon.awscdk.services.ec2.EbsDeviceVolumeType.GP2)
                .size(getVolumeSize())
                .encrypted(Boolean.TRUE)
                // .removalPolicy(RemovalPolicy.SNAPSHOT)
                .availabilityZone(az)
                .build();
                Tags.of(vol).add("Name", "goeth");
                this.volumes.add(vol);
            }
        }
        return this.volumes;
         
    }

    private Size getVolumeSize() {
        return Size.gibibytes(
            Integer.valueOf(
                (String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:volumeSize")));
    }

    protected ApplicationLoadBalancer createPrivateLoadBalancer() {
        this.privateLoadBalancer = ApplicationLoadBalancer.Builder.create(this, "beaconChainAlb")
            .vpc(this.ethBeaconChainProps.getAppVpc())
            .vpcSubnets(getAppVpcSubnets())
            .internetFacing(Boolean.FALSE)
            .build();
        
        if(this.ethBeaconChainProps.getPrivateHostedZone() != null) {
            ARecord.Builder.create(this, "goethPrivateARecord")
                .zone(this.ethBeaconChainProps.getPrivateHostedZone())
                .target(RecordTarget.fromAlias(new LoadBalancerTarget(this.privateLoadBalancer)))
                .recordName(EthereumBeaconChainNode.this.getRecordName())
                .build();
        }        
        addListenerAndTarget("prometheus", ApplicationProtocol.HTTP, Integer.valueOf(9090), null);
        return this.privateLoadBalancer;
    }

    private HealthCheck createHealthCheck(String path, Protocol protocol, String port) {
        return HealthCheck.builder()
            .enabled(Boolean.TRUE)
            .healthyHttpCodes("200-299")
            .healthyThresholdCount(2)
            .unhealthyThresholdCount(2)
            .interval(Duration.seconds(30))
            // .timeout(Duration.seconds(5)) // Not supported for NLB
            .path(path)
            .protocol(protocol)
            .port(port)
            .build();
    }

    private void addListenerAndTarget(String id, ApplicationProtocol protocol, Integer port, HealthCheck healthCheck) {
        AddApplicationTargetsProps.Builder targetPropsBuilder = AddApplicationTargetsProps.builder()
            .targets(Arrays.asList(this.getAutoscalingGroup()))
            .port(port)
            .protocol(protocol)
            .deregistrationDelay(TARGET_DEREGISTRATION_DELAY);
        if (healthCheck != null) {
            targetPropsBuilder.healthCheck(healthCheck);
        }
        this.privateLoadBalancer.addListener(id, 
            ApplicationListenerProps.builder()
                .port(port)
                .protocol(protocol)
                .loadBalancer(this.privateLoadBalancer)
                .build()
        ).addTargets(id, targetPropsBuilder.build());
        
        getAutoscalingGroupSecurityGroup().addIngressRule(
                Peer.ipv4(this.ethBeaconChainProps.getAppVpc().getVpcCidrBlock()), Port.tcp(port));
    }

    protected AutoScalingGroup getAutoscalingGroup() {
        return this.autoscalingGroup;
    }

    protected SecurityGroup getAutoscalingGroupSecurityGroup() {
        if (this.autoscalingGroupSecurityGroup == null && this.ethBeaconChainProps.getAppVpc() != null) {
            this.autoscalingGroupSecurityGroup = SecurityGroup.Builder.create(this, "backendAsgSecurityGroup")
                .vpc(this.ethBeaconChainProps.getAppVpc())
                .build();
        }
        return this.autoscalingGroupSecurityGroup;
    }

    protected AutoScalingGroup createAutoscalingGroup() {
        if (this.ethBeaconChainProps.getAppVpc() != null) {
            this.autoscalingGroup = AutoScalingGroup.Builder.create(this, "goeth")
                .vpc(this.ethBeaconChainProps.getAppVpc())
                .vpcSubnets(getAppVpcSubnets())
                .instanceType(getInstanceType())
                .machineImage(EthereumBeaconChainNode.this.getMachineImage())
                .keyName(getKeyPair())
                .initOptions(ApplyCloudFormationInitOptions.builder().printLog(Boolean.TRUE).build())
                .init(getCloudInit())
                .minCapacity(MIN_GETH_INSTANCES)
                .maxCapacity(MAX_GETH_INSTANCES)
                .allowAllOutbound(Boolean.TRUE)
                .securityGroup(this.getAutoscalingGroupSecurityGroup())
                .updatePolicy(UpdatePolicy.rollingUpdate(
                    RollingUpdateOptions.builder()
                        .minInstancesInService(MIN_GETH_INSTANCES)
                        .build()))
                .signals(Signals.waitForMinCapacity(
                        SignalsOptions.builder().timeout(
                            Duration.minutes(Integer.valueOf(5))).build()))
                .build();
                // Add CloudWatch policies
                this.autoscalingGroup.getRole().addManagedPolicy(
                    ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentServerPolicy"));
                this.autoscalingGroup.getRole().addManagedPolicy(
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonSSMManagedInstanceCore"));
                this.autoscalingGroup.getRole().addManagedPolicy(
                    ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ReadOnlyAccess"));
        }

        return this.autoscalingGroup;
    }

    private SubnetSelection getAppVpcSubnets() {
        return SubnetSelection.builder()
            .subnetType(SubnetType.PRIVATE)
            .availabilityZones(getSinleAvailabilityZone())
            .build();
    }

    protected CloudFormationInit getCloudInit() {
        return CloudFormationInit.fromElements(
            // Enable the volume services
            createServiceToggleInitCommand("chaindata-volume-attachment", enableService()),
            createServiceToggleInitCommand("var-lib-chaindata.mount", enableService()),
            createServiceToggleInitCommand("var-lib-chaindata-directory-creator.service", enableService()),
            createServiceToggleInitCommand("prometheus", getPrometheusServiceToggle()),
            // Set environment vars
            createServiceConfigurationInitCommand("geth", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            createServiceConfigurationInitCommand("lighthousebeacon", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            createServiceConfigurationInitCommand("lighthousevalidator", this.ethBeaconChainProps.getBeaconChainEnvironment()),
            // Customize env vars
            // createBeaconChainMonitoringInitCommand(),
            // Start services
            createServiceToggleInitCommand("geth", enableService()),
            createServiceToggleInitCommand("lighthousebeacon", getLighthouseBeaconServiceToggle()),
            createServiceToggleInitCommand("lighthousevalidator", getLighthouseValidatorServiceToggle()));
    }

    protected String enableService() {
        return "enable --now --all";
    }

    protected String disableService() {
        return "disable";
    }

    protected String getKeyPair() {
        return (String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:keyPair");
    }

    protected InstanceType getInstanceType() {
        return new InstanceType((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:instanceType"));
    }

    protected String beaconChainMonitoringEndpoint() {
        return (String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:beaconChainMonitoringEndpoint");
    }

    protected Boolean enableBeaconChainMonitoring() {
        String endpoint = beaconChainMonitoringEndpoint();
        return endpoint != null && endpoint.indexOf("https://") >= 0;
    }

    protected IMachineImage getMachineImage() {
        return MachineImage.lookup(
            LookupMachineImageProps.builder()
                .name((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:amiName")).build());
    }

    protected Boolean enableSessionManager() {
        return Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableSessionManager"));
    }

    protected String getLighthouseBeaconServiceToggle() {
        Boolean enableBeacon = Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableBeacon"));
        return getServiceToggle(enableBeacon);
    }

    protected String getPrometheusServiceToggle() {
        Boolean enablePrometheus = enablePrometheus();
        return getServiceToggle(enablePrometheus);
    }

    protected Boolean enablePrometheus() {
        return Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enablePrometheus"));
    }

    protected Boolean createLoadbalancer() {
        return Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:createLoadbalancer"));
    }

    protected String getLighthouseValidatorServiceToggle() {
        Boolean enableValidator = Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:enableValidator"));
        return getServiceToggle(enableValidator);
    }

    protected InitCommand createBeaconChainMonitoringInitCommand() {
        if (enableBeaconChainMonitoring()) {
            return InitCommand.shellCommand(
                String.format("echo 'Beaconchain monitoring enabled, adding LIGHTHOUSE_MONITORING_ENDPOINT_FLAG to service.env...' && echo \"LIGHTHOUSE_MONITORING_ENDPOINT_FLAG=\"--monitoring-endpoint '%s'\"\" | sudo tee -a /etc/systemd/system/lighthousebeacon.service.env > /dev/null", beaconChainMonitoringEndpoint())
                // To pipe to env file forrreal: | sudo tee -a /etc/systemd/system/lighthousebeacon.service.env > /dev/null
                , InitCommandOptions.builder().ignoreErrors(Boolean.TRUE).build()
            );
        } else {
            return InitCommand.shellCommand("echo 'Beaconchain monitoring is not enabled.'");
        }
    }

    /**
     * beaconchain.<region>.<testnet|mainnet>.<private-hosted-zone>
     * @return
     */
    protected String getRecordName() {
        return String.format("%s.%s", "goeth", 
            (String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:privateHostedZone"));
    }

    protected Boolean createAlarms() {
        return Boolean.valueOf((String) super.getNode().tryGetContext("everythingbiig/ethereum-beacon-chain-aws:createAlarms"));
    }

    protected String getServiceToggle(Boolean enabled) {
        return enabled ? enableService() : disableService();
    }

    protected @NotNull InitElement createServiceConfigurationInitCommand(String serviceName, String beaconChainEnvironment) {
        return InitCommand.shellCommand(
            MessageFormat.format("sudo ln -s /etc/systemd/system/{0}/{0}.service.{1}.env /etc/systemd/system/{0}.service.env", serviceName, beaconChainEnvironment),
            InitCommandOptions.builder().ignoreErrors(Boolean.TRUE).build()
        );
    }

    private InitCommand createServiceToggleInitCommand(String serviceName, String command) {
        return InitCommand.shellCommand(
            String.format("sudo systemctl %s %s", command, serviceName),
            InitCommandOptions.builder().ignoreErrors(Boolean.TRUE).build()
        );
    }
}
