# kubernetes
Ignore this for now.  Kubernetes turned out to be more work than it was worth for a single validator deployment.  May revisit later.

# local dev
## install and run k3s

```bash
curl -sfL https://get.k3s.io | sh -
```

# deploying

```bash
kustomize build kubernetes/development | kubectl apply -f -
```

# destroying

```bash
kustomize build kubernetes/development | kubectl delete -f -
```

# port-forwarding

```bash
kubectl port-forward --address 0.0.0.0 service/traefik 80:80 443:443 8445:8445 -n kube-system
```