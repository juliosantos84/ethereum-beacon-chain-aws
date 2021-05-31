#! /bin/bash

aws autoscaling set-desired-capacity --auto-scaling-group-name ${ASG_NAME} --desired-capacity ${ASG_DESIRED_CAPACITY}