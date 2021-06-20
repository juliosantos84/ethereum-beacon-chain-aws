# kubernetes

# cluster creation

```bash
kubectl cluster create development
```

# deploying

```bash
kustomize build kubernetes/base | kubectl apply -f -
```

# port-forwarding

```bash
kubectl port-forward --address 0.0.0.0 service/traefik 80:80 443:443 -n kube-system
```